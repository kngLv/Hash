package com.hash.net.converter

import com.google.gson.Gson
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun `plain string response is not parsed as JSON`() {
        val converter = LvDefaultConverterFactory.create(Gson()).responseBodyConverter(
            String::class.java,
            emptyArray(),
            Retrofit.Builder().baseUrl("https://example.com/").build(),
        ) as retrofit2.Converter<ResponseBody, String>

        assertEquals("payment-token", converter.convert("payment-token".toResponseBody()))
    }

    @Test
    fun `plain character response is not parsed as JSON`() {
        val converter = LvDefaultConverterFactory.create(Gson()).responseBodyConverter(
            Char::class.java,
            emptyArray(),
            Retrofit.Builder().baseUrl("https://example.com/").build(),
        ) as retrofit2.Converter<ResponseBody, Char>

        assertEquals('A', converter.convert("A".toResponseBody()))
    }

    @Test
    fun `scalar types use the text converter while objects use Gson`() {
        assertTrue(LvDefaultConverterFactory.isScalarType(String::class.java))
        assertTrue(LvDefaultConverterFactory.isScalarType(Int::class.javaObjectType))
        assertTrue(LvDefaultConverterFactory.isScalarType(Boolean::class.javaObjectType))
        assertFalse(LvDefaultConverterFactory.isScalarType(TestPayload::class.java))
    }

    private data class TestPayload(val value: String)
}
