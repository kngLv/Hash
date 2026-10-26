package com.hash.net.request

import com.hash.net.response.IResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** 负责请求执行、ResultState 分流及 begin/end 生命周期。 */
internal class RequestExecutor<Response, T, Raw>(
    private val block: suspend () -> Response,
    private val callbacks: RequestCallbacks<T, Raw>,
    private val success: (Response) -> RequestSuccess<T, Raw>,
    private val errorResponse: (Response?) -> IResponse<T>?,
) {
    fun enqueue(scope: CoroutineScope): Job = scope.launch {
        runWithLifecycle {
            when (val state = request()) {
                is ResultState.SuccessState -> {
                    val response = state.body
                    if (response == null) {
                        callbacks.dispatchEmptySuccess()
                    } else {
                        callbacks.dispatchSuccess(success(response))
                    }
                    true
                }
                is ResultState.CodeErrorState -> {
                    callbacks.dispatchCodeError(state.code, errorResponse(state.body))
                    false
                }

                is ResultState.ErrorState -> {
                    callbacks.dispatchError(state.error)
                    false
                }
            }
        }
    }

    suspend fun execute(): Response? {
        var response: Response? = null
        runWithLifecycle {
            when (val state = request()) {
                is ResultState.SuccessState -> {
                    response = state.body
                    true
                }

                is ResultState.CodeErrorState -> {
                    callbacks.dispatchCodeError(state.code, errorResponse(state.body))
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

    suspend fun request(): ResultState<Response> = LvRequest().request(block)

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
