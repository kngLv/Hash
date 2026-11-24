package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isSameSourceAs

/** 一次播放请求需要交给 ExoPlayer 执行的命令。 */
internal enum class VideoPlayCommand {
    /** 替换当前 MediaItem，并重新准备目标媒体。 */
    REPLACE,
    /** 保留当前进度，恢复已暂停的同一媒体。 */
    RESUME,
    /** 同一媒体已播放结束，跳回开头后重新播放。 */
    REPLAY,
    /** 目标媒体已在准备、缓冲或播放，无需重复向 ExoPlayer 发送命令。 */
    NO_OP,
}

/** 根据目标媒体与当前播放状态，决定本次播放请求对应的播放器命令。 */
internal fun VideoPlaybackState.resolvePlayCommand(media: VideoMedia): VideoPlayCommand {
    if (media.isSameSourceAs(this.media).not()) return VideoPlayCommand.REPLACE

    return when (status) {
        VideoPlaybackStatus.PAUSED -> VideoPlayCommand.RESUME
        VideoPlaybackStatus.ENDED -> VideoPlayCommand.REPLAY
        VideoPlaybackStatus.PREPARING,
        VideoPlaybackStatus.BUFFERING,
        VideoPlaybackStatus.PLAYING,
            -> VideoPlayCommand.NO_OP

        VideoPlaybackStatus.IDLE,
        VideoPlaybackStatus.ERROR,
            -> VideoPlayCommand.REPLACE
    }
}
