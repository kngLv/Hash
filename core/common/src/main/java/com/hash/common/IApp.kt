package com.hash.common

import android.app.Application
import android.content.pm.ApplicationInfo
import com.hash.common.config.GlideApp
import com.hash.common.manager.ActivityManager
import com.hash.common.manager.InitManager
import kotlin.properties.Delegates


/**
 * @name IApp
 * @package com.hash.main
 * @author 345 QQ:1831712732
 * @time 2024/11/26 00:03
 * @description
 */
open class IApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instant = this
        //是否是debug模式， 设置 debuggable 后无效
        isDebug = applicationInfo != null &&
                (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        initSdk()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // 清理所有图片内存缓存
        GlideApp.get(this).onLowMemory()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 根据手机内存剩余情况清理图片内存缓存
        GlideApp.get(this).onTrimMemory(level)
    }


    companion object {
        var instant: Application by Delegates.notNull()
        var isDebug: Boolean by Delegates.notNull()

        fun initSdk() {
            // 如果当前的进程不是主进程的话，则不进行第三方框架的初始化
            if (!ActivityManager.isMainProcess(instant)) {
                return
            }
            InitManager.preInitSdk(instant, isDebug)
//            if (InitManager.isAgreePrivacy()) {
            if (true) {
                InitManager.initSdk(instant, isDebug, "kngLv")
            }
        }
    }

}