package com.hash.video.player

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.annotation.MainThread
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import kotlinx.coroutines.flow.StateFlow

/**
 * 单个播放器 View 的轻量控制器。
 *
 * 不持有 ExoPlayer、Player.Listener 或页面引用，可安全地为 RecyclerView 的每个 item 创建。
 */
class VideoController(context: Context) {

    private val application = context.applicationContext as Application

    val playbackState: StateFlow<VideoPlaybackState>
        get() = VideoPlayerEngine.playbackState

    /** 详情默认显示通知；未来列表协调器可关闭。 */
    var showNotification: Boolean = true

    /** 通知默认使用视频封面；调试或特殊业务可关闭通知 artwork。 */
    var showNotificationArtwork: Boolean = true

    /** 默认每个 View 独立持有；列表或详情页可注入页面级 Owner 统一管理。 */
    var playbackOwner: VideoPlaybackOwner = VideoPlaybackOwner()

    @MainThread
    fun play(media: VideoMedia) =
        VideoPlayerEngine.play(
            application,
            playbackOwner,
            media,
            showNotification,
            showNotificationArtwork,
        )

    @MainThread
    fun pause() = VideoPlayerEngine.pause(playbackOwner)

    @MainThread
    fun seekTo(positionMs: Long) = VideoPlayerEngine.seekTo(playbackOwner, positionMs)

    @MainThread
    fun stop() = VideoPlayerEngine.stop(playbackOwner)

    @MainThread
    fun removeCurrentMedia() = VideoPlayerEngine.removeCurrentMedia(playbackOwner)

    @MainThread
    fun setNotificationActivity(pendingIntent: PendingIntent?) =
        VideoPlayerEngine.setNotificationActivity(application, pendingIntent)

    @MainThread
    fun attachRenderTo(container: ViewGroup, targetMedia: VideoMedia?) =
        VideoPlayerEngine.attachRenderTo(application, playbackOwner, container, targetMedia)

    @MainThread
    fun detachRenderFrom(container: ViewGroup): Boolean =
        VideoPlayerEngine.detachRenderFrom(playbackOwner, container)

    @MainThread
    fun hasAttachedRender(): Boolean = VideoPlayerEngine.hasAttachedRender()

    @MainThread
    internal fun pausedFrameFor(media: VideoMedia): Bitmap? = VideoPlayerEngine.pausedFrameFor(media)

    /** 页面真正结束当前播放会话时调用；引擎尚未创建时也安全。 */
    @MainThread
    fun releasePlayback() = VideoPlayerEngine.release(application, playbackOwner)
}
