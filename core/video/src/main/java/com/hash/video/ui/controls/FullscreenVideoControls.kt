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
import com.hash.video.api.isFinished
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import java.util.Locale
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
    private val progressView = SeekBar(context).apply {
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
    private var durationMs = 0L
    private var previewPositionMs: Long? = null

    var onPlaybackToggle: (() -> Unit)? = null
    var onSeekTo: ((Long) -> Unit)? = null
    var onSeekPreview: ((Long) -> Unit)? = null
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
        addView(progressView, LayoutParams(LayoutParams.MATCH_PARENT, context.dp(SEEK_HEIGHT_DP)))
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
        durationMs = progress.durationMs

        playbackAction.visibility = if (state.media != null && !status.isLoading) VISIBLE else GONE
        playbackAction.setImageResource(if (status.isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play)
        playbackAction.contentDescription = context.getString(status.playbackActionDescription())

        progressView.isEnabled = progress.isAvailable
        if (!progress.isAvailable) {
            previewPositionMs = null
            progressView.progress = 0
            progressView.secondaryProgress = 0
        } else if (!progressView.isPressed && previewPositionMs == null) {
            progressView.progress = (progress.fraction * PROGRESS_SCALE).roundToInt()
            progressView.secondaryProgress = (progress.bufferedFraction * PROGRESS_SCALE).roundToInt()
        }
        val displayedPosition = previewPositionMs ?: progress.positionMs
        timeView.text = formatTimeLine(displayedPosition, progress.durationMs)
    }

    fun renderSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        this.durationMs = durationMs
        previewPositionMs = positionMs.coerceIn(0L, durationMs)
        progressView.progress = ((previewPositionMs!!.toFloat() / durationMs) * PROGRESS_SCALE)
            .roundToInt()
            .coerceIn(0, PROGRESS_SCALE)
        timeView.text = formatTimeLine(previewPositionMs!!, durationMs)
    }

    fun finishSeekPreview() {
        previewPositionMs = null
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser && durationMs > 0L) {
            val positionMs = progress.toPositionMs()
            previewPositionMs = positionMs
            timeView.text = formatTimeLine(positionMs, durationMs)
            onSeekPreview?.invoke(positionMs)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        if (durationMs > 0L) {
            val positionMs = seekBar.progress.toPositionMs()
            onSeekTo?.invoke(positionMs)
        }
        previewPositionMs = null
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

    private fun Int.toPositionMs(): Long = toLong() * durationMs / PROGRESS_SCALE

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
    }
}
