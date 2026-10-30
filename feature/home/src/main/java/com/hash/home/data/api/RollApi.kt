package com.hash.home.data.api

import com.hash.home.data.model.NewsListBean
import com.hash.home.data.model.NewsDetailBean
import com.hash.home.data.model.NewsTypeListBean
import com.hash.common.net.NetConstants.ROLL_BASE_URL
import com.hash.common.net.response.RollResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Created by KngLv
 * @time 2026/1/4 09:47
 * @description
 */

interface RollApi {

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

    /** 根据新闻 id 获取新闻详情。 */
    @GET("$ROLL_BASE_URL/api/news/details/v2")
    suspend fun newsDetail(
        @Query("app_id") appId: String,
        @Query("app_secret") appSecret: String,
        @Query("newsId") newsId: String,
    ): RollResponse<NewsDetailBean>
}
