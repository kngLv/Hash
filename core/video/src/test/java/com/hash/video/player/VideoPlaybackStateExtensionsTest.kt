package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoPlaybackStateExtensionsTest {

    @Test
    fun `仅当前正在播放的媒体应返回真`() {
        val playingMedia = VideoMedia("video-a", "https://example.com/a.mp4")
        val nextMedia = VideoMedia("video-b", "https://example.com/b.mp4")
        val state = VideoPlaybackState(media = playingMedia, status = VideoPlaybackStatus.PLAYING)

        assertTrue(state.isPlayingCurrentMedia(playingMedia))
        assertFalse(state.isPlayingCurrentMedia(nextMedia))
    }
}
