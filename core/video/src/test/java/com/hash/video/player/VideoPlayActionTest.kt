package com.hash.video.player

import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoContentSize
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        ).resetPlaybackSession()

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
        ).resetPlaybackSession()

        assertEquals(null, releasedState.media)
        assertEquals(VideoPlaybackStatus.IDLE, releasedState.status)
        assertEquals(null, releasedState.errorMessage)
        assertEquals(VideoPlayAction.REPLACE, releasedState.resolvePlayAction(media))
    }

    @Test
    fun `视频尺寸应包含像素比例计算展示比例`() {
        val contentSize = VideoContentSize(width = 720, height = 1280, pixelWidthHeightRatio = 1.5f)

        assertEquals(0.84375f, contentSize.aspectRatio, 0.0001f)
        assertTrue(contentSize.isPortrait)
    }

    @Test
    fun `播放进度应限制在有效范围内`() {
        val progress = VideoPlaybackProgress(
            positionMs = 1_200L,
            bufferedPositionMs = 2_000L,
            durationMs = 1_000L,
        )

        assertEquals(1f, progress.fraction, 0f)
        assertEquals(1f, progress.bufferedFraction, 0f)
        assertTrue(progress.isAvailable)
        assertFalse(VideoPlaybackProgress().isAvailable)
    }
}
