package com.hash.repository.login.api

import com.hash.bean.mine.UserInfo
import com.hash.bean.mine.UserInfoBean
import com.hash.net.response.WanResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface WanLoginServiceApi {



    @POST("/user/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): WanResponse<UserInfo>
}