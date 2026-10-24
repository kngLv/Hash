package com.hash.net.request

import com.hash.net.response.IResponse

/**
 * 类型安全的扩展：当请求返回类型为 IResponse<R>（如 WanResponse<HomeListBean>）时，
 * 可直接使用 .onBodyOf { r: R -> ... } 获取内部的 R（类型安全，编译器能从 receiver 推断 R）。
 */
inline fun <reified R, T : IResponse<R>> RequestActionImpl<T>.onBodyOf(noinline block: (R) -> Unit): RequestActionImpl<T> =
    apply {
        this.parseBodyOf { any ->
            (any as? R)?.let { block(it) }
        }
    }
