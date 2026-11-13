package com.hash.video.player

import android.app.Application
import android.app.PendingIntent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.hash.video.api.VideoContentSize
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.cache.VideoCacheProvider
import com.hash.video.cache.VideoCacheKeyFactory
import com.hash.video.service.HashVideoPlaybackService
import com.hash.video.ui.render.VideoRenderCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 进程唯一的播放引擎；集中持有播放器、状态监听和渲染器。 */
@OptIn(UnstableApi::class)
internal object VideoPlayerEngine {

    /** 应用内唯一的底层播放器。 */
    private var player: ExoPlayer? = null

    /** 负责唯一 PlayerView 在不同业务容器间迁移。 */
    private var renderCoordinator: VideoRenderCoordinator? = null

    /** 当前有权控制和释放播放器的页面 Owner。 */
    private var activeOwner: VideoPlaybackOwner? = null

    /** 每个视频最近一次切走的位置；重新点回同一条视频时从这里续播。 */
    private val savedPositionsMs = mutableMapOf<String, Long>()

    /** 旧卡片的最后一帧缩略图，容量按像素内存限制，避免列表切换导致黑屏。 */
    private val pausedFrames = object : LruCache<String, Bitmap>(MAX_PAUSED_FRAME_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /** 所有播放器 View 共享的播放状态。 */
    private val _playbackState = MutableStateFlow(VideoPlaybackState())
    val playbackState: StateFlow<VideoPlaybackState> = _playbackState.asStateFlow()

    /** 用于丢弃过期的播放请求。 */
    private var latestRequestId = 0L
    private val progressHandler = Handler(Looper.getMainLooper())
    private var progressPlayer: Player? = null
    private val progressUpdater = object : Runnable {
        override fun run() {
            val source = progressPlayer ?: return
            updateProgress(source)
            if (source.shouldTrackProgress()) progressHandler.postDelayed(
                this, PROGRESS_UPDATE_INTERVAL_MS
            )
            else progressPlayer = null
        }
    }
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            // 播放器状态变化时，同步 UI 状态和进度轮询。
            currentPlayer()?.let {
                updateState(VideoPlaybackStatusMapper.from(playbackState, it.isPlaying))
                updateProgressTracking(it)
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            currentPlayer()?.let {
                updateState(VideoPlaybackStatusMapper.from(it.playbackState, isPlaying))
                updateProgressTracking(it)
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = currentPlayer()?.let(::updateProgress) ?: Unit

        override fun onVideoSizeChanged(videoSize: VideoSize) {
            val contentSize = videoSize.toContentSizeOrNull() ?: return
            if (_playbackState.value.media != null) {
                _playbackState.value = _playbackState.value.copy(contentSize = contentSize)
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            stopProgressTracking()
            updateState(VideoPlaybackStatus.ERROR, VideoPlaybackErrorMessageMapper.from(error))
        }
    }

    /** 按需创建唯一 ExoPlayer，并只注册一次底层监听。 */
    @Synchronized
    fun get(application: Application): ExoPlayer = player ?: ExoPlayer.Builder(application)
        .setSeekBackIncrementMs(NOTIFICATION_SEEK_INCREMENT_MS)
        .setSeekForwardIncrementMs(NOTIFICATION_SEEK_INCREMENT_MS).setMediaSourceFactory(
            DefaultMediaSourceFactory(
                VideoCacheProvider.getDataSourceFactory(
                    application
                )
            )
        ).build().also {
            it.addListener(playerListener)
            player = it
        }

    /** 当前 Owner 请求播放媒体；新 Owner 会接管全局播放器。 */
    fun play(
        application: Application,
        owner: VideoPlaybackOwner,
        media: VideoMedia,
        showNotification: Boolean,
        showNotificationArtwork: Boolean,
    ) {
        activeOwner = owner
        val current = get(application)
        val action = _playbackState.value.resolvePlayAction(media)
        if (action == VideoPlayAction.IGNORE) return
        val requestId = ++latestRequestId
        val previousMedia = _playbackState.value.media
        if (action == VideoPlayAction.REPLACE || action == VideoPlayAction.REPLAY) {
            _playbackState.value = _playbackState.value.prepareForPlay(media, action)
        }
        if (requestId != latestRequestId) return
        when (action) {
            VideoPlayAction.REPLACE -> current.run {
                rememberPlaybackOf(previousMedia, this)
                val startPositionMs = savedPositionsMs[media.cacheKey()].orEmptyPosition()
                setMediaItem(
                    VideoMediaItemFactory.create(media, includeArtwork = showNotificationArtwork),
                    startPositionMs,
                )
                prepare()
                play()
            }

            VideoPlayAction.RESUME -> current.play()
            VideoPlayAction.REPLAY -> current.run { seekToDefaultPosition(); play() }
            VideoPlayAction.IGNORE -> Unit
        }
        if (showNotification) {
            // 先让 Player 进入准备/播放态再启动前台服务，MediaSessionService 才会立即发布媒体通知。
            HashVideoPlaybackService.ensureSession(application)
        } else {
            // 信息流切换自详情播放时，必须先移除旧的媒体会话，避免沿用详情通知。
            HashVideoPlaybackService.stopSession(application)
        }
    }

    /** 只有当前 Owner 可以暂停播放。 */
    fun pause(owner: VideoPlaybackOwner) {
        if (owner === activeOwner) currentPlayer()?.pause()
    }

    /** 仅允许当前 Owner 跳转进度。 */
    fun seekTo(owner: VideoPlaybackOwner, positionMs: Long) {
        if (owner !== activeOwner) return
        val current = currentPlayer() ?: return
        val durationMs = _playbackState.value.progress.durationMs
        if (_playbackState.value.media != null && durationMs > 0L) current.seekTo(
            positionMs.coerceIn(
                0L, durationMs
            )
        )
    }

    /** 停止当前媒体，但保留媒体项以便后续恢复。 */
    fun stop(owner: VideoPlaybackOwner) {
        if (owner !== activeOwner) return
        latestRequestId++; stopProgressTracking(); _playbackState.value =
            _playbackState.value.resetPlaybackSession(); currentPlayer()?.stop()
    }

    /** 停止并移除当前媒体项。 */
    fun removeCurrentMedia(owner: VideoPlaybackOwner) {
        if (owner !== activeOwner) return
        val current = currentPlayer() ?: return
        latestRequestId++; stopProgressTracking(); _playbackState.value =
            _playbackState.value.resetPlaybackSession()
        val index = current.currentMediaItemIndex
        current.stop()
        if (index != C.INDEX_UNSET && index < current.mediaItemCount) current.removeMediaItem(index)
    }

    /** 通知点击目标由服务持有 PendingIntent，不保存页面实例。 */
    fun setNotificationActivity(application: Application, pendingIntent: PendingIntent?) =
        HashVideoPlaybackService.setNotificationActivity(application, pendingIntent)

    /** 将唯一画面挂载到当前 Owner 的目标容器。 */
    fun attachRenderTo(application: Application, owner: VideoPlaybackOwner, container: ViewGroup) {
        if (activeOwner != null && owner !== activeOwner) return
        if (activeOwner == null) activeOwner = owner
        val current = get(application)
        val coordinator = renderCoordinator ?: VideoRenderCoordinator(
            application, current
        ).also { renderCoordinator = it }
        coordinator.attachTo(container)
    }

    /** 仅当前 Owner 可解除自己的画面容器。 */
    fun detachRenderFrom(owner: VideoPlaybackOwner, container: ViewGroup): Boolean {
        if (owner !== activeOwner) return false
        return renderCoordinator?.detachFrom(container) ?: false
    }

    fun hasAttachedRender(): Boolean = renderCoordinator?.currentContainer() != null

    /** 释放播放器、渲染画面和媒体会话；旧 Owner 的请求会被忽略。 */
    fun release(application: Application, owner: VideoPlaybackOwner? = null) {
        if (owner != null && owner !== activeOwner) return
        latestRequestId++
        stopProgressTracking()
        renderCoordinator?.clear()
        renderCoordinator = null
        _playbackState.value = _playbackState.value.resetPlaybackSession()
        savedPositionsMs.clear()
        pausedFrames.evictAll()
        activeOwner = null
        HashVideoPlaybackService.stopSession(application)
        synchronized(this) {
            player?.removeListener(playerListener)
            player?.release()
            player = null
        }
    }

    private fun currentPlayer(): ExoPlayer? = player
    private fun updateProgress(player: Player) {
        if (_playbackState.value.media == null) return
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        _playbackState.value = _playbackState.value.copy(
            progress = VideoPlaybackProgress(
                player.currentPosition.coerceAtLeast(0L),
                player.bufferedPosition.coerceAtLeast(0L),
                duration
            ),
        )
        _playbackState.value.media?.let { media ->
            savedPositionsMs[media.cacheKey()] = player.currentPosition.coerceAtLeast(0L)
        }
    }

    private fun updateProgressTracking(player: Player) {
        if (player.shouldTrackProgress()) {
            progressPlayer =
                player; progressHandler.removeCallbacks(progressUpdater); progressUpdater.run()
        } else {
            stopProgressTracking(); updateProgress(player)
        }
    }

    private fun stopProgressTracking() {
        progressHandler.removeCallbacks(progressUpdater); progressPlayer = null
    }

    // 仅在准备播放或正在播放时持续刷新进度。
    private fun Player.shouldTrackProgress(): Boolean =
        playWhenReady && (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING)

    private fun VideoSize.toContentSizeOrNull(): VideoContentSize? =
        VideoContentSize(width, height, pixelWidthHeightRatio).takeIf { it.aspectRatio > 0f }

    private fun updateState(status: VideoPlaybackStatus, errorMessage: String? = null) {
        // Media3 报错后还会继续派发 IDLE/非播放回调，不能覆盖刚展示给用户的错误。
        if (_playbackState.value.status == VideoPlaybackStatus.ERROR && status != VideoPlaybackStatus.ERROR) {
            return
        }
        _playbackState.value =
            _playbackState.value.copy(status = status, errorMessage = errorMessage)
    }

    /** 提供给列表卡片展示；快照只在对应视频非当前渲染目标时使用。 */
    fun pausedFrameFor(media: VideoMedia): Bitmap? = pausedFrames.get(media.cacheKey())

    private fun rememberPlaybackOf(media: VideoMedia?, player: ExoPlayer) {
        media ?: return
        savedPositionsMs[media.cacheKey()] = player.currentPosition.coerceAtLeast(0L)
        renderCoordinator?.captureFrame()?.let { frame ->
            pausedFrames.put(media.cacheKey(), frame)
        }
    }

    private fun VideoMedia.cacheKey(): String = VideoCacheKeyFactory.create(this)

    private fun Long?.orEmptyPosition(): Long = this?.coerceAtLeast(0L) ?: 0L

    private const val NOTIFICATION_SEEK_INCREMENT_MS = 15_000L
    private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    private const val MAX_PAUSED_FRAME_BYTES = 12 * 1024 * 1024
}
