package com.hash.common.shared.model

import com.google.gson.annotations.SerializedName

data class UserInfoBean(
    @field:SerializedName("coinInfo") val coinInfo: CoinInfo?,
    @field:SerializedName("userInfo") val userInfo: UserInfo
)

data class CoinInfo(
    @field:SerializedName("coinCount") val coinCount: Int,
    @field:SerializedName("level") val level: Int,
    @field:SerializedName("nickname") val nickname: String,
    @field:SerializedName("rank") val rank: String,
    @field:SerializedName("userId") val userId: Int,
    @field:SerializedName("username") val username: String
)

data class UserInfo(
    @field:SerializedName("admin") val admin: Boolean,
    @field:SerializedName("chapterTops") val chapterTops: List<Any>,
    @field:SerializedName("coinCount") val coinCount: Int,
    @field:SerializedName("collectIds") val collectIds: List<Int>,
    @field:SerializedName("email") val email: String,
    @field:SerializedName("icon") val icon: String,
    @field:SerializedName("id") val id: Int,
    @field:SerializedName("nickname") val nickname: String,
    @field:SerializedName("password") val password: String,
    @field:SerializedName("publicName") val publicName: String,
    @field:SerializedName("token") val token: String,
    @field:SerializedName("type") val type: Int,
    @field:SerializedName("username") val username: String
)
