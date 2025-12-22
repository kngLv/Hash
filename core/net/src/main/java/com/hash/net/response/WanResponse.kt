package com.hash.net.response

import com.hash.net.net.response.IResponse

/**
 * @name WanResponse
 * @package com.hash.net.response
 * @author 345 QQ:1831712732
 * @time 2024/12/31 23:55
 * @description
 */
data class WanResponse<T>(
    private val data: T?,
    private val errorCode: Int,
    private val errorMsg: String
) : IResponse<T> {
    override fun data(): T = data ?: Any() as T

    override fun code(): Int = errorCode

    override fun message(): String = errorMsg
}