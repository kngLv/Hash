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
     * 当前有权控制和释放唯一 ExoPlayer 的操作凭证。
     *
     * 仅在页面实际开始播放或接管画面后赋值；页面预先配置队列时不能抢占此控制权。
     */
    private var activeToken: VideoPlaybackControlToken? = null

    /**
     * 仅供后台 MediaSession 上一条、下一条使用的可选队列会话。
     *
     * 队列可以在用户点击播放前预先设置，因此不与 [activeToken] 合并；它不会抢占真实
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
    private var notificationControlsRevision = 0L

    /** 用于丢弃过期的播放请求。 */
    private var latestRequestId = 0L
    private val progressHandler = Handler(Looper.getMainLooper())
    /** 播放期间每 500ms 刷新进度；暂停、结束或释放时停止。 */
    private val progressUpdater = object : Runnable {
        override fun run() {
            val current = currentPlayer() ?: return
            publishProgress(current)
            if (current.shouldPollProgress()) {
                progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }
    private val playerListener = object : Player.Listener {
        /** ExoPlayer 播放阶段变化：空闲、缓冲、就绪或播放结束。 */
        override fun onPlaybackStateChanged(playbackState: Int) {
            currentPlayer()?.let(::syncPlaybackSnapshot)
        }

        /** 实际是否正在输出媒体内容发生变化，例如播放、暂停或缓冲导致的停播。 */
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            currentPlayer()?.let(::syncPlaybackSnapshot)
        }

        /** 播放位置发生跳变，例如用户拖动进度、自动下一条或切换媒体。 */
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) = currentPlayer()?.let(::publishProgress) ?: Unit

        /** 当前 MediaItem 已切换，用于同步新的业务媒体和播放队列索引。 */
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
            val current = currentPlayer() ?: return
            val activePlaylist = playbackQueue?.takeIf { it.isLoaded }?.playlist ?: return
            val index = current.currentMediaItemIndex
            val media = activePlaylist.items.getOrNull(index) ?: return
            // 自动切项时，ExoPlayer 可能仍处于 READY/播放中，不一定会再派发一次状态变化。
            // 不能无条件写成 PREPARING，否则 UI 会持续显示 loading，而进度实际上仍在推进。
            val status = VideoPlaybackStatusMapper.from(current.playbackState, current.isPlaying)
            _playbackState.value = _playbackState.value.replaceMedia(
                media = media,
                status = status,
                playlistIndex = index,
                playlistSize = activePlaylist.items.size,
            )
            syncProgressPolling(current)
            updateNotificationControls(current)
        }

        /** ExoPlayer 遇到不可恢复播放错误。 */
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            stopProgressPolling()
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

    /** 当前凭证请求播放媒体；新凭证会接管全局播放器。 */
    fun play(
        application: Application,
        token: VideoPlaybackControlToken,
        media: VideoMedia,
        showNotification: Boolean,
        showNotificationArtwork: Boolean,
    ) {
        checkMainThread()
        markForegroundPlaybackRequest()
        //匹配当前队列中是否存在该视频，若存在则直接播放队列中的视频
        val matchingPlaylistIndex = playbackQueue
            ?.takeIf { it.token === token }
            ?.playlist
            ?.indexOf(media)
            ?.takeIf { it >= 0 }
        if (matchingPlaylistIndex != null) {
            playPlaylistIndex(
                application = application,
                token = token,
                index = matchingPlaylistIndex,
                showNotificationArtwork = showNotificationArtwork,
            )
            syncNotificationSession(application, showNotification)
            return
        }
        clearPlaylist()
        updateNotificationControls()
        activeToken = token
        val current = get(application)
        val command = _playbackState.value.resolvePlayCommand(media)
        if (command != VideoPlayCommand.NO_OP) {
            val requestId = ++latestRequestId
            val previousMedia = _playbackState.value.media
            if (command == VideoPlayCommand.REPLACE || command == VideoPlayCommand.REPLAY) {
                _playbackState.value = _playbackState.value.replaceMedia(
                    media = media,
                    status = VideoPlaybackStatus.PREPARING,
                )
            }
            if (requestId == latestRequestId) {
                when (command) {
                    VideoPlayCommand.REPLACE -> current.run {
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

                    VideoPlayCommand.RESUME -> current.play()
                    VideoPlayCommand.REPLAY -> current.run { seekToDefaultPosition(); play() }
                    VideoPlayCommand.NO_OP -> Unit
                }
            }
        }
        // 通知策略与播放动作独立：同源视频在详情/信息流之间迁移时也必须切换通知。
        syncNotificationSession(application, showNotification)
    }

    /** 保存可选后台播放队列；ExoPlayer 自动续播，页面只负责将画面呈现到对应宿主。 */
    fun setPlaybackQueue(
        token: VideoPlaybackControlToken,
        items: List<VideoMedia>,
    ) {
        checkMainThread()
        if (activeToken != null && activeToken !== token) return
        val newPlaylist = VideoPlaylist(items)
        if (newPlaylist.items.size <= 1) {
            clearPlaybackQueue(token)
            return
        }
        val currentQueue = playbackQueue
        //当前队列操作者相同且队列相同
        if (currentQueue?.token === token && currentQueue.playlist.items == newPlaylist.items) {
            return
        }
        //是活跃的操作者且队列已加载，清空播放器和状态
        if (activeToken === token && currentQueue?.isLoaded == true) {
            currentPlayer()?.stop()
            _playbackState.value = _playbackState.value.resetPlaybackSession()
        }
        //创建新的队列会话，通知服务
        playbackQueue = PlaybackQueueSession(token, newPlaylist)
        updateNotificationControls()
    }

    /** 将已配置队列装入播放器，并从指定索引开始播放。 */
    fun playQueueAt(
        application: Application,
        token: VideoPlaybackControlToken,
        index: Int,
        showNotification: Boolean,
        showNotificationArtwork: Boolean,
    ) {
        checkMainThread()
        markForegroundPlaybackRequest()
        val queue = playbackQueue?.takeIf { it.token === token } ?: return
        if (index !in queue.playlist.items.indices) return
        playPlaylistIndex(application, token, index, showNotificationArtwork)
        syncNotificationSession(application, showNotification)
    }

    fun clearPlaybackQueue(token: VideoPlaybackControlToken) {
        checkMainThread()
        if (playbackQueue?.token !== token) return
        clearPlaylist()
        updateNotificationControls()
    }

    fun playPrevious(token: VideoPlaybackControlToken) {
        checkMainThread()
        val current = currentPlayer() ?: return
        if (token !== activeToken || playbackQueue?.isLoaded != true || playbackQueue?.token !== token) return
        val target = (current.currentMediaItemIndex - 1).takeIf { it >= 0 } ?: return
        playLoadedPlaylistIndex(current, target)
    }

    fun playNext(token: VideoPlaybackControlToken) {
        checkMainThread()
        val current = currentPlayer() ?: return
        if (token !== activeToken || playbackQueue?.isLoaded != true || playbackQueue?.token !== token) return
        val target = current.currentMediaItemIndex + 1
        if (target !in 0 until current.mediaItemCount) return
        playLoadedPlaylistIndex(current, target)
    }

    /** 只有当前操作凭证可以暂停播放。 */
    fun pause(token: VideoPlaybackControlToken) {
        checkMainThread()
        if (token === activeToken) currentPlayer()?.pause()
    }

    /** 仅允许当前操作凭证跳转进度。 */
    fun seekTo(token: VideoPlaybackControlToken, positionMs: Long) {
        checkMainThread()
        if (token !== activeToken) return
        val current = currentPlayer() ?: return
        val durationMs = _playbackState.value.progress.durationMs
        if (_playbackState.value.media != null && durationMs > 0L) current.seekTo(
            positionMs.coerceIn(
                0L, durationMs
            )
        )
    }

    /** 停止当前媒体，但保留媒体项以便后续恢复。 */
    fun stop(token: VideoPlaybackControlToken) {
        checkMainThread()
        if (token !== activeToken) return
        latestRequestId++
        stopProgressPolling()
        _playbackState.value = _playbackState.value.resetPlaybackSession()
        currentPlayer()?.stop()
    }

    /** 停止并移除当前媒体项。 */
    fun removeCurrentMedia(token: VideoPlaybackControlToken) {
        checkMainThread()
        if (token !== activeToken) return
        val current = currentPlayer() ?: return
        latestRequestId++
        stopProgressPolling()
        _playbackState.value = _playbackState.value.resetPlaybackSession()
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

    /** 将唯一画面挂载到当前操作凭证的目标容器。 */
    fun attachRenderTo(
        application: Application,
        token: VideoPlaybackControlToken,
        container: ViewGroup,
        targetMedia: VideoMedia? = null,
    ) {
        checkMainThread()
        if (activeToken != null && token !== activeToken) return
        if (activeToken == null) activeToken = token
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

    /** 仅当前操作凭证可解除自己的画面容器。 */
    fun detachRenderFrom(token: VideoPlaybackControlToken, container: ViewGroup): Boolean {
        checkMainThread()
        if (token !== activeToken) return false
        return renderCoordinator?.detachFrom(container) ?: false
    }

    fun hasAttachedRender(): Boolean {
        checkMainThread()
        return renderCoordinator?.currentContainer() != null
    }

    /** 创建系统悬浮窗专用操作凭证，并将当前播放器凭证替换为该悬浮窗凭证。 */
    fun enterSystemOverlay(sourceToken: VideoPlaybackControlToken): VideoOverlaySession? {
        checkMainThread()
        if (activeToken !== sourceToken) return null
        return VideoPlaybackControlToken().let { overlayToken ->
            activeToken = overlayToken
            playbackQueue?.takeIf { it.token === sourceToken }?.token = overlayToken
            VideoOverlaySession(overlayToken)
        }
    }

    /** 仅允许当前悬浮窗凭证将会话交回目标页面凭证。 */
    fun restoreSystemOverlay(session: VideoOverlaySession, targetToken: VideoPlaybackControlToken): Boolean {
        checkMainThread()
        if (activeToken !== session.token) return false
        activeToken = targetToken
        playbackQueue?.takeIf { it.token === session.token }?.token = targetToken
        return true
    }

    /** 释放播放器、渲染画面和媒体会话；旧凭证的请求会被忽略。 */
    fun release(application: Application, token: VideoPlaybackControlToken? = null) {
        checkMainThread()
        if (token != null && token !== activeToken) return
        latestRequestId++
        stopProgressPolling()
        renderCoordinator?.clear()
        renderCoordinator = null
        _playbackState.value = _playbackState.value.resetPlaybackSession()
        savedPositionsMs.clear()
        pausedFrames.evictAll()
        activeToken = null
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

    /** 发布当前进度并保存续播位置。 */
    private fun publishProgress(player: Player) {
        if (_playbackState.value.media == null) return
        val positionMs = player.currentPosition.coerceAtLeast(0L)
        val bufferedPositionMs = player.bufferedPosition.coerceAtLeast(0L)
        val duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        _playbackState.value = _playbackState.value.copy(
            progress = VideoPlaybackProgress(
                positionMs = positionMs,
                bufferedPositionMs = bufferedPositionMs,
                durationMs = duration,
            ),
        )
        _playbackState.value.media?.let { media ->
            savedPositionsMs[media.cacheKey()] = positionMs
        }
    }

    /** 同步播放状态和进度轮询。 */
    private fun syncPlaybackSnapshot(player: Player) {
        updateState(VideoPlaybackStatusMapper.from(player.playbackState, player.isPlaying))
        syncProgressPolling(player)
        // 首次创建通知时播放器可能还未提供 seek 命令；状态变化后必须重新下发控制能力。
        updateNotificationControls(player)
    }

    /** 立即刷新进度，并按需管理 500ms 轮询。 */
    private fun syncProgressPolling(player: Player) {
        publishProgress(player)
        if (player.shouldPollProgress()) {
            progressHandler.removeCallbacks(progressUpdater)
            progressHandler.postDelayed(progressUpdater, PROGRESS_UPDATE_INTERVAL_MS)
        } else {
            stopProgressPolling()
        }
    }

    private fun stopProgressPolling() {
        progressHandler.removeCallbacks(progressUpdater)
    }

    /** 用户未暂停，且 ExoPlayer 处于 READY 或 BUFFERING 时继续轮询。 */
    private fun Player.shouldPollProgress(): Boolean =
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
        token: VideoPlaybackControlToken,
        index: Int,
        showNotificationArtwork: Boolean,
    ) {
        val queue = playbackQueue?.takeIf { it.token === token } ?: return
        val activePlaylist = queue.playlist
        activeToken = token
        val current = get(application)
        if (!queue.isLoaded) {
            val previousMedia = _playbackState.value.media
            //记住上一个视频的位置和播放帧
            rememberPlaybackOf(previousMedia, current, captureFrame = true)
            //更新状态为准备播放
            _playbackState.value = _playbackState.value.replaceMedia(
                media = activePlaylist.items[index],
                status = VideoPlaybackStatus.PREPARING,
                playlistIndex = index,
                playlistSize = activePlaylist.items.size,
            )
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

        val command = _playbackState.value.resolvePlayCommand(activePlaylist.items[index])
        if (index != current.currentMediaItemIndex) {
            playLoadedPlaylistIndex(current, index)
        } else {
            when (command) {
                VideoPlayCommand.RESUME -> current.play()
                VideoPlayCommand.REPLAY -> current.run { seekTo(index, 0L); play() }
                VideoPlayCommand.REPLACE -> {
                    current.seekTo(index, savedPositionsMs[activePlaylist.items[index].cacheKey()]
                        .orEmptyPosition())
                    current.prepare()
                    current.play()
                }
                VideoPlayCommand.NO_OP -> Unit
            }
        }
    }

    /** 已装载队列内跳转到指定索引，并保留每条媒体各自已保存的播放进度。 */
    private fun playLoadedPlaylistIndex(current: ExoPlayer, index: Int) {
        val activePlaylist = playbackQueue?.takeIf { it.isLoaded }?.playlist ?: return
        val target = activePlaylist.items.getOrNull(index) ?: return
        rememberPlaybackOf(_playbackState.value.media, current, captureFrame = true)
        //更新播放前状态
        _playbackState.value = _playbackState.value.replaceMedia(
            media = target,
            status = VideoPlaybackStatus.PREPARING,
            playlistIndex = index,
            playlistSize = activePlaylist.items.size,
        )
        current.seekTo(index, savedPositionsMs[target.cacheKey()].orEmptyPosition())
        // 播放失败后 ExoPlayer 会停在 ERROR/IDLE；仅 seekTo + play 无法重新加载目标媒体。
        if (current.playbackState == Player.STATE_IDLE || current.playerError != null) {
            current.prepare()
        }
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

    /** 页面主动点击播放时立即使用前台通知按钮，避免首次创建会话时沿用旧的后台快照。 */
    private fun markForegroundPlaybackRequest() {
        installProcessObserverIfNeeded()
        isAppInForeground = true
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
                revision = ++notificationControlsRevision,
            )
            return
        }
        val index = current?.currentMediaItemIndex ?: _playbackState.value.playlistIndex
        val count = activePlaylist.items.size
        _notificationControls.value = VideoNotificationControls(
            isAppInForeground = isAppInForeground,
            hasPrevious = index != null && index > 0 && count > 1,
            hasNext = index != null && index >= 0 && index < count - 1,
            revision = ++notificationControlsRevision,
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
        var token: VideoPlaybackControlToken,
        val playlist: VideoPlaylist,
        var isLoaded: Boolean = false,
    )

    private const val NOTIFICATION_SEEK_INCREMENT_MS = 15_000L
    private const val PROGRESS_UPDATE_INTERVAL_MS = 500L
    private const val MAX_PAUSED_FRAME_BYTES = 12 * 1024 * 1024
}
