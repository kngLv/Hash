package com.hash.repository.home

import com.hash.bean.home.HomeListBean
import com.hash.net.launch.request
import com.hash.net.request.RequestActionImpl
import com.hash.common.net.response.WanResponse
import com.hash.repository.wanCommonApi

/**
 * Created by KngLv
 * @time 2025/12/29 09:46
 * @description
 */

class HomeRepository {

    fun homeList(
        page: Int,
        pageSize: Int
    ): RequestActionImpl<WanResponse<HomeListBean>> {
        return request { wanCommonApi.articleList(page, pageSize) }
    }

}