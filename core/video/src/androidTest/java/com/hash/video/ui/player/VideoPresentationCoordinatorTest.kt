package com.hash.video.ui.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoPresentation
import com.hash.video.player.HashVideo
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPresentationCoordinatorTest {

    @Test
    fun attach只登记宿主而activate才切换画面承载状态() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext

        instrumentation.runOnMainSync {
            HashVideo.release()
            val controller = HashVideo.controller(context)
            val coordinator = VideoPresentationCoordinator(controller)
            val fullscreenHost = FullscreenVideoPlayerView(context)

            coordinator.attach(fullscreenHost)

            assertEquals(VideoPresentation.EMBEDDED, controller.playbackState.value.presentation)

            coordinator.activate(fullscreenHost)

            assertEquals(VideoPresentation.FULLSCREEN, controller.playbackState.value.presentation)

            coordinator.detach(fullscreenHost)
            assertEquals(VideoPresentation.EMBEDDED, controller.playbackState.value.presentation)
            HashVideo.release()
        }
    }
}
