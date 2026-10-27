package com.hash.common

import com.hash.common.net.response.WanResponse
import com.hash.common.net.response.RollResponse
import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun `response data should remain null when server omits data`() {
        val response = WanResponse<String>(
            data = null,
            errorCode = 0,
            errorMsg = "",
        )

        assertNull(response.data())
    }

    @Test
    fun `roll response data should remain null when server omits data`() {
        val response = RollResponse<String>(
            data = null,
            code = 0,
            msg = "",
        )

        assertNull(response.data())
    }
}
