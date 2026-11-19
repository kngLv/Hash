package com.hash.video.ui.panel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.hash.video.R
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isLoading
import com.hash.video.ui.bottombar.FullscreenVideoBottomBar
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.interaction.VideoInteractionIntent
import kotlin.math.roundToInt

/** 横屏全屏播放的可见控制面板：加载、错误提示与三层底部操作栏。 */
open class FullscreenVideoControlPanel(context: Context) : AbstractVideoControlPanel(context) {

    protected val loadingView = ProgressBar(context).apply {
        visibility = GONE
        isIndeterminate = true
    }
    protected val errorMessageView = createErrorMessageView().apply {
        id = R.id.video_error_message
    }
    protected val bottomBar = FullscreenVideoBottomBar(context)
    /** 业务层可按需显示并配置的退出入口；默认隐藏，不绑定业务行为。 */
    val exitAction = createIconAction(R.drawable.ic_video_exit).apply {
        contentDescription = context.getString(R.string.video_exit)
        visibility = GONE
    }

    init {
        addView(loadingView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(errorMessageView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(bottomBar, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.BOTTOM))
        addView(
            exitAction,
            LayoutParams(context.dp(ACTION_SIZE_DP), context.dp(ACTION_SIZE_DP), Gravity.TOP or Gravity.START).apply {
                leftMargin = context.dp(16)
                topMargin = context.dp(16)
            },
        )
        errorMessageView.setOnClickListener { dispatchIntent(VideoInteractionIntent.TogglePlayback) }
        bottomBar.setListener(object : FullscreenVideoBottomBar.Listener {
            override fun onTogglePlayback() = dispatchIntent(VideoInteractionIntent.TogglePlayback)
            override fun onSeek(positionMs: Long) = dispatchIntent(VideoInteractionIntent.SeekTo(positionMs))
        })
    }

    override fun toggleControls() {
        bottomBar.visibility = if (bottomBar.visibility == VISIBLE) GONE else VISIBLE
    }

    override fun render(state: VideoPlaybackState, hasMedia: Boolean) {
        loadingView.visibility = if (state.status.isLoading) VISIBLE else GONE
        val errorMessage = state.errorMessage.takeIf { state.status == VideoPlaybackStatus.ERROR }
        errorMessageView.visibility = if (errorMessage != null) VISIBLE else GONE
        errorMessageView.text = errorMessage.orEmpty()
        errorMessageView.contentDescription = errorMessage?.let { "$it，点击重试" }
        bottomBar.render(state)
    }

    override fun onGesture(event: VideoGestureEvent) {
        when (event) {
            is VideoGestureEvent.SeekPreview -> {
                bottomBar.visibility = VISIBLE
                bottomBar.renderSeekPreview(event.positionMs, event.durationMs)
            }

            is VideoGestureEvent.SeekPreviewEnd -> bottomBar.endSeekPreview()
            VideoGestureEvent.SeekPreviewCancel -> bottomBar.cancelSeekPreview()
            else -> Unit
        }
    }

    override fun onDetachedFromWindow() {
        (loadingView.indeterminateDrawable as? android.graphics.drawable.Animatable)?.stop()
        super.onDetachedFromWindow()
    }

    private fun createErrorMessageView(): TextView = TextView(context).apply {
        visibility = GONE
        setTextColor(Color.WHITE)
        textSize = 14f
        gravity = Gravity.CENTER
        background = GradientDrawable().apply {
            cornerRadius = context.dp(ERROR_CORNER_RADIUS_DP).toFloat()
            setColor(ERROR_BACKGROUND_COLOR)
        }
        setPadding(context.dp(ERROR_HORIZONTAL_PADDING_DP), context.dp(ERROR_VERTICAL_PADDING_DP), context.dp(ERROR_HORIZONTAL_PADDING_DP), context.dp(ERROR_VERTICAL_PADDING_DP))
        isClickable = true
        isFocusable = true
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

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val ERROR_BACKGROUND_COLOR = 0xAA000000.toInt()
        const val ERROR_CORNER_RADIUS_DP = 8
        const val ERROR_HORIZONTAL_PADDING_DP = 16
        const val ERROR_VERTICAL_PADDING_DP = 10
        const val ACTION_SIZE_DP = 40
        const val ACTION_BACKGROUND_COLOR = 0x66000000
    }
}
