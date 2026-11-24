package com.hash.video.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoNotificationControlsTest {

    @Test
    fun `前台使用十五秒跳转策略`() {
        assertFalse(VideoNotificationControls(isAppInForeground = true).usesPlaylistNavigation)
    }

    @Test
    fun `后台使用队列导航策略`() {
        assertTrue(
            VideoNotificationControls(
                isAppInForeground = false,
                hasPrevious = true,
                hasNext = true,
            ).usesPlaylistNavigation,
        )
    }
}
