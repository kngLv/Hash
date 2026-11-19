package com.hash.video.cache

import androidx.media3.datasource.DataSpec
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.TimeUnit
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoCacheProviderHeadersTest {

    @Test
    fun `视频数据源会将媒体请求头发送给服务端`() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody("video-data"))
        server.start()

        val dataSource = VideoCacheProvider.getDataSourceFactory(
            InstrumentationRegistry.getInstrumentation().targetContext,
            mapOf(
                "Authorization" to "Bearer video-token",
                "Referer" to "https://hash.example/news",
            ),
        ).createDataSource()

        try {
            dataSource.open(
                DataSpec.Builder().setUri(server.url("/video.mp4").toString()).build(),
            )
            dataSource.read(ByteArray(32), 0, 32)

            val request = requireNotNull(server.takeRequest(5, TimeUnit.SECONDS)) {
                "未收到视频数据请求"
            }
            assertEquals("Bearer video-token", request.getHeader("Authorization"))
            assertEquals("https://hash.example/news", request.getHeader("Referer"))
        } finally {
            dataSource.close()
            server.shutdown()
        }
    }
}
