package com.hash.video.ui.interaction

import android.annotation.SuppressLint
import android.content.Context
import android.widget.FrameLayout
import com.hash.video.api.VideoPlaybackState
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.panel.AbstractVideoControlPanel

/** 把手势和控制面板的操作统一转成 [VideoInteractionIntent]。 */
@SuppressLint("ViewConstructor")
internal class VideoInteractionCoordinator(
    context: Context,
    private val panel: AbstractVideoControlPanel,
    private val dispatchIntent: (VideoInteractionIntent) -> Unit,
) : FrameLayout(context) {

    private val gestureLayer = VideoGestureLayer(context, ::handleGesture)

    init {
        // Panel 在手势层上方，按钮和进度条优先接收触摸。
        addView(gestureLayer, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        panel.setIntentHandler(dispatchIntent)
    }

    fun render(state: VideoPlaybackState, hasMedia: Boolean) {
        gestureLayer.renderProgress(state.progress)
        panel.render(state, hasMedia)
    }

    /** 更新当前播放器的手势开关。 */
    fun updateGestureConfig(config: VideoGestureConfig) {
        gestureLayer.gestureConfig = config
    }

    fun release() = gestureLayer.release()

    private fun handleGesture(event: VideoGestureEvent) {
        when (event) {
            VideoGestureEvent.Click -> {
                panel.toggleControls()
                dispatchIntent(VideoInteractionIntent.PlayerClicked)
            }

            VideoGestureEvent.DoubleClick -> {
                panel.onGesture(event)
                dispatchIntent(VideoInteractionIntent.TogglePlayback)
            }

            is VideoGestureEvent.SeekPreviewEnd -> {
                panel.onGesture(event)
                dispatchIntent(VideoInteractionIntent.SeekTo(event.positionMs))
            }

            else -> panel.onGesture(event)
        }
    }
}
