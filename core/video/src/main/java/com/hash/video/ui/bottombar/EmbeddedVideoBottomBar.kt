package com.hash.video.ui.bottombar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.SeekBar
import com.hash.video.R
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isFinished
import com.hash.video.api.isPlaying
import com.hash.video.ui.progress.DefaultVideoProgressView
import kotlin.math.roundToInt

/** 内嵌视频使用的单行底部操作栏。 */
class EmbeddedVideoBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {

    private val playbackAction = createIconAction(R.drawable.ic_video_play).apply {
        id = R.id.video_play_toggle
    }
    private val progressControl = DefaultVideoProgressView(context)
    /** 业务层可配置的横屏入口。 */
    val landscapeAction = createIconAction(R.drawable.ic_video_landscape).apply {
        contentDescription = context.getString(R.string.video_landscape)
    }
    /** 业务层可配置的竖屏入口。 */
    val portraitAction = createIconAction(R.drawable.ic_video_portrait).apply {
        contentDescription = context.getString(R.string.video_portrait)
    }
    private var listener: Listener? = null

    init {
        id = R.id.video_embedded_controls
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = context.dp(CONTROL_HEIGHT_DP)
        setPadding(context.dp(8), 0, context.dp(8), 0)
        setBackgroundColor(CONTROL_BACKGROUND_COLOR)
        addView(playbackAction, actionLayoutParams())
        addView(progressControl, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(landscapeAction, actionLayoutParams())
        addView(portraitAction, actionLayoutParams())
        playbackAction.setOnClickListener { listener?.onTogglePlayback() }
        progressControl.setSeekListener { listener?.onSeek(it) }
    }

    fun setListener(listener: Listener?) { this.listener = listener }

    fun render(state: VideoPlaybackState, hasMedia: Boolean = state.media != null) {
        val status = state.status
        playbackAction.visibility = if (hasMedia) VISIBLE else GONE
        playbackAction.setImageResource(if (status.isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play)
        playbackAction.contentDescription = context.getString(status.playbackActionDescription())
        renderProgress(state.progress)
    }

    fun renderProgress(progress: VideoPlaybackProgress) = progressControl.renderProgress(progress)

    fun renderSeekPreview(positionMs: Long, durationMs: Long) =
        progressControl.renderSeekPreview(positionMs, durationMs)

    fun endSeekPreview() = progressControl.endSeekPreview()

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

    private fun actionLayoutParams() = LayoutParams(context.dp(ACTION_SIZE_DP), context.dp(ACTION_SIZE_DP))

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val CONTROL_HEIGHT_DP = 56
        const val ACTION_SIZE_DP = 40
        const val CONTROL_BACKGROUND_COLOR = 0x55000000
        const val ACTION_BACKGROUND_COLOR = 0x33000000
    }

    interface Listener {
        fun onTogglePlayback()
        fun onSeek(positionMs: Long)
    }
}
