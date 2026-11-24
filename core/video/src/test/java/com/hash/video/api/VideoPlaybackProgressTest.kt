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
}
