package com.hash.home.data.repository

import com.hash.common.net.api.model.home.HomeListBean
import com.hash.net.launch.request
import com.hash.net.request.RequestActionImpl
import com.hash.common.net.api.wanCommonApi

/**
 * Created by KngLv
 * @time 2025/12/29 09:46
 * @description
 */

class HomeRepository {

    fun homeList(
        page: Int,
        pageSize: Int
    ): RequestActionImpl<HomeListBean> {
        return request { wanCommonApi.articleList(page, pageSize) }
    }

}
