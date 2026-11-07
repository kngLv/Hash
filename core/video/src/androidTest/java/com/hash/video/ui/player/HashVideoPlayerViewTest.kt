package com.hash.video.ui.player

import android.view.View
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.api.VideoContentSize
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.ui.controls.EmbeddedVideoControls
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class EmbeddedVideoPlayerViewTest {

    private val media = VideoMedia("video-1", "https://example.com/video.mp4")

    @Test
    fun 内嵌控制栏渲染播放和缓冲进度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val controls = EmbeddedVideoControls(context)

            controls.render(
                VideoPlaybackState(
                    media = media,
                    status = VideoPlaybackStatus.PLAYING,
                    progress = VideoPlaybackProgress(
                        positionMs = 2_500L,
                        bufferedPositionMs = 7_500L,
                        durationMs = 10_000L,
                    ),
                ),
            )

            val seekBar = controls.findViewById<android.widget.SeekBar>(R.id.video_progress)
            assertEquals(250, seekBar.progress)
            assertEquals(750, seekBar.secondaryProgress)
            assertEquals(View.VISIBLE, seekBar.visibility)
            assertEquals("00:02 / 00:10", controls.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 手势跳转预览不应被旧的真实播放进度覆盖() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val controls = EmbeddedVideoControls(context)
            val oldState = VideoPlaybackState(
                media = media,
                status = VideoPlaybackStatus.PLAYING,
                progress = VideoPlaybackProgress(
                    positionMs = 2_500L,
                    bufferedPositionMs = 7_500L,
                    durationMs = 10_000L,
                ),
            )

            controls.render(oldState)
            controls.updateSeekPreview(positionMs = 8_000L, durationMs = 10_000L)
            controls.render(oldState)

            assertEquals(800, controls.findViewById<android.widget.SeekBar>(R.id.video_progress).progress)
            assertEquals("00:08 / 00:10", controls.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 内嵌控制栏分发播放切换() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val toggled = AtomicBoolean(false)
        instrumentation.runOnMainSync {
            val controls = EmbeddedVideoControls(context).apply {
                onPlaybackToggle = { toggled.set(true) }
            }
            controls.render(
                VideoPlaybackState(
                    media = media,
                    status = VideoPlaybackStatus.PAUSED,
                    progress = VideoPlaybackProgress(durationMs = 10_000L),
                ),
            )

            controls.findViewById<View>(R.id.video_play_toggle).performClick()
        }

        assertTrue(toggled.get())
    }

    @Test
    fun 加载状态显示进度并保留播放操作() {
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
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_play_toggle).visibility)
        }
    }

    @Test
    fun 播放状态隐藏中央操作并显示左下角暂停图标() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)
            view.setMedia(media)

            view.renderPlaybackState(
                VideoPlaybackState(media = media, status = VideoPlaybackStatus.PLAYING),
            )

            assertEquals(View.GONE, view.findViewById<View>(R.id.video_loading).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.video_play_action).visibility)
            assertEquals(
                context.getString(R.string.video_pause),
                view.findViewById<View>(R.id.video_play_toggle).contentDescription,
            )
        }
    }

    @Test
    fun 暂停状态显示中央播放和左下角播放图标() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)
            view.setMedia(media)

            view.renderPlaybackState(
                VideoPlaybackState(media = media, status = VideoPlaybackStatus.PAUSED),
            )

            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_play_action).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_play_toggle).visibility)
            assertEquals(
                context.getString(R.string.video_play),
                view.findViewById<View>(R.id.video_play_toggle).contentDescription,
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

            view.findViewById<android.view.View>(R.id.video_fullscreen_action).performClick()
        }

        assertTrue(invoked.get())
    }

    @Test
    fun 未配置展示迁移回调时隐藏对应操作() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)

            assertEquals(View.GONE, view.findViewById<View>(R.id.video_fullscreen_action).visibility)
            assertEquals(View.GONE, view.findViewById<View>(R.id.video_float_action).visibility)
        }
    }

    @Test
    fun 播放状态显示播放和缓冲进度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context)
            view.setMedia(media)

            view.renderPlaybackState(
                VideoPlaybackState(
                    media = media,
                    status = VideoPlaybackStatus.PLAYING,
                    progress = VideoPlaybackProgress(
                        positionMs = 500L,
                        bufferedPositionMs = 750L,
                        durationMs = 1_000L,
                    ),
                ),
            )

            val progressView = view.findViewById<android.widget.ProgressBar>(R.id.video_progress)
            assertEquals(View.VISIBLE, progressView.visibility)
            assertEquals(500, progressView.progress)
            assertEquals(750, progressView.secondaryProgress)
        }
    }

    @Test
    fun 竖屏视频按播放器宽度保持比例() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(360, 208)
                setMedia(media)
                layout(0, 0, 360, 208)
            }

            view.renderPlaybackState(
                VideoPlaybackState(
                    media = media,
                    status = VideoPlaybackStatus.PLAYING,
                    contentSize = VideoContentSize(width = 9, height = 16),
                ),
            )

            assertEquals(640, view.layoutParams.height)
        }
    }

    @Test
    fun RecyclerView复用为新媒体时立即清除旧进度并恢复默认高度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val oldMedia = VideoMedia("video-old", "https://example.com/old.mp4")
            val newMedia = VideoMedia("video-new", "https://example.com/new.mp4")
            val view = EmbeddedVideoPlayerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(360, 208)
                setMedia(oldMedia)
                layout(0, 0, 360, 208)
            }
            view.renderPlaybackState(
                VideoPlaybackState(
                    media = oldMedia,
                    status = VideoPlaybackStatus.PLAYING,
                    progress = VideoPlaybackProgress(
                        positionMs = 500L,
                        bufferedPositionMs = 750L,
                        durationMs = 1_000L,
                    ),
                    contentSize = VideoContentSize(width = 9, height = 16),
                ),
            )

            view.setMedia(newMedia)

            assertEquals(208, view.layoutParams.height)
            assertEquals(View.GONE, view.findViewById<View>(R.id.video_progress).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_play_action).visibility)
        }
    }
}
