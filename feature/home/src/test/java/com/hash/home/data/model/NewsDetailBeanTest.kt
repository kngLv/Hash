package com.hash.home.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsDetailBeanTest {

    @Test
    fun `detail model preserves text image and video items in response order`() {
        val detail = Gson().fromJson(
            """
            {
              "id": 10894400,
              "items": [
                {"type":"text","content":"第一段","imageUrl":""},
                {"type":"img","content":"","imageUrl":"https://example.com/image.jpg"},
                {"type":"video","content":"","imageUrl":"https://example.com/cover.jpg","videoUrl":["https://example.com/video.mp4"]}
              ]
            }
            """.trimIndent(),
            NewsDetailBean::class.java,
        )

        assertEquals(10894400L, detail.id)
        assertEquals(listOf("text", "img", "video"), detail.items.map { it.type })
        assertEquals("第一段", detail.items[0].content)
        assertEquals("https://example.com/image.jpg", detail.items[1].imageUrl)
        assertEquals("https://example.com/video.mp4", detail.items[2].videoUrls.single())
    }
}
