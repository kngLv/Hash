package com.hash.video.ui

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class EmbeddedVideoPlayerViewTest {

    private val media = VideoMedia("video-1", "https://example.com/video.mp4")

    @Test
    fun 加载状态显示进度并隐藏播放操作() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)
            view.setMedia(media)

            view.renderPlaybackState(
                VideoPlaybackState(media = media, status = VideoPlaybackStatus.PREPARING),
            )

            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_loading).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.video_play_action).visibility)
        }
    }

    @Test
    fun 播放状态隐藏进度并显示暂停操作() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)
            view.setMedia(media)

            view.renderPlaybackState(
                VideoPlaybackState(media = media, status = VideoPlaybackStatus.PLAYING),
            )

            assertEquals(View.GONE, view.findViewById<View>(R.id.video_loading).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_play_action).visibility)
            assertEquals(
                context.getString(R.string.video_pause),
                view.findViewById<android.widget.TextView>(R.id.video_play_action).text,
            )
        }
    }

    @Test
    fun 点击全屏操作会通知宿主处理展示迁移() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val invoked = AtomicBoolean(false)
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context).apply {
                onFullscreenRequested = { invoked.set(true) }
            }

            view.findViewWithTag<android.view.View>("video_fullscreen_action").performClick()
        }

        assertTrue(invoked.get())
    }

    @Test
    fun 未配置展示迁移回调时隐藏对应操作() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)

            assertEquals(View.GONE, view.findViewWithTag<View>("video_fullscreen_action").visibility)
            assertEquals(View.GONE, view.findViewWithTag<View>("video_float_action").visibility)
        }
    }
}
