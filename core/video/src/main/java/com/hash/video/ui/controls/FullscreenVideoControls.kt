package com.hash.video.ui.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
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
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.isFinished
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** 横屏全屏播放使用的三层控制栏：时间、时间轴、播放与扩展操作。 */
class FullscreenVideoControls @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs), SeekBar.OnSeekBarChangeListener {

    private val timeView = TextView(context).apply {
        id = R.id.video_time
        setTextColor(Color.WHITE)
        textSize = 13f
        gravity = Gravity.CENTER_VERTICAL
        text = formatTimeLine(0L, 0L)
    }
    private val seekBar = SeekBar(context).apply {
        id = R.id.video_progress
        max = PROGRESS_SCALE
        setOnSeekBarChangeListener(this@FullscreenVideoControls)
    }
    private val playbackAction = createIconAction(R.drawable.ic_video_play).apply {
        id = R.id.video_play_toggle
    }
    private val floatAction = createTextAction(R.string.video_float).apply {
        id = R.id.video_float_action
    }
    private val qualityAction = createTextAction(R.string.video_quality).apply {
        id = R.id.video_quality_action
    }
    private val exitAction = createTextAction(R.string.video_exit_fullscreen).apply {
        id = R.id.video_exit_fullscreen_action
    }
    private var currentDurationMs = 0L
    private var seekPreviewPositionMs: Long? = null
    private var isAwaitingSeekCompletion = false
    private var lastProgress = VideoPlaybackProgress()

    var onPlaybackToggle: (() -> Unit)? = null
    var onSeekRequested: ((Long) -> Unit)? = null
    var onFloatRequested: (() -> Unit)? = null
        set(value) {
            field = value
            floatAction.visibility = if (value == null) GONE else VISIBLE
        }

    var onQualityRequested: (() -> Unit)? = null
        set(value) {
            field = value
            qualityAction.visibility = if (value == null) GONE else VISIBLE
        }

    var onExitFullscreenRequested: (() -> Unit)? = null
        set(value) {
            field = value
            exitAction.visibility = if (value == null) GONE else VISIBLE
        }

    init {
        id = R.id.video_fullscreen_controls
        orientation = VERTICAL
        setPadding(context.dp(16), context.dp(6), context.dp(16), context.dp(8))
        setBackgroundColor(CONTROL_BACKGROUND_COLOR)

        addView(timeView, LayoutParams(LayoutParams.MATCH_PARENT, context.dp(TIME_HEIGHT_DP)))
        addView(seekBar, LayoutParams(LayoutParams.MATCH_PARENT, context.dp(SEEK_HEIGHT_DP)))
        addView(createActionRow(), LayoutParams(LayoutParams.MATCH_PARENT, context.dp(ACTION_ROW_HEIGHT_DP)))

        floatAction.visibility = GONE
        qualityAction.visibility = GONE
        exitAction.visibility = GONE
        playbackAction.setOnClickListener { onPlaybackToggle?.invoke() }
        floatAction.setOnClickListener { onFloatRequested?.invoke() }
        qualityAction.setOnClickListener { onQualityRequested?.invoke() }
        exitAction.setOnClickListener { onExitFullscreenRequested?.invoke() }
    }

    fun render(state: VideoPlaybackState) {
        val status = state.status
        val progress = state.progress
        lastProgress = progress
        currentDurationMs = progress.durationMs

        playbackAction.visibility = if (state.media != null && !status.isLoading) VISIBLE else GONE
        playbackAction.setImageResource(if (status.isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play)
        playbackAction.contentDescription = context.getString(status.playbackActionDescription())

        renderProgress(progress)
    }

    fun renderSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        currentDurationMs = durationMs
        isAwaitingSeekCompletion = false
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
        seekBar.progress = ((seekPreviewPositionMs!!.toFloat() / durationMs) * PROGRESS_SCALE)
            .roundToInt()
            .coerceIn(0, PROGRESS_SCALE)
        timeView.text = formatTimeLine(seekPreviewPositionMs!!, durationMs)
    }

    fun endSeekPreview() {
        isAwaitingSeekCompletion = seekPreviewPositionMs != null
    }

