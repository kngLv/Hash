package com.hash.video.player

import android.app.Application
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackControlToken
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertEquals
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

    @Test
    fun `队列按指定索引启动时同步当前媒体和队列位置`() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val application = instrumentation.targetContext.applicationContext as Application
        val token = VideoPlaybackControlToken()
        val first = VideoMedia("first", "https://example.com/first.mp4")
        val second = VideoMedia("second", "https://example.com/second.mp4")

        instrumentation.runOnMainSync {
            VideoPlayerEngine.release(application)
            VideoPlayerEngine.setPlaybackQueue(token, listOf(first, second))
            VideoPlayerEngine.playQueueAt(
                application = application,
                token = token,
                index = 1,
                showNotification = false,
                showNotificationArtwork = false,
            )

            val state = VideoPlayerEngine.playbackState.value
            assertEquals(second, state.media)
            assertEquals(1, state.playlistIndex)
            assertEquals(2, state.playlistSize)
            VideoPlayerEngine.release(application, token)
        }
    }
}
