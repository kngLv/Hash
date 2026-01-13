package com.hash.repository.home

import com.hash.bean.home.NewsListBean
import com.hash.bean.home.NewsTypeListBean
import com.hash.common.const.AppConst
import com.hash.net.net.launch.request
import com.hash.net.net.request.RequestActionImpl
import com.hash.net.response.RollResponse
import com.hash.repository.rollApi

/**
 * Created by KngLv
 * @time 2026/1/7 09:51
 * @description
 */

class FeaturedTabRepository {
    /**
     * 新闻列表
     */
    fun newsList(
        typeId: String,
        page: String,
    ): RequestActionImpl<RollResponse<NewsListBean>> {
        return request {
            rollApi.newsList(
                AppConst.rollAppId, AppConst.rollAppSecret,
                typeId, page
            )
        }
    }
}