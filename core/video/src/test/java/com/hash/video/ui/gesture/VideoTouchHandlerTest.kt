package com.hash.video.ui.gesture

import com.hash.video.api.VideoPlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTouchHandlerTest {

    @Test
    fun `双击应取消待执行单击并只回调播放切换`() {
        val scheduledActions = mutableListOf<Runnable>()
        val events = mutableListOf<VideoGestureEvent>()
        val handler = VideoTouchHandler(
            touchSlopPx = 8f,
            postDelayed = { action, _ -> scheduledActions += action },
            removeCallbacks = { scheduledActions.remove(it) },
            onGesture = { events += it },
        )
        val config = VideoGestureConfig(singleTapEnabled = true, doubleTapEnabled = true)
        val progress = VideoPlaybackProgress(positionMs = 0L, durationMs = 1_000L)

        handler.handle(touchEvent(VideoTouchHandler.Action.DOWN, eventTimeMs = 0L), config, progress)
        handler.handle(touchEvent(VideoTouchHandler.Action.UP, eventTimeMs = 20L), config, progress)

        assertEquals(1, scheduledActions.size)
        assertTrue(events.isEmpty())

        handler.handle(touchEvent(VideoTouchHandler.Action.DOWN, eventTimeMs = 100L), config, progress)
        handler.handle(touchEvent(VideoTouchHandler.Action.UP, eventTimeMs = 120L), config, progress)

        assertTrue(scheduledActions.isEmpty())
        assertEquals(listOf(VideoGestureEvent.TogglePlayback), events)
    }

    @Test
    fun `未开启双击时单击应立即回调`() {
        val events = mutableListOf<VideoGestureEvent>()
        val handler = VideoTouchHandler(
            touchSlopPx = 8f,
            postDelayed = { _, _ -> error("不应延迟单击") },
            removeCallbacks = {},
            onGesture = { events += it },
        )
        val config = VideoGestureConfig(singleTapEnabled = true)

        handler.handle(touchEvent(VideoTouchHandler.Action.DOWN, eventTimeMs = 0L), config, VideoPlaybackProgress())
        handler.handle(touchEvent(VideoTouchHandler.Action.UP, eventTimeMs = 20L), config, VideoPlaybackProgress())

        assertEquals(listOf(VideoGestureEvent.ToggleControls), events)
    }

    @Test
    fun `横向拖动收到取消事件时应通知界面结束预览`() {
        val events = mutableListOf<VideoGestureEvent>()
        val handler = VideoTouchHandler(
            touchSlopPx = 8f,
            postDelayed = { _, _ -> Unit },
            removeCallbacks = {},
            onGesture = { events += it },
        )
        val config = VideoGestureConfig(seekEnabled = true)
        val progress = VideoPlaybackProgress(positionMs = 0L, durationMs = 1_000L)

        handler.handle(touchEvent(VideoTouchHandler.Action.DOWN, eventTimeMs = 0L), config, progress)
        handler.handle(
            touchEvent(VideoTouchHandler.Action.MOVE, eventTimeMs = 20L, x = 200f),
            config,
            progress,
        )
        handler.handle(touchEvent(VideoTouchHandler.Action.CANCEL, eventTimeMs = 30L), config, progress)

        assertEquals(VideoGestureEvent.SeekCancelled, events.last())
    }

    private fun touchEvent(
        action: VideoTouchHandler.Action,
        eventTimeMs: Long,
        x: Float = 100f,
    ) = VideoTouchHandler.TouchEvent(
        action = action,
        x = x,
        y = 100f,
        pointerCount = 1,
        eventTimeMs = eventTimeMs,
        viewWidth = 300,
        viewHeight = 200,
    )
}
