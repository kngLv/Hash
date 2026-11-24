package com.hash.video.service

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.player.VideoPlayerEngine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoSessionReleaseCommandTest {

    @Test
    fun `后台单媒体通知不展示无意义的跳转或队列命令`() {
        val controller = createController()
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                assertEquals(15_000L, controller.seekBackIncrement)
                assertEquals(15_000L, controller.seekForwardIncrement)
                assertTrue(controller.availableSessionCommands.contains(VideoSessionCommands.dismissNotification))
                assertFalse(controller.mediaButtonPreferences.any {
                    it.playerCommand == Player.COMMAND_SEEK_BACK
                })
                assertFalse(controller.mediaButtonPreferences.any {
                    it.playerCommand == Player.COMMAND_SEEK_FORWARD
                })
                assertTrue(controller.mediaButtonPreferences.any {
                    it.sessionCommand == VideoSessionCommands.dismissNotification &&
                        it.iconResId == R.drawable.ic_video_notification_close
                })
            }
        } finally {
            releaseController(controller)
        }
    }

    @Test
    fun `通知关闭会暂停但保留播放器媒体项`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val player = AtomicReference<Player>()
        instrumentation.runOnMainSync {
            player.set(VideoPlayerEngine.get(context.applicationContext as Application))
            player.get().setMediaItem(
                MediaItem.Builder()
                    .setMediaId("notification-dismiss-test")
                    .setUri("https://example.com/video.mp4")
                    .build(),
            )
            player.get().playWhenReady = true
        }

        val controller = createController()
        try {
            val resultFuture = AtomicReference<ListenableFuture<SessionResult>>()
            instrumentation.runOnMainSync {
                resultFuture.set(
                    controller.sendCustomCommand(
                        VideoSessionCommands.dismissNotification,
                        Bundle.EMPTY,
                    ),
                )
            }
            val result = resultFuture.get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(0, result.resultCode)
                assertEquals("notification-dismiss-test", player.get().currentMediaItem?.mediaId)
                assertFalse(player.get().playWhenReady)
            }
        } finally {
            releaseController(controller)
        }
    }

    private fun createController(): MediaController {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        HashVideoPlaybackService.ensureSession(context)
        val sessionToken = SessionToken(
            context,
            ComponentName(context, HashVideoPlaybackService::class.java),
        )
        val controllerFuture = AtomicReference<ListenableFuture<MediaController>>()
        instrumentation.runOnMainSync {
            controllerFuture.set(MediaController.Builder(context, sessionToken).buildAsync())
        }
        return controllerFuture.get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    private fun releaseController(controller: MediaController) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync(controller::release)
        HashVideoPlaybackService.stopSession(instrumentation.targetContext)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