    /** 放弃未提交的拖动预览，并立即恢复最近一次真实播放进度。 */
    fun cancelSeekPreview() {
        clearSeekPreview()
        renderProgress(lastProgress)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && currentDurationMs > 0L) {
            val positionMs = progress.toPositionMs()
            isAwaitingSeekCompletion = false
            seekPreviewPositionMs = positionMs
            timeView.text = formatTimeLine(positionMs, currentDurationMs)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (currentDurationMs > 0L) {
            val positionMs = seekBar.progress.toPositionMs()
            endSeekPreview()
            onSeekRequested?.invoke(positionMs)
        }
    }

    private fun createActionRow() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(playbackAction, LayoutParams(context.dp(ACTION_SIZE_DP), context.dp(ACTION_SIZE_DP)))
        addView(View(context), LayoutParams(0, 0, 1f))
        addView(floatAction, actionTextLayoutParams())
        addView(qualityAction, actionTextLayoutParams())
        addView(exitAction, actionTextLayoutParams())
    }

    private fun createIconAction(iconRes: Int) = ImageButton(context).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        val padding = context.dp(10)
        setPadding(padding, padding, padding, padding)
    }

    private fun createTextAction(textRes: Int) = TextView(context).apply {
        setText(textRes)
        setTextColor(Color.WHITE)
        gravity = Gravity.CENTER
        setPadding(context.dp(10), 0, context.dp(10), 0)
        isClickable = true
        isFocusable = true
    }

    private fun actionTextLayoutParams() = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)

    private fun Int.toPositionMs(): Long = toLong() * currentDurationMs / PROGRESS_SCALE

    /** 根据真实播放器进度刷新控制栏；正在预览时保留用户拖动的位置。 */
    private fun renderProgress(progress: VideoPlaybackProgress) {
        seekBar.isEnabled = progress.isAvailable
        if (!progress.isAvailable) {
            clearSeekPreview()
            seekBar.progress = 0
            seekBar.secondaryProgress = 0
        } else {
            completeSeekPreviewIfNeeded(progress.positionMs)
        }
        if (progress.isAvailable && !seekBar.isPressed && seekPreviewPositionMs == null) {
            seekBar.progress = (progress.fraction * PROGRESS_SCALE).roundToInt()
            seekBar.secondaryProgress = (progress.bufferedFraction * PROGRESS_SCALE).roundToInt()
        }
        val displayedPositionMs = seekPreviewPositionMs ?: progress.positionMs
        timeView.text = formatTimeLine(displayedPositionMs, progress.durationMs)
    }

    /** 仅当播放器已接近目标位置时才恢复真实进度，避免 seek 后控制栏回跳。 */
    private fun completeSeekPreviewIfNeeded(positionMs: Long) {
        val previewPositionMs = seekPreviewPositionMs ?: return
        if (
            isAwaitingSeekCompletion &&
            abs(positionMs - previewPositionMs) <= SEEK_CONFIRM_TOLERANCE_MS
        ) {
            clearSeekPreview()
        }
    }

    private fun clearSeekPreview() {
        seekPreviewPositionMs = null
        isAwaitingSeekCompletion = false
    }

    private fun VideoPlaybackStatus.playbackActionDescription(): Int = when {
        isPlaying -> R.string.video_pause
        isFinished -> R.string.video_replay
        this == VideoPlaybackStatus.ERROR -> R.string.video_retry
        else -> R.string.video_play
    }

    private fun formatTimeLine(positionMs: Long, durationMs: Long): String =
        "${formatDuration(positionMs)} / ${formatDuration(durationMs)}"

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = totalSeconds % 3_600L / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val PROGRESS_SCALE = 1_000
        private const val TIME_HEIGHT_DP = 28
        private const val SEEK_HEIGHT_DP = 40
        private const val ACTION_ROW_HEIGHT_DP = 48
        private const val ACTION_SIZE_DP = 40
        private const val CONTROL_BACKGROUND_COLOR = 0x66000000
        private const val SEEK_CONFIRM_TOLERANCE_MS = 500L
    }
}
