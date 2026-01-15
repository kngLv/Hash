package com.hash.net.net.controller

import android.app.Application
import com.google.gson.Gson
import com.hash.net.net.error.ErrorKey
import com.hash.net.net.error.ErrorValue
import okhttp3.Interceptor


class LvParams {
    lateinit var baseUrl: String
    lateinit var appContext: Application
    var connectTimeOut: Long = 10
    var readTimeOut: Long = 10
    var writeTimeOut: Long = 30
    var isLog = false
    var isCache = false

    /** 请求成功 code */
    var successCode: IntArray = intArrayOf()

    var cacheSize: Long = 1024 * 1024 * 20
    var interceptors = arrayListOf<Interceptor>()
    val errorDisposes: MutableMap<ErrorKey, ErrorValue> = mutableMapOf()
    var cerResId: Int = -1;

    var gson: Gson? = null
}