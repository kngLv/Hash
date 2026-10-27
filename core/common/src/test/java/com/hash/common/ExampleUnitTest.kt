package com.hash.common

import com.google.gson.annotations.SerializedName
import com.hash.common.net.response.WanResponse
import com.hash.common.net.response.RollResponse
import com.hash.common.shared.model.CoinInfo
import com.hash.common.shared.model.UserInfo
import com.hash.common.shared.model.UserInfoBean
import org.junit.Test

import org.junit.Assert.*
import java.lang.reflect.Modifier

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

    @Test
    fun `shared user response fields should declare serialized names`() {
        listOf(
            UserInfoBean::class.java,
            CoinInfo::class.java,
            UserInfo::class.java,
        ).forEach(::assertJsonFieldsAreAnnotated)
    }

    @Test
    fun `response wrappers should declare serialized names`() {
        listOf(
            WanResponse::class.java,
            RollResponse::class.java,
        ).forEach(::assertJsonFieldsAreAnnotated)
    }

    private fun assertJsonFieldsAreAnnotated(modelClass: Class<*>) {
        modelClass.declaredFields
            .filterNot { it.isSynthetic || Modifier.isStatic(it.modifiers) }
            .forEach { field ->
                assertNotNull(
                    "${modelClass.simpleName}.${field.name} must declare @SerializedName",
                    field.getAnnotation(SerializedName::class.java),
                )
            }
    }
}
