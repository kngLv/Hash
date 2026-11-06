package com.hash.video.player

import androidx.media3.common.Player
import com.hash.video.api.VideoPlaybackStatus

/** 将 Media3 播放状态映射为供业务层渲染的稳定播放状态。 */
object VideoPlaybackStatusMapper {

    fun from(playbackState: Int, isPlaying: Boolean): VideoPlaybackStatus = when (playbackState) {
        Player.STATE_IDLE -> VideoPlaybackStatus.IDLE
        Player.STATE_BUFFERING -> VideoPlaybackStatus.BUFFERING
        Player.STATE_READY -> if (isPlaying) VideoPlaybackStatus.PLAYING else VideoPlaybackStatus.PAUSED
        Player.STATE_ENDED -> VideoPlaybackStatus.ENDED
        else -> VideoPlaybackStatus.IDLE
    }
}
