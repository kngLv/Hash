package com.hash.video.ui.player

import android.content.Context
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoPlaybackState
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseHashVideoPlayerViewTest {

    @Test
    fun 子类拦截双击后不应进入默认处理后的通知() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext).apply {
                interceptedEvent = VideoGestureEvent.TogglePlayback
            }

            view.doubleTap()

            assertEquals(listOf(VideoGestureEvent.TogglePlayback), view.interceptedEvents)
            assertTrue(view.handledEvents.isEmpty())
        }
    }

    @Test
    fun 子类未拦截双击时应在默认处理后收到通知() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext)

            view.doubleTap()

            assertEquals(listOf(VideoGestureEvent.TogglePlayback), view.interceptedEvents)
            assertEquals(listOf(VideoGestureEvent.TogglePlayback), view.handledEvents)
        }
    }

    @Test
    fun 标准点击应交给子类决定是否处理控制栏事件() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext).apply {
                interceptedEvent = VideoGestureEvent.ToggleControls
            }
            var clickCount = 0
            view.setOnClickListener { clickCount++ }

            view.performClick()

            assertEquals(1, clickCount)
            assertEquals(listOf(VideoGestureEvent.ToggleControls), view.interceptedEvents)
            assertTrue(view.handledEvents.isEmpty())
        }
    }

    private fun TestVideoPlayerView.doubleTap() {
        dispatchTouch(MotionEvent.ACTION_DOWN, 0L)
        dispatchTouch(MotionEvent.ACTION_UP, 20L)
        dispatchTouch(MotionEvent.ACTION_DOWN, 100L)
        dispatchTouch(MotionEvent.ACTION_UP, 120L)
    }

    private fun TestVideoPlayerView.dispatchTouch(action: Int, eventTime: Long) {
        val event = MotionEvent.obtain(0L, eventTime, action, 100f, 100f, 0)
        presentationPlayerView.dispatchTouchEvent(event)
        event.recycle()
    }

    private class TestVideoPlayerView(context: Context) : BaseHashVideoPlayerView(context) {
        val interceptedEvents = mutableListOf<VideoGestureEvent>()
        val handledEvents = mutableListOf<VideoGestureEvent>()
        var interceptedEvent: VideoGestureEvent? = null

        override val gestureConfig = VideoGestureConfig(
            singleTapEnabled = true,
            doubleTapEnabled = true,
        )

        override fun onPlaybackStateChanged(state: VideoPlaybackState) = Unit

        override fun onInterceptVideoGesture(event: VideoGestureEvent): Boolean {
            interceptedEvents += event
            return event == interceptedEvent
        }

        override fun onVideoGesture(event: VideoGestureEvent) {
            handledEvents += event
        }
    }
}
