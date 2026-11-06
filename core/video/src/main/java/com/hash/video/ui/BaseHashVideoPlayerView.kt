package com.hash.video.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.player.HashVideo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 视频播放界面的抽象基类。
 *
 * 基类提供媒体绑定、播放控制、生命周期登记与播放状态分发；具体的播放、Loading、全屏或悬浮
 * 控件由子类自行实现。视频画面只由 [VideoPresentationCoordinator] 绑定和迁移，子类不得直接
 * 调用 `PlayerView.setPlayer`。
 */
@OptIn(UnstableApi::class)
abstract class BaseHashVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val controller = HashVideo.controller(context)
    private val presentationCoordinator = HashVideo.presentationCoordinator(context)
    private var stateCollectionScope: CoroutineScope? = null

    /** 播放器画面容器；仅协调器有权为其绑定或迁移 Player。 */
    private val playerView = PlayerView(context).apply {
        useController = false
        setShutterBackgroundColor(Color.BLACK)
    }

    /** 当前 View 要展示的媒体；设置媒体不会自动播放。 */
    protected var media: VideoMedia? = null
        private set

    /** 当前真实播放状态，供具体场景处理播放 / 暂停交互。 */
    protected val currentPlaybackState: VideoPlaybackState
        get() = controller.playbackState.value

    /** 供协调器迁移视频画面使用，业务和子类均不应直接为它绑定 Player。 */
    internal val presentationPlayerView: PlayerView
        get() = playerView

    /** 当前宿主仍在 Window 中时才允许成为视频画面目标。 */
    internal val isPresentationAvailable: Boolean
        get() = isAttachedToWindow

    init {
        addView(playerView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** 绑定当前 View 要展示的视频，不会自动开始播放。 */
    fun setMedia(media: VideoMedia) {
        this.media = media
    }

    /** 请求播放已绑定的视频；控制器会自行决定切换、恢复、重播或忽略重复请求。 */
    fun play() {
        media?.let(controller::play)
    }

    /** 绑定并立即请求播放指定视频。 */
    fun play(media: VideoMedia) {
        setMedia(media)
        play()
    }

    /** 暂停当前视频；界面状态以真实播放器回调为准。 */
    fun pause() = controller.pause()

    /** 停止当前视频的播放与加载，适用于退出详情页等不再需要保留媒体会话的场景。 */
    fun stop() = controller.stop()

    /**
     * 请求当前 View 接管视频画面。
     *
     * 全屏页、小窗或列表项成为当前播放项时调用；该操作不会重新播放视频。
     */
    fun takeOverPresentation() {
        presentationCoordinator.takeOver(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observePlaybackState()
        presentationCoordinator.register(this)
    }

    override fun onDetachedFromWindow() {
        stateCollectionScope?.cancel()
        stateCollectionScope = null
        presentationCoordinator.unregister(this)
        super.onDetachedFromWindow()
    }

    /** 子类只负责把已筛选的当前媒体播放状态渲染为自己的 UI。 */
    protected abstract fun onPlaybackStateChanged(state: VideoPlaybackState)

    /** 供同模块界面测试直接验证状态到 UI 的映射。 */
    internal fun renderPlaybackState(state: VideoPlaybackState) {
        val displayedState = if (media != null && state.media == media) {
            state
        } else {
            VideoPlaybackState(status = VideoPlaybackStatus.IDLE)
        }
        onPlaybackStateChanged(displayedState)
    }

    private fun observePlaybackState() {
        stateCollectionScope?.cancel()
        stateCollectionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        stateCollectionScope?.launch {
            controller.playbackState.collect(::renderPlaybackState)
        }
    }
}
