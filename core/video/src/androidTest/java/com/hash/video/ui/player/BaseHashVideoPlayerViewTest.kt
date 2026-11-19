package com.hash.video.ui.player

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.media3.ui.PlayerView
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoMedia
import com.hash.video.player.VideoPlayerEngine
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.panel.AbstractVideoControlPanel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaseHashVideoPlayerViewTest {

    @Test
    fun 双击应由控制层通知宿主执行播放切换() {
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
    fun 标准点击应仅通知播放器监听器() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = TestVideoPlayerView(instrumentation.targetContext)
            var clickCount = 0
            view.setOnClickListener { clickCount++ }

            view.performClick()

            assertEquals(1, clickCount)
            assertEquals(0, view.controlToggleCount)
        }
    }

    @Test
    fun 相同媒体的新嵌入View不应抢占已有画面() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(
            VideoPlayerViewTestActivity::class.java,
        )
        try {
            scenario.onActivity { activity ->
                val media = VideoMedia("same-media", "https://example.com/same.mp4")
                val first = TestVideoPlayerView(activity).apply { setMedia(media) }
                val second = TestVideoPlayerView(activity).apply { setMedia(media) }
                activity.findViewById<ViewGroup>(android.R.id.content).addView(first)
                first.play()

                activity.findViewById<ViewGroup>(android.R.id.content).addView(second)

                assertTrue(first.getChildAt(0) is PlayerView)
                assertTrue(second.getChildAt(0) !is PlayerView)
            }
        } finally {
            scenario.onActivity { activity ->
                VideoPlayerEngine.release(activity.application)
            }
            scenario.close()
        }
    }

    @Test
    fun 显式接管画面不应重新发起播放请求() {
        val scenario = androidx.test.core.app.ActivityScenario.launch(
            VideoPlayerViewTestActivity::class.java,
        )
        try {
            scenario.onActivity { activity ->
                val view = TestVideoPlayerView(activity)
                activity.findViewById<ViewGroup>(android.R.id.content).addView(view)

                view.attachRender()

                assertTrue(view.getChildAt(0) is PlayerView)
            }
        } finally {
            scenario.onActivity { activity ->
                VideoPlayerEngine.release(activity.application)
            }
            scenario.close()
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
        (getChildAt(childCount - 1) as ViewGroup).getChildAt(0).dispatchTouchEvent(event)
        event.recycle()
    }

    private class TestVideoPlayerView(context: Context) : BaseHashVideoPlayerView(context) {
        private val panel = TestVideoControlPanel(context)
        val handledEvents: List<VideoGestureEvent>
            get() = panel.handledEvents
        var userSeek: Pair<Long, Long>? = null
        val controlToggleCount: Int
            get() = panel.toggleCount

        init {
            installControlPanel(
                panel = panel,
                gestureConfig = com.hash.video.ui.gesture.VideoGestureConfig(
                    singleTapEnabled = true,
                    doubleTapEnabled = true,
                ),
            )
        }

        override fun onPlaybackStateChanged(state: VideoPlaybackState) = Unit

        override fun handleUserSeek(positionMs: Long, durationMs: Long) {
            userSeek = positionMs to durationMs
        }

        fun dispatchUserSeek(positionMs: Long, durationMs: Long) {
            handleUserSeek(positionMs, durationMs)
        }
    }

    private class TestVideoControlPanel(context: Context) : AbstractVideoControlPanel(context) {
        val handledEvents = mutableListOf<VideoGestureEvent>()
        var toggleCount = 0

        override fun toggleControls() {
            toggleCount++
        }

        override fun render(state: VideoPlaybackState, hasMedia: Boolean) = Unit

        override fun onGesture(event: VideoGestureEvent) {
            handledEvents += event
        }

    }
}

class VideoPlayerViewTestActivity : ComponentActivity()
