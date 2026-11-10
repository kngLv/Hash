package com.hash.video.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HashVideoTest {

    @Test
    fun `释放后下次使用应创建新的播放控制器`() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val previousController = HashVideo.controller(context)

        HashVideo.release()

        val newController = HashVideo.controller(context)
        assertNotSame(previousController, newController)

        HashVideo.release()
    }

    @Test
    fun `后台线程也可以安全释放播放会话`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val controller = HashVideo.controller(instrumentation.targetContext)
        val connected = CountDownLatch(1)
        controller.withPlayer { connected.countDown() }
        assertTrue(connected.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
        val error = AtomicReference<Throwable?>()

        thread {
            runCatching(HashVideo::release).onFailure(error::set)
        }.join()

        assertNull(error.get())
        instrumentation.runOnMainSync(HashVideo::release)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
    }
}
