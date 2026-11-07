package com.hash.video.ui.player

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ProgressBar
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.R
import com.hash.video.api.VideoContentSize
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import com.hash.video.ui.controls.EmbeddedVideoControls
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import kotlin.math.roundToInt

/** 详情页等普通页面使用的嵌入式播放器，承载播放、Loading、全屏和悬浮操作 UI。 */
@OptIn(UnstableApi::class)
class EmbeddedVideoPlayerView @JvmOverloads constructor(
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
            centerPlayAction,
            LayoutParams(context.dp(CENTER_ACTION_SIZE_DP), context.dp(CENTER_ACTION_SIZE_DP), Gravity.CENTER),
        )
        addView(
            controls,
            LayoutParams(LayoutParams.MATCH_PARENT, context.dp(CONTROL_HEIGHT_DP), Gravity.BOTTOM),
        )
        centerPlayAction.setOnClickListener { play() }
        controls.onPlaybackToggle = ::togglePlayback
        controls.onSeekTo = ::seekTo
    }

    override fun onPlaybackStateChanged(state: VideoPlaybackState) {
        val status = state.status
        val hasMedia = media != null
        val isLoading = status.isLoading
        val isPlaying = status.isPlaying
        loadingView.visibility = if (isLoading) VISIBLE else GONE
        centerPlayAction.visibility = if (hasMedia && !isLoading && !isPlaying) VISIBLE else GONE
        centerPlayAction.contentDescription = context.getString(
            if (status.isPlaying) R.string.video_pause else R.string.video_play,
        )
        controls.render(state)
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
            VideoGestureEvent.ToggleControls -> controls.visibility =
                if (controls.visibility == VISIBLE) GONE else VISIBLE

            is VideoGestureEvent.SeekPreview -> {
                controls.visibility = VISIBLE
                controls.renderSeekPreview(event.positionMs, event.durationMs)
            }

            else -> Unit
        }
    }

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

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val CENTER_ACTION_SIZE_DP = 64
        const val CONTROL_HEIGHT_DP = 56
        const val ACTION_BACKGROUND_COLOR = 0x66000000
    }
}
