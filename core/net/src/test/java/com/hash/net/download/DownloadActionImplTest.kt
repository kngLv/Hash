package com.hash.net.download

import com.hash.net.launch.download
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadActionImplTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cancellation does not trigger download error callback`() = runTest {
        var errorCallbackCalled = false
        val downloadDir = Files.createTempDirectory("hash-download-test").toFile()

        try {
            val job = download<ResponseBody> {
                throw CancellationException("cancelled")
            }
                .onError { errorCallbackCalled = true }
                .enqueue(
                    dir = downloadDir.absolutePath,
                    fileName = "test.apk",
                    scope = this,
                )

            job.join()

            assertTrue(job.isCancelled)
            assertFalse(errorCallbackCalled)
        } finally {
            downloadDir.deleteRecursively()
        }
    }
}
