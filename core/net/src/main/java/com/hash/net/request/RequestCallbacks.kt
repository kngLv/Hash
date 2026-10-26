package com.hash.net.request

import com.hash.net.response.IResponse

/** 保存请求回调，并负责分发业务数据与原始返回值。 */
internal class RequestCallbacks<T, Raw> {
    var begin: (() -> Unit)? = null
    var rawBody: ((Raw) -> Unit)? = null
    var body: ((T) -> Unit)? = null
    var nullData: (() -> Unit)? = null
    var errorCode: ((Int, IResponse<T>?) -> Unit)? = null
    var error: (Exception) -> Unit = Exception::printStackTrace
    var end: ((Boolean) -> Unit)? = null

    fun dispatchSuccess(response: RequestSuccess<T, Raw>) {
        rawBody?.invoke(response.raw)
        val data = response.body
        if (data == null) {
            nullData?.invoke()
        } else {
            body?.invoke(data)
        }
    }

    fun dispatchEmptySuccess() {
        nullData?.invoke()
    }

    fun dispatchCodeError(code: Int, response: IResponse<T>?) {
        errorCode?.invoke(code, response)
    }

    fun dispatchError(exception: Exception) {
        error(exception)
    }
}
