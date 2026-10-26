package com.hash.net.request

import com.hash.net.response.IResponse

/** 保存请求回调，并负责将响应分发为原始响应或业务 data。 */
internal class RequestCallbacks<T> {
    var begin: (() -> Unit)? = null
    var rawBody: ((IResponse<T>) -> Unit)? = null
    var body: ((T) -> Unit)? = null
    var nullData: (() -> Unit)? = null
    var errorCode: ((Int, IResponse<T>?) -> Unit)? = null
    var error: (Exception) -> Unit = Exception::printStackTrace
    var end: ((Boolean) -> Unit)? = null

    fun dispatchSuccess(response: IResponse<T>?) {
        if (response == null) {
            nullData?.invoke()
            return
        }

        rawBody?.invoke(response)
        val callback = body ?: return
        val data = response.data()
        if (data == null) {
            nullData?.invoke()
        } else {
            callback(data)
        }
    }

    fun dispatchCodeError(code: Int, response: IResponse<T>?) {
        errorCode?.invoke(code, response)
    }

    fun dispatchError(exception: Exception) {
        error(exception)
    }
}
