package com.hash.net.converter

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.Buffer
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.io.OutputStreamWriter
import java.io.Writer
import java.lang.reflect.Type
import java.nio.charset.Charset


/**
 * @name LvConverterFactory
 * @package com.www.net.converter
 * @author 345 QQ:1831712732
 * @time 2020/6/22 20:21
 * @description ConverterFactory
 */
@Suppress("UNCHECKED_CAST")
class LvDefaultConverterFactory(private val gson: Gson) : Converter.Factory() {


    companion object {
        fun create(gson: Gson): LvDefaultConverterFactory {
            return LvDefaultConverterFactory(gson)
        }

        internal fun isScalarType(type: Type): Boolean {
            return type is Class<*> && type in SCALAR_TYPES
        }

        private val SCALAR_TYPES = setOf(
            String::class.java,
            Boolean::class.javaPrimitiveType,
            Boolean::class.javaObjectType,
            Byte::class.javaPrimitiveType,
            Byte::class.javaObjectType,
            Short::class.javaPrimitiveType,
            Short::class.javaObjectType,
            Int::class.javaPrimitiveType,
            Int::class.javaObjectType,
            Long::class.javaPrimitiveType,
            Long::class.javaObjectType,
            Float::class.javaPrimitiveType,
            Float::class.javaObjectType,
            Double::class.javaPrimitiveType,
            Double::class.javaObjectType,
            Char::class.javaPrimitiveType,
            Char::class.javaObjectType,
        )
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        if (type == RequestBody::class.java) {
            return null
        }
        val adapter = gson.getAdapter(TypeToken.get(type))
        return GsonRequestBodyConverter(gson, adapter)
    }

    override fun responseBodyConverter(
        type: Type, annotations: Array<Annotation>, retrofit: Retrofit
    ): Converter<ResponseBody, *>? {
        if (type == ResponseBody::class.java) {
            return null
        }
        if (isScalarType(type)) {
            return ScalarResponseBodyConverter(type as Class<*>)
        }
        return LvResponseBodyConverter<Any>(gson, type)
    }


    class LvResponseBodyConverter<T>(private val gson: Gson, private val type: Type) :
        Converter<ResponseBody, T> {
        override fun convert(value: ResponseBody): T? {
            val string = value.string()
            return gson.fromJson(string, type)
        }

    }

    private class ScalarResponseBodyConverter(
        private val type: Class<*>,
    ) : Converter<ResponseBody, Any> {

        override fun convert(value: ResponseBody): Any {
            return convertText(value.string())
        }

        private fun convertText(value: String): Any = when (type) {
            String::class.java -> value
            Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> value.toStrictBoolean()
            Byte::class.javaPrimitiveType, Byte::class.javaObjectType -> value.toByte()
            Short::class.javaPrimitiveType, Short::class.javaObjectType -> value.toShort()
            Int::class.javaPrimitiveType, Int::class.javaObjectType -> value.toInt()
            Long::class.javaPrimitiveType, Long::class.javaObjectType -> value.toLong()
            Float::class.javaPrimitiveType, Float::class.javaObjectType -> value.toFloat()
            Double::class.javaPrimitiveType, Double::class.javaObjectType -> value.toDouble()
            Char::class.javaPrimitiveType, Char::class.javaObjectType -> value.toSingleChar()
            else -> error("Unsupported scalar response type: $type")
        }

        private fun String.toStrictBoolean(): Boolean = when (this) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Expected true or false but was '$this'")
        }

        private fun String.toSingleChar(): Char {
            require(length == 1) { "Expected a single character but was '$this'" }
            return single()
        }
    }

}

internal class GsonRequestBodyConverter<T : Any>(
    private val gson: Gson,
    private val adapter: TypeAdapter<T>
) : Converter<T, RequestBody> {

    @Throws(IOException::class)
    override fun convert(value: T): RequestBody? {
        val buffer = Buffer()
        val writer: Writer =
            OutputStreamWriter(buffer.outputStream(), UTF_8)
        val jsonWriter = gson.newJsonWriter(writer)
        adapter.write(jsonWriter, value)
        jsonWriter.close()
        return buffer.readByteString().toRequestBody(MEDIA_TYPE)
    }

    companion object {
        private val MEDIA_TYPE: MediaType = "application/json; charset=UTF-8".toMediaType()
        private val UTF_8 = Charset.forName("UTF-8")
    }
}
