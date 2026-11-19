package com.hash.video.ui.bottombar

import android.widget.SeekBar
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

@RunWith(AndroidJUnit4::class)
class FullscreenVideoBottomBarTest {

    @Test
    fun 用户结束拖动后应保持预览直到播放器返回目标位置() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bottomBar = FullscreenVideoBottomBar(instrumentation.targetContext)
            val oldState = VideoPlaybackState(
                media = VideoMedia("video", "https://example.com/video.mp4"),
                status = VideoPlaybackStatus.PLAYING,
                progress = VideoPlaybackProgress(positionMs = 2_500L, durationMs = 10_000L),
            )
            bottomBar.render(oldState)
            val seekBar = bottomBar.findViewById<SeekBar>(R.id.video_progress).apply { progress = 800 }
            bottomBar.onProgressChanged(seekBar, seekBar.progress, true)
            bottomBar.onStopTrackingTouch(seekBar)
            bottomBar.render(oldState)

            assertEquals(800, seekBar.progress)
        }
    }

    @Test
    fun 播放器到达跳转位置后应恢复真实进度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bottomBar = FullscreenVideoBottomBar(instrumentation.targetContext)
            val state = VideoPlaybackState(
                media = VideoMedia("video", "https://example.com/video.mp4"),
                status = VideoPlaybackStatus.PLAYING,
                progress = VideoPlaybackProgress(positionMs = 2_500L, durationMs = 10_000L),
            )
            bottomBar.render(state)
            val seekBar = bottomBar.findViewById<SeekBar>(R.id.video_progress).apply { progress = 800 }
            bottomBar.onProgressChanged(seekBar, seekBar.progress, true)
            bottomBar.onStopTrackingTouch(seekBar)

            bottomBar.render(state.copy(progress = state.progress.copy(positionMs = 8_000L)))
            bottomBar.render(state.copy(progress = state.progress.copy(positionMs = 8_500L)))

            assertEquals(850, seekBar.progress)
        }
    }

    @Test
    fun 取消拖动预览后应立即恢复最近的真实进度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val bottomBar = FullscreenVideoBottomBar(instrumentation.targetContext)
            val state = VideoPlaybackState(
                media = VideoMedia("video", "https://example.com/video.mp4"),
                status = VideoPlaybackStatus.PAUSED,
                progress = VideoPlaybackProgress(positionMs = 2_500L, durationMs = 10_000L),
            )
            bottomBar.render(state)
            bottomBar.renderSeekPreview(positionMs = 8_000L, durationMs = 10_000L)

            bottomBar.cancelSeekPreview()

            val seekBar = bottomBar.findViewById<SeekBar>(R.id.video_progress)
            assertEquals(250, seekBar.progress)
        }
    }
}
