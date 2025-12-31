package com.hash.repository

import com.hash.bean.home.HomeListBean
import com.hash.net.response.WanResponse
import retrofit2.http.Field
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Created by KngLv
 * @time 2025/12/29 09:50
 * @description
 */

interface CommonServiceApi {
    /** 文章列表 */
    @GET("/article/list/{page}/json")
    suspend fun articleList(
        @Path("page") page: Int,
        @Query("page_size") pageSize: Int
    ): WanResponse<HomeListBean>
}