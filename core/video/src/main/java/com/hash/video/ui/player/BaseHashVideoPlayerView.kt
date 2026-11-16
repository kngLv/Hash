package com.hash.video.ui.player

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isPlaying
import com.hash.video.api.isSameSourceAs
import com.hash.video.player.VideoController
import com.hash.video.player.VideoPlaybackOwner
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureDirection
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.gesture.VideoTouchHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 视频播放界面的抽象基类。
 *
 * 基类提供媒体绑定、播放控制、生命周期登记与播放状态分发；具体的播放、Loading、全屏或悬浮
 * 控件由子类自行实现。当前 View 本身就是视频画面容器；唯一 `PlayerView` 仅能由控制器挂载，
 * 子类不得直接操作它。
 */
@OptIn(UnstableApi::class)
abstract class BaseHashVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /* ----------- 基础依赖与状态 ----------- */

    private val controller = VideoController(context)
    private val gestureHandler = Handler(Looper.getMainLooper())
    private val touchHandler = VideoTouchHandler(
        touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
        doubleTapSlopPx = ViewConfiguration.get(context).scaledDoubleTapSlop.toFloat(),
        postDelayed = gestureHandler::postDelayed,
        removeCallbacks = gestureHandler::removeCallbacks,
        onGesture = ::handleVideoGesture,
    )
    private var stateCollectionScope: CoroutineScope? = null
    private var gestureConfig: VideoGestureConfig? = null
    private var lastRenderedPlaybackState: VideoPlaybackState? = null
    private var resolvedDisplaySize: VideoDisplaySize? = null

    /** 覆盖在视频画面之上的透明手势层，位于控制栏和 Loading 等业务 UI 之下。 */
    private val gestureOverlay = FrameLayout(context)

    /** 统一展示封面、旧卡片暂停帧和跨容器迁移帧。 */
    private val posterFrameView = VideoPosterFrameView(context)
    private var displayingTransferredFrame = false

    /** 当前 View 要展示的媒体；设置媒体不会自动播放。 */
    protected var media: VideoMedia? = null
        private set

    /** 当前真实播放状态，供具体场景处理播放 / 暂停交互。 */
    protected val currentPlaybackState: VideoPlaybackState
        get() = controller.playbackState.value

    /** 列表等前台-only 场景可关闭系统媒体通知；详情默认开启。 */
    var showNotification: Boolean
        get() = controller.showNotification
        set(value) {
            controller.showNotification = value
        }

    /** 是否将视频封面作为系统媒体通知 artwork。 */
    var showNotificationArtwork: Boolean
        get() = controller.showNotificationArtwork
        set(value) {
            controller.showNotificationArtwork = value
        }

    /** 多个播放器 View 可注入同一页面 Owner，共享全局播放会话的释放权限。 */
    var playbackOwner: VideoPlaybackOwner
        get() = controller.playbackOwner
        set(value) {
            controller.playbackOwner = value
        }

    /* ----------- 初始化相关 ----------- */

    init {
        setBackgroundColor(Color.BLACK)
        addView(posterFrameView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(gestureOverlay, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        isClickable = true
        bindVideoTouchHandler()
    }

    /* ----------- 手势事件相关 ----------- */

    /** 返回当前视频画面启用的手势配置；默认关闭，由具体播放器按场景重写。 */
    protected open fun createGestureConfig(): VideoGestureConfig = VideoGestureConfig()

    /** 使手势配置在下次触摸时重新创建，供依赖可变状态的子类刷新开关。 */
    protected fun invalidateGestureConfig() {
        gestureConfig = null
    }

    /**
     * 触摸事件由 [VideoTouchHandler] 识别，确认单击后会回调本 View 的 [performClick]。
     * Lint 无法追踪该间接调用，因此只在此绑定点抑制误报。
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun bindVideoTouchHandler() {
        gestureOverlay.setOnTouchListener { _, event ->
            val direction = touchHandler.handleTouchEvent(
                event = event.toVideoTouchEvent(),
                config = currentGestureConfig(),
                progress = currentPlaybackState.progress,
            )
            coordinateParentTouchInterception(event.actionMasked, direction)
            true
        }
    }

    /**
     * 播放器嵌在可纵向滚动的列表中：按下时先保留事件，方向明确后才把纵向滑动交回父列表。
     * 横向拖动因此不会因轻微上下偏移被 `RecyclerView` 提前拦截并触发取消。
     */
    private fun coordinateParentTouchInterception(action: Int, direction: VideoGestureDirection) {
        val shouldDisallowIntercept = when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE,
                -> direction != VideoGestureDirection.VERTICAL

            else -> false
        }
        parent?.requestDisallowInterceptTouchEvent(shouldDisallowIntercept)
    }

    /** 触摸过程中复用配置，避免高频 MOVE 事件反复创建对象。 */
    private fun currentGestureConfig(): VideoGestureConfig =
        gestureConfig ?: createGestureConfig().also {
            gestureConfig = it
        }

    /** 单击统一走 [performClick]；其余手势先通知子类更新 UI，再执行基类的默认播放命令。 */
    private fun handleVideoGesture(event: VideoGestureEvent) {
        if (event == VideoGestureEvent.Click) {
            performClick()
            return
        }
        dispatchVideoGesture(event)
    }

    /** 普通单击通过标准点击语义分发控制栏显隐，同时保留无障碍点击事件。 */
    override fun performClick(): Boolean {
        super.performClick()
        dispatchVideoGesture(VideoGestureEvent.Click)
        return true
    }

    /** UI 通知与默认播放命令分离：子类处理展示，基类统一处理播放意图。 */
    private fun dispatchVideoGesture(event: VideoGestureEvent) {
        onVideoGesture(event)

        when (event) {
            VideoGestureEvent.DoubleClick -> {
                if (currentPlaybackState.status.isPlaying) pause() else play()
            }

            is VideoGestureEvent.SeekPreviewEnd -> {
                handleUserSeek(event.positionMs, currentPlaybackState.progress.durationMs)
            }

            else -> Unit
        }
    }

    /** 基类默认手势处理完成后通知子类，用于更新控件、动画等附加 UI。 */
    protected open fun onVideoGesture(event: VideoGestureEvent) = Unit

    /**
     * 处理用户正常结束进度预览后的跳转请求。
     *
     * 默认直接跳转；内嵌、列表等场景可重写以保护结尾位置、切换下一条或循环播放。UI 层的预览结束
     * 通知仍由 [onVideoGesture] 统一接收，因此子类无需拦截完整手势事件。
     */
    protected open fun handleUserSeek(positionMs: Long, durationMs: Long) {
        seekTo(positionMs)
    }

    private fun MotionEvent.toVideoTouchEvent() = VideoTouchHandler.TouchEvent(
        action = when (actionMasked) {
            MotionEvent.ACTION_DOWN -> VideoTouchHandler.Action.DOWN
            MotionEvent.ACTION_MOVE -> VideoTouchHandler.Action.MOVE
            MotionEvent.ACTION_UP -> VideoTouchHandler.Action.UP
            MotionEvent.ACTION_POINTER_DOWN -> VideoTouchHandler.Action.POINTER_DOWN
            else -> VideoTouchHandler.Action.CANCEL
        },
        x = x,
        y = y,
        pointerCount = pointerCount,
        eventTimeMs = eventTime,
        viewWidth = width,
        viewHeight = height,
    )

    /* ----------- 媒体与播放控制相关 ----------- */

    /** 绑定当前 View 要展示的视频，不会自动开始播放。 */
    fun setMedia(media: VideoMedia) {
        if (this.media == media) return
        clearTransferredFrame()
        lastRenderedPlaybackState = null
        resolvedDisplaySize = null
        this.media = media
        onMediaChanged(media)
        renderPlaybackState(currentPlaybackState)
        resolveDisplaySizeIfNeeded()
        restoreRenderIfCurrentMedia()
    }

    /** 媒体绑定变化时通知子类刷新封面、比例等与业务媒体关联的展示。 */
    protected open fun onMediaChanged(media: VideoMedia) = Unit

    /** 请求播放已绑定的视频；控制器会自行决定切换、恢复、重播或忽略重复请求。 */
    open fun play() {
        media?.let {
            attachRender()
            controller.play(it)
        }
    }

    /** 绑定并立即请求播放指定视频。 */
    fun play(media: VideoMedia) {
        setMedia(media)
        play()
    }

    /** 暂停当前视频；界面状态以真实播放器回调为准。 */
    fun pause() = controller.pause()

    /** 跳转当前视频到指定位置；未知时长的媒体会忽略该请求。 */
    fun seekTo(positionMs: Long) = controller.seekTo(positionMs)

    /** 停止当前视频的播放与加载，但保留播放列表，后续仍可重新准备并播放。 */
    fun stop() = controller.stop()

    /** 停止并移除当前视频，使其不能再由系统媒体控制恢复。 */
    fun removeCurrentMedia() = controller.removeCurrentMedia()

    /** 更新详情播放通知的点击目标。 */
    fun setNotificationActivity(pendingIntent: PendingIntent?) =
        controller.setNotificationActivity(pendingIntent)

    /** 解除当前 View 的画面承载，但不停止播放器。 */
    fun detachRender(): Boolean = controller.detachRenderFrom(this)

    /** 页面真正结束播放会话时释放应用级播放器。 */
    fun releasePlayback() = controller.releasePlayback()

    /** 当前媒体此前切走时保留的缩略暂停帧；没有时由子类回退到封面。 */
    protected fun pausedFrameFor(media: VideoMedia) = controller.pausedFrameFor(media)

    /** 绑定新媒体的封面；封面与暂停帧始终复用同一展示 View。 */
    protected fun setPosterCover(media: VideoMedia) = posterFrameView.setCover(media.coverUrl)

    /** 当前卡片不再承载真实画面时，展示之前缓存的暂停帧。 */
    protected fun showPosterFrame(frame: Bitmap) = posterFrameView.showFrozenFrame(frame)

    /** 当前卡片没有暂停帧可用时，显示它自己的封面。 */
    protected fun showPosterCover() = posterFrameView.showCover()

    /** 当前真实视频画面可用时隐藏封面或暂停帧。 */
    protected fun hidePoster() = posterFrameView.hide()


    /**
     * 根据播放器所在容器和业务传入的展示宽高，计算当前媒体首次展示时使用的固定尺寸。
     *
     * 默认让宽度填满容器，高度按传入比例计算；未传有效宽高时使用 16:9 兜底。
     */
    protected open fun resolveDisplaySize(
        containerWidth: Int,
        containerHeight: Int,
        media: VideoMedia,
    ): VideoDisplaySize {
        val aspectRatio = media.coverAspectRatioOrNull() ?: DEFAULT_DISPLAY_ASPECT_RATIO
        return VideoDisplaySize(
            width = containerWidth,
            height = (containerWidth / aspectRatio).roundToInt().coerceAtLeast(1),
        )
    }

    /** 默认把计算结果应用到播放器根 View；全屏播放器可覆盖并交给父容器控制。 */
    protected open fun applyDisplaySize(size: VideoDisplaySize) {
        val params = layoutParams ?: return
        if (params.width == size.width && params.height == size.height) return
        layoutParams = params.apply {
            width = size.width
            height = size.height
        }
    }

    /** 业务明确要求按当前容器尺寸重新计算时调用；普通尺寸变化不会自动触发。 */
    fun refreshDisplaySize() {
        resolvedDisplaySize = null
        resolveDisplaySizeIfNeeded()
    }

    /** 首次获得有效容器宽度时确定尺寸；缓存存在后即使方向变化也不再自动重算。 */
    private fun resolveDisplaySizeIfNeeded() {
        if (resolvedDisplaySize != null) return
        val boundMedia = media ?: return
        if (width <= 0 || height < 0) return
        resolveDisplaySize(width, height, boundMedia).also {
            resolvedDisplaySize = it
            applyDisplaySize(it)
        }
    }

    /* ----------- 画面迁移与生命周期相关 ----------- */

    /**
     * 让当前播放器 View 接管正在播放的视频画面，不改变媒体和播放状态。
     *
     * 从业务侧的全屏或小窗容器返回内嵌页面时调用即可，无需通过 [play] 间接恢复画面。
     */
    fun attachRender() {
        controller.attachRenderTo(this, media)
        showTransferredFrameIfNeeded()
    }

    /**
     * 请求当前 View 接管视频画面。
     *
     * 全屏页、小窗或列表项成为当前播放项时调用；该操作不会重新播放视频。
     */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lastRenderedPlaybackState = null
        observePlaybackState()
        restoreRenderIfCurrentMedia()
        resolveDisplaySizeIfNeeded()
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        super.onSizeChanged(width, height, oldWidth, oldHeight)
        resolveDisplaySizeIfNeeded()
    }

    override fun onDetachedFromWindow() {
        gestureConfig?.let(touchHandler::cancel)
        stateCollectionScope?.cancel()
        stateCollectionScope = null
        detachRender()
        super.onDetachedFromWindow()
    }

    /* ----------- 播放状态观察相关 ----------- */

    private fun observePlaybackState() {
        stateCollectionScope?.cancel()
        stateCollectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        stateCollectionScope?.launch {
            controller.playbackState.collect(::renderPlaybackState)
        }
    }

    /** 供同模块界面测试直接验证状态到 UI 的映射。 */
    internal fun renderPlaybackState(state: VideoPlaybackState) {
        val displayedState = if (media?.isSameSourceAs(state.media) == true) {
            state
        } else {
            VideoPlaybackState(status = VideoPlaybackStatus.IDLE)
        }
        if (displayedState == lastRenderedPlaybackState) return
        lastRenderedPlaybackState = displayedState
        updateTransferredFrame(displayedState)
        onPlaybackStateChanged(displayedState)
    }

    /** 仅在迁移后的暂停或结束状态保留快照；恢复播放后立即交还真实渲染画面。 */
    private fun updateTransferredFrame(state: VideoPlaybackState) {
        if (state.status == VideoPlaybackStatus.PLAYING) {
            clearTransferredFrame()
        }
    }

    /** 迁移到新容器后，暂停或播完的视频优先显示之前保存的画面，防止黑屏。 */
    private fun showTransferredFrameIfNeeded() {
        val boundMedia = media ?: return
        val state = currentPlaybackState
        if (
            boundMedia.isSameSourceAs(state.media) &&
            (state.status == VideoPlaybackStatus.PAUSED || state.status == VideoPlaybackStatus.ENDED)
        ) {
            pausedFrameFor(boundMedia)?.let(::showTransferredFrame)
        }
    }

    /** 将保存的画面盖在真实播放器上方，直到视频恢复播放。 */
    private fun showTransferredFrame(frame: Bitmap) {
        showPosterFrame(frame)
        displayingTransferredFrame = true
    }

    /** 视频恢复播放后移除静态画面，重新显示真实视频。 */
    private fun clearTransferredFrame() {
        if (!displayingTransferredFrame) return
        posterFrameView.hideFrozenFrame()
        displayingTransferredFrame = false
    }


    /** 子类只负责把已筛选的当前媒体播放状态渲染为自己的 UI。 */
    protected abstract fun onPlaybackStateChanged(state: VideoPlaybackState)


    /**
     * View 因重建或列表复用重新绑定到当前播放媒体时，恢复它原本的画面输出。
     *
     * 首次绑定不会满足条件，仍必须由 [play] 主动激活。若已有其他容器承载画面，不能抢占。
     */
    private fun restoreRenderIfCurrentMedia() {
        val state = currentPlaybackState
        if (
            !isAttachedToWindow ||
            controller.hasAttachedRender() ||
            media?.isSameSourceAs(state.media) != true
        ) {
            return
        }
        attachRender()
    }

    private companion object {
        const val DEFAULT_DISPLAY_ASPECT_RATIO = 16f / 9f
    }

}
