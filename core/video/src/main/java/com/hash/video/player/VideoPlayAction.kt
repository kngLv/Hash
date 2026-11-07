package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isSameSourceAs

/** `play(media)` 对当前播放会话应执行的最小动作。 */
internal enum class VideoPlayAction {
    /** 替换当前 MediaItem，并重新准备目标媒体。 */
    REPLACE,
    /** 保留当前进度，恢复已暂停的同一媒体。 */
    RESUME,
    /** 同一媒体已播放结束，跳回开头后重新播放。 */
    REPLAY,
    /** 目标媒体已在准备、缓冲或播放，无需重复发送播放指令。 */
    IGNORE,
}

/** 根据目标媒体与当前播放状态，决定切换、恢复、重播或忽略请求。 */
internal fun VideoPlaybackState.resolvePlayAction(media: VideoMedia): VideoPlayAction {
    if (media.isSameSourceAs(this.media).not()) return VideoPlayAction.REPLACE

    return when (status) {
        VideoPlaybackStatus.PAUSED -> VideoPlayAction.RESUME
        VideoPlaybackStatus.ENDED -> VideoPlayAction.REPLAY
        VideoPlaybackStatus.PREPARING,
        VideoPlaybackStatus.BUFFERING,
        VideoPlaybackStatus.PLAYING,
            -> VideoPlayAction.IGNORE

        VideoPlaybackStatus.IDLE,
        VideoPlaybackStatus.ERROR,
            -> VideoPlayAction.REPLACE
    }
}

/**
 * 为替换媒体或重播创建准备状态。
 *
 * 重播仍使用同一条媒体流，已解析的视频尺寸有效，不能清空；只有替换媒体时才丢弃上一条的尺寸，
 * 避免旧比例影响新视频的布局。
 */
internal fun VideoPlaybackState.prepareForPlay(
    media: VideoMedia,
    action: VideoPlayAction,
): VideoPlaybackState = copy(
    media = media,
    status = VideoPlaybackStatus.PREPARING,
    progress = VideoPlaybackProgress(),
    contentSize = if (action == VideoPlayAction.REPLACE) null else contentSize,
    errorMessage = null,
)

/**
 * 清除已停止或已移除媒体的会话状态。
 *
 * `Player.stop()` 会保留播放列表，因此 UI 状态必须同步清空当前媒体；否则下一次播放同一视频时会
 * 被误判为恢复播放，而不会重新设置 MediaItem 并 prepare。
 */
internal fun VideoPlaybackState.resetPlaybackSession(): VideoPlaybackState = copy(
    media = null,
    status = VideoPlaybackStatus.IDLE,
    progress = VideoPlaybackProgress(),
    contentSize = null,
    errorMessage = null,
)
