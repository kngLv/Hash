package com.hash.video.service

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.app.PendingIntent
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.ui.player.VideoPlayerViewTestActivity
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoSessionReleaseCommandTest {

    @Test
    fun `内部控制器可以设置并清空通知点击目标`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sessionToken = SessionToken(
            context,
            ComponentName(context, HashVideoPlaybackService::class.java),
        )
        val controller = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val notificationCommand = VideoSessionCommands.setNotificationActivity
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, VideoPlayerViewTestActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            val setResult = controller.sendCustomCommand(
                notificationCommand,
                Bundle().apply {
                    putParcelable(VideoSessionCommands.ARGUMENT_NOTIFICATION_ACTIVITY, pendingIntent)
                },
            ).get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            instrumentation.waitForIdleSync()
            assertEquals(SessionResult.RESULT_SUCCESS, setResult.resultCode)
            assertEquals(pendingIntent, controller.sessionActivity)

            val clearResult = controller.sendCustomCommand(notificationCommand, Bundle.EMPTY)
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)

            instrumentation.waitForIdleSync()
            assertEquals(SessionResult.RESULT_SUCCESS, clearResult.resultCode)
            assertEquals(null, controller.sessionActivity)
        } finally {
            instrumentation.runOnMainSync(controller::release)
        }
    }

    @Test
    fun `内部控制器可以请求服务释放播放会话`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val sessionToken = SessionToken(
            context,
            ComponentName(context, HashVideoPlaybackService::class.java),
        )
        val controller = MediaController.Builder(context, sessionToken)
            .buildAsync()
            .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        val releaseCommand = SessionCommand(RELEASE_COMMAND_ACTION, Bundle.EMPTY)

        try {
            instrumentation.runOnMainSync {
                assertTrue(controller.availableSessionCommands.contains(releaseCommand))
            }

            val resultFuture = AtomicReference<ListenableFuture<SessionResult>>()
            instrumentation.runOnMainSync {
                resultFuture.set(controller.sendCustomCommand(releaseCommand, Bundle.EMPTY))
            }

            val result = resultFuture.get().get(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            assertEquals(SessionResult.RESULT_SUCCESS, result.resultCode)
        } finally {
            instrumentation.runOnMainSync(controller::release)
        }
    }

    private companion object {
        const val RELEASE_COMMAND_ACTION = "com.hash.video.RELEASE_PLAYBACK_SESSION"
        const val TIMEOUT_SECONDS = 5L
    }
}
