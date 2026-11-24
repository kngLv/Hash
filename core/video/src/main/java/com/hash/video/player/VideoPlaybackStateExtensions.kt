package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.isPlaying
import com.hash.video.api.isSameSourceAs

/** 当前状态正在播放，且指定媒体就是当前播放源。 */
internal fun VideoPlaybackState.isPlayingCurrentMedia(media: VideoMedia?): Boolean =
    status.isPlaying && media?.isSameSourceAs(this.media) == true
