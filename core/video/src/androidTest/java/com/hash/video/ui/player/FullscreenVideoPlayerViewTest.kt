package com.hash.video.ui.player

import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class FullscreenVideoPlayerViewTest {

    private val media = VideoMedia("video-1", "https://example.com/video.mp4")

    @Test
    fun 全屏控制栏按三层结构渲染时间和进度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = FullscreenVideoPlayerView(context).apply { setMedia(media) }

            view.renderPlaybackState(
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

            assertEquals("00:02 / 00:10", view.findViewById<TextView>(R.id.video_time).text.toString())
            assertEquals(250, view.findViewById<SeekBar>(R.id.video_progress).progress)
            assertEquals(750, view.findViewById<SeekBar>(R.id.video_progress).secondaryProgress)
            assertEquals(View.VISIBLE, view.findViewById<View>(R.id.video_fullscreen_controls).visibility)
        }
    }

    @Test
    fun 退出全屏操作只通知宿主处理展示迁移() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val invoked = AtomicBoolean(false)
        instrumentation.runOnMainSync {
            val view = FullscreenVideoPlayerView(instrumentation.targetContext).apply {
                onExitFullscreenRequested = { invoked.set(true) }
            }

            view.findViewById<View>(R.id.video_exit_fullscreen_action).performClick()
        }

        org.junit.Assert.assertTrue(invoked.get())
    }

    @Test
    fun 切换媒体时清除全屏进度预览() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val oldMedia = media
            val view = FullscreenVideoPlayerView(context).apply { setMedia(oldMedia) }
            view.renderPlaybackState(
                VideoPlaybackState(
                    media = oldMedia,
                    status = VideoPlaybackStatus.PAUSED,
                    progress = VideoPlaybackProgress(positionMs = 5_000L, durationMs = 10_000L),
                ),
            )
            view.findViewById<SeekBar>(R.id.video_progress).apply {
                progress = 800
            }

            view.setMedia(VideoMedia("video-2", "https://example.com/video-2.mp4"))

            assertEquals(0, view.findViewById<SeekBar>(R.id.video_progress).progress)
            assertEquals("00:00 / 00:00", view.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 播放失败时显示具体错误文案() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        instrumentation.runOnMainSync {
            val view = FullscreenVideoPlayerView(context).apply { setMedia(media) }

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
        }
    }
}
