package com.hash.umengsdk

import android.app.Application
import android.util.Log
import com.umeng.commonsdk.UMConfigure
import com.umeng.umcrash.UMCrash

/**
 * Created by KngLv
 * @time 2026/1/16 10:23
 * @description 友盟统计：https://developer.umeng.com/docs/66632/detail/101814#h1-u521Du59CBu5316u53CAu901Au7528u63A5u53E32
 */

object UmengClient {

    fun init(application: Application) {
        //context, appKey, channel, deviceType, pushSecret
        UMConfigure.init(
            application,
            BuildConfig.UM_KEY,
            "umeng",
            UMConfigure.DEVICE_TYPE_PHONE,
            ""
        )
    }

    /** 预初始化 SDK（在用户没有同意隐私协议前调用） */
    fun preInit(application: Application, isLogEnabled: Boolean) {
        //context, appKey, channel
        UMConfigure.preInit(application, BuildConfig.UM_KEY, "umeng")
        // 是否开启日志打印
        UMConfigure.setLogEnabled(isLogEnabled)
    }

    /** 用于客户记录App使用过程中（非崩溃时）的关键信息，最大键值对数量为10个  */
    fun addCustomInfo(key: CrashCustomInfoKey, value: String) {
        UMCrash.addCustomInfo(key.name, value)
    }

    /**
     * 自定义异常日志上传到友盟
     * @param e Throwable 捕获的异常
     * @param type String 异常类型
     * @param withLogCat Boolean 是否携带 LogCat 日志
     * @param allThreadsDump Boolean 是否携带所有线程堆栈信息
     * */
    fun generateCustomLog(
        e: Throwable,
        type: String,
        withLogCat: Boolean = true,
        allThreadsDump: Boolean = false
    ) {
        UMCrash.generateCustomLog(e, type, withLogCat, allThreadsDump)
    }
}