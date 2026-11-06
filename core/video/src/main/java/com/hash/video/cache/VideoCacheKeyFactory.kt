package com.hash.video.cache

import com.hash.video.api.VideoMedia
import java.security.MessageDigest

/** 为媒体缓存生成稳定且不会泄露鉴权信息的键。 */
object VideoCacheKeyFactory {

    fun create(media: VideoMedia): String {
        val rawKey = buildString {
            append(media.id)
            append('|')
            append(media.url)
            media.headers.toSortedMap().forEach { (name, value) ->
                append('|')
                append(name)
                append('=')
                append(value)
            }
        }
        return MessageDigest.getInstance("SHA-256")
            .digest(rawKey.encodeToByteArray())
            .joinToString(separator = "") { byte -> "%02x".format(byte) }
    }
}
