package com.hash.net.net.interceptor

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import android.util.Log
import java.io.EOFException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * @name LogInterceptor
 * @description 日志拦截器
 *
 * 注意：为避免影响下载等大体积响应的读取，拦截器不再主动 buffer 整个响应体，只在
 *  - 内容类型为文本（如 application/json、text/..）且内容较小的情况下，尝试读取部分内容用于日志；
 *  - 对于二进制或大文件下载，仅打印基础信息（code、url、content-type、content-length），不读取 body，
 *    防止阻塞实际的响应流消费导致进度“卡住，最后一下子完成”的现象。
 */

class LogInterceptor : Interceptor {

    companion object {
        private const val TAG = "LvHttp"
        private val COUNTER = AtomicLong(0)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = request.body
        val requestBuffer = StringBuffer()

        val contentType = requestBody?.contentType()
        val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

        // generate request id for correlation
        val requestId = COUNTER.incrementAndGet()

        // 请求日志（在发起请求时打印）
        requestBuffer.apply {
            append("--> HTTP id=$requestId START\n")
            append("{url:${request.url}} \n")
            append("{method:${request.method}} \n")
            if (requestBody != null && !bodyHasUnknownEncoding(request.headers)
                && !requestBody.isDuplex() && !requestBody.isOneShot()
            ) {
                val buffer = Buffer()
                try {
                    requestBody.writeTo(buffer)
                    if (buffer.isProbablyUtf8()) {
                        append("{arguments:{${buffer.readString(charset)}}}\n")
                    } else {
                        append("{arguments:(binary body omitted)}\n")
                    }
                } catch (e: Exception) {
                    append("{arguments:(failed to read request body: ${e.message})}\n")
                }
            }
        }

        // log request now so that start and end can be correlated by id
        Log.d(TAG, requestBuffer.toString())

        val response = chain.proceed(request)

        try {
            val responseBody = response.body
            if (responseBody == null) {
                Log.d(TAG, String.format(Locale.getDefault(), "<-- HTTP id=%d END (no response body) code=%d url=%s", requestId, response.code, response.request.url))
                return response
            }

            val mediaType = responseBody.contentType()
            val isTextType = mediaType?.type == "text" || mediaType?.subtype?.contains("json", true) == true
            val contentLength = responseBody.contentLength()

            val responseBuffer = StringBuffer()
            responseBuffer.apply {
                append("<-- HTTP id=$requestId RESPONSE START\n")
                append("{Code:${response.code}}\n")
                append("{URL:${response.request.url}}\n")
                append("{Content-Type:$mediaType  Content-Length:${contentLength}}\n")
                response.headers.forEach {
                    append("{Header: ${it.first}=${it.second}}\n")
                }
            }

            // 对于大文件/非文本内容，不再尝试读取 body，避免阻塞下载
            if (!isTextType || contentLength > 1024 * 1024) { // >1MB 视为大内容，打印占位
                responseBuffer.append("<-- END HTTP (body omitted for non-text or large content)\n")
                Log.d(TAG, responseBuffer.toString())
                return response
            }

            // 使用 peekBody 安全地获取响应的前一部分内容用于日志，peek 不会消耗真正的流
            val peekBytes = 1024L // 预览最多 1KB
            val peekBody = try {
                response.peekBody(peekBytes)
            } catch (e: Exception) {
                Log.w(TAG, String.format(Locale.getDefault(), "failed to peek body for id=%d", requestId), e)
                null
            }

            if (peekBody == null) {
                responseBuffer.append("<-- END HTTP (failed to peek body)\n")
                Log.d(TAG, responseBuffer.toString())
                return response
            }

            val preview = peekBody.string()
            responseBuffer.apply {
                if (preview.isBlank()) {
                    append("body-preview: (empty)\n")
                } else {
                    if (isProbablyUtf8(preview)) {
                        append("body-preview: ${prettyJson(preview)}\n")
                    } else {
                        append("<-- END HTTP (binary body preview omitted)\n")
                    }
                }
                append("<-- HTTP id=$requestId END\n")
            }

            Log.d(TAG, responseBuffer.toString())
        } catch (e: Exception) {
            // 保证任何异常不会影响正常返回
            Log.e(TAG, String.format(Locale.getDefault(), "LogInterceptor failed for id=%d: %s", requestId, e.message), e)
        }

        return response
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    // 扩展：将字符串尝试格式化为漂亮的 JSON
    private fun prettyJson(content: String): String {
        if (content.isBlank()) return content
        return try {
            val jsonElement = JsonParser.parseString(content)
            GsonBuilder().setPrettyPrinting().create().toJson(jsonElement)
        } catch (_: JsonSyntaxException) {
            content // 非 JSON 时返回原始字符串
        }
    }

    // helper: 判断字符串是否很可能是 utf8 可读文本（针对 peek 的字符串）
    private fun isProbablyUtf8(s: String): Boolean {
        // 简单策略：如果包含大量不可见控制字符则认为不是文本
        var control = 0
        var total = 0
        for (ch in s) {
            total++
            if (ch.isISOControl() && !ch.isWhitespace()) control++
            if (total >= 64) break
        }
        return control * 100 / (if (total == 0) 1 else total) < 10
    }

    private fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(64)
            copyTo(prefix, 0, byteCount)
            repeat(16) {
                if (prefix.exhausted()) {
                    return true
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (_: EOFException) {
            return false // Truncated UTF-8 sequence.
        }
    }
}