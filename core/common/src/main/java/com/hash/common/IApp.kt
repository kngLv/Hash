package com.hash.common

import android.app.Application
import android.content.pm.ApplicationInfo
import com.hash.common.cache.CacheTrimManager
import com.hash.common.ext.logD
import com.hash.common.config.GlideApp
import com.hash.common.manager.ActivityManager
import com.hash.common.manager.InitManager
import com.tencent.vasdolly.helper.ChannelReaderUtil
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
        // 清理模块注册的缓存
        try {
            CacheTrimManager.clearAll()
        } catch (_: Throwable) {
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // 根据手机内存剩余情况清理图片内存缓存
        GlideApp.get(this).onTrimMemory(level)
        // 通知各模块根据系统内存等级调整缓存
        try {
            CacheTrimManager.trimAll(level)
            // 输出 trim 统计，便于调试与参数调整
            CacheTrimManager.getStats().let { stats ->
                "CacheTrim stats: clear=${stats.totalClearCount}, trim=${stats.totalTrimCount}, lastTrim=${stats.lastTrimTs}, perLevel=${stats.trimCountsByLevel}".logD()
            }
        } catch (_: Throwable) {
        }
    }


    companion object {
        var instant: Application by Delegates.notNull()
        var isDebug: Boolean by Delegates.notNull()

        val channel: String by lazy {
            val c = ChannelReaderUtil.getChannel(instant)
            if (c.isNullOrEmpty()) "hashApp" else c
        }

        fun initSdk() {
            // 如果当前的进程不是主进程的话，则不进行第三方框架的初始化
            if (!ActivityManager.isMainProcess(instant)) {
                return
            }
            InitManager.preInitSdk(instant, isDebug)
//            if (InitManager.isAgreePrivacy()) {
            if (true) {
                InitManager.initSdk(instant, isDebug, channel)
            }
        }
    }

}