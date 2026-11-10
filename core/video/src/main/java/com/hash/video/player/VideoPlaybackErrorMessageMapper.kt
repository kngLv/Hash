package com.hash.video.player

import androidx.media3.datasource.HttpDataSource
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** 将 Media3 的底层播放异常转换为可直接展示给用户的错误文案。 */
internal object VideoPlaybackErrorMessageMapper {

    fun from(error: Throwable): String {
        val causes = generateSequence(error) { it.cause }.toList()
        causes.filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull()
            ?.let { return fromHttpResponseCode(it.responseCode) }

        return when {
            causes.any { it is UnknownHostException } -> "网络连接不可用，请检查网络后重试"
            causes.any { it is SocketTimeoutException } -> "视频加载超时，请稍后重试"
            else -> "视频播放失败，请稍后重试"
        }
    }

    fun fromHttpResponseCode(responseCode: Int): String = when (responseCode) {
        401 -> "视频鉴权已失效，请重新登录"
        403 -> "视频地址无访问权限或已失效"
        404 -> "视频资源不存在或已被删除"
        else -> "视频资源请求失败（HTTP $responseCode）"
    }
}
