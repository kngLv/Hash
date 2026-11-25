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

    @Test
    fun `后台首项仅显示下一条`() {
        val controls = VideoNotificationControls(isAppInForeground = false, hasNext = true)

        assertFalse(controls.shouldShowPrevious)
        assertTrue(controls.shouldShowNext)
    }

    @Test
    fun `后台末项仅显示上一条`() {
        val controls = VideoNotificationControls(isAppInForeground = false, hasPrevious = true)

        assertTrue(controls.shouldShowPrevious)
        assertFalse(controls.shouldShowNext)
    }

    @Test
    fun `前台即使存在相邻项也不显示队列按钮`() {
        val controls = VideoNotificationControls(
            isAppInForeground = true,
            hasPrevious = true,
            hasNext = true,
        )

        assertFalse(controls.shouldShowPrevious)
        assertFalse(controls.shouldShowNext)
    }
}
