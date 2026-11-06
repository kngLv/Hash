package com.hash.video.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.hash.video.api.VideoMedia
import com.hash.video.cache.VideoCacheKeyFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoMediaItemFactoryTest {

    @Test
    fun `媒体项保留标识地址标题封面并写入缓存键`() {
        val media = VideoMedia(
            id = "news-1",
            url = "https://example.com/news-1.mp4",
            title = "新闻视频",
            coverUrl = "https://example.com/cover.jpg",
            headers = mapOf("Authorization" to "token"),
        )

        val mediaItem = VideoMediaItemFactory.create(media)

        assertEquals(media.id, mediaItem.mediaId)
        assertEquals(media.url, mediaItem.localConfiguration?.uri.toString())
        assertEquals(media.title, mediaItem.mediaMetadata.title)
        assertEquals(media.coverUrl, mediaItem.mediaMetadata.artworkUri.toString())
        assertEquals(VideoCacheKeyFactory.create(media), mediaItem.localConfiguration?.customCacheKey)
    }
}
