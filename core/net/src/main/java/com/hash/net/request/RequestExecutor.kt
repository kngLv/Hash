package com.hash.net.request

import com.hash.net.response.IResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 负责请求执行、ResultState 分流及 begin/end 生命周期。 */
internal class RequestExecutor<T>(
    private val block: AbstractRequestBlock<IResponse<T>>,
    private val callbacks: RequestCallbacks<T>,
) {
    fun enqueue(scope: CoroutineScope): Job = scope.launch {
        runWithLifecycle {
            when (val state = request()) {
                is ResultState.SuccessState -> callbacks.dispatchSuccess(state.body)
                is ResultState.CodeErrorState -> {
                    callbacks.dispatchCodeError(state.code, state.body)
                    false
                }

                is ResultState.ErrorState -> {
                    callbacks.dispatchError(state.error)
                    false
                }
            }
        }
    }

    suspend fun execute(): IResponse<T>? {
        var response: IResponse<T>? = null
        runWithLifecycle {
            when (val state = request()) {
                is ResultState.SuccessState -> {
                    response = state.body
                    true
                }

                is ResultState.CodeErrorState -> {
                    callbacks.dispatchCodeError(state.code, state.body)
                    false
                }

                is ResultState.ErrorState -> {
                    callbacks.dispatchError(state.error)
                    false
                }
            }
        }
        return response
    }

    suspend fun request(): ResultState<IResponse<T>> = LvRequest().request(block)

    private suspend fun runWithLifecycle(block: suspend () -> Boolean): Boolean {
        var result = false
        var primaryError: Throwable? = null
        try {
            callbacks.begin?.invoke()
            result = block()
            return result
        } catch (error: Throwable) {
            primaryError = error
            throw error
        } finally {
            try {
                callbacks.end?.invoke(result)
            } catch (endError: Throwable) {
                primaryError?.addSuppressed(endError) ?: throw endError
            }
        }
    }
}
