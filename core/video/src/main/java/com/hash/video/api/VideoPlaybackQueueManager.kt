package com.hash.video.api

import android.app.Application
import android.content.Context
import com.hash.video.player.VideoPlayerEngine
import kotlinx.coroutines.flow.StateFlow

/**
 * 页面级播放队列协调入口。
 *
 * 只要页面存在选集、连续播放或多个播放器 View 共用一个 ExoPlayer，就通过此入口管理队列和
 * 当前播放项。页面再将当前视频画面交给对应的播放器 View 承载；没有队列的单视频页面直接调用
 * 播放器 View 的方法。
 */
object VideoPlaybackQueueManager {
    /** 当前队列的全局播放状态；用于选择应承载画面的播放器。 */
    val playbackState: StateFlow<VideoPlaybackState>
        get() = VideoPlayerEngine.playbackState

    /** 设置 [token] 对应的播放队列，不会立即开始播放。 */
    fun setPlaybackQueue(token: VideoPlaybackControlToken, items: List<VideoMedia>) =
        VideoPlayerEngine.setPlaybackQueue(token, items)

    /** 清除 [token] 当前持有的播放队列。 */
    fun clearPlaybackQueue(token: VideoPlaybackControlToken) = VideoPlayerEngine.clearPlaybackQueue(token)

    /** 播放队列中 [index] 对应的视频，并将它设为当前播放项。 */
    fun playQueueAt(
        context: Context,
        token: VideoPlaybackControlToken,
        index: Int,
        options: VideoQueuePlaybackOptions = VideoQueuePlaybackOptions(),
    ) = VideoPlayerEngine.playQueueAt(
        application = app(context),
        token = token,
        index = index,
        showNotification = options.showNotification,
        showNotificationArtwork = options.showNotificationArtwork,
    )

    /** 队列页面销毁时释放 [token] 对应的播放会话。 */
    fun release(context: Context, token: VideoPlaybackControlToken) =
        VideoPlayerEngine.release(app(context), token)

    private fun app(context: Context) = context.applicationContext as Application
}
