package com.hash.video.ui.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.hash.video.R
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isFinished
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import kotlin.math.roundToInt

/**
 * 内嵌视频使用的单行控制栏。
 *
 * 控制栏只负责状态渲染和用户操作分发，不直接持有或控制播放器。进度轨道视觉上保持紧凑，
 * [SeekBar] 实际占满控制栏高度，保证拖动区域足够大。
 */
class EmbeddedVideoControls @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs), SeekBar.OnSeekBarChangeListener {

    private val playbackAction = createIconAction(R.drawable.ic_video_play).apply {
        id = R.id.video_play_toggle
    }
    private val progressView = SeekBar(context).apply {
        id = R.id.video_progress
        max = PROGRESS_SCALE
        visibility = GONE
        setOnSeekBarChangeListener(this@EmbeddedVideoControls)
    }
    private val floatAction = createTextAction(R.string.video_float).apply {
        id = R.id.video_float_action
    }
    private val fullscreenAction = createTextAction(R.string.video_fullscreen).apply {
        id = R.id.video_fullscreen_action
    }
    private var durationMs = 0L

    var onPlaybackToggle: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onSeekPreview: ((Long) -> Unit)? = null

    var onFullscreenRequested: (() -> Unit)? = null
        set(value) {
            field = value
            fullscreenAction.visibility = if (value == null) GONE else VISIBLE
        }

    var onFloatRequested: (() -> Unit)? = null
        set(value) {
            field = value
            floatAction.visibility = if (value == null) GONE else VISIBLE
        }

    init {
        id = R.id.video_embedded_controls
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(CONTROL_HEIGHT_DP)
        setPadding(context.dp(8), 0, context.dp(8), 0)
        setBackgroundColor(CONTROL_BACKGROUND_COLOR)

        floatAction.visibility = GONE
        fullscreenAction.visibility = GONE
        addView(playbackAction, actionLayoutParams())
        addView(progressView, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(floatAction, textActionLayoutParams())
        addView(fullscreenAction, textActionLayoutParams())

        playbackAction.setOnClickListener { onPlaybackToggle?.invoke() }
        floatAction.setOnClickListener { onFloatRequested?.invoke() }
        fullscreenAction.setOnClickListener { onFullscreenRequested?.invoke() }
    }

    /** 将真实播放状态渲染为按钮与播放/缓冲进度。 */
    fun render(state: VideoPlaybackState) {
        val hasMedia = state.media != null
        val status = state.status
        durationMs = state.progress.durationMs

        playbackAction.visibility = if (hasMedia && !status.isLoading) VISIBLE else GONE
        playbackAction.setImageResource(if (status.isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play)
        playbackAction.contentDescription = context.getString(status.playbackActionDescription())

        progressView.visibility = if (state.progress.isAvailable) VISIBLE else GONE
        if (state.progress.isAvailable && !progressView.isPressed) {
            progressView.progress = (state.progress.fraction * PROGRESS_SCALE).roundToInt()
            progressView.secondaryProgress = (state.progress.bufferedFraction * PROGRESS_SCALE).roundToInt()
        }
    }

    /** 横向画面手势拖动时临时展示目标位置，等待真实播放器状态追上。 */
    fun renderSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        this.durationMs = durationMs
        progressView.visibility = VISIBLE
        progressView.progress = ((positionMs.toFloat() / durationMs) * PROGRESS_SCALE)
            .roundToInt()
            .coerceIn(0, PROGRESS_SCALE)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && durationMs > 0L) {
            onSeekPreview?.invoke(progress.toPositionMs())
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (durationMs > 0L) {
            onSeekTo?.invoke(seekBar.progress.toPositionMs())
        }
    }

    private fun Int.toPositionMs(): Long = (toLong() * durationMs / PROGRESS_SCALE)

    private fun VideoPlaybackStatus.playbackActionDescription(): Int = when {
        isPlaying -> R.string.video_pause
        isFinished -> R.string.video_replay
        this == VideoPlaybackStatus.ERROR -> R.string.video_retry
        else -> R.string.video_play
    }

    private fun createIconAction(iconRes: Int): ImageButton = ImageButton(context).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ACTION_BACKGROUND_COLOR)
        }
        val padding = context.dp(10)
        setPadding(padding, padding, padding, padding)
        isClickable = true
        isFocusable = true
    }

    private fun createTextAction(textRes: Int): TextView = TextView(context).apply {
        setText(textRes)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(context.dp(8), 0, context.dp(8), 0)
        isClickable = true
        isFocusable = true
    }

    private fun actionLayoutParams() = LayoutParams(context.dp(ACTION_SIZE_DP), context.dp(ACTION_SIZE_DP))

    private fun textActionLayoutParams() = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val CONTROL_HEIGHT_DP = 56
        private const val ACTION_SIZE_DP = 40
        private const val PROGRESS_SCALE = 1_000
        private const val CONTROL_BACKGROUND_COLOR = 0x55000000
        private const val ACTION_BACKGROUND_COLOR = 0x33000000
    }
}
