package com.hash.bean.home


class NewsTypeListBean : ArrayList<NewsTypeListBean.NewsTypeListBeanItem>() {
    data class NewsTypeListBeanItem(
        val typeId: Int, // 532
        val typeName: String // 新闻
    )
}