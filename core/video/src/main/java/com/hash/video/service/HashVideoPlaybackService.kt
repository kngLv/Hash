package com.hash.video.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.hash.video.cache.VideoCacheProvider

/**
 * 持有 App 唯一 ExoPlayer 的媒体前台服务。
 *
 * 页面只通过 MediaController 访问播放器，因此 Activity 销毁、全屏和悬浮窗迁移不会重建播放会话。
 */
@OptIn(UnstableApi::class)
class HashVideoPlaybackService : MediaSessionService() {

    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(VideoCacheProvider.getDataSourceFactory(this)),
            )
            .build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    override fun onTaskRemoved(rootIntent: android.content.Intent?) {
        if (!mediaSession.player.playWhenReady) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}
