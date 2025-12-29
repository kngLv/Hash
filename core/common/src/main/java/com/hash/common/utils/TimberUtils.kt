package com.hash.common.utils

import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

/**
 * Timber 轻量封装工具（用于 common 模块）
 *
 * 功能：
 * - init(isDebug) 在应用启动时初始化 Timber（仅负责日志输出）
 * - 提供简单的 v/d/i/w/e 日志方法
 * - 支持带异常的 e 日志
 * - 支持 JSON 美化打印（处理 JSONObject/JSONArray）
 * - 支持超长日志分段打印以避免 Android log 被截断
 */
object TimberUtils {
    private const val CHUNK_SIZE = 4000

    // 存储已植入的 ReportingTree，以便后续可移除，避免重复植入
    private var plantedReportingTree: Timber.Tree? = null

    /**
     * 初始化 Timber（仅负责植入输出 Tree）
     * @param isDebug 是否为调试模式（true：植入 DebugTree；false：植入 ReleaseTree）
     * @param enableReporting 是否同时启用上报（会植入 ReportingTree 并启用 ReportingManager）
     * @param reportingMinPriority 上报采集的最低优先级（默认 WARN）
     */
    fun init(isDebug: Boolean, enableReporting: Boolean = false, reportingMinPriority: Int = Log.WARN) {
        // 清除已存在的 Tree，避免重复打印
        try {
            Timber.uprootAll()
        } catch (_: Throwable) {
            // 忽略异常
        }

        if (isDebug) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(ReleaseTree())
        }

        // 如果需要开启上报，使用集中方法（防止重复植入）
        if (enableReporting) {
            enableReporting(reportingMinPriority)
        }
    }

    /** 运行时开启上报（会设置阈值、启用 ReportingManager 并植入 ReportingTree），幂等 */
    fun enableReporting(reportingMinPriority: Int = Log.WARN) {
        // 设置阈值并启用 manager
        ReportingManager.setThreshold(reportingMinPriority)
        ReportingManager.enable()
        // 仅当尚未植入时才植入 ReportingTree 并保存引用
        if (plantedReportingTree == null) {
            val tree = ReportingManager.ReportingTree(reportingMinPriority)
            Timber.plant(tree)
            plantedReportingTree = tree
        }
    }

    /** 运行时关闭上报（停止 ReportingManager 并移除已植入的 ReportingTree） */
    fun disableReporting() {
        ReportingManager.disable()
        plantedReportingTree?.let { tree ->
            try {
                Timber.uproot(tree)
            } catch (_: Throwable) {
                // 忽略 uproot 错误
            }
            plantedReportingTree = null
        }
    }

    // 下面是各级别的日志快捷方法（仅打印到 Timber）
    /**
     * Verbose 级别日志
     * 适合打印非常详细的调试信息，例如方法内部的局部变量、循环中的中间状态、详细的流程跟踪等。
     * 仅在开发调试时使用，生产环境通常不显示。避免打印频繁且体积大的信息到生产日志。
     */
    fun v(message: String?, vararg args: Any?) {
        if (args.isNotEmpty()) Timber.v(message, *args) else Timber.v(message ?: "null")
    }

    /**
     * Debug 级别日志
     * 适合打印调试相关的信息，如函数入口/出口、重要变量值、业务逻辑的关键分支信息等。
     * 用于帮助开发定位问题，在生产环境可以选择不启用或降低输出量。
     */
    fun d(message: String?, vararg args: Any?) {
        if (args.isNotEmpty()) Timber.d(message, *args) else Timber.d(message ?: "null")
    }

    /**
     * Info 级别日志
     * 适合打印运行时的重要事件或状态变化（但非错误），例如用户登录、重要接口调用成功、关键业务流程完成等。
     * Info 日志可用于审计和统计，但应避免过于冗长。
     */
    fun i(message: String?, vararg args: Any?) {
        if (args.isNotEmpty()) Timber.i(message, *args) else Timber.i(message ?: "null")
    }

    /**
     * Warn 级别日志
     * 适合打印可能导致问题但尚未影响程序继续运行的异常或不正常状态，例如降级、参数异常但已使用默认值、第三方接口延迟等。
     * 这些日志在生产环境应被保留以便排查潜在问题。
     */
    fun w(message: String?, vararg args: Any?) {
        if (args.isNotEmpty()) Timber.w(message, *args) else Timber.w(message ?: "null")
    }

    /**
     * Error 级别日志
     * 适合打印错误、异常或导致功能失败的场景，通常伴随 Throwable 对象一起记录（例如 try/catch 中捕获的异常）。
     * 这些日志应被上报或持久化以便快速定位和修复。
     */
    fun e(message: String?, throwable: Throwable? = null, vararg args: Any?) {
        if (throwable != null) {
            if (args.isNotEmpty()) Timber.e(throwable, message ?: "", *args) else Timber.e(throwable, message ?: "")
        } else {
            if (args.isNotEmpty()) Timber.e(message, *args) else Timber.e(message ?: "null")
        }
    }

    /** 将超长消息按块分割打印，避免被截断 */
    fun logLong(level: Int = Log.DEBUG, message: String?) {
        if (message.isNullOrEmpty()) return
        var start = 0
        val length = message.length
        while (start < length) {
            val end = (start + CHUNK_SIZE).coerceAtMost(length)
            val part = message.substring(start, end)
            when (level) {
                Log.VERBOSE -> Timber.v(part)
                Log.DEBUG -> Timber.d(part)
                Log.INFO -> Timber.i(part)
                Log.WARN -> Timber.w(part)
                Log.ERROR -> Timber.e(part)
                else -> Timber.d(part)
            }
            start = end
        }
    }

    /**
     * 对 JSON 字符串进行美化并分块打印（支持对象和数组）。
     * 若 JSON 无效则回退打印原始内容。
     */
    fun json(json: String?) {
        if (json.isNullOrBlank()) {
            d("Empty/Null json content")
            return
        }

        try {
            val trimmed = json.trim()
            val pretty = when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(4)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(4)
                else -> trimmed
            }

            logLong(Log.DEBUG, pretty)
        } catch (je: JSONException) {
            e("Invalid JSON", je)
            // 回退：分块打印原始内容
            logLong(Log.DEBUG, json)
        }
    }

    /** 临时切换 tag，使用方法：TimberUtils.tag("TAG").d("msg") */
    fun tag(tag: String) = Timber.tag(tag)

    // ReleaseTree：生产环境下只允许 WARN 及以上日志，避免过多噪音
    private class ReleaseTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            return priority >= Log.WARN
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (!isLoggable(tag, priority)) return

            val logTag = tag ?: "Timber"
            if (t != null) {
                // 包含栈信息
                Log.println(priority, logTag, "$message\n${Log.getStackTraceString(t)}")
            } else {
                Log.println(priority, logTag, message)
            }
        }
    }
}
