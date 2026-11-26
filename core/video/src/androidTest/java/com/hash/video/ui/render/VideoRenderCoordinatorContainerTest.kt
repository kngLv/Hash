package com.hash.video.ui.render

import android.widget.FrameLayout
import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.player.VideoPlayerEngine
import com.hash.video.api.VideoPlaybackControlToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoRenderCoordinatorContainerTest {

    @Test
    fun 旧页面凭证不能释放当前页面的画面() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val context = instrumentation.targetContext
            val application = context.applicationContext as Application
            val currentToken = VideoPlaybackControlToken()
            val oldToken = VideoPlaybackControlToken()
            val container = FrameLayout(context)

            VideoPlayerEngine.release(application)
            VideoPlayerEngine.attachRenderTo(application, currentToken, container)
            VideoPlayerEngine.release(application, oldToken)

            assertEquals(1, container.childCount)
            VideoPlayerEngine.release(application, currentToken)
            assertEquals(0, container.childCount)
        }
    }

    @Test
    fun 新容器接管画面后旧容器应自动移除画面() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val application = instrumentation.targetContext.applicationContext as Application
            val token = VideoPlaybackControlToken()
            VideoPlayerEngine.release(application)
            val firstContainer = FrameLayout(instrumentation.targetContext)
            val secondContainer = FrameLayout(instrumentation.targetContext)

            VideoPlayerEngine.attachRenderTo(application, token, firstContainer)
            VideoPlayerEngine.attachRenderTo(application, token, secondContainer)

            assertEquals(0, firstContainer.childCount)
            assertEquals(1, secondContainer.childCount)
            assertFalse(VideoPlayerEngine.detachRenderFrom(token, firstContainer))
            assertTrue(VideoPlayerEngine.detachRenderFrom(token, secondContainer))
            assertEquals(0, secondContainer.childCount)
            VideoPlayerEngine.release(application)
        }
    }
}
