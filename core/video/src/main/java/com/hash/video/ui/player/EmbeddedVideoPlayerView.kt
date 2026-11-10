package com.hash.video.ui.player

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.R
import com.hash.video.api.VideoContentSize
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import com.hash.video.ui.controls.EmbeddedVideoControls
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import kotlin.math.roundToInt

/** 详情页等普通页面使用的嵌入式播放器，承载播放、Loading、全屏和悬浮操作 UI。 */
@OptIn(UnstableApi::class)
open class EmbeddedVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseHashVideoPlayerView(context, attrs, defStyleAttr) {

    override val gestureConfig = VideoGestureConfig(
        singleTapEnabled = true,
        doubleTapEnabled = true,
        seekEnabled = true,
    )

    private val loadingView = ProgressBar(context).apply {
        id = R.id.video_loading
        visibility = GONE
        isIndeterminate = true
    }
    private val errorMessageView = createErrorMessageView().apply {
        id = R.id.video_error_message
    }
    private val centerPlayAction = createIconAction(R.drawable.ic_video_play, CENTER_ACTION_SIZE_DP).apply {
        id = R.id.video_play_action
    }
    private val controls = EmbeddedVideoControls(context)
    private var defaultHeight: Int? = null
    private var renderedContentSize: VideoContentSize? = null

    /** 宿主负责将当前画面迁移到横屏全屏容器；未设置时不展示全屏入口。 */
    var onFullscreenRequested: (() -> Unit)? = null
        set(value) {
            field = value
            controls.onFullscreenRequested = value
        }

    /** 宿主负责将当前画面迁移到悬浮容器；未设置时不展示悬浮入口。 */
    var onFloatRequested: (() -> Unit)? = null
        set(value) {
            field = value
            controls.onFloatRequested = value
        }

    init {
        setBackgroundColor(Color.BLACK)
        addView(loadingView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(
            errorMessageView,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        addView(
            centerPlayAction,
            LayoutParams(context.dp(CENTER_ACTION_SIZE_DP), context.dp(CENTER_ACTION_SIZE_DP), Gravity.CENTER),
        )
        addView(
            controls,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(CONTROL_HEIGHT_DP), Gravity.BOTTOM),
        )
        centerPlayAction.setOnClickListener { play() }
        errorMessageView.setOnClickListener { play() }
        controls.onPlaybackToggle = ::togglePlayback
        controls.onSeekRequested = { positionMs ->
            handleUserSeek(positionMs, currentPlaybackState.progress.durationMs)
        }
    }

    override fun onPlaybackStateChanged(state: VideoPlaybackState) {
        val status = state.status
        val hasMedia = media != null
        val isLoading = status.isLoading
        val isPlaying = status.isPlaying
        val errorMessage = state.errorMessage.takeIf { status == VideoPlaybackStatus.ERROR }
        loadingView.visibility = if (isLoading) VISIBLE else GONE
        errorMessageView.visibility = if (errorMessage != null) VISIBLE else GONE
        errorMessageView.text = errorMessage.orEmpty()
        errorMessageView.contentDescription = errorMessage?.let { "$it，点击重试" }
        centerPlayAction.visibility = if (hasMedia && !isLoading && !isPlaying && errorMessage == null) VISIBLE else GONE
        centerPlayAction.contentDescription = context.getString(
            if (status.isPlaying) R.string.video_pause else R.string.video_play,
        )
        controls.render(state, hasMedia = media != null)
        renderContentSize(state.contentSize)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        renderedContentSize?.let(::applyContentHeight)
    }

    private fun togglePlayback() {
        val currentMedia = media ?: return
        val state = currentPlaybackState
        if (state.media == currentMedia && state.status.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    override fun onVideoGesture(event: VideoGestureEvent) {
        when (event) {
            VideoGestureEvent.Click -> controls.visibility =
                if (controls.visibility == VISIBLE) GONE else VISIBLE

            is VideoGestureEvent.SeekPreview -> {
                controls.visibility = VISIBLE
                controls.renderSeekPreview(event.positionMs, event.durationMs)
            }

            is VideoGestureEvent.SeekPreviewEnd -> controls.endSeekPreview()

            VideoGestureEvent.SeekPreviewCancel -> controls.cancelSeekPreview()

            else -> Unit
        }
    }

//    /**
//     * 处理控制栏和画面手势发起的跳转。
//     *
//     * 详情页默认避免直接落到末尾；播放列表或短视频子类可重写以切换下一条或循环播放。
//     */
//    protected open override fun handleUserSeek(positionMs: Long, durationMs: Long) {
//        if (durationMs <= 0L) return
//        val targetPositionMs = if (durationMs > END_SEEK_GUARD_MS) {
//            positionMs.coerceAtMost(durationMs - END_SEEK_GUARD_MS)
//        } else {
//            positionMs
//        }
//        seekTo(targetPositionMs)
//    }

    private fun renderContentSize(contentSize: VideoContentSize?) {
        if (renderedContentSize == contentSize) return

        renderedContentSize = contentSize
        if (contentSize == null) {
            restoreDefaultHeight()
        } else {
            applyContentHeight(contentSize)
        }
    }

    /** 真实尺寸到达后，按当前可用宽度计算高度，保证视频画面完整铺满横向空间而不发生形变。 */
    private fun applyContentHeight(contentSize: VideoContentSize) {
        if (width <= 0 || contentSize.aspectRatio <= 0f) return

        val params = layoutParams ?: return
        defaultHeight = defaultHeight ?: params.height.takeIf { it > 0 }
        val targetHeight = (width / contentSize.aspectRatio).roundToInt().coerceAtLeast(1)
        if (params.height != targetHeight) {
            layoutParams = params.apply { height = targetHeight }
        }
    }

    /** 新媒体尚未解析出尺寸时恢复 XML 指定的默认高度，避免沿用上一条视频的比例。 */
    private fun restoreDefaultHeight() {
        val targetHeight = defaultHeight ?: return
        val params = layoutParams ?: return
        if (params.height != targetHeight) {
            layoutParams = params.apply { height = targetHeight }
        }
    }

    private fun createIconAction(iconRes: Int, sizeDp: Int): ImageButton = ImageButton(context).apply {
        setImageResource(iconRes)
        imageTintList = ColorStateList.valueOf(Color.WHITE)
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ACTION_BACKGROUND_COLOR)
        }
        setPadding(context.dp(sizeDp / 4), context.dp(sizeDp / 4), context.dp(sizeDp / 4), context.dp(sizeDp / 4))
        isClickable = true
        isFocusable = true
    }

    /** 播放失败时展示具体原因；点击后由当前播放器重新发起播放。 */
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
            context.dp(ERROR_VERTICAL_PADDING_DP),
        )
        isClickable = true
        isFocusable = true
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val CENTER_ACTION_SIZE_DP = 64
        const val CONTROL_HEIGHT_DP = 56
        const val ACTION_BACKGROUND_COLOR = 0x66000000
        const val ERROR_BACKGROUND_COLOR = 0xAA000000.toInt()
        const val ERROR_CORNER_RADIUS_DP = 8
        const val ERROR_HORIZONTAL_PADDING_DP = 16
        const val ERROR_VERTICAL_PADDING_DP = 10
        const val END_SEEK_GUARD_MS = 2_000L
    }
}
