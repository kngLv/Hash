package com.hash.video.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.OptIn
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
import com.hash.video.player.VideoNotificationControls
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val notificationControlScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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
            // 先回传命令结果；立即释放会话会使控制器收不到成功回执。
            mainHandler.post(::releaseSessionAndStop)
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    override fun onCreate() {
        super.onCreate()
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        mediaSession = MediaSession.Builder(this, VideoPlayerEngine.get(application))
            .setCallback(sessionCallback)
            .setMediaButtonPreferences(notificationMediaButtonPreferences(VideoPlayerEngine.notificationControls.value))
            // 详情页已直接观察 Player 进度；通知仅需在状态/跳转时同步，避免周期位置同步影响列表滚动。
            .setPeriodicPositionUpdateEnabled(false)
            .build()
        // 由服务主动启动时不会自动调用 onGetSession，必须显式注册，Media3 才会创建通知控制器。
        addSession(mediaSession)
        mediaSession.setSessionActivity(notificationActivity)
        notificationControlScope.launch {
            VideoPlayerEngine.notificationControls.collect { controls ->
                if (!sessionReleased) {
                    mediaSession.setMediaButtonPreferences(notificationMediaButtonPreferences(controls))
                }
            }
        }
        Log.d(LOG_TAG, "MediaSession 已创建")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Media3 在注册会话、播放器状态变更时会自行刷新通知。这里手动刷新会触发
        // Media3 再次 startForegroundService，自身 onStartCommand 又再次刷新，从而形成循环。
        return START_NOT_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession.takeUnless { sessionReleased }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!mediaSession.player.playWhenReady) stopSelf()
    }

    override fun onDestroy() {
        notificationControlScope.cancel()
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
        if (getSessions().contains(mediaSession)) {
            removeSession(mediaSession)
        }
        mediaSession.release()
    }

    /** 前台使用 15 秒跳转；后台仅在队列有相邻项时提供上一条、下一条。 */
    private fun notificationMediaButtonPreferences(
        controls: VideoNotificationControls,
    ): List<CommandButton> = buildList {
        if (controls.usesPlaylistNavigation) {
            if (controls.hasPrevious) {
                add(
                    CommandButton.Builder(CommandButton.ICON_PREVIOUS)
                        .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .setDisplayName(getString(R.string.video_previous))
                        .build(),
                )
            }
            if (controls.hasNext) {
                add(
                    CommandButton.Builder(CommandButton.ICON_NEXT)
                        .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .setDisplayName(getString(R.string.video_next))
                        .build(),
                )
            }
        } else {
            add(
                CommandButton.Builder(CommandButton.ICON_SKIP_BACK_15)
                    .setPlayerCommand(Player.COMMAND_SEEK_BACK)
                    .setDisplayName(getString(R.string.video_skip_back_15_seconds))
                    .build(),
            )
            add(
                CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_15)
                    .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
                    .setDisplayName(getString(R.string.video_skip_forward_15_seconds))
                    .build(),
            )
        }
        add(
            CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                .setCustomIconResId(R.drawable.ic_video_notification_close)
                .setSessionCommand(VideoSessionCommands.dismissNotification)
                .setDisplayName(getString(R.string.video_close_notification))
                .build(),
        )
    }

    companion object {
        private const val LOG_TAG = "HashVideoLifecycle"
        private const val ACTION_ENSURE_SESSION = "com.hash.video.action.ENSURE_SESSION"
        @Volatile
        private var notificationActivity: PendingIntent? = null

        /**
         * 前台页面播放后建立媒体会话。
         *
         * MediaSessionService 会根据播放器状态自行发布通知并提升为前台服务；不能以
         * startForegroundService 强制启动，否则视频源立即失败时没有通知可供服务及时提升前台。
         */
        fun ensureSession(context: Context) {
            context.startService(
                Intent(context, HashVideoPlaybackService::class.java).setAction(ACTION_ENSURE_SESSION),
            )
        }

        /** 更新通知点击目标；未创建服务时仅暂存，不提前启动空闲会话。 */
        fun setNotificationActivity(context: Context, pendingIntent: PendingIntent?) {
            notificationActivity = pendingIntent
        }

        /** 页面真正退出时销毁会话；播放器释放仍由 VideoController 负责。 */
        fun stopSession(context: Context) {
            notificationActivity = null
            context.stopService(Intent(context, HashVideoPlaybackService::class.java))
        }
    }
}
