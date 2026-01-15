package com.hash.net.net

import android.app.Application
import androidx.annotation.RawRes
import com.google.gson.Gson
import com.hash.net.net.controller.LvController
import com.hash.net.net.controller.LvParams
import com.hash.net.net.error.ErrorKey
import com.hash.net.net.error.ErrorValue
import okhttp3.Interceptor


object LvHttp {

    private lateinit var p: LvParams
    private lateinit var mController: LvController


    /** 创建 API */
    @JvmStatic
    fun <T> createApi(clazz: Class<T>): T {
        return mController.newInstance(clazz)
    }

    /** 获取异常处理 */
    @JvmStatic
    fun getErrorDispose(errorKey: ErrorKey): ErrorValue? {
        return p.errorDisposes[errorKey]
    }

    /** 是否打印日志 */
    fun getIsLogging(): Boolean {
        return p.isLog
    }

    /** 获取 Application */
    fun getAppContext(): Application {
        return p.appContext
    }

    /** 获取 code */
    fun getCode(): IntArray {
        return p.successCode
    }


    class Builder(application: Application) {
        init {
            p = LvParams()
            p.appContext = application
        }

        /** 设置 BaseUrl */
        fun setBaseUrl(baseUrl: String): Builder {
            p.baseUrl = baseUrl
            return this
        }

        /**
         * 连接时间，秒为单位
         */
        fun setConnectTimeOut(connectTime: Long): Builder {
            p.connectTimeOut = connectTime
            return this
        }

        /**
         * 下载响应的时候等待时间，秒为单位
         */
        fun setReadTimeOut(readTime: Long): Builder {
            p.readTimeOut = readTime
            return this
        }

        /**
         * 写入请求的等待时间，秒为单位
         */
        fun setWriteTimeOut(writeTimeOut: Long): Builder {
            p.writeTimeOut = writeTimeOut
            return this
        }

        /**
         * 是否打印 log
         * @param isLog true 表示打印
         */
        fun isLog(isLog: Boolean): Builder {
            p.isLog = isLog
            return this
        }

        /**
         * 是否开启缓存，默认关闭
         * @param isCache true 表示开启缓存
         */
        fun isCache(isCache: Boolean): Builder {
            p.isCache = isCache
            return this
        }

        /**
         * 设置 code 值，如果请求 == code，则说明请求成功
         * 不设置默认不进行 code 判断
         */
        fun setCode(vararg code: Int): Builder {
            p.successCode = code
            return this
        }

        /**
         * 设置缓存大小，默认 20mb
         * @param cacheSize 缓存大小
         */
        fun setCacheSize(cacheSize: Long): Builder {
            p.cacheSize = cacheSize
            return this
        }

        /**
         * 添加拦截器
         */
        fun addInterceptor(vararg interceptor: Interceptor): Builder {
            p.interceptors.addAll(interceptor)
            return this
        }

        fun setCerResId(@RawRes cerRes: Int): Builder {
            p.cerResId = cerRes
            return this
        }

        /**
         * 设置异常处理
         */
        fun setErrorDispose(errorKey: ErrorKey, errorValue: ErrorValue): Builder {
            p.errorDisposes[errorKey] = errorValue
            return this
        }

        /**
         * 设置 Gson 解析器，默认使用gson，如果有自定义的Gson解析器，可以在此设置
         */
        fun setGsonParser(gson: Gson): Builder {
            p.gson = gson
            return this
        }

        fun build() {
            mController = LvController().build(p)
        }
    }
}