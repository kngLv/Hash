package com.hash.common.cache

import com.hash.common.ext.logD
import com.hash.common.ext.logE
import com.hash.common.ext.logW
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 全局缓存回收注册器，供各模块注册自己的内存清理逻辑，
 * 由 Application 在 onTrimMemory/onLowMemory 时统一触发。
 */
object CacheTrimManager {

    private const val TAG = "CacheTrimManager"

    interface CacheTrimHandler {
        fun clear()
        fun trim(level: Int)
    }

    private val handlers = CopyOnWriteArrayList<CacheTrimHandler>()

    // 统计信息（线程安全）
    data class CacheTrimStats(
        val totalClearCount: Int,
        val totalTrimCount: Int,
        val trimCountsByLevel: Map<Int, Int>,
        val lastClearTs: Long?,
        val lastTrimTs: Long?
    )

    private val totalClearCount = AtomicInteger(0)
    private val totalTrimCount = AtomicInteger(0)
    private val trimCountsByLevel = ConcurrentHashMap<Int, AtomicInteger>()
    @Volatile
    private var lastClearTs: Long? = null
    @Volatile
    private var lastTrimTs: Long? = null

    fun register(handler: CacheTrimHandler) {
        if (!handlers.contains(handler)) handlers.add(handler)
    }

    @Suppress("unused")
    fun unregister(handler: CacheTrimHandler) {
        handlers.remove(handler)
    }

    fun clearAll() {
        // update stats
        totalClearCount.incrementAndGet()
        lastClearTs = System.currentTimeMillis()
        TAG.logD("clearAll triggered, handlers=${handlers.size}")

        for (h in handlers) {
            try {
                h.clear()
            } catch (t: Throwable) {
                TAG.logW("handler.clear() failed: ${t.message}")
                t.logE("handler.clear() failed", t)
            }
        }
    }

    fun trimAll(level: Int) {
        // update stats
        totalTrimCount.incrementAndGet()
        trimCountsByLevel.computeIfAbsent(level) { AtomicInteger(0) }.incrementAndGet()
        lastTrimTs = System.currentTimeMillis()
        TAG.logD("trimAll triggered level=$level, handlers=${handlers.size}")

        for (h in handlers) {
            try {
                h.trim(level)
            } catch (t: Throwable) {
                TAG.logW("handler.trim($level) failed: ${t.message}")
                t.logE("handler.trim($level) failed", t)
            }
        }
    }

    /**
     * 获取当前统计快照（线程安全）
     */
    @Suppress("unused")
    fun getStats(): CacheTrimStats {
        val map = HashMap<Int, Int>()
        for ((k, v) in trimCountsByLevel) map[k] = v.get()
        return CacheTrimStats(
            totalClearCount = totalClearCount.get(),
            totalTrimCount = totalTrimCount.get(),
            trimCountsByLevel = map,
            lastClearTs = lastClearTs,
            lastTrimTs = lastTrimTs
        )
    }

    /**
     * 重置统计（供测试或调试使用）
     */
    @Suppress("unused")
    fun resetStats() {
        totalClearCount.set(0)
        totalTrimCount.set(0)
        trimCountsByLevel.clear()
        lastClearTs = null
        lastTrimTs = null
        TAG.logD("stats reset")
    }
}
