package com.hash.video.ui.controls

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import com.hash.video.R
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isFinished
import com.hash.video.api.isPlaying
import com.hash.video.ui.progress.DefaultVideoProgressView
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
) : LinearLayout(context, attrs) {

    private val playbackAction = createIconAction(R.drawable.ic_video_play).apply {
        id = R.id.video_play_toggle
    }
    private val progressControl = DefaultVideoProgressView(context)
    private val floatAction = createTextAction(R.string.video_float).apply {
        id = R.id.video_float_action
    }
    private val fullscreenAction = createTextAction(R.string.video_fullscreen).apply {
        id = R.id.video_fullscreen_action
    }
    var onPlaybackToggle: (() -> Unit)? = null
    var onSeekRequested: ((Long) -> Unit)? = null
    var onSeekPreviewChanged: ((Long) -> Unit)? = null

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
        addView(progressControl, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(floatAction, textActionLayoutParams())
        addView(fullscreenAction, textActionLayoutParams())

        playbackAction.setOnClickListener { onPlaybackToggle?.invoke() }
        progressControl.onSeekRequested = { onSeekRequested?.invoke(it) }
        progressControl.onSeekPreviewChanged = { onSeekPreviewChanged?.invoke(it) }
        floatAction.setOnClickListener { onFloatRequested?.invoke() }
        fullscreenAction.setOnClickListener { onFullscreenRequested?.invoke() }
    }

    fun render(state: VideoPlaybackState, hasMedia: Boolean = state.media != null) {
        val status = state.status
        playbackAction.visibility = if (hasMedia) VISIBLE else GONE
        playbackAction.setImageResource(if (status.isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play)
        playbackAction.contentDescription = context.getString(status.playbackActionDescription())
        renderProgress(state.progress)
    }

    /** 将真实播放状态渲染为按钮与播放/缓冲进度。 */
    fun renderProgress(progress: VideoPlaybackProgress) =
        progressControl.renderProgress(progress)

    /** 更新滑动预览 */
    fun renderSeekPreview(positionMs: Long, durationMs: Long) =
        progressControl.renderSeekPreview(positionMs, durationMs)

    /** 结束滑动预览 */
    fun endSeekPreview() = progressControl.endSeekPreview()

    /** 取消滑动预览。 */
    fun cancelSeekPreview() = progressControl.cancelSeekPreview()

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

    private fun actionLayoutParams() =
        LayoutParams(context.dp(ACTION_SIZE_DP), context.dp(ACTION_SIZE_DP))

    private fun textActionLayoutParams() =
        LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    companion object {
        private const val CONTROL_HEIGHT_DP = 56
        private const val ACTION_SIZE_DP = 40
        private const val CONTROL_BACKGROUND_COLOR = 0x55000000
        private const val ACTION_BACKGROUND_COLOR = 0x33000000
    }
}
