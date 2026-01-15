package com.hash.net.net.controller

import com.google.gson.Gson
import com.hash.net.net.converter.LvDefaultConverterFactory
import com.hash.net.net.interceptor.CacheInterceptor
import com.hash.net.net.interceptor.LogInterceptor
import com.hash.net.net.utils.HTTPSCerUtils
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Protocol
import retrofit2.Retrofit
import java.util.*
import java.util.concurrent.TimeUnit

@Suppress("UNCHECKED_CAST")
class LvController {

    private val apiServiceCache = mutableMapOf<String, Any>()
    private lateinit var retrofit: Retrofit
    private lateinit var params: LvParams

    fun <T> newInstance(clazz: Class<T>): T {
        if (apiServiceCache[clazz.name] == null) {
            val create = retrofit.create(clazz) as Any
            apiServiceCache[clazz.name] = create
            return create as T
        }
        return apiServiceCache[clazz.name] as T
    }


    private fun createOkhttpBuilder(): OkHttpClient.Builder {
        return OkHttpClient.Builder()
            .readTimeout(params.readTimeOut, TimeUnit.SECONDS)
            .writeTimeout(params.writeTimeOut, TimeUnit.SECONDS)
            .connectTimeout(params.connectTimeOut, TimeUnit.SECONDS)
            .protocols(Collections.singletonList(Protocol.HTTP_1_1))
            .retryOnConnectionFailure(true)
    }

    private fun createRetrofit(baseUrl: String, client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(LvDefaultConverterFactory.create(params.gson ?: Gson()))
            .build()
    }

    private fun setOkhttpCommonParams(builder: OkHttpClient.Builder): OkHttpClient.Builder {
        if (params.isCache) {
            builder.cache(Cache(params.appContext.cacheDir, params.cacheSize))
            builder.addNetworkInterceptor(CacheInterceptor())
        }
        if (params.isLog) {
            builder.addInterceptor(LogInterceptor())
//                builder.addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        }
        //设置拦截器
        for (interceptor in params.interceptors) {
            builder.addInterceptor(interceptor)
        }
        return builder
    }

    fun build(p: LvParams): LvController {
        params = p
        val builder = createOkhttpBuilder()
        if (params.cerResId != -1) //验证证书
            HTTPSCerUtils.setCertificate(params.appContext, builder, params.cerResId)
        //设置 baseUrl
        retrofit = createRetrofit(params.baseUrl, setOkhttpCommonParams(builder).build())
        return this
    }


}