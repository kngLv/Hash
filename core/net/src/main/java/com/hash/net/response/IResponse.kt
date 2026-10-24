package com.hash.net.response

/**
 * @name IResponse
 * @package com.hash.net.net.response
 * @author 345 QQ:1831712732
 * @time 2024/12/21 23:28
 * @description
 */
interface IResponse<T> {
    fun data():T
    fun code():Int
    fun message():String
}