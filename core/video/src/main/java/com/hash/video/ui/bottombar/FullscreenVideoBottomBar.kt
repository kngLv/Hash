package com.hash.video.ui.bottombar

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
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isFinished
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/** 横屏全屏播放使用的三层底部操作栏：时间、时间轴、播放操作。 */
class FullscreenVideoBottomBar @JvmOverloads constructor(
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
        setOnSeekBarChangeListener(this@FullscreenVideoBottomBar)
    }
    private val playbackAction = createIconAction(R.drawable.ic_video_play).apply { id = R.id.video_play_toggle }
    private var currentDurationMs = 0L
    private var seekPreviewPositionMs: Long? = null
    private var isAwaitingSeekCompletion = false
    private var lastProgress = VideoPlaybackProgress()
    private var listener: Listener? = null

    init {
        id = R.id.video_fullscreen_controls
        orientation = VERTICAL
        setPadding(context.dp(16), context.dp(6), context.dp(16), context.dp(8))
        setBackgroundColor(CONTROL_BACKGROUND_COLOR)
        addView(timeView, LayoutParams(LayoutParams.MATCH_PARENT, context.dp(TIME_HEIGHT_DP)))
        addView(seekBar, LayoutParams(LayoutParams.MATCH_PARENT, context.dp(SEEK_HEIGHT_DP)))
        addView(createActionRow(), LayoutParams(LayoutParams.MATCH_PARENT, context.dp(ACTION_ROW_HEIGHT_DP)))
        playbackAction.setOnClickListener { listener?.onTogglePlayback() }
    }

    fun setListener(listener: Listener?) { this.listener = listener }

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
        seekBar.progress = ((seekPreviewPositionMs!!.toFloat() / durationMs) * PROGRESS_SCALE).roundToInt().coerceIn(0, PROGRESS_SCALE)
        timeView.text = formatTimeLine(seekPreviewPositionMs!!, durationMs)
    }

    fun endSeekPreview() { isAwaitingSeekCompletion = seekPreviewPositionMs != null }

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
            endSeekPreview()
            listener?.onSeek(seekBar.progress.toPositionMs())
        }
    }

    private fun createActionRow() = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(playbackAction, LayoutParams(context.dp(ACTION_SIZE_DP), context.dp(ACTION_SIZE_DP)))
        addView(View(context), LayoutParams(0, 0, 1f))
    }

    private fun createIconAction(iconRes: Int) = ImageButton(context).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        setBackgroundColor(Color.TRANSPARENT)
        val padding = context.dp(10)
        setPadding(padding, padding, padding, padding)
    }

    private fun Int.toPositionMs(): Long = toLong() * currentDurationMs / PROGRESS_SCALE

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
        timeView.text = formatTimeLine(seekPreviewPositionMs ?: progress.positionMs, progress.durationMs)
    }

    private fun completeSeekPreviewIfNeeded(positionMs: Long) {
        val preview = seekPreviewPositionMs ?: return
        if (isAwaitingSeekCompletion && abs(positionMs - preview) <= SEEK_CONFIRM_TOLERANCE_MS) clearSeekPreview()
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

    private fun formatTimeLine(positionMs: Long, durationMs: Long) =
        "${formatDuration(positionMs)} / ${formatDuration(durationMs)}"

    private fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = totalSeconds % 3_600L / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val PROGRESS_SCALE = 1_000
        const val TIME_HEIGHT_DP = 28
        const val SEEK_HEIGHT_DP = 40
        const val ACTION_ROW_HEIGHT_DP = 48
        const val ACTION_SIZE_DP = 40
        const val CONTROL_BACKGROUND_COLOR = 0x66000000
        const val SEEK_CONFIRM_TOLERANCE_MS = 500L
    }

    interface Listener {
        fun onTogglePlayback()
        fun onSeek(positionMs: Long)
    }
}
