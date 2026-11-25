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

    /** 默认每个 View 独立创建；多播放器页面可注入共享操作凭证。 */
    var playbackToken: VideoPlaybackControlToken = VideoPlaybackControlToken()

    @MainThread
    fun play(media: VideoMedia) =
        VideoPlayerEngine.play(
            application,
            playbackToken,
            media,
            showNotification,
            showNotificationArtwork,
        )

    /** 设置当前播放器的播放队列，不会立即开始播放。 */
    @MainThread
    fun setPlaybackQueue(items: List<VideoMedia>) = VideoPlayerEngine.setPlaybackQueue(playbackToken, items)

    /** 播放当前播放器队列中 [index] 对应的视频。 */
    @MainThread
    fun playQueueAt(index: Int) =
        VideoPlayerEngine.playQueueAt(application, playbackToken, index, showNotification, showNotificationArtwork)

    /** 清除当前播放器的播放队列。 */
    @MainThread
    fun clearPlaybackQueue() = VideoPlayerEngine.clearPlaybackQueue(playbackToken)

    /** 设置通知点击后返回应用的 Intent。 */
    @MainThread
    fun setNotificationActivity(pendingIntent: PendingIntent?) =
        VideoPlayerEngine.setNotificationActivity(application, pendingIntent)

    /** 释放当前播放器拥有的播放会话。 */
    @MainThread
    fun releasePlayback() = VideoPlayerEngine.release(application, playbackToken)


    /** 播放队列中的上一项；位于首项时安全忽略。 */
    @MainThread
    fun playPrevious() = VideoPlayerEngine.playPrevious(playbackToken)

    /** 播放队列中的下一项；位于末项时安全忽略。 */
    @MainThread
    fun playNext() = VideoPlayerEngine.playNext(playbackToken)

    @MainThread
    fun pause() = VideoPlayerEngine.pause(playbackToken)

    @MainThread
    fun seekTo(positionMs: Long) = VideoPlayerEngine.seekTo(playbackToken, positionMs)

    @MainThread
    fun stop() = VideoPlayerEngine.stop(playbackToken)

    @MainThread
    fun removeCurrentMedia() = VideoPlayerEngine.removeCurrentMedia(playbackToken)

    @MainThread
    fun attachRenderTo(container: ViewGroup, targetMedia: VideoMedia?) =
        VideoPlayerEngine.attachRenderTo(application, playbackToken, container, targetMedia)

    @MainThread
    fun detachRenderFrom(container: ViewGroup): Boolean =
        VideoPlayerEngine.detachRenderFrom(playbackToken, container)

    @MainThread
    fun hasAttachedRender(): Boolean = VideoPlayerEngine.hasAttachedRender()

    @MainThread
    internal fun pausedFrameFor(media: VideoMedia): Bitmap? = VideoPlayerEngine.pausedFrameFor(media)

}
