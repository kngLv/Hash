package com.hash.video.ui.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoGestureDetectorTest {

    @Test
    fun `双击应产生播放切换事件`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(doubleTapEnabled = true)

        detector.onDown(x = 100f, y = 100f, viewWidth = 300, viewHeight = 200, currentPositionMs = 0L, durationMs = 1_000L)
        assertNull(detector.onUp(x = 100f, y = 100f, eventTimeMs = 20L, config = config))
        detector.onDown(x = 100f, y = 100f, viewWidth = 300, viewHeight = 200, currentPositionMs = 0L, durationMs = 1_000L)

        assertEquals(VideoGestureEvent.DoubleClick, detector.onUp(100f, 100f, 120L, config))
    }

    @Test
    fun `相距过远的两次点击不应识别为双击`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f, doubleTapSlopPx = 64f)
        val config = VideoGestureConfig(singleTapEnabled = true, doubleTapEnabled = true)

        detector.onDown(x = 20f, y = 100f, viewWidth = 300, viewHeight = 200, currentPositionMs = 0L, durationMs = 1_000L)
        assertEquals(VideoGestureEvent.Click, detector.onUp(20f, 100f, 20L, config))
        detector.onDown(x = 280f, y = 100f, viewWidth = 300, viewHeight = 200, currentPositionMs = 0L, durationMs = 1_000L)

        assertEquals(VideoGestureEvent.Click, detector.onUp(280f, 100f, 120L, config))
    }

    @Test
    fun `有效横向拖动应先预览并在抬手时提交跳转`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(seekEnabled = true)
        detector.onDown(x = 100f, y = 100f, viewWidth = 400, viewHeight = 200, currentPositionMs = 2_000L, durationMs = 10_000L)

        assertEquals(VideoGestureEvent.SeekPreview(4_500L, 10_000L), detector.onMove(200f, 105f, 1, config))
        assertEquals(VideoGestureEvent.SeekPreviewEnd(4_500L), detector.onUp(200f, 105f, 100L, config))
    }

    @Test
    fun `未知时长的横向拖动不应产生跳转`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(seekEnabled = true)
        detector.onDown(x = 100f, y = 100f, viewWidth = 400, viewHeight = 200, currentPositionMs = 0L, durationMs = 0L)

        assertNull(detector.onMove(200f, 105f, 1, config))
        assertNull(detector.onUp(200f, 105f, 100L, config))
    }

    @Test
    fun `方向锁定后不应被后续垂直位移改写`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(seekEnabled = true, brightnessEnabled = true)
        detector.onDown(x = 100f, y = 100f, viewWidth = 400, viewHeight = 200, currentPositionMs = 0L, durationMs = 10_000L)

        detector.onMove(140f, 105f, 1, config)

        assertEquals(VideoGestureEvent.SeekPreview(1_125L, 10_000L), detector.onMove(145f, 300f, 1, config))
    }

    @Test
    fun `多指触摸应取消当前视频手势`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(seekEnabled = true)
        detector.onDown(x = 100f, y = 100f, viewWidth = 400, viewHeight = 200, currentPositionMs = 0L, durationMs = 10_000L)

        assertNull(detector.onMove(200f, 100f, 2, config))
        assertNull(detector.onUp(200f, 100f, 100L, config))
    }

    @Test
    fun `横向拖动被取消时应结束跳转预览`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(seekEnabled = true)
        detector.onDown(x = 100f, y = 100f, viewWidth = 400, viewHeight = 200, currentPositionMs = 0L, durationMs = 10_000L)
        detector.onMove(200f, 100f, 1, config)

        assertEquals(VideoGestureEvent.SeekPreviewCancel, detector.cancel(config))
    }

    @Test
    fun `亮度手势移动和抬手应分别发出更新与结束阶段`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(brightnessEnabled = true)
        detector.onDown(x = 50f, y = 150f, viewWidth = 400, viewHeight = 200, currentPositionMs = 0L, durationMs = 0L)

        assertEquals(
            VideoGestureEvent.BrightnessAdjustment(0.5f, VideoGesturePhase.UPDATE),
            detector.onMove(50f, 50f, 1, config),
        )
        assertEquals(
            VideoGestureEvent.BrightnessAdjustment(0.5f, VideoGesturePhase.END),
            detector.onUp(50f, 50f, 100L, config),
        )
    }

    @Test
    fun `音量手势被取消时应发出取消阶段`() {
        val detector = VideoGestureDetector(touchSlopPx = 8f)
        val config = VideoGestureConfig(volumeEnabled = true)
        detector.onDown(x = 350f, y = 150f, viewWidth = 400, viewHeight = 200, currentPositionMs = 0L, durationMs = 0L)
        detector.onMove(350f, 50f, 1, config)

        assertEquals(
            VideoGestureEvent.VolumeAdjustment(0.5f, VideoGesturePhase.CANCEL),
            detector.cancel(config),
        )
    }
}
