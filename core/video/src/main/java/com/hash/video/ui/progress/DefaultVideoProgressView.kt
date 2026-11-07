package com.hash.video.ui.progress

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.hash.video.R
import java.util.Locale
import kotlin.math.roundToInt

/** 内嵌播放场景的默认单行进度 UI：可拖动进度条与“当前位置 / 总时长”。 */
class DefaultVideoProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseVideoProgressView(context, attrs, defStyleAttr), SeekBar.OnSeekBarChangeListener {
    private var displayedDurationMs = 0L

    private val progressView = SeekBar(context).apply {
        id = R.id.video_progress
        max = PROGRESS_SCALE
        visibility = GONE
        setOnSeekBarChangeListener(this@DefaultVideoProgressView)
    }
    private val timeView = TextView(context).apply {
        id = R.id.video_time
        setTextColor(Color.WHITE)
        textSize = 12f
        gravity = Gravity.CENTER
        text = formatTimeLine(0L, 0L)
    }

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(progressView, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(timeView, LayoutParams(context.dp(TIME_WIDTH_DP), LayoutParams.MATCH_PARENT))
    }

    override fun renderProgress(state: ProgressUiState) {
        displayedDurationMs = state.durationMs
        progressView.visibility = if (state.isAvailable) VISIBLE else GONE
        progressView.isEnabled = state.isAvailable
        if (state.isAvailable) {
            progressView.progress = state.positionMs.toProgress(state.durationMs)
            progressView.secondaryProgress = state.bufferedPositionMs.toProgress(state.durationMs)
        } else {
            progressView.progress = 0
            progressView.secondaryProgress = 0
        }
        timeView.text = formatTimeLine(state.positionMs, state.durationMs)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (fromUser) {
            dispatchUserSeekPreview(progress.toPositionMs(), displayedDurationMs)
            timeView.text = formatTimeLine(progress.toPositionMs(), displayedDurationMs)
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) = Unit

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        dispatchUserSeek(seekBar.progress.toPositionMs(), displayedDurationMs)
    }

    private fun Long.toProgress(durationMs: Long): Int = if (durationMs > 0L) {
        ((toFloat() / durationMs) * PROGRESS_SCALE).roundToInt().coerceIn(0, PROGRESS_SCALE)
    } else {
        0
    }

    private fun Int.toPositionMs(): Long = toLong() * displayedDurationMs / PROGRESS_SCALE

    private fun formatTimeLine(positionMs: Long, durationMs: Long): String {
        return "${formatDuration(positionMs)} / ${formatDuration(durationMs)}"
    }

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

    private companion object {
        const val PROGRESS_SCALE = 1_000
        const val TIME_WIDTH_DP = 88
    }
}
