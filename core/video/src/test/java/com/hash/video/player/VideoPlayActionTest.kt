package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlayActionTest {

    private val media = VideoMedia("video-1", "https://example.com/1.mp4")

    @Test
    fun `同一暂停视频应恢复播放`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.PAUSED)

        assertEquals(VideoPlayAction.RESUME, state.resolvePlayAction(media))
    }

    @Test
    fun `同一视频更新展示信息不应重新加载`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.PAUSED)
        val updatedPresentation = media.copy(title = "更新后的标题", coverUrl = "https://example.com/cover.jpg")

        assertEquals(VideoPlayAction.RESUME, state.resolvePlayAction(updatedPresentation))
    }

    @Test
    fun `同一加载或播放中的视频应忽略重复播放`() {
        assertEquals(
            VideoPlayAction.IGNORE,
            VideoPlaybackState(media = media, status = VideoPlaybackStatus.PREPARING)
                .resolvePlayAction(media),
        )
        assertEquals(
            VideoPlayAction.IGNORE,
            VideoPlaybackState(media = media, status = VideoPlaybackStatus.BUFFERING)
                .resolvePlayAction(media),
        )
        assertEquals(
            VideoPlayAction.IGNORE,
            VideoPlaybackState(media = media, status = VideoPlaybackStatus.PLAYING)
                .resolvePlayAction(media),
        )
    }

    @Test
    fun `同一结束视频应从头重播`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.ENDED)

        assertEquals(VideoPlayAction.REPLAY, state.resolvePlayAction(media))
    }

    @Test
    fun `不同视频应替换当前媒体`() {
        val state = VideoPlaybackState(media = media, status = VideoPlaybackStatus.PLAYING)
        val nextMedia = VideoMedia("video-2", "https://example.com/2.mp4")

        assertEquals(VideoPlayAction.REPLACE, state.resolvePlayAction(nextMedia))
    }

    @Test
    fun `停止后再次播放同一视频应重新准备媒体`() {
        val stoppedState = VideoPlaybackState(
            media = media,
            status = VideoPlaybackStatus.PLAYING,
            errorMessage = "之前的错误",
        ).resetAfterStop()

        assertEquals(null, stoppedState.media)
        assertEquals(VideoPlaybackStatus.IDLE, stoppedState.status)
        assertEquals(null, stoppedState.errorMessage)
        assertEquals(VideoPlayAction.REPLACE, stoppedState.resolvePlayAction(media))
    }

    @Test
    fun `移除当前媒体后不应保留可恢复的播放状态`() {
        val releasedState = VideoPlaybackState(
            media = media,
            status = VideoPlaybackStatus.PLAYING,
            errorMessage = "之前的错误",
        ).resetAfterCurrentMediaRemoved()

        assertEquals(null, releasedState.media)
        assertEquals(VideoPlaybackStatus.IDLE, releasedState.status)
        assertEquals(null, releasedState.errorMessage)
        assertEquals(VideoPlayAction.REPLACE, releasedState.resolvePlayAction(media))
    }
}
