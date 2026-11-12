package com.hash.video.ui.render

import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.player.HashVideo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoRenderCoordinatorContainerTest {

    @Test
    fun 新容器接管画面后旧容器应自动移除画面() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            HashVideo.release()
            val firstContainer = FrameLayout(instrumentation.targetContext)
            val secondContainer = FrameLayout(instrumentation.targetContext)

            HashVideo.attachRenderTo(firstContainer)
            HashVideo.attachRenderTo(secondContainer)

            assertEquals(0, firstContainer.childCount)
            assertEquals(1, secondContainer.childCount)
            assertFalse(HashVideo.detachRenderFrom(firstContainer))
            assertTrue(HashVideo.detachRenderFrom(secondContainer))
            assertEquals(0, secondContainer.childCount)
            HashVideo.release()
        }
    }
}
