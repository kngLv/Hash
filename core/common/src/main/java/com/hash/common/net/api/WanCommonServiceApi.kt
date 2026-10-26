package com.hash.common.net.api

import com.hash.common.net.api.model.home.BannerListBean
import com.hash.common.net.api.model.home.HomeListBean
import com.hash.common.net.response.WanResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Created by KngLv
 * @time 2025/12/29 09:50
 * @description
 */

interface WanCommonServiceApi {
    /** 文章列表 */
    @GET("/article/list/{page}/json")
    suspend fun articleList(
        @Path("page") page: Int,
        @Query("page_size") pageSize: Int
    ): WanResponse<HomeListBean>

    /** 鸿蒙专栏 */
    @GET("/banner/json")
    suspend fun banner(): WanResponse<BannerListBean>




    /** 常用网站 */
    @GET("/friend/json")
    suspend fun friend(): String

    /** 热搜词 */
    @GET("/hotkey/json")
    suspend fun hotKey(): WanResponse<String>

    @GET("/article/top/json")
    suspend fun topList(): WanResponse<Any>

    @Streaming
    @GET("https://yesme-public.oss-cn-hongkong.aliyuncs.com/app/resources/345/petal/petal_y_7.apk")
    suspend fun downFile(@Header("Range") range: String? = null): Response<ResponseBody>
}
