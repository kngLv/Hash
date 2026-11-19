package com.hash.video.ui.panel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import com.hash.video.R
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import com.hash.video.ui.bottombar.EmbeddedVideoBottomBar
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.interaction.VideoInteractionIntent
import kotlin.math.roundToInt

/** 详情等普通页面的可见控制面板：加载/错误提示、中央播放按钮和底部操作栏。 */
open class EmbeddedVideoControlPanel(context: Context) : AbstractVideoControlPanel(context) {

    protected val loadingView = ProgressBar(context).apply {
        id = R.id.video_loading
        visibility = GONE
        isIndeterminate = true
    }
    protected val errorMessageView = createErrorMessageView().apply {
        id = R.id.video_error_message
    }
    protected val centerPlayAction =
        createIconAction(R.drawable.ic_video_play, CENTER_ACTION_SIZE_DP).apply {
            id = R.id.video_play_action
        }
    protected val bottomBar = EmbeddedVideoBottomBar(context)
    /** 底栏右侧的横屏入口；业务层可配置点击与样式。 */
    val landscapeAction
        get() = bottomBar.landscapeAction
    /** 底栏右侧的竖屏入口；业务层可配置点击与样式。 */
    val portraitAction
        get() = bottomBar.portraitAction

    init {
        addView(
            loadingView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        addView(
            errorMessageView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        )
        addView(
            centerPlayAction,
            LayoutParams(
                context.dp(CENTER_ACTION_SIZE_DP),
                context.dp(CENTER_ACTION_SIZE_DP),
                Gravity.CENTER
            ),
        )
        addView(
            bottomBar,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(CONTROL_HEIGHT_DP), Gravity.BOTTOM),
        )
        centerPlayAction.setOnClickListener { dispatchIntent(VideoInteractionIntent.TogglePlayback) }
        errorMessageView.setOnClickListener { dispatchIntent(VideoInteractionIntent.TogglePlayback) }
        bottomBar.setListener(object : EmbeddedVideoBottomBar.Listener {
            override fun onTogglePlayback() = dispatchIntent(VideoInteractionIntent.TogglePlayback)
            override fun onSeek(positionMs: Long) = dispatchIntent(VideoInteractionIntent.SeekTo(positionMs))
        })
    }

    override fun toggleControls() {
        bottomBar.visibility = if (bottomBar.visibility == VISIBLE) GONE else VISIBLE
    }

    override fun render(state: VideoPlaybackState, hasMedia: Boolean) {
        val status = state.status
        val isLoading = status.isLoading
        val isPlaying = status.isPlaying
        val errorMessage = state.errorMessage.takeIf { status == VideoPlaybackStatus.ERROR }
        loadingView.visibility = if (isLoading) VISIBLE else GONE
        errorMessageView.visibility = if (errorMessage != null) VISIBLE else GONE
        errorMessageView.text = errorMessage.orEmpty()
        errorMessageView.contentDescription = errorMessage?.let { "$it，点击重试" }
        centerPlayAction.visibility =
            if (hasMedia && !isLoading && !isPlaying && errorMessage == null) VISIBLE else GONE
        centerPlayAction.contentDescription = context.getString(
            if (status.isPlaying) R.string.video_pause else R.string.video_play,
        )
        bottomBar.render(state, hasMedia)
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

    private fun createIconAction(iconRes: Int, sizeDp: Int): ImageButton =
        ImageButton(context).apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(ACTION_BACKGROUND_COLOR)
            }
            setPadding(
                context.dp(sizeDp / 4),
                context.dp(sizeDp / 4),
                context.dp(sizeDp / 4),
                context.dp(sizeDp / 4)
            )
            isClickable = true
            isFocusable = true
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
        setPadding(
            context.dp(ERROR_HORIZONTAL_PADDING_DP),
            context.dp(ERROR_VERTICAL_PADDING_DP),
            context.dp(ERROR_HORIZONTAL_PADDING_DP),
            context.dp(ERROR_VERTICAL_PADDING_DP)
        )
        isClickable = true
        isFocusable = true
    }

    private fun Context.dp(value: Int): Int =
        (value * resources.displayMetrics.density).roundToInt()

    private companion object {
        const val CENTER_ACTION_SIZE_DP = 64
        const val CONTROL_HEIGHT_DP = 56
        const val ACTION_BACKGROUND_COLOR = 0x66000000
        const val ERROR_BACKGROUND_COLOR = 0xAA000000.toInt()
        const val ERROR_CORNER_RADIUS_DP = 8
        const val ERROR_HORIZONTAL_PADDING_DP = 16
        const val ERROR_VERTICAL_PADDING_DP = 10
    }
}
