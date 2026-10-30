package com.hash.home.data.model

import com.google.gson.annotations.SerializedName

/** 精选新闻详情，内容由接口返回的 [items] 按顺序组成。 */
data class NewsDetailBean(
    @field:SerializedName("id") val id: Long = 0L,
    @field:SerializedName("items") val items: List<NewsDetailItem> = emptyList(),
)

/** 新闻详情中的一个原子内容块。 */
data class NewsDetailItem(
    @field:SerializedName("type") val type: String = "",
    @field:SerializedName("content") val content: String = "",
    @field:SerializedName("imageUrl") val imageUrl: String = "",
    @field:SerializedName("videoUrl") val videoUrls: List<String> = emptyList(),
)
