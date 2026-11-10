package com.hash.video.player

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotSame
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
}
