package com.hash.home.data.model

import com.google.gson.annotations.SerializedName

/**
 * 创建者: KngLv
 * 时间: 2026/1/8 09:29
 * 描述: 新闻列表数据实体
 */

class NewsListBean : ArrayList<NewsListBean.NewsListBeanItem>() {
    data class NewsListBeanItem(
        @field:SerializedName("digest") val digest: String, // 在刘一诺承认与檀健次恋爱过且分手后，刘一诺的评论区十分热闹，
        @field:SerializedName("imgList") val imgList: List<String>,
        @field:SerializedName("newsId") val newsId: String, // 8507427
        @field:SerializedName("postTime") val postTime: String, // 2026-01-07 20:40:27
        @field:SerializedName("source") val source: String, // 萌神木木
        @field:SerializedName("title") val title: String // 刘一诺点名檀健次！自曝恋爱时她已成年
    )
}
