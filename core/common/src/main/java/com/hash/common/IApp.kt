package com.hash.common

import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.hash.common.config.GlideApp
import com.hash.common.ext.getColor
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.SpinnerStyle
import com.scwang.smart.refresh.layout.listener.DefaultRefreshFooterCreator
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator
import timber.log.Timber
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
            defaultRefresh()
        }

        private fun initTimer() {
            if (isDebug) {
                Timber.plant(Timber.DebugTree());
            } else {
                Timber.plant(object : Timber.Tree() {
                    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                        // 上传到服务器
                    }
                });
            }
        }

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