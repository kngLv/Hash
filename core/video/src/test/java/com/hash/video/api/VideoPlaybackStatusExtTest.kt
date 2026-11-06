package com.hash.video.api

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackStatusExtTest {

    @Test
    fun `准备和缓冲状态属于加载中`() {
        assertTrue(VideoPlaybackStatus.PREPARING.isLoading)
        assertTrue(VideoPlaybackStatus.BUFFERING.isLoading)
        assertFalse(VideoPlaybackStatus.PLAYING.isLoading)
    }

    @Test
    fun `播放暂停和结束状态拥有明确判断`() {
        assertTrue(VideoPlaybackStatus.PLAYING.isPlaying)
        assertTrue(VideoPlaybackStatus.PAUSED.isPaused)
        assertTrue(VideoPlaybackStatus.ENDED.isFinished)
        assertFalse(VideoPlaybackStatus.ERROR.isFinished)
    }
}
