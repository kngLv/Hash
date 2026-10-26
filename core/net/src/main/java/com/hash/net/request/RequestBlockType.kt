package com.hash.net.request

import okhttp3.ResponseBody
import retrofit2.Response

abstract class AbstractRequestBlock<T>() {
    abstract suspend fun invoke(): T
}


class RequestBlockObject<T>(val block: suspend () -> T) : AbstractRequestBlock<T>() {
    override suspend fun invoke(): T {
        return block.invoke()
    }
}

class DownloadBlockObject<T : ResponseBody>(val block: suspend (existingLength: Long) -> Response<T>) :
    AbstractRequestBlock<Response<T>>() {

    /** 下载开始之前获取的断点进度，断点续传时需要 */
    var existingLength: Long = 0L

    override suspend fun invoke(): Response<T> {
        return block.invoke(existingLength)
    }
}
