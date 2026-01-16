package com.hash.umengsdk

import android.app.Application
import com.umeng.commonsdk.UMConfigure

/**
 * Created by KngLv
 * @time 2026/1/16 10:23
 * @description 友盟统计：https://developer.umeng.com/docs/66632/detail/101814#h1-u521Du59CBu5316u53CAu901Au7528u63A5u53E32
 */

object UmengClient {

    fun init(application: Application,channel: String) {
        //context, appKey, channel, deviceType, pushSecret
        UMConfigure.init(
            application,
            BuildConfig.UM_KEY,
            channel,
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

}