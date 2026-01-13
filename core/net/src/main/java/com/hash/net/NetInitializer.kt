package com.hash.net

import android.app.Application
import android.content.Context
import androidx.startup.Initializer
import com.hash.common.ext.showToast
import com.hash.common.storage.userInfo.UserInfoStore
import com.hash.net.interceptor.WanCookieInterceptor
import com.hash.net.net.LvHttp
import com.hash.net.net.error.CodeException
import com.hash.net.net.error.ErrorKey
import com.hash.net.net.error.ErrorValue

/**
 * @name NInitializer
 * @package com.hash.net
 * @author 345 QQ:1831712732
 * @time 2024/12/19 23:13
 * @description
 */
class NetInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        LvHttp.Builder(context as Application)
            .setBaseUrl(NetConstants.BASE_URL)
            //是否开启缓存
            .isCache(false)
            .setCode(0,1)
            //是否打印 log
            .isLog(true)
            .addInterceptor(WanCookieInterceptor())
            //对 Code 异常的处理，可自定义,参考 ResponseData 类
            .setErrorDispose(ErrorKey.ErrorCode, ErrorValue {
                (it as? CodeException)?.run {
                    if (code == -1001) {
                        UserInfoStore.clearUserInfo()
                    }
                    "${it.message}".showToast()
                }
            })
            //全局异常处理，参考 ILaunch.kt ，可自定义异常处理，参考 ErrorKey 即可
            .setErrorDispose(ErrorKey.AllException, ErrorValue {
                it.printStackTrace()
                "${it.message}".showToast()
            })
            .build()
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> {
        return mutableListOf()
    }
}