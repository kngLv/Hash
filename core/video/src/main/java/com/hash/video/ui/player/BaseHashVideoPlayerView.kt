package com.hash.video.ui.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackControlToken
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isPlaying
import com.hash.video.api.isSameSourceAs
import com.hash.video.player.VideoController
import com.hash.video.player.isPlayingCurrentMedia
import com.hash.video.ui.interaction.VideoInteractionCoordinator
import com.hash.video.ui.interaction.VideoInteractionIntent
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.panel.AbstractVideoControlPanel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** 视频播放器的基类：管理媒体、画面迁移和交互协调器。具体 UI 由子类提供 Panel。 */
@OptIn(UnstableApi::class)
abstract class BaseHashVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    /* ----------- 基础依赖与状态 ----------- */

    private val controller = VideoController(context)
    private var stateCollectionScope: CoroutineScope? = null
    private var lastRenderedPlaybackState: VideoPlaybackState? = null
    private var resolvedDisplaySize: VideoDisplaySize? = null
    private var interactionCoordinator: VideoInteractionCoordinator? = null

    /** 视频画面不可用时显示封面或静态帧。 */
    private val posterFrameView = VideoPosterFrameView(context)
    private var displayingTransferredFrame = false

    /** 当前 View 要展示的媒体；设置媒体不会自动播放。 */
    protected var media: VideoMedia? = null
        private set

    /** 全局播放器的当前状态。 */
    protected val currentPlaybackState: VideoPlaybackState
        get() = controller.playbackState.value

    /** 当前全局播放器状态；单播放器页面可据此更新选集等 UI。 */
    val playbackState: StateFlow<VideoPlaybackState>
        get() = controller.playbackState

    /** 是否显示系统媒体通知。 */
    var showNotification: Boolean
        get() = controller.showNotification
        set(value) {
            controller.showNotification = value
        }

    /** 是否在系统媒体通知中显示视频封面。 */
    var showNotificationArtwork: Boolean
        get() = controller.showNotificationArtwork
        set(value) {
            controller.showNotificationArtwork = value
        }

    /** 队列页面共享的操作凭证；没有队列的单播放器通常不需要设置。 */
    var playbackToken: VideoPlaybackControlToken
        get() = controller.playbackToken
        set(value) {
            controller.playbackToken = value
        }

    /* ----------- 初始化相关 ----------- */

    init {
        setBackgroundColor(Color.BLACK)
        addView(posterFrameView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        isClickable = true
    }

    /** 安装控制面板，并创建与它配套的手势协调器。 */
    protected fun installControlPanel(
        panel: AbstractVideoControlPanel,
        gestureConfig: VideoGestureConfig,
    ) {
        interactionCoordinator?.let {
            it.release()
            removeView(it)
        }
        interactionCoordinator = VideoInteractionCoordinator(
            context = context,
            panel = panel,
            dispatchIntent = ::handleInteractionIntent,
        ).also { coordinator ->
            coordinator.updateGestureConfig(gestureConfig)
            addView(coordinator, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
    }

    /** 更新当前播放器的手势开关。 */
    protected fun updateGestureConfig(config: VideoGestureConfig) {
        interactionCoordinator?.updateGestureConfig(config)
    }

    /** 只派发播放器点击事件；控制栏显隐由协调器处理。 */
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    /** 处理点击、播放和跳转等通用交互。 */
    protected open fun handleInteractionIntent(intent: VideoInteractionIntent) {
        when (intent) {
            VideoInteractionIntent.PlayerClicked -> performClick()

            VideoInteractionIntent.TogglePlayback -> {
                if (currentPlaybackState.isPlayingCurrentMedia(media)) pause() else play()
            }

            is VideoInteractionIntent.SeekTo -> handleUserSeek(
                intent.positionMs,
                currentPlaybackState.progress.durationMs,
            )

            // 系统悬浮窗专用意图由 FloatingVideoPlayerView 接管。
            VideoInteractionIntent.RestoreFromOverlay,
            VideoInteractionIntent.CloseOverlay,
                -> Unit
        }
    }

    /** 用户确认跳转位置后调用；子类可改为切下一条或循环播放。 */
    protected open fun handleUserSeek(positionMs: Long, durationMs: Long) {
        seekTo(positionMs)
    }

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

    /** 媒体切换后刷新封面等 UI。 */
    protected open fun onMediaChanged(media: VideoMedia) = Unit

    /** 播放当前绑定的视频。 */
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

    /** 页面销毁时释放当前播放器拥有的播放会话。 */
    fun releasePlayback() = controller.releasePlayback()

    /** 仅在绑定媒体为当前播放源时暂停。 */
    fun pause() {
        if (currentPlaybackState.isPlayingCurrentMedia(media)) controller.pause()
    }

    /** 仅在绑定媒体为当前播放源时跳转进度。 */
    fun seekTo(positionMs: Long) {
        if (media?.isSameSourceAs(currentPlaybackState.media) == true) controller.seekTo(positionMs)
    }

    /** 仅停止绑定媒体的当前播放会话，保留媒体项。 */
    fun stop() {
        if (media?.isSameSourceAs(currentPlaybackState.media) == true) controller.stop()
    }

    /** 仅停止并移除绑定媒体的当前媒体项。 */
    fun removeCurrentMedia() {
        if (media?.isSameSourceAs(currentPlaybackState.media) == true) controller.removeCurrentMedia()
    }


    /** 解除当前 View 的画面承载，但不停止播放器。 */
    fun detachRender(): Boolean = controller.detachRenderFrom(this)

    /** 获取该媒体切走前保存的静态帧。 */
    protected fun pausedFrameFor(media: VideoMedia) = controller.pausedFrameFor(media)

    /** 设置当前媒体的封面。 */
    protected fun setPosterCover(media: VideoMedia) = posterFrameView.setCover(media.coverUrl)

    /** 显示缓存的静态帧。 */
    protected fun showPosterFrame(frame: Bitmap) = posterFrameView.showFrozenFrame(frame)

    /** 显示封面。 */
    protected fun showPosterCover() = posterFrameView.showCover()

    /** 显示真实视频画面。 */
    protected fun hidePoster() = posterFrameView.hide()


    /** 默认宽度铺满容器，高度按媒体比例计算；没有比例时使用 16:9。 */
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

    /** 应用计算出的显示尺寸；全屏播放器可交给父容器处理。 */
    protected open fun applyDisplaySize(size: VideoDisplaySize) {
        val params = layoutParams ?: return
        if (params.width == size.width && params.height == size.height) return
        layoutParams = params.apply {
            width = size.width
            height = size.height
        }
    }

    /** 业务主动要求按当前容器重新计算尺寸时调用。 */
    fun refreshDisplaySize() {
        resolvedDisplaySize = null
        resolveDisplaySizeIfNeeded()
    }

    /** 首次拿到容器宽度时确定尺寸，之后保持不变。 */
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

    /** 让当前 View 接管视频画面，不改变播放状态。 */
    fun attachRender() {
        controller.attachRenderTo(this, media)
        showTransferredFrameIfNeeded()
    }

    /** View 回到窗口时，尝试恢复它应承载的视频画面。 */
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
        interactionCoordinator?.release()
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
        interactionCoordinator?.render(displayedState, hasMedia = media != null)
        onPlaybackStateChanged(displayedState)
    }

    /** 恢复播放后去掉静态帧。 */
    private fun updateTransferredFrame(state: VideoPlaybackState) {
        if (state.status == VideoPlaybackStatus.PLAYING) {
            clearTransferredFrame()
        }
    }

    /** 暂停或播完后迁移画面时，优先显示保存的静态帧。 */
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

    /** 用静态帧盖住可能短暂黑屏的真实画面。 */
    private fun showTransferredFrame(frame: Bitmap) {
        showPosterFrame(frame)
        displayingTransferredFrame = true
    }

    /** 播放恢复后移除静态帧。 */
    private fun clearTransferredFrame() {
        if (!displayingTransferredFrame) return
        posterFrameView.hideFrozenFrame()
        displayingTransferredFrame = false
    }


    /** 子类只渲染当前媒体的状态。 */
    protected abstract fun onPlaybackStateChanged(state: VideoPlaybackState)


    /** 重建或复用后，若没有其他容器承载画面，则恢复当前画面。 */
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
