package com.hash.net.interceptor

import com.hash.net.LvHttp
import com.hash.net.connectivity.isNetworkAvailable
import okhttp3.Interceptor
import okhttp3.CacheControl
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * @name CacheInterceptor
 * @package com.www.net.interceptor
 * @author 345 QQ:1831712732
 * @time 2020/6/22 23:32
 * @description 缓存
 */
class CacheInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.method != "GET") {
            return chain.proceed(request)
        }

        if (!isNetworkAvailable(LvHttp.getAppContext())) {
            request = request.newBuilder()
                .cacheControl(CacheControl.Builder().onlyIfCached().maxStale(7, TimeUnit.DAYS).build())
                .build()
        }
        return chain.proceed(request)
    }
}
