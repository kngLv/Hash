package com.hash.net.request

import com.hash.net.response.IResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * 请求动作的链式 API。
 *
 * 本类仅负责配置回调并委托执行；回调分发和生命周期处理分别由
 * [RequestCallbacks]、[RequestExecutor] 负责。
 */
class RequestActionImpl<T>(
    block: suspend () -> IResponse<T>,
) {
    private val callbacks = RequestCallbacks<T, IResponse<T>>()
    private val executor = RequestExecutor(
        block = block,
        callbacks = callbacks,
        success = { response -> RequestSuccess(response.data(), response) },
        errorResponse = { it },
    )

    fun onBegin(block: () -> Unit): RequestActionImpl<T> = apply { callbacks.begin = block }

    /** 注册 data 回调。 */
    fun onBody(block: (T) -> Unit): RequestActionImpl<T> = apply { callbacks.body = block }

    /** 注册完整响应回调。 */
    fun onBodyRaw(block: (IResponse<T>) -> Unit): RequestActionImpl<T> = apply {
        callbacks.rawBody = block
    }

    fun onNullData(block: () -> Unit): RequestActionImpl<T> = apply { callbacks.nullData = block }

    fun onErrorCode(block: (Int, IResponse<T>?) -> Unit): RequestActionImpl<T> = apply {
        callbacks.errorCode = block
    }

    fun onError(block: (Exception) -> Unit): RequestActionImpl<T> = apply { callbacks.error = block }

    fun onEnd(block: (Boolean) -> Unit): RequestActionImpl<T> = apply { callbacks.end = block }

    fun enqueue(scope: CoroutineScope): Job = executor.enqueue(scope)

    /** 同步执行请求，保留原有语义：成功时返回完整 IResponse。 */
    suspend fun execute(): IResponse<T>? = executor.execute()

}
