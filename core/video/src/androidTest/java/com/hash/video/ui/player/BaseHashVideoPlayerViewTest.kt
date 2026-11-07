package com.hash.video.ui.player

import android.content.Context
import android.view.MotionEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoPlaybackState
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseHashVideoPlayerViewTest {

    @Test
    fun 双击应通知子类更新界面() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext)

            view.doubleTap()

            assertEquals(listOf(VideoGestureEvent.DoubleClick), view.handledEvents)
        }
    }

    @Test
    fun 预览结束应由统一的用户跳转处理入口接收() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext)

            view.dispatchUserSeek(positionMs = 4_500L, durationMs = 10_000L)

            assertEquals(4_500L to 10_000L, view.userSeek)
        }
    }

    @Test
    fun 标准点击应通知子类处理控制栏事件() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext)
            var clickCount = 0
            view.setOnClickListener { clickCount++ }

            view.performClick()

            assertEquals(1, clickCount)
            assertEquals(listOf(VideoGestureEvent.Click), view.handledEvents)
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
        val handledEvents = mutableListOf<VideoGestureEvent>()
        var userSeek: Pair<Long, Long>? = null

        override val gestureConfig = VideoGestureConfig(
            singleTapEnabled = true,
            doubleTapEnabled = true,
        )

        override fun onPlaybackStateChanged(state: VideoPlaybackState) = Unit

        override fun onVideoGesture(event: VideoGestureEvent) {
            handledEvents += event
        }

        override fun handleUserSeek(positionMs: Long, durationMs: Long) {
            userSeek = positionMs to durationMs
        }

        fun dispatchUserSeek(positionMs: Long, durationMs: Long) {
            handleUserSeek(positionMs, durationMs)
        }
    }
}
