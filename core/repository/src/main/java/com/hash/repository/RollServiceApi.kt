package com.hash.repository

import com.hash.bean.home.NewsListBean
import com.hash.bean.home.NewsTypeListBean
import com.hash.net.NetConstants.ROLL_BASE_URL
import com.hash.net.response.RollResponse
import com.hash.net.response.WanResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by KngLv
 * @time 2026/1/4 09:47
 * @description
 */

interface RollServiceApi {

    /**
     * 新闻类型列表
     */
    @GET("$ROLL_BASE_URL/api/news/types/v2")
    suspend fun newsTypeList(
        @Query("app_id") appId: String,
        @Query("app_secret") appSecret: String,
    ): RollResponse<NewsTypeListBean>

    /**
     * 新闻列表
     */
    @GET("$ROLL_BASE_URL/api/news/list/v2")
    suspend fun newsList(
        @Query("app_id") appId: String,
        @Query("app_secret") appSecret: String,
        @Query("typeId") type: String,
        @Query("page") page: String,
    ): RollResponse<NewsListBean>
}