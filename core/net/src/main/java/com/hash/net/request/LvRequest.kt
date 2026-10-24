package com.hash.net.request

import com.hash.net.LvHttp
import com.hash.net.error.CodeException
import com.hash.net.error.ErrorKey
import com.hash.net.response.IResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @name Request
 * @package com.hash.net.net.launch
 * @author 345 QQ:1831712732
 * @time 2024/12/23 23:23
 * @description 请求发起类，返回自定义的数据类型
 */
class LvRequest() {

    suspend fun <T> request(block: AbstractRequestBlock<T>): ResultState<T> {
        var t: ResultState<T>? = null
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
                        if (s == null) {
                            @Suppress("UNCHECKED_CAST")
                            return ResultState.SuccessState(invoke as T)
                        }
                        @Suppress("UNCHECKED_CAST")
                        return ResultState.CodeErrorState(s.first, invoke as T)
                    }
                    return ResultState.SuccessState(invoke)
                }

                is DownloadBlockObject -> {
                    // 确保阻塞/网络调用在 IO 线程池执行
                    val invoke = withContext(Dispatchers.IO) { block.invoke() }
                    return ResultState.SuccessState(invoke)
                }
            }
        } catch (e: Exception) {
            t = parseError(e)
        }
        return t ?: ResultState.ErrorState(error = Exception("Unknown error"))
    }

    private fun <T> parseError(e: Exception): ResultState<T> {
        val t: ResultState<T> = ResultState.ErrorState(error = e)
        // 自动匹配异常
        ErrorKey.entries.forEach {
            if (it.name == e::class.java.simpleName) {
                LvHttp.getErrorDispose(it)?.error?.let { handler -> handler(e) }
            }
        }
        // 如果全局异常启用
        LvHttp.getErrorDispose(ErrorKey.AllException)?.error?.let { handler ->
            handler(e)
        }
        return t
    }


    private fun parseIResponse(response: IResponse<*>): Pair<Int, String>? {
        val code = response.code()
        val msg = response.message()
        // Code 验证
        if (!LvHttp.getCode().contains(code)) {
            // Code 异常处理
            LvHttp.getErrorDispose(ErrorKey.ErrorCode)?.error?.invoke(
                CodeException(
                    code,
                    msg
                )
            )
            return Pair(code, msg)
        }
        return null
    }

}