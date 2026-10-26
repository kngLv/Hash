package com.hash.net.interceptor

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
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/** 输出完整请求与文本响应内容的日志拦截器。 */
class LogInterceptor : Interceptor {

    companion object {
        private const val TAG = "LvHttp"
        private const val MAX_LOGGABLE_BODY_SIZE = 1024 * 1024L
        private const val MAX_RESPONSE_PREVIEW_SIZE = 1024 * 100L
        private val COUNTER = AtomicLong(0)
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestId = COUNTER.incrementAndGet()
        logRequest(request, requestId)

        val response = chain.proceed(request)
        logResponse(response, requestId)
        return response
    }

    private fun logRequest(request: okhttp3.Request, requestId: Long) {
        try {
            val requestBody = request.body
            val contentType = requestBody?.contentType()
            val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
            val requestBuffer = StringBuilder().apply {
                append("--> HTTP id=$requestId START\n")
                append("{url:${request.url}} \n")
                append("{method:${request.method}} \n")
                request.headers.forEach { (name, value) -> append("{Header: $name=$value}\n") }
                if (requestBody != null && !bodyHasUnknownEncoding(request.headers) &&
                    !requestBody.isDuplex() && !requestBody.isOneShot()
                ) {
                    val buffer = Buffer()
                    try {
                        requestBody.writeTo(buffer)
                        if (buffer.isProbablyUtf8()) {
                            append("{arguments:{${buffer.readString(charset)}}}\n")
                        } else {
                            append("{arguments:(binary body omitted)}\n")
                        }
                    } catch (error: Exception) {
                        append("{arguments:(failed to read request body: ${error.message})}\n")
                    }
                }
            }
            Log.d(TAG, requestBuffer.toString())
        } catch (error: Exception) {
            Log.e(TAG, "Failed to log request id=$requestId: ${error.message}", error)
        }
    }

    private fun logResponse(response: Response, requestId: Long) {
        try {
            val responseBody = response.body
            if (responseBody == null) {
                Log.d(TAG, String.format(Locale.getDefault(), "<-- HTTP id=%d END (no response body) code=%d url=%s", requestId, response.code, response.request.url))
                return
            }

            val mediaType = responseBody.contentType()
            val contentLength = responseBody.contentLength()
            val isTextType = mediaType?.type == "text" || mediaType?.subtype?.contains("json", true) == true
            val responseBuffer = StringBuilder().apply {
                append("<-- HTTP id=$requestId RESPONSE START\n")
                append("{Code:${response.code}}\n")
                append("{URL:${response.request.url}}\n")
                append("{Content-Type:$mediaType  Content-Length:$contentLength}\n")
                response.headers.forEach { (name, value) -> append("{Header: $name=$value}\n") }
            }

            if (!isTextType || contentLength > MAX_LOGGABLE_BODY_SIZE) {
                responseBuffer.append("<-- END HTTP (body omitted for non-text or large content)\n")
                Log.d(TAG, responseBuffer.toString())
                return
            }

            val preview = response.peekBody(MAX_RESPONSE_PREVIEW_SIZE).string()
            if (preview.isBlank()) {
                responseBuffer.append("body-preview: (empty)\n")
            } else if (preview.isProbablyUtf8()) {
                responseBuffer.append("body-preview: ${prettyJson(preview)}\n")
            } else {
                responseBuffer.append("<-- END HTTP (binary body preview omitted)\n")
            }
            responseBuffer.append("<-- HTTP id=$requestId END\n")
            Log.d(TAG, responseBuffer.toString())
        } catch (error: Exception) {
            Log.e(TAG, "Failed to log response id=$requestId: ${error.message}", error)
        }
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
            !contentEncoding.equals("gzip", ignoreCase = true)
    }

    private fun prettyJson(content: String): String = try {
        GsonBuilder().setPrettyPrinting().create().toJson(JsonParser.parseString(content))
    } catch (_: JsonSyntaxException) {
        content
    }

    private fun String.isProbablyUtf8(): Boolean {
        val inspected = take(64)
        return inspected.count { it.isISOControl() && !it.isWhitespace() } * 100 /
            inspected.length.coerceAtLeast(1) < 10
    }

    private fun Buffer.isProbablyUtf8(): Boolean = try {
        val prefix = Buffer()
        copyTo(prefix, 0, size.coerceAtMost(64))
        repeat(16) {
            if (prefix.exhausted()) return true
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
        }
        true
    } catch (_: EOFException) {
        false
    }
}
