package com.hash.home.data.repository

import com.hash.common.net.api.model.home.NewsTypeListBean
import com.hash.common.const.AppConst
import com.hash.net.launch.request
import com.hash.net.request.RequestActionImpl
import com.hash.common.net.api.rollApi

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
    ): RequestActionImpl<NewsTypeListBean> {
        return request { rollApi.newsTypeList(AppConst.rollAppId, AppConst.rollAppSecret) }
    }
}
