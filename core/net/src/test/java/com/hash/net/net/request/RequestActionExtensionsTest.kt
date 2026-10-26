package com.hash.net.net.request

import android.app.Application
import com.hash.net.LvHttp
import com.hash.net.launch.request
import com.hash.net.response.IResponse
import com.hash.net.request.onBodyOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RequestActionExtensionsTest {

    @Before
    fun setUp() {
        LvHttp.Builder(Application())
            .setBaseUrl("https://example.com/")
            .setCode(0)
            .build()
    }

    @Test
    fun `onBodyOf should call block when response has data`() = runBlocking {
        val expected = "value"

        val deferred = CompletableDeferred<String>()

        request {
            object : IResponse<String> {
                override fun data(): String = expected
                override fun code(): Int = 0
                override fun message(): String = ""
            }
        }
            .onBodyOf { body: String ->
                deferred.complete(body)
            }
            .onNullData {
                deferred.completeExceptionally(AssertionError("onNullData should not be called"))
            }
            .onError { e ->
                deferred.completeExceptionally(e)
            }
            .enqueue(this)

        val result = deferred.await()
        assertEquals(expected, result)
    }

    @Test
    fun `onBodyOf should trigger onNullData when response data is null`() = runBlocking {
        val deferred = CompletableDeferred<Boolean>()

        request {
            object : IResponse<String?> {
                override fun data(): String? = null
                override fun code(): Int = 0
                override fun message(): String = ""
            }
        }
            .onBodyOf { _: String? ->
                deferred.completeExceptionally(AssertionError("block should not be called when data is null"))
            }
            .onNullData {
                deferred.complete(true)
            }
            .onError { e ->
                deferred.completeExceptionally(e)
            }
            .enqueue(this)

        val called = deferred.await()
        assertTrue(called)
    }

    @Test
    fun `onBodyOf should trigger onError when data throws`() = runBlocking {
        val deferred = CompletableDeferred<Throwable>()
        val ended = CompletableDeferred<Boolean>()

        request {
            object : IResponse<String> {
                override fun data(): String {
                    throw RuntimeException("data parse failed")
                }

                override fun code(): Int = 0
                override fun message(): String = "err"
            }
        }
            .onBodyOf { _ ->
                deferred.completeExceptionally(AssertionError("block should not be called when data throws"))
            }
            .onNullData {
                deferred.completeExceptionally(AssertionError("onNullData should not be called when data throws"))
            }
            .onError { e ->
                deferred.complete(e)
            }
            .onEnd { ended.complete(it) }
            .enqueue(this)

        val err = withTimeout(1_000) { deferred.await() }
        assertTrue(err is RuntimeException)
        assertEquals("data parse failed", err.message)
        assertFalse(ended.await())
    }

    @Test
    fun `onBodyOf business callback failure is not routed to onError`() = runBlocking {
        val failure = CompletableDeferred<Throwable>()
        var errorCallbackCalled = false
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, error ->
                failure.complete(error)
            },
        )

        request {
            object : IResponse<String> {
                override fun data(): String = "value"
                override fun code(): Int = 0
                override fun message(): String = ""
            }
        }
            .onBodyOf { throw IllegalStateException("business callback failed") }
            .onError { errorCallbackCalled = true }
            .enqueue(scope)

        val error = withTimeout(1_000) { failure.await() }
        assertEquals("business callback failed", error.message)
        assertFalse(errorCallbackCalled)
    }

    @Test
    fun `onErrorCode callback failure is propagated to caller scope`() = runBlocking {
        val failure = CompletableDeferred<Throwable>()
        val ended = CompletableDeferred<Boolean>()
        val scope = CoroutineScope(
            SupervisorJob() + Dispatchers.Unconfined + CoroutineExceptionHandler { _, error ->
                failure.complete(error)
            },
        )

        request {
            object : IResponse<String> {
                override fun data(): String = "value"
                override fun code(): Int = 1
                override fun message(): String = "business error"
            }
        }
            .onErrorCode { _, _ -> throw IllegalStateException("error code callback failed") }
            .onEnd { ended.complete(it) }
            .enqueue(scope)

        val error = withTimeout(1_000) { failure.await() }
        assertEquals("error code callback failed", error.message)
        assertFalse(ended.await())
    }

    @Test
    fun `executeState rethrows cancellation`() = runBlocking {
        val action = request<String> { throw CancellationException("cancelled") }

        try {
            action.request()
            throw AssertionError("CancellationException was expected")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
    }
}
