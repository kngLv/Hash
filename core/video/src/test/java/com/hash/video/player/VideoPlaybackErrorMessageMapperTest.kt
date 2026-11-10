package com.hash.video.player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class VideoPlaybackErrorMessageMapperTest {

    @Test
    fun `403 应提示视频地址无访问权限或已失效`() {
        assertEquals(
            "视频地址无访问权限或已失效",
            VideoPlaybackErrorMessageMapper.fromHttpResponseCode(403),
        )
    }

    @Test
    fun `未知来源异常应使用通用播放失败提示`() {
        assertEquals(
            "视频播放失败，请稍后重试",
            VideoPlaybackErrorMessageMapper.from(IOException("source error")),
        )
    }
}
