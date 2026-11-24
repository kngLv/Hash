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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaylist
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isSameSourceAs
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

    /**
     * 当前有权控制和释放唯一 ExoPlayer 的页面 Owner。
     *
     * 仅在页面实际开始播放或接管画面后赋值；页面预先配置队列时不能抢占此控制权。
     */
    private var activeOwner: VideoPlaybackOwner? = null

    /**
     * 仅供后台 MediaSession 上一条、下一条使用的可选队列会话。
     *
     * 队列可以在用户点击播放前预先设置，因此不与 [activeOwner] 合并；它不会抢占真实
     * 播放器控制权。
     */
    private var playbackQueue: PlaybackQueueSession? = null

    private var hasInstalledProcessObserver = false
    private var isAppInForeground = true

    /** 每个视频最近一次切走的位置；重新点回同一条视频时从这里续播。 */
    private val savedPositionsMs = mutableMapOf<String, Long>()

    /** 旧卡片的最后一帧缩略图，容量按像素内存限制，避免列表切换导致黑屏。 */
    private val pausedFrames = object : LruCache<String, Bitmap>(MAX_PAUSED_FRAME_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    /** 本次画面迁移已保存的旧媒体帧，避免替换媒体时被新 Surface 的空帧覆盖。 */
    private var transferredFrameMediaKey: String? = null

    /** 所有播放器 View 共享的播放状态。 */
    private val _playbackState = MutableStateFlow(VideoPlaybackState())
    val playbackState: StateFlow<VideoPlaybackState> = _playbackState.asStateFlow()

    private val _notificationControls = MutableStateFlow(VideoNotificationControls())
    val notificationControls: StateFlow<VideoNotificationControls> = _notificationControls.asStateFlow()

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

        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            val current = currentPlayer() ?: return
            val activePlaylist = playbackQueue?.takeIf { it.isLoaded }?.playlist ?: return
            val index = current.currentMediaItemIndex
            val media = activePlaylist.items.getOrNull(index) ?: return
            // 通知播放index发生变化
            _playbackState.value = _playbackState.value.prepareForPlay(media, VideoPlayAction.REPLACE)
                .copy(playlistIndex = index, playlistSize = activePlaylist.items.size)
            updateNotificationControls(current)
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            stopProgressTracking()
            updateState(VideoPlaybackStatus.ERROR, VideoPlaybackErrorMessageMapper.from(error))
        }
    }

    /** 按需创建唯一 ExoPlayer，并只注册一次底层监听。 */
    @Synchronized
    fun get(application: Application): ExoPlayer {
        checkMainThread()
        installProcessObserverIfNeeded()
        return player ?: ExoPlayer.Builder(application)
            .setSeekBackIncrementMs(NOTIFICATION_SEEK_INCREMENT_MS)
            .setSeekForwardIncrementMs(NOTIFICATION_SEEK_INCREMENT_MS)
            .build().also {
                it.addListener(playerListener)
                player = it
            }
    }

    /** 当前 Owner 请求播放媒体；新 Owner 会接管全局播放器。 */
    fun play(
        application: Application,
        owner: VideoPlaybackOwner,
        media: VideoMedia,
        showNotification: Boolean,
        showNotificationArtwork: Boolean,
    ) {
        checkMainThread()
        installProcessObserverIfNeeded()
        //匹配当前队列中是否存在该视频，若存在则直接播放队列中的视频
        val matchingPlaylistIndex = playbackQueue
            ?.takeIf { it.owner === owner }
            ?.playlist
            ?.indexOf(media)
            ?.takeIf { it >= 0 }
        if (matchingPlaylistIndex != null) {
            playPlaylistIndex(
                application = application,
                owner = owner,
                index = matchingPlaylistIndex,
                showNotification = showNotification,
                showNotificationArtwork = showNotificationArtwork,
            )
            syncNotificationSession(application, showNotification)
            return
        }
        clearPlaylist()
        updateNotificationControls()
        activeOwner = owner
        val current = get(application)
        val action = _playbackState.value.resolvePlayAction(media)
        if (action != VideoPlayAction.IGNORE) {
            val requestId = ++latestRequestId
            val previousMedia = _playbackState.value.media
            if (action == VideoPlayAction.REPLACE || action == VideoPlayAction.REPLAY) {
                _playbackState.value = _playbackState.value.prepareForPlay(media, action)
            }
            if (requestId == latestRequestId) {
                when (action) {
                    VideoPlayAction.REPLACE -> current.run {
                        rememberPlaybackOf(
                            previousMedia,
                            this,
                            captureFrame = previousMedia?.cacheKey() != transferredFrameMediaKey,
                        )
                        val startPositionMs = savedPositionsMs[media.cacheKey()].orEmptyPosition()
                        val mediaItem = VideoMediaItemFactory.create(media, includeArtwork = showNotificationArtwork)
                        val sourceFactory = DefaultMediaSourceFactory(
                            VideoCacheProvider.getDataSourceFactory(application, media.headers),
                        )
                        setMediaSource(sourceFactory.createMediaSource(mediaItem), startPositionMs)
                        prepare()
                        play()
                    }

                    VideoPlayAction.RESUME -> current.play()
                    VideoPlayAction.REPLAY -> current.run { seekToDefaultPosition(); play() }
                    VideoPlayAction.IGNORE -> Unit
                }
            }
        }
        // 通知策略与播放动作独立：同源视频在详情/信息流之间迁移时也必须切换通知。
        syncNotificationSession(application, showNotification)
    }

    /** 保存可选后台播放队列；ExoPlayer 自动续播，页面只负责将画面呈现到对应宿主。 */
    fun setPlaybackQueue(
        owner: VideoPlaybackOwner,
        items: List<VideoMedia>,
    ) {
        checkMainThread()
        if (activeOwner != null && activeOwner !== owner) return
        val newPlaylist = VideoPlaylist(items)
        if (newPlaylist.items.size <= 1) {
            clearPlaybackQueue(owner)
            return
        }
        val currentQueue = playbackQueue
        //当前队列操作者相同且队列相同
        if (currentQueue?.owner === owner && currentQueue.playlist.items == newPlaylist.items) {
            return
        }
        //是活跃的操作者且队列已加载，清空播放器和状态
        if (activeOwner === owner && currentQueue?.isLoaded == true) {
            currentPlayer()?.stop()
            _playbackState.value = _playbackState.value.resetPlaybackSession()
        }
        //创建新的队列会话，通知服务
        playbackQueue = PlaybackQueueSession(owner, newPlaylist)
        updateNotificationControls()
    }

    /** 将已配置队列装入播放器，并从指定索引开始播放。 */
    fun playQueueAt(
        application: Application,
        owner: VideoPlaybackOwner,
        index: Int,
        showNotification: Boolean,
        showNotificationArtwork: Boolean,
    ) {
        checkMainThread()
        val queue = playbackQueue?.takeIf { it.owner === owner } ?: return
        if (index !in queue.playlist.items.indices) return
        playPlaylistIndex(application, owner, index, showNotification, showNotificationArtwork)
        syncNotificationSession(application, showNotification)
    }

    fun clearPlaybackQueue(owner: VideoPlaybackOwner) {
        checkMainThread()
        if (playbackQueue?.owner !== owner) return
        clearPlaylist()
        updateNotificationControls()
    }

    fun playPrevious(owner: VideoPlaybackOwner) {
        checkMainThread()
        val current = currentPlayer() ?: return
        if (owner !== activeOwner || playbackQueue?.isLoaded != true || playbackQueue?.owner !== owner) return
        val target = (current.currentMediaItemIndex - 1).takeIf { it >= 0 } ?: return
        playLoadedPlaylistIndex(current, target)
    }

    fun playNext(owner: VideoPlaybackOwner) {
        checkMainThread()
        val current = currentPlayer() ?: return
        if (owner !== activeOwner || playbackQueue?.isLoaded != true || playbackQueue?.owner !== owner) return
        val target = current.currentMediaItemIndex + 1
        if (target !in 0 until current.mediaItemCount) return
        playLoadedPlaylistIndex(current, target)
    }

    /** 只有当前 Owner 可以暂停播放。 */
    fun pause(owner: VideoPlaybackOwner) {
        checkMainThread()
        if (owner === activeOwner) currentPlayer()?.pause()
    }

    /** 仅允许当前 Owner 跳转进度。 */
    fun seekTo(owner: VideoPlaybackOwner, positionMs: Long) {
        checkMainThread()
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
        checkMainThread()
        if (owner !== activeOwner) return
        latestRequestId++; stopProgressTracking(); _playbackState.value =
            _playbackState.value.resetPlaybackSession(); currentPlayer()?.stop()
    }

    /** 停止并移除当前媒体项。 */
    fun removeCurrentMedia(owner: VideoPlaybackOwner) {
        checkMainThread()
        if (owner !== activeOwner) return
        val current = currentPlayer() ?: return
        latestRequestId++; stopProgressTracking(); _playbackState.value =
            _playbackState.value.resetPlaybackSession()
        val index = current.currentMediaItemIndex
        current.stop()
        if (index != C.INDEX_UNSET && index < current.mediaItemCount) current.removeMediaItem(index)
        clearPlaylist()
        updateNotificationControls()
    }

    /** 通知点击目标由服务持有 PendingIntent，不保存页面实例。 */
    fun setNotificationActivity(application: Application, pendingIntent: PendingIntent?) {
        checkMainThread()
        HashVideoPlaybackService.setNotificationActivity(application, pendingIntent)
    }

    /** 将唯一画面挂载到当前 Owner 的目标容器。 */
    fun attachRenderTo(
        application: Application,
        owner: VideoPlaybackOwner,
        container: ViewGroup,
        targetMedia: VideoMedia? = null,
    ) {
        checkMainThread()
        if (activeOwner != null && owner !== activeOwner) return
        if (activeOwner == null) activeOwner = owner
        val currentPlayer = get(application)
        val coordinator = renderCoordinator ?: VideoRenderCoordinator(
            application, currentPlayer
        ).also { renderCoordinator = it }
        transferredFrameMediaKey = null
        val currentState = _playbackState.value
        val currentMedia = currentState.media
        // 切换播放源后旧卡片失去唯一画面，必须保存旧帧。相同播放源迁移时，只有暂停或结束
        // 的播放器不会向新 Surface 重绘，才保存画面兜底。
        if (coordinator.currentContainer() !== container) {
            currentMedia?.let { media ->
                val isSwitchingSource = !media.isSameSourceAs(targetMedia)
                val needsFrozenFrame = currentState.status == VideoPlaybackStatus.PAUSED ||
                    currentState.status == VideoPlaybackStatus.ENDED
                //播放源已切换 or 播放处于暂停或者结束
                if (isSwitchingSource || needsFrozenFrame) {
                    coordinator.captureFrame()?.let { frame ->
                        pausedFrames.put(media.cacheKey(), frame)
                        transferredFrameMediaKey = media.cacheKey()
                    }
                }
            }
        }
        coordinator.attachTo(container)
    }

    /** 仅当前 Owner 可解除自己的画面容器。 */
    fun detachRenderFrom(owner: VideoPlaybackOwner, container: ViewGroup): Boolean {
        checkMainThread()
        if (owner !== activeOwner) return false
        return renderCoordinator?.detachFrom(container) ?: false
    }

    fun hasAttachedRender(): Boolean {
        checkMainThread()
        return renderCoordinator?.currentContainer() != null
    }

    /** 释放播放器、渲染画面和媒体会话；旧 Owner 的请求会被忽略。 */
    fun release(application: Application, owner: VideoPlaybackOwner? = null) {
        checkMainThread()
        if (owner != null && owner !== activeOwner) return
        latestRequestId++
        stopProgressTracking()
        renderCoordinator?.clear()
        renderCoordinator = null
        _playbackState.value = _playbackState.value.resetPlaybackSession()
        savedPositionsMs.clear()
        pausedFrames.evictAll()
        activeOwner = null
        clearPlaylist()
        updateNotificationControls()
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

    private fun updateState(status: VideoPlaybackStatus, errorMessage: String? = null) {
        // Media3 报错后还会继续派发 IDLE/非播放回调，不能覆盖刚展示给用户的错误。
        if (_playbackState.value.status == VideoPlaybackStatus.ERROR && status != VideoPlaybackStatus.ERROR) {
            return
        }
        _playbackState.value =
            _playbackState.value.copy(status = status, errorMessage = errorMessage)
    }

    /**
     * 播放可选队列中的指定索引。
     *
     * 队列尚未装入 ExoPlayer 时，会先创建全部 MediaSource 并从该索引开始播放；已装入时，
     * 则按当前媒体状态恢复、重播或跳转。
     */
    private fun playPlaylistIndex(
        application: Application,
        owner: VideoPlaybackOwner,
        index: Int,
        showNotification: Boolean,
        showNotificationArtwork: Boolean,
    ) {
        val queue = playbackQueue?.takeIf { it.owner === owner } ?: return
        val activePlaylist = queue.playlist
        activeOwner = owner
        val current = get(application)
        if (!queue.isLoaded) {
            val previousMedia = _playbackState.value.media
            //记住上一个视频的位置和播放帧
            rememberPlaybackOf(previousMedia, current, captureFrame = true)
            //更新状态为准备播放
            _playbackState.value = _playbackState.value.prepareForPlay(
                activePlaylist.items[index],
                VideoPlayAction.REPLACE,
            ).copy(playlistIndex = index, playlistSize = activePlaylist.items.size)
            //设置播放列表
            current.setMediaSources(
                activePlaylist.items.map { media ->
                    createMediaSource(application, media, showNotificationArtwork)
                },
                index,
                savedPositionsMs[activePlaylist.items[index].cacheKey()].orEmptyPosition(),
            )
            queue.isLoaded = true
            // 队列由 ExoPlayer 自动续播；页面只根据新的 playlistIndex 迁移画面宿主。
            current.pauseAtEndOfMediaItems = false
            current.prepare()
            current.play()
            updateNotificationControls(current)
            return
        }

        val action = _playbackState.value.resolvePlayAction(activePlaylist.items[index])
        if (index != current.currentMediaItemIndex) {
            playLoadedPlaylistIndex(current, index)
        } else {
            when (action) {
                VideoPlayAction.RESUME -> current.play()
                VideoPlayAction.REPLAY -> current.run { seekTo(index, 0L); play() }
                VideoPlayAction.REPLACE -> {
                    current.seekTo(index, savedPositionsMs[activePlaylist.items[index].cacheKey()]
                        .orEmptyPosition())
                    current.prepare()
                    current.play()
                }
                VideoPlayAction.IGNORE -> Unit
            }
        }
    }

    /** 已装载队列内跳转到指定索引，并保留每条媒体各自已保存的播放进度。 */
    private fun playLoadedPlaylistIndex(current: ExoPlayer, index: Int) {
        val activePlaylist = playbackQueue?.takeIf { it.isLoaded }?.playlist ?: return
        val target = activePlaylist.items.getOrNull(index) ?: return
        rememberPlaybackOf(_playbackState.value.media, current, captureFrame = true)
        //更新播放前状态
        _playbackState.value = _playbackState.value.prepareForPlay(target, VideoPlayAction.REPLACE)
            .copy(playlistIndex = index, playlistSize = activePlaylist.items.size)
        current.seekTo(index, savedPositionsMs[target.cacheKey()].orEmptyPosition())
        current.play()
        updateNotificationControls(current)
    }

    private fun createMediaSource(
        application: Application,
        media: VideoMedia,
        includeArtwork: Boolean,
    ): MediaSource = DefaultMediaSourceFactory(
        VideoCacheProvider.getDataSourceFactory(application, media.headers),
    ).createMediaSource(VideoMediaItemFactory.create(media, includeArtwork))

    /**
     * 仅注册一次进程级生命周期观察，跟踪应用是否处于前台。
     *
     * 状态变化时更新通知控制快照：前台使用快退/快进，后台切换为队列上一条/下一条。观察者只引用
     * 进程单例引擎，不持有 Activity 或页面 View。
     */
    private fun installProcessObserverIfNeeded() {
        if (hasInstalledProcessObserver) return
        hasInstalledProcessObserver = true
        isAppInForeground = ProcessLifecycleOwner.get().lifecycle.currentState
            .isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                isAppInForeground = true
                currentPlayer()?.let {
                    updateNotificationControls(it)
                } ?: updateNotificationControls()
            }

            override fun onStop(owner: LifecycleOwner) {
                isAppInForeground = false
                currentPlayer()?.let {
                    updateNotificationControls(it)
                }
                if (currentPlayer() == null) updateNotificationControls()
            }
        })
    }

    private fun clearPlaylist() {
        playbackQueue = null
        currentPlayer()?.pauseAtEndOfMediaItems = false
    }

    /**
     * 根据前后台状态及当前队列索引更新通知控制快照。
     *
     * 前台使用快退/快进；后台仅在存在相邻媒体时提供上一条/下一条。服务订阅该快照后自行刷新
     * MediaSession 按钮，本方法不持有或直接操作通知 UI。
     */
    private fun updateNotificationControls(current: Player? = currentPlayer()) {
        val activePlaylist = playbackQueue?.takeIf { it.isLoaded }?.playlist
        if (activePlaylist == null) {
            _notificationControls.value = VideoNotificationControls(
                isAppInForeground = isAppInForeground,
            )
            return
        }
        val index = current?.currentMediaItemIndex ?: _playbackState.value.playlistIndex
        val count = activePlaylist.items.size
        _notificationControls.value = VideoNotificationControls(
            isAppInForeground = isAppInForeground,
            hasPrevious = index != null && index > 0 && count > 1,
            hasNext = index != null && index >= 0 && index < count - 1,
        )
    }

    /** 提供给列表卡片展示；快照只在对应视频非当前渲染目标时使用。 */
    fun pausedFrameFor(media: VideoMedia): Bitmap? {
        checkMainThread()
        return pausedFrames.get(media.cacheKey())
    }

    private fun syncNotificationSession(application: Application, showNotification: Boolean) {
        if (showNotification) {
            HashVideoPlaybackService.ensureSession(application)
        } else {
            HashVideoPlaybackService.stopSession(application)
        }
    }

    private fun checkMainThread() {
        check(Looper.myLooper() == Looper.getMainLooper()) {
            "VideoPlayerEngine must be called on the main thread."
        }
    }

    /**
     * 切换媒体前保存旧媒体进度；需要时抓取当前画面，供旧卡片在渲染 View 迁走后显示静态帧。
     */
    private fun rememberPlaybackOf(media: VideoMedia?, player: ExoPlayer, captureFrame: Boolean) {
        media ?: return
        savedPositionsMs[media.cacheKey()] = player.currentPosition.coerceAtLeast(0L)
        if (captureFrame) {
            renderCoordinator?.captureFrame()?.let { frame ->
                pausedFrames.put(media.cacheKey(), frame)
            }
        }
    }

    private fun VideoMedia.cacheKey(): String = VideoCacheKeyFactory.create(this)

    private fun Long?.orEmptyPosition(): Long = this?.coerceAtLeast(0L) ?: 0L

    /** 一个页面预配置的可选后台队列，以及它是否已实际装载进唯一播放器。 */
    private data class PlaybackQueueSession(
        val owner: VideoPlaybackOwner,
        val playlist: VideoPlaylist,
        var isLoaded: Boolean = false,
    )

    private const val NOTIFICATION_SEEK_INCREMENT_MS = 15_000L
    private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    private const val MAX_PAUSED_FRAME_BYTES = 12 * 1024 * 1024
}
