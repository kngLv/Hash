package com.hash.login.data.api

import com.hash.common.shared.model.UserInfo
import com.hash.common.net.response.WanResponse
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface LoginApi {



    @POST("/user/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): WanResponse<UserInfo>
}
