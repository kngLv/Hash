package com.hash.home

import com.google.gson.annotations.SerializedName
import com.hash.home.data.model.BannerListBean
import com.hash.home.data.model.HomeListBean
import com.hash.home.data.model.NewsListBean
import com.hash.home.data.model.NewsTypeListBean
import org.junit.Assert.*
import org.junit.Test
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
    fun `network model fields should declare serialized names`() {
        listOf(
            BannerListBean.BannerListBeanItem::class.java,
            HomeListBean::class.java,
            HomeListBean.HomeListItem::class.java,
            HomeListBean.HomeListItem.Tag::class.java,
            NewsListBean.NewsListBeanItem::class.java,
            NewsTypeListBean.NewsTypeListBeanItem::class.java,
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
