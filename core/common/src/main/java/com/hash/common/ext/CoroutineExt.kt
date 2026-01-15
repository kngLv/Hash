package com.hash.common.ext

import kotlinx.coroutines.*

/**
 * 常用协程扩展工具
 * 放在 core/common 的 ext 包下，供全局使用
 * 设计要点：
 * - 提供一个应用级别的可复用 scope（默认 IO），用于短期后台任务或上报/记录
 * - 提供常用的安全启动函数 safeLaunchIO / safeLaunchMain
 * - 提供 withIO 的简洁封装用于在 suspend 环境切换 Dispatcher
 */

object AppCoroutine {
    // 全局共享 Scope：SupervisorJob 避免子协程失败取消整个 Scope
    // 默认使用 IO Dispatcher（适合数据库、文件、网络 I/O）
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 取消全局 scope（谨慎使用，通常不需要在应用生命周期内调用）
     */
    fun cancel() {
        scope.coroutineContext[Job]?.cancel()
    }
}

/**
 * 在全局 IO scope 上安全启动一个协程，内部自动捕获异常并打印（不抛出）
 * 适合用于上报、日志写入、非关键后台任务
 */
fun safeLaunchIO(
    block: suspend CoroutineScope.() -> Unit
): Job = AppCoroutine.scope.launch {
    try {
        block()
    } catch (t: Throwable) {
        // 避免在 release 环境抛出导致崩溃，使用打印或替换成日志上报
        t.printStackTrace()
    }
}

/**
 * 在调用方的 Scope 中以 IO Dispatcher 启动并安全执行
 */
fun CoroutineScope.launchIOSafe(
    block: suspend CoroutineScope.() -> Unit
): Job = this.launch(Dispatchers.IO) {
    try {
        block()
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

/**
 * 在调用方的 Scope 中以 Main Dispatcher 启动并安全执行（UI 相关）
 */
fun CoroutineScope.launchMainSafe(
    block: suspend CoroutineScope.() -> Unit
): Job = this.launch(Dispatchers.Main) {
    try {
        block()
    } catch (t: Throwable) {
        t.printStackTrace()
    }
}

/**
 * 在 suspend 环境中切换到 IO 并执行，返回结果
 */
suspend fun <T> withIO(block: suspend CoroutineScope.() -> T): T = withContext(Dispatchers.IO) {
    block()
}

/**
 * 创建一个带默认异常处理的 CoroutineExceptionHandler
 * 使用示例：
 * val handler = defaultExceptionHandler("UploadJob")
 * CoroutineScope(SupervisorJob() + Dispatchers.IO + handler).launch { ... }
 */
fun defaultExceptionHandler(tag: String? = null): CoroutineExceptionHandler {
    return CoroutineExceptionHandler { _, throwable ->
        if (tag != null) {
            try {
                println("[$tag] uncaught coroutine error: ${'$'}throwable")
            } catch (_: Throwable) {
            }
        } else {
            throwable.printStackTrace()
        }
    }
}

