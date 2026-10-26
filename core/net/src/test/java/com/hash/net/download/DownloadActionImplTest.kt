package com.hash.net.download

import com.hash.net.launch.download
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.IOException
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

    @Test
    fun `download callback failure is propagated instead of routed to onError`() = runTest {
        val failure = CompletableDeferred<Throwable>()
        var errorCallbackCalled = false
        val downloadDir = Files.createTempDirectory("hash-download-test").toFile()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, _ -> },
        )

        try {
            val job = download<ResponseBody> {
                Response.success("content".toResponseBody())
            }
                .onBegin { _, _ -> throw IllegalStateException("begin callback failed") }
                .onError { errorCallbackCalled = true }
                .enqueue(downloadDir.absolutePath, "test.apk", scope)
            job.invokeOnCompletion { error -> error?.let(failure::complete) }
            job.join()

            val error = withTimeout(1_000) { failure.await() }
            assertEquals("begin callback failed", error.message)
            assertFalse(errorCallbackCalled)
        } finally {
            downloadDir.deleteRecursively()
        }
    }

    @Test
    fun `truncated response is not promoted to final file`() = runTest {
        val failure = CompletableDeferred<Exception>()
        val downloadDir = Files.createTempDirectory("hash-download-test").toFile()
        val body = object : ResponseBody() {
            override fun contentType() = "application/octet-stream".toMediaType()
            override fun contentLength() = 4L
            override fun source() = Buffer().writeUtf8("abc")
        }

        try {
            val job = download<ResponseBody> { Response.success(body) }
                .onError { failure.complete(it) }
                .enqueue(downloadDir.absolutePath, "test.apk", this)

            job.join()

            assertTrue(withTimeout(1_000) { failure.await() } is IOException)
            assertFalse(downloadDir.resolve("test.apk").exists())
            assertTrue(downloadDir.resolve("test.apk.temp").exists())
        } finally {
            downloadDir.deleteRecursively()
        }
    }

    @Test
    fun `duplicate download does not replace an existing file with a stale temp file`() = runTest {
        val downloadDir = Files.createTempDirectory("hash-download-test").toFile()
        val original = downloadDir.resolve("test.apk")
        val staleTemp = downloadDir.resolve("test.apk.temp")
        original.writeText("original")
        staleTemp.writeText("stale")

        try {
            val job = download<ResponseBody> { existingLength ->
                assertEquals(0L, existingLength)
                Response.success("new-content".toResponseBody())
            }.enqueue(
                dir = downloadDir.absolutePath,
                fileName = "test.apk",
                scope = this,
                enableBreakpointResume = true,
                allowDuplicateFileName = true,
            )
            job.join()

            assertEquals("original", original.readText())
            assertEquals("new-content", downloadDir.resolve("test(1).apk").readText())
            assertEquals("stale", staleTemp.readText())
        } finally {
            downloadDir.deleteRecursively()
        }
    }
}
