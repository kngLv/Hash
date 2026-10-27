package com.hash.home.data.model

import com.google.gson.annotations.SerializedName

class NewsTypeListBean : ArrayList<NewsTypeListBean.NewsTypeListBeanItem>() {
    data class NewsTypeListBeanItem(
        @field:SerializedName("typeId") val typeId: Int, // 532
        @field:SerializedName("typeName") val typeName: String // 新闻
    )
}
