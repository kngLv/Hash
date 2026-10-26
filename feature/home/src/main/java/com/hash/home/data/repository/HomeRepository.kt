package com.hash.home.data.repository

import com.hash.home.data.model.HomeListBean
import com.hash.net.launch.request
import com.hash.net.request.RequestActionImpl
import com.hash.home.data.api.homeApi

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
        return request { homeApi.articleList(page, pageSize) }
    }

}
