package com.hash.video.ui.panel

import android.content.Context
import android.widget.FrameLayout
import com.hash.video.api.VideoPlaybackState
import com.hash.video.ui.interaction.VideoInteractionIntent
import com.hash.video.ui.gesture.VideoGestureEvent

/** 可替换的视频控制面板基类；只显示状态和发出操作，不直接控制播放器。 */
abstract class AbstractVideoControlPanel(context: Context) : FrameLayout(context) {

    private var intentHandler: ((VideoInteractionIntent) -> Unit)? = null

    internal fun setIntentHandler(handler: (VideoInteractionIntent) -> Unit) {
        intentHandler = handler
    }

    /** 把按钮操作交给交互协调器。 */
    protected fun dispatchIntent(intent: VideoInteractionIntent) {
        intentHandler?.invoke(intent)
    }

    abstract fun render(state: VideoPlaybackState, hasMedia: Boolean)

    abstract fun toggleControls()

    /** 接收需要同步到 UI 的手势，例如横滑预览。 */
    open fun onGesture(event: VideoGestureEvent) = Unit
}
