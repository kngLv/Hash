package com.hash.home.ui.featured.detail.adapter

import com.hash.home.data.model.NewsDetailItem
import org.junit.Assert.assertEquals
import org.junit.Test

class NewsDetailPageItemFactoryTest {

    @Test
    fun `create maps API items directly and preserves renderable item order`() {
        val detailItems = listOf(
            NewsDetailItem(type = "text", content = "第一段"),
            NewsDetailItem(type = "img", imageUrl = "https://example.com/image.jpg"),
            NewsDetailItem(
                type = "video",
                imageUrl = "https://example.com/cover.jpg",
                videoUrls = listOf("https://example.com/video.mp4"),
            ),
        )

        val items = NewsDetailPageItemFactory.create(
            title = "新闻标题",
            source = "新闻来源",
            postTime = "2026-10-30 10:00:00",
            detailItems = detailItems,
        )

        assertEquals(NewsDetailPageItem.Header("新闻标题", "新闻来源 · 2026-10-30 10:00:00"), items.first())
        assertEquals(
            listOf(
                NewsDetailPageItem.Text("第一段"),
                NewsDetailPageItem.Image("https://example.com/image.jpg"),
                NewsDetailPageItem.Video(
                    coverUrl = "https://example.com/cover.jpg",
                    videoUrl = "https://example.com/video.mp4",
                ),
            ),
            items.drop(1),
        )
    }
}
