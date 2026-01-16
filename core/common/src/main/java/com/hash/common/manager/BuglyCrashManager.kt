package com.hash.common.manager

import android.app.Application
import android.content.Context
import com.hash.common.utils.GlobalUtil
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy

/**
 * Created by KngLv
 * @time 2026/1/16 14:50
 * @description
 */

object BuglyCrashManager {
    /** 初始化 Bugly Crash SDK */
    fun initBugly(application: Application, isDebug: Boolean, channel: String) {
        val strategy = UserStrategy(application)
        strategy.deviceID = GlobalUtil.getDeviceId()
        strategy.appVersion = "${GlobalUtil.appVersionName}+${GlobalUtil.appVersionCode}"
        strategy.appPackageName = GlobalUtil.appPackage
        strategy.appChannel = channel
        // 设置anr时是否获取系统trace文件，可能造成crash，建议只对少量用户开启
        strategy.isEnableCatchAnrTrace = false
        // 设置是否获取anr过程中的主线程堆栈，可能造成crash，建议只对少量用户开启
        strategy.isEnableRecordAnrMainStack = false
        //可能引起crash，建议只对少量用户开启
        CrashReport.setAllThreadStackEnable(application, false, false)
        CrashReport.initCrashReport(application, "49a3721003", isDebug, strategy)
    }

    /** 自定义Map参数可以保存发生Crash时的一些自定义的环境信息。
     *  在发生Crash时会随着异常信息一起上报并在页面展示。
     * */
    fun putUserData(context: Context, key: String, value: String) {
        CrashReport.putUserData(context, key, value);
    }

    /** 设置用户ID，可以定位到该用户发生Crash的情况。*/
    fun setUserId(userId: String) {
        CrashReport.setUserId(userId)
    }

    /** 主动上报异常信息 */
    fun postCatchException(throwable: Throwable) {
        CrashReport.postCatchedException(throwable, Thread.currentThread(),true)
    }
}