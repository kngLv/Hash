package com.hash.video.cache

import com.hash.video.api.VideoMedia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class VideoCacheKeyFactoryTest {

    @Test
    fun `相同媒体与不同请求头顺序生成相同缓存键`() {
        val first = VideoMedia(
            id = "news-1",
            url = "https://example.com/news-1.mp4",
            headers = linkedMapOf("Authorization" to "token", "Referer" to "hash"),
        )
        val second = first.copy(
            headers = linkedMapOf("Referer" to "hash", "Authorization" to "token"),
        )

        assertEquals(
            VideoCacheKeyFactory.create(first),
            VideoCacheKeyFactory.create(second),
        )
    }

    @Test
    fun `媒体地址或影响内容的请求头变化时生成不同缓存键`() {
        val media = VideoMedia(
            id = "news-1",
            url = "https://example.com/news-1.mp4",
            headers = mapOf("Authorization" to "token-a"),
        )

        assertNotEquals(
            VideoCacheKeyFactory.create(media),
            VideoCacheKeyFactory.create(media.copy(url = "https://example.com/news-2.mp4")),
        )
        assertNotEquals(
            VideoCacheKeyFactory.create(media),
            VideoCacheKeyFactory.create(media.copy(headers = mapOf("Authorization" to "token-b"))),
        )
    }
}
