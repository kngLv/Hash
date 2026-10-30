package com.hash.home.data.repository

import com.hash.common.const.AppConst
import com.hash.home.data.api.RollApi
import com.hash.home.data.api.rollApi
import com.hash.home.data.model.NewsDetailBean
import com.hash.net.launch.request

class NewsDetailRepository(
    private val api: RollApi = rollApi,
) {

    suspend fun load(newsId: String): NewsDetailBean? =
        request {
            api.newsDetail(
                appId = AppConst.rollAppId,
                appSecret = AppConst.rollAppSecret,
                newsId = newsId,
            )
        }
            .execute()
            ?.data()
}
