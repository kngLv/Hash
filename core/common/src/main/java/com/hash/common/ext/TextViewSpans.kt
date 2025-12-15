package com.hash.common.ext

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.TextPaint
import android.view.View
import android.widget.TextView

/**
 * SpanPart: 表示要追加到 TextView 的一段文本内容，支持单独颜色和可选点击回调。
 * - text: 要追加的文本
 * - color: 可选的文字颜色（传入已解析的 color int）
 * - onClick: 可选的点击回调（传入则会把该段设为可点击）
 */
data class SpanPart(
    val text: CharSequence,
    val color: Int? = null,
    val onClick: (() -> Unit)? = null
)

/**
 * 给 TextView 设置由多段组成的 Spannable 文本，支持为每段设置颜色与可点击回调。
 *
 * 用法示例：
 * val parts = listOf(
 *   SpanPart("登录即代表同意", secondaryColor),
 *   SpanPart("用户协议", linkColor) { openUserAgreement() },
 *   SpanPart("隐私政策", linkColor) { openPrivacyPolicy() }
 * )
 * textView.setSpannableParts(parts)
 *
 * 参数说明：
 * - parts: 要显示的分段列表（按顺序拼接）。
 * - highlightTransparent: 点击时是否将 TextView 的 highlightColor 设为透明（默认 true）。
 */
fun TextView.setSpannableParts(parts: List<SpanPart>, highlightTransparent: Boolean = true) {
    val sb = SpannableStringBuilder()
    var anyClickable = false

    parts.forEach { part ->
        val start = sb.length
        sb.append(part.text)
        val end = sb.length

        // 颜色
        part.color?.let { colorInt ->
            sb.setSpan(ForegroundColorSpan(colorInt), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // 点击
        part.onClick?.let { action ->
            anyClickable = true
            val clickable = object : ClickableSpan() {
                override fun onClick(widget: View) {
                    action()
                }

                override fun updateDrawState(ds: TextPaint) {
                    // 不使用默认下划线，颜色由可能设置的 ForegroundColorSpan 覆盖；若未设置 color，则使用 TextView 的 currentTextColor
                    ds.isUnderlineText = false
                }
            }
            sb.setSpan(clickable, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    this.text = sb

    if (anyClickable) {
        // 使 ClickableSpan 生效
        this.movementMethod = LinkMovementMethod.getInstance()
        this.isClickable = true
        this.isFocusable = false
        if (highlightTransparent) {
            this.highlightColor = Color.TRANSPARENT
        }
    }
}

