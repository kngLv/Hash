package com.hash.net.net.request

import com.hash.bean.home.HomeListBean
import com.hash.net.response.IResponse
import com.hash.net.launch.request
import com.hash.net.request.onBodyOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RequestActionExtensionsTest {

    @Test
    fun `onBodyOf should call block when response has data`() = runBlocking {
        val expected = HomeListBean(
            curPage = 1,
            datas = emptyList(),
            offset = 0,
            over = false,
            pageCount = 0,
            size = 0,
            total = 0
        )

        val deferred = CompletableDeferred<HomeListBean>()

        request {
            object : IResponse<HomeListBean> {
                override fun data(): HomeListBean = expected
                override fun code(): Int = 0
                override fun message(): String = ""
            }
        }
            .onBodyOf { body: HomeListBean ->
                deferred.complete(body)
            }
            .onNullData {
                deferred.completeExceptionally(AssertionError("onNullData should not be called"))
            }
            .onError { e ->
                deferred.completeExceptionally(e)
            }
            .enqueue()

        val result = deferred.await()
        assertEquals(expected, result)
    }

    @Test
    fun `onBodyOf should trigger onNullData when response data is null`() = runBlocking {
        val deferred = CompletableDeferred<Boolean>()

        request {
            object : IResponse<HomeListBean?> {
                override fun data(): HomeListBean? = null
                override fun code(): Int = 0
                override fun message(): String = ""
            }
        }
            .onBodyOf { _: HomeListBean? ->
                deferred.completeExceptionally(AssertionError("block should not be called when data is null"))
            }
            .onNullData {
                deferred.complete(true)
            }
            .onError { e ->
                deferred.completeExceptionally(e)
            }
            .enqueue()

        val called = deferred.await()
        assertTrue(called)
    }

    @Test
    fun `onBodyOf should trigger onError when data throws`() = runBlocking {
        val deferred = CompletableDeferred<Throwable>()

        request {
            object : IResponse<HomeListBean> {
                override fun data(): HomeListBean {
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
            .enqueue()

        val err = deferred.await()
        assertTrue(err is RuntimeException)
        assertEquals("data parse failed", err.message)
    }
}
