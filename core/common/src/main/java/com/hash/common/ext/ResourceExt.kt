package com.hash.common.ext

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.hash.common.IApp

/** Color 资源扩展函数* */
fun @receiver:ColorRes Int.getColor(): Int {
    return ContextCompat.getColor(IApp.instant, this)
}

/**
 *  字符串资源扩展函数
 * */
fun @receiver:StringRes Int.getString(): String {
    return ContextCompat.getString(IApp.instant, this)
}