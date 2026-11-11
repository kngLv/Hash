package com.hash.video.service

import android.os.Bundle
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
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
            Log.d(LOG_TAG, "MediaController 正在连接：package=${controller.packageName}")
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
                Log.d(LOG_TAG, "已向应用内 MediaController 开放释放命令")
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
            Log.d(
                LOG_TAG,
                "收到自定义命令：action=${customCommand.customAction}, " +
                    "package=${controller.packageName}",
            )
            if (
                controller.packageName != packageName ||
                customCommand != VideoSessionCommands.releasePlaybackSession
            ) {
                Log.w(LOG_TAG, "已拒绝自定义命令")
                return Futures.immediateFuture(
                    SessionResult(SessionError.ERROR_NOT_SUPPORTED),
                )
            }

            Log.d(
                LOG_TAG,
                "正在释放播放会话：mediaItemCount=${session.player.mediaItemCount}, " +
                    "playbackState=${session.player.playbackState}",
            )
            session.player.stop()
            Log.d(LOG_TAG, "ExoPlayer 已停止")
            session.player.clearMediaItems()
            Log.d(LOG_TAG, "播放列表已清空：mediaItemCount=${session.player.mediaItemCount}")
            pauseAllPlayersAndStopSelf()
            Log.d(LOG_TAG, "已请求停止 HashVideoPlaybackService")
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }
    }

    /** 创建服务唯一的 ExoPlayer 和 MediaSession，并关闭播放器空闲时的通知。 */
    override fun onCreate() {
        super.onCreate()
        Log.d(LOG_TAG, "HashVideoPlaybackService onCreate")
        setShowNotificationForIdlePlayer(SHOW_NOTIFICATION_FOR_IDLE_PLAYER_NEVER)
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(VideoCacheProvider.getDataSourceFactory(this)),
            )
            .build()
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(sessionCallback)
            .build()
        Log.d(LOG_TAG, "ExoPlayer 和 MediaSession 已创建")
    }

    /** 为连接到该服务的控制器返回唯一的媒体会话。 */
    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        Log.d(LOG_TAG, "MediaController 请求 MediaSession：package=${controllerInfo.packageName}")
        return mediaSession
    }

    /** App 任务从最近任务中移除时，若当前没有继续播放的意图，则请求停止服务。 */
    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        Log.d(
            LOG_TAG,
            "应用任务已从最近任务移除：playWhenReady=${mediaSession.player.playWhenReady}",
        )
        if (!mediaSession.player.playWhenReady) {
            stopSelf()
            Log.d(LOG_TAG, "任务移除后已请求停止 HashVideoPlaybackService")
        }
    }

    /** 服务真正销毁时释放底层播放器和媒体会话资源。 */
    override fun onDestroy() {
        Log.d(
            LOG_TAG,
            "HashVideoPlaybackService onDestroy 开始：mediaItemCount=${mediaSession.player.mediaItemCount}",
        )
        mediaSession.run {
            player.release()
            Log.d(LOG_TAG, "ExoPlayer 已释放")
            release()
            Log.d(LOG_TAG, "MediaSession 已释放")
        }
        super.onDestroy()
        Log.d(LOG_TAG, "HashVideoPlaybackService onDestroy 完成")
    }

    private companion object {
        const val LOG_TAG = "HashVideoLifecycle"
    }
}
