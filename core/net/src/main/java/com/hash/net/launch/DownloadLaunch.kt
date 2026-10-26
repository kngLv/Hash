package com.hash.net.launch

import com.hash.net.download.DownloadActionImpl
import okhttp3.ResponseBody
import retrofit2.Response

/** 将下载请求封装为支持进度与断点续传的下载动作。 */
fun <T : ResponseBody> download(block: suspend (existingLength: Long) -> Response<T>) =
    DownloadActionImpl(block)
