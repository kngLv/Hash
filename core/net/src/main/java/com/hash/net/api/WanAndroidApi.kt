package com.hash.net.api

import com.hash.bean.home.BannerListBean
import com.hash.bean.home.HomeListBean
import com.hash.bean.mine.UserInfoBean
import com.hash.net.response.WanResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

/**
 * @name WanAndroidApi
 * @package com.hash.net.api
 * @author 345 QQ:1831712732
 * @time 2024/12/30 23:32
 * @description
 */
interface WanAndroidApi {

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
    suspend fun downFile(@Header("Range") range: String? = null): ResponseBody
}