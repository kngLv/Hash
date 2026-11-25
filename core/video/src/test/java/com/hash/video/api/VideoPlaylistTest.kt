package com.hash.video.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun `空队列没有可定位的媒体`() {
        val playlist = VideoPlaylist(emptyList())

        assertTrue(playlist.items.isEmpty())
        assertEquals(-1, playlist.indexOf(first))
    }

    @Test
    fun `重复媒体定位首次出现的位置`() {
        val playlist = VideoPlaylist(listOf(first, second, first.copy(title = "重复展示")))

        assertEquals(0, playlist.indexOf(first))
    }
}
