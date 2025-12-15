package com.hash.net.net.request

import android.util.Log


/**
 * 请求动作抽象基类
 *
 * 该类负责保存请求生命周期中的通用回调（开始/结束/异常）。
 * 具体的请求实现（例如 RequestActionImpl）继承该类并在合适的时机
 * 调用这些回调。回调均为可选，可由调用方通过链式方法注册。
 *
 * @param T 请求返回的原始类型（可能是业务对象或实现了 IResponse<T> 的包装类型）
 */
abstract class AbstractRequestAction<T> {

    /**
     * 请求开始时的回调（UI 可显示加载中）
     * 类型：无参无返回值的函数，可为空
     */
    protected var begin: (() -> Unit)? = null

    /**
     * 请求结束时的回调（无论成功/失败均会调用，用于关闭加载态）
     */
    protected var end: ((Boolean) -> Unit)? = null

    /**
     * 请求发生异常时的回调（例如抛出 Exception 时）
     * 参数为 Exception，调用者可在此处做统一的错误处理或上报
     *
     * 说明：提供一个默认实现，会把 Exception 打印到日志，方便在未显式设置 onError 的情况下也能看到错误信息。
     */
    protected var error: ((Exception) -> Unit) = { t: Exception ->
        t.printStackTrace()
    }

}