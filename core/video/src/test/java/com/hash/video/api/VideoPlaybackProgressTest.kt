package com.hash.video.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackProgressTest {

    @Test
    fun `播放进度应限制在有效范围内`() {
        val progress = VideoPlaybackProgress(
            positionMs = 1_200L,
            bufferedPositionMs = 2_000L,
            durationMs = 1_000L,
        )

        assertEquals(1f, progress.fraction, 0f)
        assertEquals(1f, progress.bufferedFraction, 0f)
        assertTrue(progress.isAvailable)
        assertFalse(VideoPlaybackProgress().isAvailable)
    }

    @Test
    fun `未播完的视频应从缓存位置继续播放`() {
        val progress = VideoPlaybackProgress(
            positionMs = 600L,
            bufferedPositionMs = 800L,
            durationMs = 1_000L,
        )

        assertEquals(progress, progress.forResume())
        assertEquals(600L, progress.forResume().resumePositionMs())
    }

    @Test
    fun `已播完的视频再次选择时应从头播放`() {
        val progress = VideoPlaybackProgress(
            positionMs = 1_000L,
            bufferedPositionMs = 1_000L,
            durationMs = 1_000L,
        )

        assertEquals(0L, progress.forResume().positionMs)
        assertEquals(0L, progress.forResume().bufferedPositionMs)
    }

    @Test
    fun `时长未知但已完成的视频再次选择时应从头播放`() {
        val progress = VideoPlaybackProgress(
            positionMs = 600L,
            bufferedPositionMs = 600L,
            completed = true,
        )

        assertEquals(0L, progress.forResume().positionMs)
        assertEquals(0L, progress.forResume().bufferedPositionMs)
        assertFalse(progress.forResume().completed)
    }
}
