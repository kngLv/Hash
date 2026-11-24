package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackStateTransitionsTest {

    private val media = VideoMedia("video-1", "https://example.com/1.mp4")

    @Test
    fun `替换媒体状态应一次设置媒体状态进度和队列信息`() {
        val nextMedia = VideoMedia("video-2", "https://example.com/2.mp4")
        val state = VideoPlaybackState(
            media = media,
            status = VideoPlaybackStatus.ERROR,
            progress = VideoPlaybackProgress(positionMs = 300L, durationMs = 1_000L),
            errorMessage = "旧错误",
            playlistIndex = 0,
            playlistSize = 2,
        )

        val result = state.replaceMedia(
            media = nextMedia,
            status = VideoPlaybackStatus.PREPARING,
            playlistIndex = 1,
            playlistSize = 3,
        )

        assertEquals(nextMedia, result.media)
        assertEquals(VideoPlaybackStatus.PREPARING, result.status)
        assertEquals(VideoPlaybackProgress(), result.progress)
        assertEquals(null, result.errorMessage)
        assertEquals(1, result.playlistIndex)
        assertEquals(3, result.playlistSize)
    }

    @Test
    fun `重置播放会话应清空媒体进度错误和队列信息`() {
        val result = VideoPlaybackState(
            media = media,
            status = VideoPlaybackStatus.PLAYING,
            progress = VideoPlaybackProgress(positionMs = 300L, durationMs = 1_000L),
            errorMessage = "旧错误",
            playlistIndex = 0,
            playlistSize = 2,
        ).resetPlaybackSession()

        assertEquals(null, result.media)
        assertEquals(VideoPlaybackStatus.IDLE, result.status)
        assertEquals(VideoPlaybackProgress(), result.progress)
        assertEquals(null, result.errorMessage)
        assertEquals(null, result.playlistIndex)
        assertEquals(0, result.playlistSize)
    }
}
