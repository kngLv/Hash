package com.hash.home.ui.featured.detail.adapter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsDetailContentFormatterTest {

    @Test
    fun `toPlainText converts paragraph and line break tags`() {
        assertEquals(
            "第一段\n第二段\n第三段",
            NewsDetailContentFormatter.toPlainText("<p>第一段</p><p>第二段<br/>第三段</p>"),
        )
    }

    @Test
    fun `toPlainText returns empty text for null content`() {
        assertTrue(NewsDetailContentFormatter.toPlainText(null).isEmpty())
    }
}
