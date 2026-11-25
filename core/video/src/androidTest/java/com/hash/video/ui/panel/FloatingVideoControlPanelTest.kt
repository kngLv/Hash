package com.hash.video.ui.panel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.ui.interaction.VideoInteractionIntent
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FloatingVideoControlPanelTest {

    @Test
    fun 控制栏应发出播放返回和关闭意图() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val panel = FloatingVideoControlPanel(instrumentation.targetContext)
            val intents = mutableListOf<VideoInteractionIntent>()
            panel.setIntentHandler(intents::add)
            panel.render(VideoPlaybackState(media = VideoMedia("id", "https://example.com/a.mp4")), true)

            panel.play.performClick()
            panel.restore.performClick()
            panel.close.performClick()

            assertEquals(
                listOf(
                    VideoInteractionIntent.TogglePlayback,
                    VideoInteractionIntent.RestoreFromOverlay,
                    VideoInteractionIntent.CloseOverlay,
                ),
                intents,
            )
        }
    }

    @Test
    fun 播放状态应更新播放按钮图标描述() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val panel = FloatingVideoControlPanel(instrumentation.targetContext)
            panel.render(
                VideoPlaybackState(
                    media = VideoMedia("id", "https://example.com/a.mp4"),
                    status = VideoPlaybackStatus.PLAYING,
                ),
                true,
            )

            assertEquals("暂停", panel.play.contentDescription)
        }
    }
}
