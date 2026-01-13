package com.hash.bean.home

/**
 * 创建者: KngLv
 * 时间: 2026/1/8 09:29
 * 描述: 新闻列表数据实体
 */

class NewsListBean : ArrayList<NewsListBean.NewsListBeanItem>() {
    data class NewsListBeanItem(
        val digest: String, // 在刘一诺承认与檀健次恋爱过且分手后，刘一诺的评论区十分热闹，
        val imgList: List<String>,
        val newsId: String, // 8507427
        val postTime: String, // 2026-01-07 20:40:27
        val source: String, // 萌神木木
        val title: String // 刘一诺点名檀健次！自曝恋爱时她已成年
    )
}