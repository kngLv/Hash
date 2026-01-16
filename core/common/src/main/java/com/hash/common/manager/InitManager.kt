package com.hash.common.manager

import android.app.Application
import com.hash.common.R
import com.hash.common.ext.getColor
import com.hash.common.ext.mmkvGetBoolean
import com.hash.common.ext.mmkvPut
import com.hash.common.impl.GsonFactoryParseExceptionDefaultImpl
import com.hash.common.storage.MMKVConst
import com.hash.common.utils.GlobalUtil
import com.hash.common.utils.timber.TimberUtils
import com.hash.database.AppDataBase
import com.hash.umengsdk.UmengClient
import com.hjq.gson.factory.GsonFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.tencent.bugly.crashreport.CrashReport
import com.tencent.bugly.crashreport.CrashReport.UserStrategy


/**
 * Created by KngLv
 * @time 2026/1/16 09:48
 * @description
 */

object InitManager {

    /** 是否同意了隐私协议 */
    fun isAgreePrivacy(): Boolean {
        return MMKVConst.KEY_AGREE_PRIVACY_RESULT.mmkvGetBoolean(false)
    }

    /** 设置隐私协议结果 */
    fun setAgreePrivacy(result: Boolean) {
        MMKVConst.KEY_AGREE_PRIVACY_RESULT.mmkvPut(result)
    }

    /** 预初始化第三方 SDK  */
    fun preInitSdk(application: Application, isDebug: Boolean) {
        initTimer(isDebug)
        initDb(application)
        initGsonFactoryException(isDebug)
        defaultRefresh()
        UmengClient.preInit(application, isDebug)
    }

    /** 初始化第三方 SDK,在同意隐私权限后调用  */
    fun initSdk(application: Application, isDebug: Boolean, channel: String) {
        UmengClient.init(application, channel)
        BuglyCrashManager.initBugly(application, isDebug, channel)
    }

    private fun initTimer(isDebug: Boolean) = TimberUtils.init(isDebug, enableReporting = false)

    private fun initDb(application: Application) = AppDataBase.init(application)

    private fun initGsonFactoryException(isDebug: Boolean) = GsonFactory.setParseExceptionCallback(
        GsonFactoryParseExceptionDefaultImpl(isDebug)
    )


    private fun defaultRefresh() {
        //设置全局的Header构建器
        SmartRefreshLayout.setDefaultRefreshHeaderCreator { context, layout ->
            layout.setPrimaryColorsId(R.color.primary, R.color.colorOnPrimary)
            MaterialHeader(context).setColorSchemeColors(
                R.color.primary.getColor(),
            )
        }
        //设置全局的Footer构建器
        SmartRefreshLayout.setDefaultRefreshFooterCreator { context, layout ->
            ClassicsFooter(context).setDrawableSize(20f)
        }
    }

}