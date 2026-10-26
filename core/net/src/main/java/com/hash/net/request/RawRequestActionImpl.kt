package com.hash.net.request

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/** 不遵循 [com.hash.net.response.IResponse] 协议的普通接口请求动作。 */
class RawRequestActionImpl<T>(
    block: suspend () -> T,
) {
    private val callbacks = RequestCallbacks<T, T>()
    private val executor = RequestExecutor(
        block = block,
        callbacks = callbacks,
        success = { value -> RequestSuccess(value, value) },
        errorResponse = { null },
    )

    fun onBegin(block: () -> Unit): RawRequestActionImpl<T> = apply { callbacks.begin = block }

    fun onBody(block: (T) -> Unit): RawRequestActionImpl<T> = apply { callbacks.body = block }

    fun onBodyRaw(block: (T) -> Unit): RawRequestActionImpl<T> = apply {
        callbacks.rawBody = block
    }

    fun onNullData(block: () -> Unit): RawRequestActionImpl<T> = apply { callbacks.nullData = block }

    fun onError(block: (Exception) -> Unit): RawRequestActionImpl<T> = apply { callbacks.error = block }

    fun onEnd(block: (Boolean) -> Unit): RawRequestActionImpl<T> = apply { callbacks.end = block }

    fun enqueue(scope: CoroutineScope): Job = executor.enqueue(scope)

    /** 同步执行请求，成功时返回原始结果。 */
    suspend fun execute(): T? = executor.execute()
}
