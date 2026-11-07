package com.hash.video.ui.gesture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoGestureConfigTest {

    @Test
    fun `默认不启用任何可选手势`() {
        val config = VideoGestureConfig()

        assertFalse(config.singleTapEnabled)
        assertFalse(config.doubleTapEnabled)
        assertFalse(config.seekEnabled)
        assertFalse(config.brightnessEnabled)
        assertFalse(config.volumeEnabled)
    }

    @Test
    fun `每项手势可被独立启用`() {
        val config = VideoGestureConfig(
            singleTapEnabled = true,
            seekEnabled = true,
            volumeEnabled = true,
        )

        assertTrue(config.singleTapEnabled)
        assertFalse(config.doubleTapEnabled)
        assertTrue(config.seekEnabled)
        assertFalse(config.brightnessEnabled)
        assertTrue(config.volumeEnabled)
    }
}
