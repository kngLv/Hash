package com.hash.home.ui.featured.detail.adapter

import androidx.core.text.HtmlCompat

/** 将接口正文转换为原生文本，避免页面展示 HTML 源码。 */
object NewsDetailContentFormatter {

    fun format(content: String?): CharSequence = HtmlCompat.fromHtml(
        content.orEmpty(),
        HtmlCompat.FROM_HTML_MODE_LEGACY,
    )

    fun toPlainText(content: String?): String = content.orEmpty()
        .replace(BREAK_TAG, "\n")
        .replace(BLOCK_END_TAG, "\n")
        .replace(ANY_TAG, "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .lines()
        .joinToString("\n") { it.trim() }
        .trim()

    private val BREAK_TAG = Regex("(?i)<br\\s*/?>")
    private val BLOCK_END_TAG = Regex("(?i)</(?:p|div|li|h[1-6])\\s*>")
    private val ANY_TAG = Regex("<[^>]+>")
}
