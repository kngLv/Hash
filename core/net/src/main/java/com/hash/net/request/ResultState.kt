package com.hash.net.request

/**
 * @name ResultState
 * @package com.hash.net.net.response
 * @author 345 QQ:1831712732
 * @time 2021/01/05 10:19
 * @description
 */

sealed class ResultState<T> {

    /** 请求成功 */
    class SuccessState<T>(val body: T?) : ResultState<T>()


    /** 请求成功，但 Code 验证失败  实现 [com.hash.net.response.IResponse] 接口会去验证 code */
    class CodeErrorState<T>(val code: Int, val body: T?) : ResultState<T>()

    /** 请求失败：请求过程中发生异常 */
    class ErrorState<T>(val error: Exception) : ResultState<T>()

}