package com.hash.video.api

import android.app.Application
import android.content.Context
import com.hash.video.player.VideoController
import com.hash.video.player.VideoPlaybackControlToken
import com.hash.video.player.VideoPlayerEngine
import kotlinx.coroutines.flow.StateFlow

/**
 * 多播放器页面的播放队列协调入口。
 *
 * 当一个页面有多个播放器 View、但共用同一个 ExoPlayer 时使用。它负责队列和当前播放项，
 * 页面再将当前视频画面交给对应的播放器 View 承载。单播放器页面直接调用播放器 View 的方法。
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
    fun playQueueAt(context: Context, token: VideoPlaybackControlToken, index: Int) =
        VideoPlayerEngine.playQueueAt(app(context), token, index, true, true)

    /** 多播放器页面销毁时释放 [token] 的队列播放会话。 */
    fun release(context: Context, token: VideoPlaybackControlToken) =
        VideoPlayerEngine.release(app(context), token)

    private fun app(context: Context) = context.applicationContext as Application
}
