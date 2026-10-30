package com.hash.home.ui.featured.detail.adapter

import com.hash.home.data.model.NewsDetailItem

/** 新闻详情列表使用的页面条目。 */
sealed interface NewsDetailPageItem {
    data class Header(val title: String, val meta: String) : NewsDetailPageItem
    data class Text(val content: String) : NewsDetailPageItem
    data class Image(val imageUrl: String) : NewsDetailPageItem
    data class Video(val coverUrl: String, val videoUrl: String) : NewsDetailPageItem
    data object Empty : NewsDetailPageItem
}

/** 将接口详情条目转换为可展示的列表条目，并保持服务端顺序。 */
object NewsDetailPageItemFactory {

    fun create(
        title: String,
        source: String,
        postTime: String,
        detailItems: List<NewsDetailItem>,
    ): List<NewsDetailPageItem> = buildList {
        add(
            NewsDetailPageItem.Header(
                title = title,
                meta = listOf(source, postTime).filter(String::isNotBlank).joinToString(" · "),
            ),
        )
        detailItems.forEach { item ->
            when (item.type) {
                TYPE_TEXT -> item.content.takeIf(String::isNotBlank)?.let(NewsDetailPageItem::Text)
                TYPE_IMAGE -> item.imageUrl.takeIf(String::isNotBlank)?.let(NewsDetailPageItem::Image)
                TYPE_VIDEO -> item.videoUrls.firstOrNull(String::isNotBlank)?.let { videoUrl ->
                    NewsDetailPageItem.Video(item.imageUrl, videoUrl)
                }

                else -> null
            }?.let(::add)
        }
        if (size == HEADER_ITEM_COUNT) {
            add(NewsDetailPageItem.Empty)
        }
    }

    private const val TYPE_TEXT = "text"
    private const val TYPE_IMAGE = "img"
    private const val TYPE_VIDEO = "video"
    private const val HEADER_ITEM_COUNT = 1
}
