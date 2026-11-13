package com.hash.video.ui.player

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.hash.video.R
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying
import com.hash.video.api.isSameSourceAs
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

    override fun createGestureConfig() = VideoGestureConfig(
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
    private val coverView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        setBackgroundColor(Color.BLACK)
        isClickable = false
        isFocusable = false
    }
    private var coverAspectRatio = DEFAULT_COVER_ASPECT_RATIO
    private var displayingPausedFrame = false

    /** 宿主负责横屏、竖屏、小窗等呈现策略；未设置时不展示全屏入口。 */
    var onFullscreenRequested: (() -> Unit)? = null
        set(value) {
            field = value
            controls.onFullscreenRequested = value
        }

    /** 宿主负责展示竖屏沉浸播放；未设置时不展示竖屏入口。 */
    var onPortraitRequested: (() -> Unit)? = null
        set(value) {
            field = value
            controls.onPortraitRequested = value
        }

    /** 宿主负责将当前画面迁移到悬浮容器；未设置时不展示悬浮入口。 */
    var onFloatRequested: (() -> Unit)? = null
        set(value) {
            field = value
            controls.onFloatRequested = value
        }

    /** 用户请求播放前通知宿主，用于准备通知跳转目标等页面级状态。 */
    var onPlaybackRequested: (() -> Unit)? = null

    init {
        setBackgroundColor(Color.BLACK)
        addContentOverlay(coverView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
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

    override fun onMediaChanged(media: VideoMedia) {
        displayingPausedFrame = false
        loadCover(media)
        updateCoverAspectRatio(media.coverAspectRatioOrNull() ?: DEFAULT_COVER_ASPECT_RATIO)
    }

    override fun play() {
        onPlaybackRequested?.invoke()
        super.play()
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
        renderCover(state)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        applyCoverHeight()
    }

    override fun onDetachedFromWindow() {
        (loadingView.indeterminateDrawable as? Animatable)?.stop()
        super.onDetachedFromWindow()
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

    /** 非当前播放项展示暂停帧或封面；当前项保留真实暂停画面，不被封面覆盖。 */
    private fun renderCover(state: VideoPlaybackState) {
        val boundMedia = media ?: return
        if (!boundMedia.isSameSourceAs(state.media)) {
            pausedFrameFor(boundMedia)?.let { frame ->
                if (!displayingPausedFrame) {
                    Glide.with(coverView).clear(coverView)
                    coverView.setImageBitmap(frame)
                    displayingPausedFrame = true
                }
            }
            coverView.visibility = VISIBLE
            return
        }
        displayingPausedFrame = false
        coverView.visibility = when (state.status) {
            VideoPlaybackStatus.PLAYING,
            VideoPlaybackStatus.PAUSED,
            VideoPlaybackStatus.ENDED,
                -> GONE

            else -> VISIBLE
        }
    }

    /** 封面比例是内嵌容器唯一尺寸来源，不再使用媒体流解析出的实际视频尺寸。 */
    private fun updateCoverAspectRatio(aspectRatio: Float) {
        coverAspectRatio = aspectRatio.takeIf { it > 0f } ?: DEFAULT_COVER_ASPECT_RATIO
        applyCoverHeight()
    }

    private fun applyCoverHeight() {
        if (width <= 0) return
        val params = layoutParams ?: return
        val targetHeight = (width / coverAspectRatio).roundToInt().coerceAtLeast(1)
        if (params.height != targetHeight) {
            layoutParams = params.apply { height = targetHeight }
        }
    }

    private fun loadCover(media: VideoMedia) {
        val coverUrl = media.coverUrl
        if (coverUrl.isNullOrBlank()) {
            Glide.with(coverView).clear(coverView)
            coverView.setImageDrawable(null)
            return
        }
        Glide.with(coverView)
            .load(coverUrl)
            .into(coverView)
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
        const val DEFAULT_COVER_ASPECT_RATIO = 16f / 9f
    }
}
