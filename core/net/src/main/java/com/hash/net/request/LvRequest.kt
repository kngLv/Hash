package com.hash.net.request

import com.hash.net.LvHttp
import com.hash.net.error.CodeException
import com.hash.net.error.ErrorKey
import com.hash.net.response.IResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @name Request
 * @package com.hash.net.net.launch
 * @author 345 QQ:1831712732
 * @time 2024/12/23 23:23
 * @description 请求发起类，返回自定义的数据类型
 */
class LvRequest {

    suspend fun <T> request(block: AbstractRequestBlock<T>): ResultState<T> {
        try {
            when (block) {
                is RequestBlockObject -> {
                    // 确保阻塞/网络调用在 IO 线程池执行
                    val invoke = withContext(Dispatchers.IO) { block.invoke() }
                    if (invoke == null) {
                        return ResultState.SuccessState(null)
                    }
                    if (invoke is IResponse<*>) {
                        val s = parseIResponse(invoke as IResponse<*>)
                        if (s) return ResultState.SuccessState(invoke as T)
                        return ResultState.CodeErrorState(invoke.code(), invoke as T)
                    }
                    return ResultState.SuccessState(invoke)
                }

                is DownloadBlockObject -> {
                    // 确保阻塞/网络调用在 IO 线程池执行
                    val invoke = withContext(Dispatchers.IO) { block.invoke() }
                    return ResultState.SuccessState(invoke)
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (e: Exception) {
            return parseError(e)
        }
        return ResultState.ErrorState(error = IllegalStateException("Unsupported request block"))
    }

    private fun <T> parseError(e: Exception): ResultState<T> {
        val t: ResultState<T> = ResultState.ErrorState(error = e)
        // 自动匹配异常
        ErrorKey.entries.forEach {
            if (it.name == e::class.java.simpleName) {
                notifyError(it, e)
            }
        }
        // 如果全局异常启用
        notifyError(ErrorKey.AllException, e)
        return t
    }

    private fun notifyError(errorKey: ErrorKey, error: Exception) {
        try {
            LvHttp.getErrorDispose(errorKey)?.error?.invoke(error)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (handlerError: Exception) {
            handlerError.printStackTrace()
        }
    }

    private fun parseIResponse(response: IResponse<*>): Boolean {
        val code = response.code()
        val msg = response.message()
        // Code 验证
        if (!LvHttp.getCode().contains(code)) {
            // Code 异常处理
            notifyError(ErrorKey.ErrorCode, CodeException(code, msg))
            return false
        }
        return true
    }
}
