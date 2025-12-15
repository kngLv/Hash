package com.hash.net.net.interceptor

import android.util.Log
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import java.io.EOFException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

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
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBody = request.body
        val requestBuffer = StringBuffer()

        val contentType = requestBody?.contentType()
        val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8

        // 请求日志
        requestBuffer.apply {
            append("{url:${request.url}} \n")
            append("{method:${request.method} \n")
            if (requestBody != null && !bodyHasUnknownEncoding(request.headers)
                && !requestBody.isDuplex() && !requestBody.isOneShot()
            ) {
                val buffer = Buffer()
                requestBody.writeTo(buffer)
                if (buffer.isProbablyUtf8()) {
                    append("{arguments:{${buffer.readString(charset)}\n")
                }
            }
        }

        val response = chain.proceed(request)

        try {
            val responseBody = response.body ?: return response
            val mediaType = responseBody.contentType()
            val isTextType = mediaType?.type == "text" || mediaType?.subtype?.contains("json", true) == true
            val contentLength = responseBody.contentLength()

            requestBuffer.apply {
                append("\n{Code:${response.code}\n")
                append("{URL：${response.request.url}\n")
                append("{Content-Type：$mediaType  Content-Length：${contentLength}\n")
                response.headers.forEach {
                    append("{Header: ${it.first}=${it.second}}\n")
                }
            }

            // 对于大文件/非文本内容，不再尝试读取 body，避免阻塞下载
            if (!isTextType || contentLength > 1024 * 1024) { // >1MB 视为大内容，打印占位
                requestBuffer.append("<-- END HTTP (body omitted for non-text or large content)\n")
                Log.d("LvHttp ---- END HTTP>", requestBuffer.toString())
                return response
            }

            // 文本且体积可控的响应，尝试读取少量预览
            val source = responseBody.source()
            val peekBytes = 1024L // 预览最多 1KB
            source.request(peekBytes) // 请求最多 1KB 到缓冲区用于预览
            val bufferResponse = source.buffer

            val previewBuffer = Buffer()
            val byteCount = bufferResponse.size.coerceAtMost(peekBytes)
            bufferResponse.copyTo(previewBuffer, 0, byteCount)

            // 使用响应的 charset 进行解码（fallback 到 UTF-8）
            val responseCharset: Charset = run {
                val mt = responseBody.contentType()
                if (mt == null) StandardCharsets.UTF_8 else mt.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            }

            requestBuffer.apply {
                if (!previewBuffer.isProbablyUtf8()) {
                    append("<-- END HTTP (binary body preview omitted)\n")
                } else {
                    append("body-preview：${prettyJson(previewBuffer.readString(responseCharset))}\n")
                }
            }

            Log.d("LvHttp ---- END HTTP>", requestBuffer.toString())
        } catch (e: Exception) {
            e.printStackTrace()
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