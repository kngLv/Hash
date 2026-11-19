package com.hash.video.player

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlayerEngineTest {

    @Test
    fun `释放后下次使用应创建新的播放器`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        instrumentation.runOnMainSync {
            val previousPlayer = VideoPlayerEngine.get(application)
            VideoPlayerEngine.release(application)
            val newPlayer = VideoPlayerEngine.get(application)
            assertNotSame(previousPlayer, newPlayer)
            VideoPlayerEngine.release(application)
        }
    }

    @Test
    fun `后台线程调用播放引擎会明确失败`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        instrumentation.runOnMainSync { VideoPlayerEngine.get(application) }
        val error = AtomicReference<Throwable?>()

        thread {
            runCatching { VideoPlayerEngine.release(application) }.onFailure(error::set)
        }.join()

        assertNotNull(error.get())
        instrumentation.runOnMainSync { VideoPlayerEngine.release(application) }
    }
}
