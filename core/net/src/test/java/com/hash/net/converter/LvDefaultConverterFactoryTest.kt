package com.hash.net.converter

import com.google.gson.Gson
import okhttp3.ResponseBody
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.Retrofit

class LvDefaultConverterFactoryTest {

    @Test
    fun `ResponseBody is left to Retrofit built-in converter`() {
        val converter = LvDefaultConverterFactory.create(Gson()).responseBodyConverter(
            ResponseBody::class.java,
            emptyArray(),
            Retrofit.Builder().baseUrl("https://example.com/").build(),
        )

        assertNull(converter)
    }
}
