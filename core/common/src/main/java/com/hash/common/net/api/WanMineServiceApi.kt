package com.hash.common.net.api

import com.hash.common.shared.model.UserInfoBean
import com.hash.common.net.response.WanResponse
import retrofit2.http.GET

interface WanMineServiceApi {

    @GET("/user/lg/userinfo/json")
    suspend fun userInfo(): WanResponse<UserInfoBean>

    @GET("/string")
    suspend fun test(): String
}
