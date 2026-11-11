package com.hash.video.presentation

import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPresentation
import com.hash.video.player.HashVideo
import com.hash.video.ui.player.FullscreenVideoPlayerView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoFullscreenLifecycleTest {

    @Test
    fun 全屏页面接管和退出不改变当前媒体与进度() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val controller = HashVideo.controller(context)
        val media = VideoMedia(
            id = "fullscreen-video",
            url = "https://example.com/fullscreen.mp4",
        )
        controller.play(media)
        val stateBeforeFullscreen = controller.playbackState.value
        val intent = VideoFullscreenActivity.startIntent(context)

        val activity = instrumentation.startActivitySync(intent)
        try {
            instrumentation.waitForIdleSync()
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            assertTrue(content.getChildAt(0) is FullscreenVideoPlayerView)
            assertEquals(VideoPresentation.FULLSCREEN, controller.playbackState.value.presentation)
            assertEquals(stateBeforeFullscreen.media, controller.playbackState.value.media)
            assertEquals(stateBeforeFullscreen.progress, controller.playbackState.value.progress)
        } finally {
            activity.finish()
            instrumentation.waitForIdleSync()
            assertEquals(VideoPresentation.EMBEDDED, controller.playbackState.value.presentation)
            HashVideo.release()
        }
    }

}
