package com.hash.net.response

import com.hash.net.net.response.IResponse

/**
 * Created by KngLv
 * @time 2026/1/5 09:53
 * @description https://www.mxnzp.com/
 */

class RollResponse<T>(
    private val data: T?,
    private val code: Int,
    private val msg: String
) : IResponse<T> {
    override fun data(): T = data ?: Any() as T

    override fun code(): Int = code

    override fun message(): String = msg
}