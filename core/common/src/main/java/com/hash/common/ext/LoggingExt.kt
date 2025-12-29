package com.hash.common.ext

import com.hash.common.utils.TimberUtils
import com.hash.common.utils.ReportingManager

/**
 * 日志扩展函数集合，放在 ext 包下，方便在任意对象或字符串上直接调用。
 */


/** 在任意对象上打印 Verbose 日志（非常详细，开发调试用） */
fun Any?.logV(message: String? = null, vararg args: Any?) {
    if (args.isNotEmpty()) TimberUtils.v(message, *args) else TimberUtils.v(
        message ?: this.toString()
    )
}

/** 在任意对象上打印 Debug 日志（调试信息，如函数入口/出口等） */
fun Any?.logD(message: String? = null, vararg args: Any?) {
    if (args.isNotEmpty()) TimberUtils.d(message, *args) else TimberUtils.d(
        message ?: this.toString()
    )
}

/** 在任意对象上打印 Info 日志（重要运行时事件） */
fun Any?.logI(message: String? = null, vararg args: Any?) {
    if (args.isNotEmpty()) TimberUtils.i(message, *args) else TimberUtils.i(
        message ?: this.toString()
    )
}

/** 在任意对象上打印 Warn 日志（潜在问题） */
fun Any?.logW(message: String? = null, vararg args: Any?) {
    if (args.isNotEmpty()) TimberUtils.w(message, *args) else TimberUtils.w(
        message ?: this.toString()
    )
}

/** 在任意对象上打印 Error 日志（错误/异常），可带 throwable */
fun Any?.logE(message: String? = null, throwable: Throwable? = null, vararg args: Any?) {
    if (throwable != null) {
        if (args.isNotEmpty()) TimberUtils.e(message, throwable, *args) else TimberUtils.e(
            message,
            throwable
        )
    } else {
        if (args.isNotEmpty()) TimberUtils.e(message, null, *args) else TimberUtils.e(
            message ?: this.toString()
        )
    }
}