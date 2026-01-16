package com.hash.common.impl

import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonToken
import com.hash.common.ext.logE
import com.hash.database.AppDataBase
import com.hash.database.entity.GsonFactoryPaseExceptionEntity
import com.hjq.gson.factory.ParseExceptionCallback
import com.hash.common.ext.safeLaunchIO
import com.hash.common.manager.BuglyCrashManager
import com.hash.umengsdk.UmengClient

/**
 * Created by KngLv
 * @time 2026/1/15 09:58
 * @description Gson 解析异常回调默认实现
 */

class GsonFactoryParseExceptionDefaultImpl(val isDebug: Boolean) : ParseExceptionCallback {
    override fun onParseObjectException(
        typeToken: TypeToken<*>?,
        fieldName: String?,
        jsonToken: JsonToken?
    ) {
        handlerGsonParseException("解析对象析异常：$typeToken#$fieldName，后台返回的类型为：$jsonToken")
    }

    override fun onParseListItemException(
        typeToken: TypeToken<*>?,
        fieldName: String?,
        listItemJsonToken: JsonToken?
    ) {
        handlerGsonParseException("解析 List 异常：$typeToken#$fieldName，后台返回的条目类型为：$listItemJsonToken")
    }

    override fun onParseMapItemException(
        typeToken: TypeToken<*>?,
        fieldName: String?,
        mapItemKey: String?,
        mapItemJsonToken: JsonToken?
    ) {
        handlerGsonParseException("解析 Map 异常：$typeToken#$fieldName，mapItemKey = $mapItemKey，后台返回的条目类型为：$mapItemJsonToken")
    }

    private fun handlerGsonParseException(message: String?) {
        logE(message)
        val exception = IllegalArgumentException(message)
        BuglyCrashManager.postCatchException(exception)
        if (isDebug) {
            throw exception
        } else {
            val entity = GsonFactoryPaseExceptionEntity()
            entity.isDebug = isDebug
            entity.message = message
            safeLaunchIO {
                try {
                    AppDataBase.db.gsonFactoryPaseExceptionDao().insert(entity)
                } catch (t: Throwable) {
                    // swallow any exceptions (already handled in safeLaunchIO, but keep guard)
                    t.printStackTrace()
                }
            }
        }
    }
}