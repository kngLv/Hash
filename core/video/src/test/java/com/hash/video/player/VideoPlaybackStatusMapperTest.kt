package com.hash.video.player

import androidx.media3.common.Player
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoPlaybackStatusMapperTest {

    @Test
    fun `播放中的就绪状态映射为播放状态`() {
        assertEquals(
            VideoPlaybackStatus.PLAYING,
            VideoPlaybackStatusMapper.from(Player.STATE_READY, isPlaying = true),
        )
    }

    @Test
    fun `暂停的就绪状态映射为暂停状态`() {
        assertEquals(
            VideoPlaybackStatus.PAUSED,
            VideoPlaybackStatusMapper.from(Player.STATE_READY, isPlaying = false),
        )
    }

    @Test
    fun `空闲缓冲和结束状态映射准确`() {
        assertEquals(
            VideoPlaybackStatus.IDLE,
            VideoPlaybackStatusMapper.from(Player.STATE_IDLE, isPlaying = false),
        )
        assertEquals(
            VideoPlaybackStatus.BUFFERING,
            VideoPlaybackStatusMapper.from(Player.STATE_BUFFERING, isPlaying = false),
        )
        assertEquals(
            VideoPlaybackStatus.ENDED,
            VideoPlaybackStatusMapper.from(Player.STATE_ENDED, isPlaying = false),
        )
    }
}
