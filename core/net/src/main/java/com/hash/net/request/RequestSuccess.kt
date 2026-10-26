package com.hash.net.request

/** 成功请求中供回调分发的业务数据与原始返回值。 */
internal data class RequestSuccess<T, Raw>(
    val body: T?,
    val raw: Raw,
)
