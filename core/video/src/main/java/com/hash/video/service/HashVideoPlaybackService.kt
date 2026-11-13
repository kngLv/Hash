package com.hash.video.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.os.BundleCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.hash.video.R
import com.hash.video.player.VideoPlayerEngine

/**
 * 系统媒体会话服务。
 *
 * 播放器由 [VideoPlayerEngine] 在应用进程内持有，服务只在需要通知时为它创建
 * [MediaSession]。因此关闭通知不会清空媒体、释放播放器或移除页面画面。
 */
@OptIn(UnstableApi::class)
class HashVideoPlaybackService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession
    private var sessionReleased = false

    private val sessionCallback = object : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val resultBuilder = MediaSession.ConnectionResult.AcceptedResultBuilder(session, controller)
            val defaultCommands = resultBuilder.build().availableSessionCommands
            resultBuilder.setAvailableSessionCommands(
                defaultCommands.buildUpon()
                    .add(VideoSessionCommands.dismissNotification)
                    .build(),
            )
            return resultBuilder.build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (customCommand != VideoSessionCommands.dismissNotification) {
                return Futures.immediateFuture(SessionResult(SessionError.ERROR_BAD_VALUE))
            }

            // 保留媒体项、进度与渲染画面，仅结束能产生通知的媒体会话。
            session.player.pause()
            releaseSessionAndStop()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        mediaSession = MediaSession.Builder(this, VideoPlayerEngine.get(application))
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(notificationMediaButtonPreferences())
            .build()
        Log.d(LOG_TAG, "MediaSession 已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_SET_NOTIFICATION_ACTIVITY) {
            val pendingIntent = intent.extras?.let { extras ->
                BundleCompat.getParcelable(
                    extras,
                    EXTRA_NOTIFICATION_ACTIVITY,
                    PendingIntent::class.java,
                )
            }
            mediaSession.setSessionActivity(pendingIntent)
        }
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession.takeUnless { sessionReleased }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!mediaSession.player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        releaseSession()
        super.onDestroy()
    }

    private fun releaseSessionAndStop() {
        if (sessionReleased) return
        mediaSession.setSessionActivity(null)
        releaseSession()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(LOG_TAG, "通知关闭：已暂停播放器并释放 MediaSession")
    }

    private fun releaseSession() {
        if (sessionReleased) return
        sessionReleased = true
        mediaSession.release()
    }

    /** 通知完整视图依序展示后退 15 秒、播放暂停、前进 15 秒和关闭。 */
    private fun notificationMediaButtonPreferences(): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setDisplayName(getString(R.string.video_skip_back_15_seconds))
            .build(),
        CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_15)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setDisplayName(getString(R.string.video_skip_forward_15_seconds))
            .build(),
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setCustomIconResId(R.drawable.ic_video_notification_close)
            .setSessionCommand(VideoSessionCommands.dismissNotification)
            .setDisplayName(getString(R.string.video_close_notification))
            .build(),
    )

    companion object {
        private const val LOG_TAG = "HashVideoLifecycle"
        private const val ACTION_ENSURE_SESSION = "com.hash.video.action.ENSURE_SESSION"
        private const val ACTION_SET_NOTIFICATION_ACTIVITY =
            "com.hash.video.action.SET_NOTIFICATION_ACTIVITY"
        private const val EXTRA_NOTIFICATION_ACTIVITY = "notification_activity"

        /** 前台页面播放前建立会话；空闲播放器不会显示通知。 */
        fun ensureSession(context: Context) {
            context.startService(
                Intent(context, HashVideoPlaybackService::class.java).setAction(ACTION_ENSURE_SESSION),
            )
        }

        /** 更新通知点击目标；目标仅保存在服务的 MediaSession 中，不会引用 Activity。 */
        fun setNotificationActivity(context: Context, pendingIntent: PendingIntent?) {
            context.startService(
                Intent(context, HashVideoPlaybackService::class.java)
                    .setAction(ACTION_SET_NOTIFICATION_ACTIVITY)
                    .putExtra(EXTRA_NOTIFICATION_ACTIVITY, pendingIntent),
            )
        }

        /** 页面真正退出时销毁会话；播放器释放仍由 VideoController 负责。 */
        fun stopSession(context: Context) {
            context.stopService(Intent(context, HashVideoPlaybackService::class.java))
        }
    }
}
