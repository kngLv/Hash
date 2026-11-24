package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus

/**
 * 用新的当前媒体完整替换播放状态。
 *
 * [status] 由调用方在媒体切换边界确定：业务刚发起换源时传 [VideoPlaybackStatus.PREPARING]；
 * ExoPlayer 已完成自动切项时传其真实状态。这样不会先写入一个状态、再用 copy 覆盖它。
 */
internal fun VideoPlaybackState.replaceMedia(
    media: VideoMedia,
    status: VideoPlaybackStatus,
    playlistIndex: Int? = null,
    playlistSize: Int = 0,
): VideoPlaybackState = copy(
    media = media,
    status = status,
    progress = VideoPlaybackProgress(),
    errorMessage = null,
    playlistIndex = playlistIndex,
    playlistSize = playlistSize,
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
    errorMessage = null,
    playlistIndex = null,
    playlistSize = 0,
)
