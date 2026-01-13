package com.hash.repository.home

import com.hash.bean.home.HomeListBean
import com.hash.bean.home.NewsTypeListBean
import com.hash.common.const.AppConst
import com.hash.net.net.launch.request
import com.hash.net.net.request.RequestActionImpl
import com.hash.net.response.RollResponse
import com.hash.net.response.WanResponse
import com.hash.repository.rollApi
import com.hash.repository.wanCommonApi

/**
 * Created by KngLv
 * @time 2026/1/5 09:30
 * @description
 */

class FeaturedRepository {

    /**
     * 新闻类型列表
     */
    fun newsType(
    ): RequestActionImpl<RollResponse<NewsTypeListBean>> {
        return request { rollApi.newsTypeList(AppConst.rollAppId, AppConst.rollAppSecret) }
    }
}