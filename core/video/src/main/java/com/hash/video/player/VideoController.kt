package com.hash.video.player

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.service.HashVideoPlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 连接 [HashVideoPlaybackService] 中唯一播放会话，并向 UI 提供简化的播放控制。
 *
 * 该类不创建 ExoPlayer；它通过 [MediaController] 远程控制 Service 中由 MediaSession 持有的
 * ExoPlayer。MediaController 的建立是异步的，因此所有实际播放器操作都通过 [withPlayer] 延后执行。
 */
@OptIn(UnstableApi::class)
class VideoController internal constructor(private val context: Context) {

    private val _playbackState = MutableStateFlow(VideoPlaybackState())

    /** 供界面持续观察的播放状态，不直接暴露 Media3 类型。 */
    val playbackState: StateFlow<VideoPlaybackState> = _playbackState.asStateFlow()

    /** 用于定位媒体服务并建立 MediaController 连接的会话凭据。 */
    private val sessionToken = SessionToken(
        context,
        ComponentName(context, HashVideoPlaybackService::class.java),
    )

    /**
     * 异步创建的服务端播放器代理。
     *
     * 连接尚未完成时不能直接取得 Player，因此不能在此处调用 get()。
     */
    private val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

    /** 用于丢弃媒体服务尚未连接时已过期的播放控制请求。 */
    private var latestRequestId = 0L

    init {
        // Future 完成表示已成功连接到媒体服务，或连接过程已经失败。
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess(::attachPlayerListener)
                    .onFailure {
                        updateState(
                            status = VideoPlaybackStatus.ERROR,
                            errorMessage = it.message ?: "播放器连接失败",
                        )
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /**
     * 在 MediaController 已连接后执行播放器操作。
     *
     * [onReady] 总是派发到主线程：Media3 的 Player 调用及其关联 View 的更新需要在同一主线程
     * 串行执行，避免连接回调与界面生命周期发生并发竞争。
     */
    internal fun withPlayer(onReady: (Player) -> Unit) {
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }.onSuccess(onReady)
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    /**
     * 确保指定媒体正在播放。
     *
     * 同一暂停媒体会恢复，同一结束媒体会从头重播；只有不同媒体、空闲或错误状态才会替换
     * MediaItem。替换媒体时立即更新为准备状态，使界面无需等待媒体服务连接即可展示 loading。
     */
    fun play(media: VideoMedia) {
        val action = _playbackState.value.resolvePlayAction(media)
        if (action == VideoPlayAction.IGNORE) return

        val requestId = ++latestRequestId
        if (action == VideoPlayAction.REPLACE || action == VideoPlayAction.REPLAY) {
            _playbackState.value = _playbackState.value.copy(
                media = media,
                status = VideoPlaybackStatus.PREPARING,
                errorMessage = null,
            )
        }

        withPlayer { player ->
            if (requestId != latestRequestId) return@withPlayer

            when (action) {
                VideoPlayAction.REPLACE -> {
                    player.setMediaItem(VideoMediaItemFactory.create(media))
                    player.prepare()
                    player.play()
                }

                VideoPlayAction.RESUME -> player.play()
                VideoPlayAction.REPLAY -> {
                    player.seekToDefaultPosition()
                    player.play()
                }

                VideoPlayAction.IGNORE -> Unit
            }
        }
    }

    /** 暂停当前媒体；最终状态以 Media3 的监听回调为准。 */
    fun pause() {
        // 连接尚未完成时，暂停操作应排在已请求的播放操作之后执行，不能取消该播放请求。
        withPlayer(Player::pause)
    }

    /**
     * 停止当前媒体的播放与加载，但不销毁全局播放器实例。
     *
     * 适用于退出详情页等不再需要继续播放或缓冲的场景。停止后清空 UI 会话状态，使下一次播放同一
     * 视频时重新设置 MediaItem 并 prepare，而不是错误地尝试恢复已停止的播放。
     */
    fun stop() {
        val requestId = ++latestRequestId
        _playbackState.value = _playbackState.value.resetAfterStop()
        withPlayer { player ->
            // 后续新的播放请求优先，不能让较早的异步 stop 误停新媒体。
            if (requestId == latestRequestId) {
                player.stop()
            }
        }
    }

    /**
     * 停止并从播放列表移除当前媒体，但保留其他待播放的媒体和全局播放器实例。
     *
     * 适用于退出详情页等当前视频不应再由系统媒体控制恢复的场景。与 [stop] 不同，此操作会使
     * 当前媒体不再存在于播放列表中；若列表中还有其他媒体，它们不会被清空。
     */
    fun removeCurrentMedia() {
        val requestId = ++latestRequestId
        _playbackState.value = _playbackState.value.resetAfterCurrentMediaRemoved()
        withPlayer { player ->
            // 后续新的播放请求优先，不能让较早的异步移除误操作新媒体。
            if (requestId != latestRequestId) return@withPlayer

            val currentIndex = player.currentMediaItemIndex
            player.stop()
            if (currentIndex != C.INDEX_UNSET && currentIndex < player.mediaItemCount) {
                player.removeMediaItem(currentIndex)
            }
        }
    }

    /**
     * 为已连接的 MediaController 注册状态监听。
     *
     * MediaController 是 Service 内 MediaSession 的客户端代理；这些回调反映的是其关联的
     * ExoPlayer 状态变化，而不是页面自身的 View 状态。
     */
    private fun attachPlayerListener(player: MediaController) {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // 媒体准备、缓冲、播放结束等底层播放阶段发生变化。
                updateState(status = VideoPlaybackStatusMapper.from(playbackState, player.isPlaying))
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 实际播放开关发生变化，例如用户暂停、恢复播放或缓冲结束后自动开始播放。
                updateState(status = VideoPlaybackStatusMapper.from(player.playbackState, isPlaying))
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // 数据源、网络、解码器等播放链路出现不可恢复错误。
                updateState(
                    status = VideoPlaybackStatus.ERROR,
                    errorMessage = error.message ?: "视频播放失败",
                )
            }
        })
    }

    /** 更新供 UI 持续观察的不可变状态。 */
    private fun updateState(
        status: VideoPlaybackStatus,
        errorMessage: String? = null,
    ) {
        _playbackState.value = _playbackState.value.copy(
            status = status,
            errorMessage = errorMessage,
        )
    }
}
