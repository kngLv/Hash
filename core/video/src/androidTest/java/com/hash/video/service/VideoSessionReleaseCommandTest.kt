package com.hash.video.service

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.player.VideoPlayerEngine
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoSessionReleaseCommandTest {

    @Test
    fun `通知提供十五秒跳转与关闭命令`() {
        withController { controller ->
            assertEquals(15_000L, controller.seekBackIncrement)
            assertEquals(15_000L, controller.seekForwardIncrement)
            assertTrue(controller.availableSessionCommands.contains(VideoSessionCommands.dismissNotification))
            assertTrue(controller.mediaButtonPreferences.any {
                it.playerCommand == Player.COMMAND_SEEK_BACK
            })
            assertTrue(controller.mediaButtonPreferences.any {
                it.playerCommand == Player.COMMAND_SEEK_FORWARD
            })
            assertTrue(controller.mediaButtonPreferences.any {
                it.sessionCommand == VideoSessionCommands.dismissNotification &&
                    it.iconResId == R.drawable.ic_video_notification_close
            })
        }
    }

    @Test
    fun `通知关闭会暂停但保留播放器媒体项`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val player = VideoPlayerEngine.get(context.applicationContext as Application)
        instrumentation.runOnMainSync {
            player.setMediaItem(
                MediaItem.Builder()
                    .setMediaId("notification-dismiss-test")
                    .setUri("https://example.com/video.mp4")
                    .build(),
            )
            player.playWhenReady = true
        }

        withController { controller ->
            val result = controller.sendCustomCommand(
                VideoSessionCommands.dismissNotification,
                Bundle.EMPTY,
            ).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            instrumentation.waitForIdleSync()
            assertEquals(0, result.resultCode)
            assertEquals("notification-dismiss-test", player.currentMediaItem?.mediaId)
            assertFalse(player.playWhenReady)
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        HashVideoPlaybackService.ensureSession(context)
        val sessionToken = SessionToken(
            context,
            ComponentName(context, HashVideoPlaybackService::class.java),
        )
        val controller = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        try {
            block(controller)
        } finally {
            instrumentation.runOnMainSync(controller::release)
            HashVideoPlaybackService.stopSession(context)
        }
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
