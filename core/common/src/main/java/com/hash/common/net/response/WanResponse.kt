package com.hash.common.net.response

import com.google.gson.annotations.SerializedName
import com.hash.net.response.IResponse

/**
 * @name WanResponse
 * @package com.hash.net.response
 * @author 345 QQ:1831712732
 * @time 2024/12/31 23:55
 * @description
 */
data class WanResponse<T>(
    @field:SerializedName("data") private val data: T?,
    @field:SerializedName("errorCode") private val errorCode: Int,
    @field:SerializedName("errorMsg") private val errorMsg: String
) : IResponse<T> {
    override fun data(): T? = data

    override fun code(): Int = errorCode

    override fun message(): String = errorMsg
}
