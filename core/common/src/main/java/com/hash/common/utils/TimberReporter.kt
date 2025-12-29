package com.hash.common.utils

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.ConcurrentLinkedQueue

/** 上报日志的公共数据结构 */
data class ReportLogEntry(
    val timestamp: Long,
    val level: Int,
    val tag: String?,
    val message: String,
    val throwable: String?
)

/**
 * 日志上报管理器：负责缓存日志并按周期批量上报。
 * 将上报逻辑与打印逻辑拆分，使用时可单独启用上报（或植入 ReportingTree）
 */
object ReportingManager {
    private val reportQueue: ConcurrentLinkedQueue<ReportLogEntry> = ConcurrentLinkedQueue()

    // reporter 会接收一批日志进行上报；默认实现只是通过 Timber 打印一条信息
    @Volatile
    private var reporter: (List<ReportLogEntry>) -> Unit = { list ->
        if (list.isNotEmpty()) {
            Timber.tag("ReportingManager").i("Reporting %d logs", list.size)
        }
    }

    // 是否启用上报（默认关闭，用户可通过 enable() 开启）
    @Volatile
    private var enabled: Boolean = false

    // 上报阈值：只有优先级 >= threshold 的日志会被缓存并上报（默认 WARN）
    @Volatile
    private var threshold: Int = Log.WARN

    // 上报周期，单位毫秒（默认 60 秒）
    @Volatile
    private var periodMillis: Long = 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null

    /** 设置自定义上报函数 */
    fun setReporter(fn: (List<ReportLogEntry>) -> Unit) {
        reporter = fn
    }

    /** 立即启用上报（会启动周期任务） */
    fun enable() {
        if (enabled) return
        enabled = true
        start()
    }

    /** 立即禁用上报（会停止周期任务并 flush 剩余日志） */
    fun disable() {
        if (!enabled) return
        enabled = false
        stop()
    }

    /** 切换上报开关 */
    fun toggle() {
        if (enabled) disable() else enable()
    }

    /** 当前是否启用上报 */
    fun isEnabled(): Boolean = enabled

    /** 设置上报阈值 */
    fun setThreshold(minPriority: Int) {
        threshold = minPriority
    }

    /** 设置上报周期（毫秒），最小 1000ms */
    fun setPeriodMillis(ms: Long) {
        periodMillis = ms.coerceAtLeast(1000L)
        if (job != null && job?.isActive == true) {
            stop()
            start()
        }
    }

    private fun start() {
        if (!enabled) return
        if (job != null && job?.isActive == true) return

        job = scope.launch {
            while (isActive && enabled) {
                try {
                    delay(periodMillis)
                    flush()
                } catch (t: Throwable) {
                    Timber.tag("ReportingManager").e(t, "Reporter loop error")
                }
            }
        }
    }

    private fun stop() {
        job?.cancel()
        job = null
        flush()
    }

    /** 将队列中的日志取出并交给 reporter 处理 */
    fun flush() {
        val batch = mutableListOf<ReportLogEntry>()
        while (true) {
            val e = reportQueue.poll() ?: break
            batch.add(e)
        }
        if (batch.isNotEmpty()) {
            try {
                reporter(batch)
            } catch (t: Throwable) {
                Timber.tag("ReportingManager").e(t, "Reporter invocation failed")
            }
        }
    }

    /** 入队一条日志用于后续批量上报。若未启用或等级低于 threshold，则直接丢弃。 */
    fun enqueue(level: Int, tag: String?, message: String?, throwable: Throwable?) {
        if (!enabled) return
        if (level < threshold) return

        val msg = message ?: "null"
        val stack = throwable?.let { Log.getStackTraceString(it) }
        val entry = ReportLogEntry(System.currentTimeMillis(), level, tag, msg, stack)
        reportQueue.add(entry)
    }

    /** 提供一个可植入的 Timber.Tree：仅用于把日志入队（不负责打印） */
    class ReportingTree(private val minPriority: Int = Log.WARN) : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            return priority >= minPriority
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // 只做入队操作，不做打印
            enqueue(priority, tag, message, t)
        }
    }
}

