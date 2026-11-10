package com.hash.video.ui.progress

import android.widget.SeekBar
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.R
import com.hash.video.api.VideoPlaybackProgress
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class DefaultVideoProgressViewTest {

    @Test
    fun 渲染真实进度时显示播放缓冲和时间() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DefaultVideoProgressView(context)

            view.renderProgress(
                VideoPlaybackProgress(
                    positionMs = 2_500L,
                    bufferedPositionMs = 7_500L,
                    durationMs = 10_000L,
                ),
            )

            assertEquals(250, view.findViewById<SeekBar>(R.id.video_progress).progress)
            assertEquals(750, view.findViewById<SeekBar>(R.id.video_progress).secondaryProgress)
            assertEquals("00:02 / 00:10", view.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 待确认跳转期间应持续显示用户预览位置() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DefaultVideoProgressView(context)
            val oldProgress = VideoPlaybackProgress(
                positionMs = 2_500L,
                bufferedPositionMs = 7_500L,
                durationMs = 10_000L,
            )

            view.renderProgress(oldProgress)
            view.renderSeekPreview(positionMs = 8_000L, durationMs = 10_000L)
            view.renderProgress(oldProgress)

            assertEquals(800, view.findViewById<SeekBar>(R.id.video_progress).progress)
            assertEquals("00:08 / 00:10", view.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 收到跳转目标位置后应结束预览并接收后续真实进度() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DefaultVideoProgressView(context)
            val oldProgress = VideoPlaybackProgress(
                positionMs = 2_500L,
                bufferedPositionMs = 7_500L,
                durationMs = 10_000L,
            )

            view.renderProgress(oldProgress)
            view.renderSeekPreview(positionMs = 8_000L, durationMs = 10_000L)
            view.endSeekPreview()
            view.renderProgress(oldProgress.copy(positionMs = 8_000L))
            view.renderProgress(oldProgress.copy(positionMs = 8_500L))

            assertEquals(850, view.findViewById<SeekBar>(R.id.video_progress).progress)
            assertEquals("00:08 / 00:10", view.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 收到接近跳转目标的真实位置后应结束预览() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DefaultVideoProgressView(context)
            val oldProgress = VideoPlaybackProgress(
                positionMs = 2_500L,
                bufferedPositionMs = 7_500L,
                durationMs = 10_000L,
            )

            view.renderProgress(oldProgress)
            view.renderSeekPreview(positionMs = 8_000L, durationMs = 10_000L)
            view.endSeekPreview()
            view.renderProgress(oldProgress.copy(positionMs = 8_300L))
            view.renderProgress(oldProgress.copy(positionMs = 8_500L))

            assertEquals(850, view.findViewById<SeekBar>(R.id.video_progress).progress)
        }
    }

    @Test
    fun 取消拖动预览时应立即恢复最后一次真实进度() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DefaultVideoProgressView(context)
            view.renderProgress(
                VideoPlaybackProgress(
                    positionMs = 2_500L,
                    bufferedPositionMs = 7_500L,
                    durationMs = 10_000L,
                ),
            )
            view.renderSeekPreview(positionMs = 8_000L, durationMs = 10_000L)

            view.cancelSeekPreview()

            assertEquals(250, view.findViewById<SeekBar>(R.id.video_progress).progress)
            assertEquals("00:02 / 00:10", view.findViewById<TextView>(R.id.video_time).text.toString())
        }
    }

    @Test
    fun 用户结束拖动时应提交目标位置() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val positionMs = AtomicLong(-1L)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = DefaultVideoProgressView(context).apply {
                onSeekRequested = positionMs::set
            }
            view.renderProgress(VideoPlaybackProgress(durationMs = 10_000L))
            val seekBar = view.findViewById<SeekBar>(R.id.video_progress).apply { progress = 600 }

            view.onStopTrackingTouch(seekBar)
        }

        assertEquals(6_000L, positionMs.get())
    }
}
