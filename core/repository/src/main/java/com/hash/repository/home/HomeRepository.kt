package com.hash.repository.home

import com.hash.bean.home.HomeListBean
import com.hash.net.api.api
import com.hash.net.net.launch.request
import com.hash.net.net.request.RequestActionImpl
import com.hash.net.net.request.onBodyOf
import com.hash.net.response.WanResponse
import com.hash.repository.wanCommonApi
import kotlinx.coroutines.CoroutineScope

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