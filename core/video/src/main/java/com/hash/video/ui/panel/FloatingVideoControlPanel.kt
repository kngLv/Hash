package com.hash.video.ui.panel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ImageButton
import com.hash.video.R
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.isPlaying
import com.hash.video.ui.interaction.VideoInteractionIntent

/** 系统悬浮窗的图标控制层：右上关闭、左下播放、右下返回应用。 */
class FloatingVideoControlPanel(context: Context) : AbstractVideoControlPanel(context) {
    internal val play = action(R.drawable.ic_video_play).apply {
        contentDescription = context.getString(R.string.video_play)
    }
    internal val restore = action(R.drawable.ic_video_landscape).apply {
        contentDescription = context.getString(R.string.video_float_restore)
    }
    internal val close = action(R.drawable.ic_video_close).apply {
        contentDescription = context.getString(R.string.video_float_close)
    }

    init {
        setBackgroundColor(CONTROL_SCRIM_COLOR)
        addView(play, buttonLayout(Gravity.BOTTOM or Gravity.START))
        addView(restore, buttonLayout(Gravity.BOTTOM or Gravity.END))
        addView(close, buttonLayout(Gravity.TOP or Gravity.END))
        play.setOnClickListener { dispatchIntent(VideoInteractionIntent.TogglePlayback) }
        restore.setOnClickListener { dispatchIntent(VideoInteractionIntent.RestoreFromOverlay) }
        close.setOnClickListener { dispatchIntent(VideoInteractionIntent.CloseOverlay) }
    }

    override fun render(state: VideoPlaybackState, hasMedia: Boolean) {
        val isPlaying = state.status.isPlaying
        play.setImageResource(if (isPlaying) R.drawable.ic_video_pause else R.drawable.ic_video_play)
        play.contentDescription = context.getString(if (isPlaying) R.string.video_pause else R.string.video_play)
        play.isEnabled = hasMedia
    }

    override fun toggleControls() { visibility = if (visibility == VISIBLE) GONE else VISIBLE }

    /** 显示悬浮窗蒙层和控制图标。 */
    fun showControls() {
        visibility = VISIBLE
    }

    /** 隐藏悬浮窗蒙层和控制图标。 */
    fun hideControls() {
        visibility = GONE
    }

    private fun buttonLayout(gravity: Int) = FrameLayout.LayoutParams(dp(BUTTON_SIZE_DP), dp(BUTTON_SIZE_DP), gravity).apply {
        val margin = dp(BUTTON_MARGIN_DP)
        setMargins(margin, margin, margin, margin)
    }

    private fun action(iconRes: Int) = ImageButton(context).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(BUTTON_BACKGROUND_COLOR)
        }
        val padding = dp(8)
        setPadding(padding, padding, padding, padding)
        isClickable = true
        isFocusable = true
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val BUTTON_SIZE_DP = 36
        const val BUTTON_MARGIN_DP = 6
        const val BUTTON_BACKGROUND_COLOR = 0x66000000
        const val CONTROL_SCRIM_COLOR = 0x55000000
    }
}
