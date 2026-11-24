package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlayCommandTest {

    private val media = VideoMedia("video-1", "https://example.com/1.mp4")

    @Test
    fun `同一暂停视频应恢复播放`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.PAUSED)

        assertEquals(VideoPlayCommand.RESUME, state.resolvePlayCommand(media))
    }

    @Test
    fun `同一视频更新展示信息不应重新加载`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.PAUSED)
        val updatedPresentation = media.copy(title = "更新后的标题", coverUrl = "https://example.com/cover.jpg")

        assertEquals(VideoPlayCommand.RESUME, state.resolvePlayCommand(updatedPresentation))
    }

    @Test
    fun `同一加载或播放中的视频应忽略重复播放`() {
        assertEquals(
            VideoPlayCommand.NO_OP,
            VideoPlaybackState(media = media, status = VideoPlaybackStatus.PREPARING)
                .resolvePlayCommand(media),
        )
        assertEquals(
            VideoPlayCommand.NO_OP,
            VideoPlaybackState(media = media, status = VideoPlaybackStatus.BUFFERING)
                .resolvePlayCommand(media),
        )
        assertEquals(
            VideoPlayCommand.NO_OP,
            VideoPlaybackState(media = media, status = VideoPlaybackStatus.PLAYING)
                .resolvePlayCommand(media),
        )
    }

    @Test
    fun `同一结束视频应从头重播`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.ENDED)

        assertEquals(VideoPlayCommand.REPLAY, state.resolvePlayCommand(media))
    }

    @Test
    fun `不同视频应替换当前媒体`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.PLAYING)
        val nextMedia = VideoMedia("video-2", "https://example.com/2.mp4")

        assertEquals(VideoPlayCommand.REPLACE, state.resolvePlayCommand(nextMedia))
    }

}
