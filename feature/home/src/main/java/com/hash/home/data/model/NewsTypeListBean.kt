package com.hash.home.data.model


class NewsTypeListBean : ArrayList<NewsTypeListBean.NewsTypeListBeanItem>() {
    data class NewsTypeListBeanItem(
        val typeId: Int, // 532
        val typeName: String // 新闻
    )
}
