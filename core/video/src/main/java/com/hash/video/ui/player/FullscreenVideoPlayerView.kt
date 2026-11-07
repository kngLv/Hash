package com.hash.video.ui.player

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import com.hash.video.ui.controls.FullscreenVideoControls
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.gesture.VideoGesturePhase

/** 应用内横屏全屏播放宿主，组合完整画面手势与三层控制栏。 */
@OptIn(UnstableApi::class)
class FullscreenVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseHashVideoPlayerView(context, attrs, defStyleAttr) {

    override val gestureConfig = VideoGestureConfig(
        singleTapEnabled = true,
        doubleTapEnabled = true,
        seekEnabled = true,
        brightnessEnabled = true,
        volumeEnabled = true,
    )

    private val loadingView = ProgressBar(context).apply {
        visibility = GONE
        isIndeterminate = true
    }
    private val controls = FullscreenVideoControls(context)

    /** 左侧纵向手势的亮度变化量，范围 -1 到 1；由 Activity 等宿主修改 Window 亮度。 */
    var onBrightnessGesture: ((Float, VideoGesturePhase) -> Unit)? = null

    /** 右侧纵向手势的媒体音量变化量，范围 -1 到 1；由宿主修改系统媒体音量。 */
    var onVolumeGesture: ((Float, VideoGesturePhase) -> Unit)? = null

    var onFloatRequested: (() -> Unit)?
        get() = controls.onFloatRequested
        set(value) {
            controls.onFloatRequested = value
        }

    var onQualityRequested: (() -> Unit)?
        get() = controls.onQualityRequested
        set(value) {
            controls.onQualityRequested = value
        }

    var onExitFullscreenRequested: (() -> Unit)?
        get() = controls.onExitFullscreenRequested
        set(value) {
            controls.onExitFullscreenRequested = value
        }

    init {
        setBackgroundColor(Color.BLACK)
        addView(loadingView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        controls.onPlaybackToggle = ::togglePlayback
        controls.onSeekRequested = { positionMs ->
            handleUserSeek(positionMs, currentPlaybackState.progress.durationMs)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        takeOverPresentation()
    }

    override fun onPlaybackStateChanged(state: VideoPlaybackState) {
        loadingView.visibility = if (state.status.isLoading) VISIBLE else GONE
        controls.render(state)
    }

    private fun togglePlayback() {
        if (currentPlaybackState.status.isPlaying) pause() else play()
    }

    override fun onVideoGesture(event: VideoGestureEvent) {
        when (event) {
            VideoGestureEvent.Click -> controls.visibility =
                if (controls.visibility == VISIBLE) GONE else VISIBLE

            is VideoGestureEvent.SeekPreview -> {
                controls.visibility = VISIBLE
                controls.updateSeekPreview(event.positionMs, event.durationMs)
            }

            is VideoGestureEvent.SeekPreviewEnd,
            VideoGestureEvent.SeekPreviewCancel,
                -> controls.endSeekPreview()

            is VideoGestureEvent.BrightnessAdjustment ->
                onBrightnessGesture?.invoke(event.delta, event.phase)

            is VideoGestureEvent.VolumeAdjustment ->
                onVolumeGesture?.invoke(event.delta, event.phase)

            else -> Unit
        }
    }
}
