package com.hash.net.launch

import com.hash.net.request.LvRequest
import com.hash.net.request.RawRequestActionImpl
import com.hash.net.request.RequestActionImpl
import com.hash.net.request.ResultState
import com.hash.net.response.IResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

/** 将一个接口请求封装为支持生命周期回调的请求动作。 */
fun <T> request(block: suspend () -> IResponse<T>) = RequestActionImpl(block)

/** 将不遵循 [IResponse] 协议的普通接口封装为请求动作。 */
fun <T> requestRaw(block: suspend () -> T) = RawRequestActionImpl(block)

/**
 * 并发执行请求列表，保持输入顺序返回结果。
 *
 * 单个请求发生普通异常或业务 code 错误时，对应结果为 null；协程取消会继续向外传播。
 */
@Suppress("unused")
suspend fun <T> CoroutineScope.zipSyncHttp(block: List<suspend () -> T?>): List<T?> {
    val requests = block.map { request -> async { executeRaw(request) } }
    return requests.awaitAll()
}

private suspend fun <T> executeRaw(block: suspend () -> T?): T? = when (val state = LvRequest().request(block)) {
    is ResultState.SuccessState -> state.body
    is ResultState.CodeErrorState -> null
    is ResultState.ErrorState -> null
}
