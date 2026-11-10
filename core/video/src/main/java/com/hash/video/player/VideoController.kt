package com.hash.video.player

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.hash.video.api.VideoContentSize
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
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
 *
 * 职责边界：
 * - 将 Media3 的播放器回调转换为业务层可观察的 [VideoPlaybackState]；
 * - 处理播放、暂停、停止和移除当前媒体等播放意图；
 * - 定时同步进度，并接收媒体流解析出的真实视频尺寸；
 * - 不持有任何页面或 PlayerView，画面绑定由 `VideoPresentationCoordinator` 负责。
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

    /**
     * 用于丢弃媒体服务尚未连接时已过期的播放控制请求。
     *
     * 每次会改变播放会话的操作都会递增该值；异步回调执行前比对其快照，确保“先点播放 A、再点播放 B”
     * 时，延迟到达的 A 不会覆盖 B。
     */
    private var latestRequestId = 0L

    /**
     * 仅在播放器实际播放或缓冲时刷新进度，避免空闲状态保留无意义主线程任务。
     *
     * Media3 的状态回调不会持续提供精确播放位置，因此使用主线程 Handler 每 [PROGRESS_UPDATE_INTERVAL_MS]
     * 同步一次。`progressPlayer` 同时作为轮询是否仍有效的标识。
     */
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressPlayer: Player? = null
    private val progressUpdater = object : Runnable {
        override fun run() {
            val player = progressPlayer ?: return
            updateProgress(player)
            if (player.shouldTrackProgress()) {
                progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            } else {
                progressPlayer = null
            }
        }
    }

    init {
        // Future 完成表示已成功连接到媒体服务，或连接过程已经失败。连接成功后只注册一次监听器。
        controllerFuture.addListener(
            {
                runCatching { controllerFuture.get() }
                    .onSuccess(::attachPlayerListener)
                    .onFailure {
                        // 服务不可用时也向状态流报告，页面无需单独感知 Future 异常。
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
     *
     * 该方法仅供 video 模块内部协作使用，业务层只调用公开的播放 API，不直接接触 Media3 Player。
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
     *
     * 方法返回前不代表已经开始输出画面；真实播放、缓冲或错误结果仍以播放器监听回调更新的
     * [playbackState] 为准。
     */
    fun play(media: VideoMedia) {
        val action = _playbackState.value.resolvePlayAction(media)
        if (action == VideoPlayAction.IGNORE) return

        // 快照只属于本次 play；后续操作递增 latestRequestId 后，本次异步回调将自动失效。
        val requestId = ++latestRequestId
        if (action == VideoPlayAction.REPLACE || action == VideoPlayAction.REPLAY) {
            _playbackState.value = _playbackState.value.prepareForPlay(media, action)
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

    /**
     * 暂停当前媒体，但保留媒体、当前位置与缓冲数据。
     *
     * 最终状态以 Media3 的监听回调为准，而不是在这里预先修改状态流。
     */
    fun pause() {
        // 连接尚未完成时，暂停操作应排在已请求的播放操作之后执行，不能取消该播放请求。
        withPlayer(Player::pause)
    }

    /**
     * 跳转当前媒体的播放位置。
     *
     * 仅对已知总时长且仍存在当前媒体的会话生效；目标位置会限制在有效范围内，避免直播流或尚未
     * 解析出时长的媒体收到无意义的跳转请求。
     */
    fun seekTo(positionMs: Long) {
        val state = _playbackState.value
        val durationMs = state.progress.durationMs
        val currentMedia = state.media ?: return
        if (durationMs <= 0L) return

        val seekPositionMs = positionMs.coerceIn(0L, durationMs)
        withPlayer { player ->
            if (_playbackState.value.media?.id == currentMedia.id) {
                player.seekTo(seekPositionMs)
            }
        }
    }

    /**
     * 停止当前媒体的播放与加载，但不销毁全局播放器实例。
     *
     * 适用于只需停止加载、但不希望修改播放器播放列表的场景。停止后清空 UI 会话状态，使下一次播放
     * 同一视频时重新设置 MediaItem 并 prepare；退出详情页并彻底释放当前媒体时应使用
     * [removeCurrentMedia]。
     */
    fun stop() {
        val requestId = ++latestRequestId
        stopProgressTracking()
        _playbackState.value = _playbackState.value.resetPlaybackSession()
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
        stopProgressTracking()
        _playbackState.value = _playbackState.value.resetPlaybackSession()
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
                // 媒体准备、缓冲、就绪、播放结束等底层播放阶段发生变化。
                updateState(
                    status = VideoPlaybackStatusMapper.from(playbackState, player.isPlaying)
                )
                updateProgressTracking(player)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 实际播放开关发生变化，例如用户暂停、恢复播放，或缓冲结束后自动开始播放。
                updateState(
                    status = VideoPlaybackStatusMapper.from(player.playbackState, isPlaying)
                )
                updateProgressTracking(player)
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // 暂停状态不会持续轮询；跳转完成后也必须立即同步真实位置。
                updateProgress(player)
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                // 流媒体解析到真实尺寸后通知 UI，用于详情、自适应全屏和小窗展示。
                val contentSize = videoSize.toContentSizeOrNull() ?: return
                if (_playbackState.value.media != null) {
                    _playbackState.value = _playbackState.value.copy(contentSize = contentSize)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // 数据源、网络、解码器等播放链路出现不可恢复错误。
                stopProgressTracking()
                updateState(
                    status = VideoPlaybackStatus.ERROR,
                    errorMessage = VideoPlaybackErrorMessageMapper.from(error),
                )
            }
        })
    }

    /**
     * 以固定频率同步真实播放位置和缓冲位置，供详情、全屏和小窗等任意展示宿主复用。
     *
     * 未知总时长（直播等）统一写为 0，UI 可据此隐藏确定进度条。
     */
    private fun updateProgress(player: Player) {
        if (_playbackState.value.media == null) return

        val durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        _playbackState.value = _playbackState.value.copy(
            progress = VideoPlaybackProgress(
                positionMs = player.currentPosition.coerceAtLeast(0L),
                bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L),
                durationMs = durationMs,
            ),
        )
    }

    /** 根据播放器实际状态启动或停止进度轮询；暂停时仍同步最后一次静态进度。 */
    private fun updateProgressTracking(player: Player) {
        if (player.shouldTrackProgress()) {
            progressPlayer = player
            progressHandler.removeCallbacks(progressUpdater)
            progressUpdater.run()
        } else {
            stopProgressTracking()
            updateProgress(player)
        }
    }

    /** 取消尚未执行的轮询消息，并断开对当前播放器代理的引用。 */
    private fun stopProgressTracking() {
        progressHandler.removeCallbacks(progressUpdater)
        progressPlayer = null
    }

    /** 仅“准备播放中的缓冲”或“正在输出”需要持续刷新位置。 */
    private fun Player.shouldTrackProgress(): Boolean = playWhenReady && (
            playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING
            )

    /** 将 Media3 尺寸转换为公开模型，过滤宽高未知的中间状态。 */
    private fun VideoSize.toContentSizeOrNull(): VideoContentSize? =
        VideoContentSize(width, height, pixelWidthHeightRatio).takeIf { it.aspectRatio > 0f }

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

    private companion object {
        const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    }
}
