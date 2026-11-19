package com.hash.video.ui.player

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.ui.bottombar.EmbeddedVideoBottomBar
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
            val bottomBar = EmbeddedVideoBottomBar(context)

            bottomBar.render(
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

            val seekBar = bottomBar.findViewById<android.widget.SeekBar>(R.id.video_progress)
            assertEquals(250, seekBar.progress)
            assertEquals(750, seekBar.secondaryProgress)
            assertEquals(View.VISIBLE, seekBar.visibility)
            assertEquals("00:02 / 00:10", bottomBar.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 手势跳转预览不应被旧的真实播放进度覆盖() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val bottomBar = EmbeddedVideoBottomBar(context)
            val oldState = VideoPlaybackState(
                media = media,
                status = VideoPlaybackStatus.PLAYING,
                progress = VideoPlaybackProgress(
                    positionMs = 2_500L,
                    bufferedPositionMs = 7_500L,
                    durationMs = 10_000L,
                ),
            )

            bottomBar.render(oldState)
            bottomBar.renderSeekPreview(positionMs = 8_000L, durationMs = 10_000L)
            bottomBar.render(oldState)

            assertEquals(800, bottomBar.findViewById<android.widget.SeekBar>(R.id.video_progress).progress)
            assertEquals("00:08 / 00:10", bottomBar.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 内嵌控制栏分发播放切换() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val toggled = AtomicBoolean(false)
        instrumentation.runOnMainSync {
            val bottomBar = EmbeddedVideoBottomBar(context).apply {
                setListener(object : EmbeddedVideoBottomBar.Listener {
                    override fun onTogglePlayback() = toggled.set(true)
                    override fun onSeek(positionMs: Long) = Unit
                })
            }
            bottomBar.render(
                VideoPlaybackState(
                    media = media,
                    status = VideoPlaybackStatus.PAUSED,
                    progress = VideoPlaybackProgress(durationMs = 10_000L),
                ),
            )

            bottomBar.findViewById<View>(R.id.video_play_toggle).performClick()
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
    fun 播放失败时显示具体错误文案() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = EmbeddedVideoPlayerView(context).apply { setMedia(media) }

            view.renderPlaybackState(
                VideoPlaybackState(
                    media = media,
                    status = VideoPlaybackStatus.ERROR,
                    errorMessage = "视频地址无访问权限或已失效",
                ),
            )

            val errorMessage = view.findViewById<TextView>(R.id.video_error_message)
            assertEquals(View.VISIBLE, errorMessage.visibility)
            assertEquals("视频地址无访问权限或已失效", errorMessage.text.toString())
            assertEquals(View.GONE, view.findViewById<View>(R.id.video_play_action).visibility)
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
    fun 配置的竖屏封面应按最终宽度更新高度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val scenario = androidx.test.core.app.ActivityScenario.launch(
            VideoPlayerViewTestActivity::class.java,
        )
        try {
            lateinit var view: EmbeddedVideoPlayerView
            scenario.onActivity { activity ->
                view = EmbeddedVideoPlayerView(activity).apply {
                    layoutParams = android.widget.FrameLayout.LayoutParams(360, 208)
                }
                activity.findViewById<android.view.ViewGroup>(android.R.id.content).addView(view)
                view.apply {
                    setMedia(media.copy(coverWidth = 9, coverHeight = 16))
                    layout(0, 0, 360, 208)
                }
                assertEquals(640, view.layoutParams.height)
            }
        } finally {
            scenario.close()
        }
    }

    @Test
    fun 已确定的内嵌尺寸不应随容器宽度变化自动重算() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val view = EmbeddedVideoPlayerView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(360, 208)
                setMedia(media.copy(coverWidth = 9, coverHeight = 16))
                layout(0, 0, 360, 208)
            }

            assertEquals(360, view.layoutParams.width)
            assertEquals(640, view.layoutParams.height)

            view.layout(0, 0, 640, 360)

            assertEquals(360, view.layoutParams.width)
            assertEquals(640, view.layoutParams.height)

            view.refreshDisplaySize()

            assertEquals(640, view.layoutParams.width)
            assertEquals(1_138, view.layoutParams.height)
        }
    }

    @Test
    fun RecyclerView复用新媒体时立即清除旧进度并应用新封面比例() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val oldMedia = VideoMedia("video-old", "https://example.com/old.mp4", coverWidth = 9, coverHeight = 16)
            val newMedia = VideoMedia("video-new", "https://example.com/new.mp4", coverWidth = 16, coverHeight = 9)
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
                ),
            )

            view.setMedia(newMedia)

            assertEquals(203, view.layoutParams.height)
            assertEquals(View.GONE, view.findViewById<View>(R.id.video_progress).visibility)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_play_action).visibility)
        }
    }
}
