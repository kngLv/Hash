package com.hash.video.player

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoPlayerEngineTest {

    @Test
    fun `释放后下次使用应创建新的播放器`() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        val previousPlayer = VideoPlayerEngine.get(application)

        VideoPlayerEngine.release(application)

        val newPlayer = VideoPlayerEngine.get(application)
        assertNotSame(previousPlayer, newPlayer)
        VideoPlayerEngine.release(application)
    }

    @Test
    fun `后台线程也可以安全释放播放引擎`() {
        val application = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as Application
        VideoPlayerEngine.get(application)
        val error = AtomicReference<Throwable?>()

        thread {
            runCatching { VideoPlayerEngine.release(application) }.onFailure(error::set)
        }.join()

        assertNull(error.get())
    }
}
