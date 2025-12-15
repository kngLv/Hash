package com.hash.net.net.request

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hash.net.net.response.IResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 请求动作实现类（链式回调式 API）
 *
 * 说明：
 * - 该类封装一次网络请求的各类回调（成功、空数据、code 错误、异常、开始/结束等），
 *   以链式方法注册回调后调用 `enqueue()` 发起异步请求或 `execute()` 发起同步请求。
 * - 泛型 T 表示请求返回的原始类型，可能是业务对象，也可能是实现了 `IResponse<R>` 的包装类型。
 *
 * 设计要点：
 * - 将通用生命周期回调放在基类 `AbstractRequestAction`（如 begin/end/error），
 *   本类负责更具体的数据回调（body / parsed body / code 错误等）。
 */
class RequestActionImpl<T>(val block: AbstractRequestBlock<T>) : AbstractRequestAction<T>() {

    /**
     * 当请求返回原始类型 T（无包装或 caller 直接想拿原始 T）时的回调
     * 用法：`.onBody { t -> ... }`
     */
    private var bodyCallback: ((T) -> Unit)? = null

    /**
     * 解析后的 body 回调（内部用于把 IResponse.data() 取出并回调）
     * 用法：扩展方法或内部方法会调用 `parseBodyOf` 来设置该回调，
     * 然后在 dispatchData 时如果 T 实现了 IResponse 会触发此回调。
     */
    private var parsedBodyCallback: ((T) -> Unit)? = null

    /**
     * 当解析后的数据为 null 时的回调（data == null）
     */
    private var nullBodyCallback: (() -> Unit)? = null

    /**
     * 当后端返回的 code 校验失败时的回调（业务层可根据 code 做特殊处理）
     * 参数：code, 可选的原始 body（可能为包装类型或 null）
     */
    private var errorCodeCallback: ((Int, T?) -> Unit)? = null

    /**
     * 注册：请求开始的回调（继承自 AbstractRequestAction.begin）
     */
    fun onBegin(block: () -> Unit): RequestActionImpl<T> = apply { this.begin = block }

    /**
     * 注册：原始 body 回调（当返回值本身就是业务对象或调用方想拿到 T）
     */
    fun onBody(block: (T) -> Unit): RequestActionImpl<T> = apply { this.bodyCallback = block }

    /**
     * 注册：当解析出的 data 为 null 时回调
     */
    fun onNullData(block: () -> Unit): RequestActionImpl<T> =
        apply { this.nullBodyCallback = block }

    /**
     * 注册：当后端返回 code 不在成功集合中时回调（部分场景仍然希望获得 body）
     * 注意：此回调不会阻断全局 Code 错误处理，仅用于局部处理
     */
    fun onErrorCode(block: (Int, T?) -> Unit): RequestActionImpl<T> =
        apply { this.errorCodeCallback = block }

    /**
     * 注册：当请求抛出异常时的回调（继承自 AbstractRequestAction.error）
     */
    fun onError(block: (Exception) -> Unit): RequestActionImpl<T> = apply { this.error = block }

    /**
     * 注册：请求结束的回调（继承自 AbstractRequestAction.end）
     */
    fun onEnd(block: (Boolean) -> Unit): RequestActionImpl<T> = apply { this.end = block }


    /**
     * 内部方法：解析包装类型 IResponse 并回调内部的 parsed body（Any?）
     * 标记为 internal 并用 @PublishedApi 暴露给同模块的 inline 扩展使用，外部模块不可见
     */
    @PublishedApi
    internal fun parseBodyOf(block: (Any?) -> Unit): RequestActionImpl<T> = apply {
        this.parsedBodyCallback = { t: T ->
            if (t is IResponse<*>) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val inner = (t as IResponse<Any?>).data()
                    if (inner == null) {
                        dispatchNullData()
                    } else {
                        block(inner)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                // 非 IResponse，直接当作业务对象回调
                try {
                    block(t as Any?)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 异步发起请求：在协程上下文中执行 request，并根据结果分发到对应回调
     */
    fun enqueue(scope: CoroutineScope = CoroutineScope(Dispatchers.Main)) {
        scope.launch {
            var result = false
            // protect begin callback
            dispatchBegin()
            try {
                when (val state = LvRequest().request(block)) { // 发起请求
                    is ResultState.SuccessState -> {
                        result = true
                        dispatchBody(state.body)
                    }

                    is ResultState.CodeErrorState -> dispatchErrorCode(state.code, state.body)
                    is ResultState.ErrorState -> dispatchError(state.error)
                }
            } catch (e: Exception) {
                dispatchError(e)
            } finally {
                dispatchEnd(result)
            }
        }
        return
    }

    /**
     * 同步执行请求（挂起函数），返回原始 T 或 null
     */
    suspend fun execute(): T? {
        dispatchBegin()
        var result = false
        try {
            return when (val state = LvRequest().request(block)) {
                is ResultState.SuccessState -> {
                    result = true
                    state.body
                }

                is ResultState.ErrorState -> {
                    dispatchError(state.error)
                    null
                }

                is ResultState.CodeErrorState<T> -> {
                    dispatchErrorCode(state.code, state.body)
                    null
                }
            }
        } catch (e: Exception) {
            dispatchError(e)
            return null
        } finally {
            dispatchEnd(result)
        }
    }

    suspend fun responseState(): ResultState<T> {
        return try {
            LvRequest().request(block)
        } catch (e: Exception) {
            ResultState.ErrorState(error = e)
        }
    }

    /**
     * 内部工具：分发开始/结束/数据/错误回调
     */
    private fun dispatchBegin() {
        try {
            begin?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatchEnd(result: Boolean) {
        try {
            end?.invoke(result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatchNullData() {
        try {
            this.nullBodyCallback?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatchBody(body: T?) {
        if (body == null) {
            dispatchNullData()
            return
        }

        // 先回调注册的原始 body 回调
        if (this.bodyCallback != null) {
            try {
                this.bodyCallback?.invoke(body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 再回调解析/包装后的 body 回调（例如 IResponse.data()）
        if (this.parsedBodyCallback != null) {
            try {
                this.parsedBodyCallback?.invoke(body)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun dispatchErrorCode(code: Int, data: T?) {
        try {
            this.errorCodeCallback?.invoke(code, data)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dispatchError(t: Exception) = this.error.invoke(t)
}
