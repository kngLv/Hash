package com.hash.video.service

import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.hash.video.cache.VideoCacheProvider
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/**
 * 持有 App 唯一 ExoPlayer 的媒体前台服务。
 *
 * 页面只通过 MediaController 访问播放器，因此 Activity 销毁、全屏和悬浮窗迁移不会重建播放会话。
 */
@OptIn(UnstableApi::class)
class HashVideoPlaybackService : MediaSessionService() {

    /** 服务持有的唯一媒体会话，负责向 App 内的 MediaController 暴露播放器能力。 */
    private lateinit var mediaSession: MediaSession

    /** 处理控制器连接和 App 内部自定义命令。 */
    private val sessionCallback = object : MediaSession.Callback {
        /**
         * MediaController 连接服务时调用。
         *
         * 保留 Media3 默认命令，并且只向本应用进程开放“释放播放会话”命令，避免外部应用
         * 停止播放器或清空播放列表。
         */
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val resultBuilder = MediaSession.ConnectionResult.AcceptedResultBuilder(
                session,
                controller,
            )
            if (controller.packageName == packageName) {
                val defaultCommands = resultBuilder.build().availableSessionCommands
                resultBuilder.setAvailableSessionCommands(
                    defaultCommands.buildUpon()
                        .add(VideoSessionCommands.releasePlaybackSession)
                        .build(),
                )
            }
            return resultBuilder.build()
        }

        /**
         * 接收控制器发送的自定义会话命令。
         *
         * 当前仅处理本应用发起的“释放播放会话”：停止播放、清空列表并请求服务退出；
         * 其他来源或未支持的命令统一返回不支持。
         */
        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (
                controller.packageName != packageName ||
                customCommand != VideoSessionCommands.releasePlaybackSession
            ) {
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED),
                )
            }

            session.player.stop()
            session.player.clearMediaItems()
            pauseAllPlayersAndStopSelf()
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** 创建服务唯一的 ExoPlayer 和 MediaSession，并关闭播放器空闲时的通知。 */
    override fun onCreate() {
        super.onCreate()
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(VideoCacheProvider.getDataSourceFactory(this)),
            )
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
    }

    /** 为连接到该服务的控制器返回唯一的媒体会话。 */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession =
        mediaSession

    /** App 任务从最近任务中移除时，若当前没有继续播放的意图，则请求停止服务。 */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!mediaSession.player.playWhenReady) {
            stopSelf()
        }
    }

    /** 服务真正销毁时释放底层播放器和媒体会话资源。 */
    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
