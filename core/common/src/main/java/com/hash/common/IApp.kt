package com.hash.common

import android.app.Application
import android.content.pm.ApplicationInfo
import com.hash.common.config.GlideApp
import com.hash.common.ext.getColor
import com.hash.common.impl.GsonFactoryParseExceptionDefaultImpl
import com.hash.common.utils.timber.TimberUtils
import com.hash.database.AppDataBase
import com.hjq.gson.factory.GsonFactory
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
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
            initTimer()
            initDb()
            defaultRefresh()
        }

        private fun initDb() = AppDataBase.init(instant)

        private fun initTimer() = TimberUtils.init(isDebug, enableReporting = false)

        private fun initGsonFactoryException() = GsonFactory.setParseExceptionCallback(
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

}