package com.hash.repository.mine.api

import com.hash.bean.mine.UserInfoBean
import com.hash.net.response.WanResponse
import retrofit2.http.GET

interface WanMineServiceApi {

    @GET("/user/lg/userinfo/json")
    suspend fun userInfo(): WanResponse<UserInfoBean>
}