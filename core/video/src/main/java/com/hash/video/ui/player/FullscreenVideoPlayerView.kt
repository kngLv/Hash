package com.hash.video.ui.player

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import com.hash.video.ui.controls.FullscreenVideoControls
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.gesture.VideoGesturePhase
import kotlin.math.roundToInt

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
    private val errorMessageView = createErrorMessageView().apply {
        id = com.hash.video.R.id.video_error_message
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
        addView(
            errorMessageView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        addView(controls, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))

        errorMessageView.setOnClickListener { play() }
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
        val errorMessage = state.errorMessage.takeIf { state.status == VideoPlaybackStatus.ERROR }
        errorMessageView.visibility = if (errorMessage != null) VISIBLE else GONE
        errorMessageView.text = errorMessage.orEmpty()
        errorMessageView.contentDescription = errorMessage?.let { "$it，点击重试" }
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
                controls.renderSeekPreview(event.positionMs, event.durationMs)
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

    /** 播放失败时展示具体原因；点击后由当前播放器重新发起播放。 */
    private fun createErrorMessageView(): TextView = TextView(context).apply {
        visibility = GONE
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            cornerRadius = context.dp(ERROR_CORNER_RADIUS_DP).toFloat()
            setColor(ERROR_BACKGROUND_COLOR)
        }
        setPadding(
            context.dp(ERROR_HORIZONTAL_PADDING_DP),
            context.dp(ERROR_VERTICAL_PADDING_DP),
            context.dp(ERROR_HORIZONTAL_PADDING_DP),
            context.dp(ERROR_VERTICAL_PADDING_DP),
        )
        isClickable = true
        isFocusable = true
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val ERROR_BACKGROUND_COLOR = 0xAA000000.toInt()
        const val ERROR_CORNER_RADIUS_DP = 8
        const val ERROR_HORIZONTAL_PADDING_DP = 16
        const val ERROR_VERTICAL_PADDING_DP = 10
    }
}
