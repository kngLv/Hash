package com.hash.video.api

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaylistTest {

    private val first = VideoMedia("first", "https://example.com/first.mp4")
    private val second = VideoMedia("second", "https://example.com/second.mp4")
    private val third = VideoMedia("third", "https://example.com/third.mp4")

    @Test
    fun `队列保留媒体的传入顺序`() {
        val playlist = VideoPlaylist(listOf(first, second, third))

        assertEquals(listOf(first, second, third), playlist.items)
    }

    @Test
    fun `队列复制输入列表避免业务层后续修改`() {
        val source = mutableListOf(first, second)
        val playlist = VideoPlaylist(source)

        source.clear()

        assertEquals(listOf(first, second), playlist.items)
    }

    @Test
    fun `按播放源定位媒体索引`() {
        val playlist = VideoPlaylist(listOf(first, second))

        assertEquals(1, playlist.indexOf(second.copy(title = "展示标题更新")))
    }
}
