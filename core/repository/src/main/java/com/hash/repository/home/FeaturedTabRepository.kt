package com.hash.repository.home

import android.content.ComponentCallbacks2
import android.util.LruCache
import com.hash.bean.home.NewsListBean
import com.hash.common.cache.CacheTrimManager
import com.hash.common.const.AppConst
import com.hash.common.ext.mmkvGetObject
import com.hash.common.ext.mmkvPutObject
import com.hash.common.manager.BuglyCrashManager
import com.hash.net.net.launch.request
import com.hash.net.net.request.RequestActionImpl
import com.hash.net.response.RollResponse
import com.hash.repository.rollApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * FeaturedTabRepository
 *
 * 概要：
 * 提供首页/标签页场景下按页加载的仓库能力，包含内存 LRU 缓存（页级）、磁盘缓存（MMKV，按需写入）、
 * 并发请求去重（in-flight 去重）以及 stale-while-revalidate 的静默后台刷新机制。
 *
 * 主要职责：
 * - 提供网络请求封装（`newsList`）和缓存优先的读取接口（`getNewsListCachedWithSource`）。
 * - 避免因频繁切 tab 或短时间重复请求导致的网络浪费（通过内存/磁盘缓存 + in-flight 去重）。
 * - 在内存紧张时对外暴露缓存清理/裁剪方法并通过 `CacheTrimManager` 注册，交由 Application 统一触发。
 *
 * 行为与设计要点：
 * - 缓存键格式："{typeId}:{page}"，粒度为一页（page-level）。
 * - 内存缓存：共享的 LRU（每个 entry 表示一页，sizeOf 返回 1），全局页数上限由 DEFAULT_MAX_CACHE_ENTRIES 控制。
 * - 磁盘缓存：按页持久化为 CacheEntry（包含时间戳），为避免磁盘膨胀，默认只将指定页（diskCachePage）写入磁盘。
 * - perTabMemoryPages：限制单个 tab 向内存缓存写入的最大页数，防止单个 tab 占满全局缓存。
 * - stale-while-revalidate：当内存/磁盘命中时若 enableBackgroundRefresh 为 true，会静默触发后台刷新并在完成后通过
 *   onBackgroundUpdated 在主线程通知最新结果；静默刷新不会触发 onNetworkStarted(true)，因此 UI 不应显示全屏加载。
 *
 * 并发/生命周期注意事项：
 * - repoScope 是应用级 CoroutineScope（Dispatchers.IO + SupervisorJob），后台任务不会随单个 UI 生命周期自动取消；
 *   在 UI 层（例如 ViewModel）使用该仓库时，应在自己的 scope 内管理显示/取消逻辑。
 * - inFlightRequests 使用 ConcurrentHashMap + CompletableDeferred 去重并复用同一请求的结果；请求完成或发生异常后
 *   会从 map 中移除对应项，避免内存泄漏。
 *
 * 参数：
 * @param perTabMemoryPages 每个 tab 在内存中允许缓存的页数（默认 2），用于限制单个 tab 占用内存。
 * @param diskCachePage      需要写入磁盘的页码（通常为首页 1），用于降低磁盘写入频率（默认 1）。
 */

class FeaturedTabRepository(
    /**
     * 每个 tab 在内存中允许缓存的页数（默认 2）
     */
    private val perTabMemoryPages: Int = 2,

    /**
     * 要写入磁盘的页码（通常为首页 = 1），用于降低磁盘写入量（默认 1）
     */
    private val diskCachePage: Int = 1,
) {

    init {
        // 在初始化时向 CacheTrimManager 注册清理/trim 处理器
        try {
            CacheTrimManager.register(object : CacheTrimManager.CacheTrimHandler {
                override fun clear() {
                    clearMemoryCache()
                }

                override fun trim(level: Int) {
                    trimMemory(level)
                }
            })
        } catch (_: Throwable) {
        }
    }

    /**
     * 原始网络请求封装（向后兼容）
     *
     * @param typeId tab 类型 id
     * @param page 页码（字符串形式，直接传递给底层 API）
     */
    fun newsList(
        typeId: String,
        page: String,
    ): RequestActionImpl<RollResponse<NewsListBean>> {
        return request {
            rollApi.newsList(
                AppConst.rollAppId, AppConst.rollAppSecret,
                typeId, page
            )
        }
    }

    // 默认 TTL：1 分钟
    private val defaultTtlMs = 60_000L

    companion object {
        /**
         * 默认允许缓存的页数（单位：页）。
         * 说明：memoryCache 的 sizeOf 返回 1（每个 entry 代表一页），因此该常量表示页数上限。
         */
        private const val DEFAULT_MAX_CACHE_ENTRIES = 50

        // 当前内存缓存的条目上限（可在运行时由第一份创建的实例决定）
        @Volatile
        private var currentMaxEntries: Int = DEFAULT_MAX_CACHE_ENTRIES

        // LRU 内存缓存（共享），键格式："{typeId}:{page}"，sizeOf 固定为 1（每页计 1）
        private var memoryCache: LruCache<String, CacheEntry<NewsListBean>> =
            object : LruCache<String, CacheEntry<NewsListBean>>(currentMaxEntries) {
                override fun sizeOf(key: String?, value: CacheEntry<NewsListBean>?): Int {
                    return 1
                }
            }

        // in-flight 去重：key -> CompletableDeferred<CachedResult>
        private val inFlightRequests =
            ConcurrentHashMap<String, CompletableDeferred<CachedResult<NewsListBean>?>>()

        // 共享的 repo scope：应用级别，不与 UI 生命周期绑定
        val repoScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        /**
         * 清空内存缓存（在 onLowMemory 或严重内存压力时调用）
         */
        @Suppress("unused")
        fun clearMemoryCache() {
            try {
                memoryCache.evictAll()
            } catch (_: Throwable) {
            }
        }

        /**
         * 根据系统内存等级裁剪缓存大小或清理（在 onTrimMemory 中调用）
         *
         * 说明：对较严重的等级直接 evictAll，UI 隐藏等级则尝试 trimToSize(half)
         */
        @Suppress("unused", "DEPRECATION")
        fun trimMemory(level: Int) {
            try {
                when (level) {
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                    ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
                    ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                    ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
                    ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                        memoryCache.evictAll()
                    }

                    ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                        val half = (currentMaxEntries / 2).coerceAtLeast(1)
                        memoryCache.trimToSize(half)
                    }

                    else -> {
                        // 其余等级不作处理
                    }
                }
            } catch (_: Throwable) {
            }
        }
    }

    // CacheEntry 包含数据与时间戳，既用于内存也用于磁盘序列化
    data class CacheEntry<T>(val data: T, val timestampMs: Long)

    // 数据来源枚举
    enum class CacheSource {
        MEMORY, DISK, NETWORK
    }

    // 带来源的结果包装
    data class CachedResult<T>(val data: T?, val source: CacheSource)

    /**
     * 直接读取内存缓存（非阻塞）
     *
     * @param typeId tab id
     * @param page 页码
     * @return 命中时返回 NewsListBean，否则返回 null
     */
    @Suppress("unused")
    fun getCached(typeId: String, page: Int): NewsListBean? {
        val key = "$typeId:$page"
        return memoryCache.get(key)?.data
    }

    /**
     * 主要的缓存读取方法：优先内存 -> 磁盘 -> 网络，支持去重与静默后台刷新
     *
     * @param typeId tab 的类型 id，用于构造缓存 key 并作为网络参数
     * @param page 要获取的页码
     * @param ttlMs 缓存有效期（毫秒），仅在 ignoreTtl == false 时生效，默认 60_000ms
     * @param forceRefresh 是否强制跳过内存/磁盘缓存并走网络（例如用户下拉刷新）
     * @param onNetworkStarted 可选回调：
     *        - true：表明正在/将要使用网络（或复用已有 in-flight），UI 可显示刷新动画；
     *        - false：表明直接从内存或磁盘返回，UI 不应显示网络加载动画。
     * @param enableBackgroundRefresh 当内存/磁盘命中且为 true 时，会在后台静默发起网络刷新（不会触发 onNetworkStarted(true)），
     *        刷新完成后会通过 onBackgroundUpdated 在主线程通知最新结果（可能为 null 表示失败）。
     * @param onBackgroundUpdated 后台刷新完成时的回调（在主线程回调）
     * @param ignoreTtl 若为 true，则只要缓存存在即命中，不再检查是否到期（适合切 tab 快速恢复场景）
     *
     * @return CachedResult 包含 data 与 source（MEMORY/DISK/NETWORK），data 可能为 null（网络或解析失败）
     */
    suspend fun getNewsListCachedWithSource(
        typeId: String,
        page: Int,
        ttlMs: Long = defaultTtlMs,
        forceRefresh: Boolean = false,
        onNetworkStarted: ((Boolean) -> Unit)? = null,
        enableBackgroundRefresh: Boolean = false,
        onBackgroundUpdated: ((CachedResult<NewsListBean>?) -> Unit)? = null,
        // 当 ignoreTtl 为 true 时只要有缓存即命中，不再检查是否超过 ttl
        ignoreTtl: Boolean = false
    ): CachedResult<NewsListBean>? {
        val key = "$typeId:$page"
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            memoryCache.get(key)?.let { entry ->
                if (ignoreTtl || now - entry.timestampMs <= ttlMs) {
                    onNetworkStarted?.invoke(false)
                    if (enableBackgroundRefresh) {
                        // 静默后台刷新（stale-while-revalidate）
                        backgroundRevalidate(key, typeId, page, onBackgroundUpdated)
                    }
                    return CachedResult(entry.data, CacheSource.MEMORY)
                }
            }
        }

        // 尝试从磁盘读取（MMKV），命中则回写到内存并返回（并校验磁盘时间戳）
        val diskKey = "featured:$key"
        try {
            val diskEntry: CacheEntry<NewsListBean>? = diskKey.mmkvGetObject()
            if (diskEntry != null && !forceRefresh) {
                if (ignoreTtl || now - diskEntry.timestampMs <= ttlMs) {
                    if (page <= perTabMemoryPages) memoryCache.put(key, diskEntry)
                    onNetworkStarted?.invoke(false)
                    if (enableBackgroundRefresh) {
                        backgroundRevalidate(key, typeId, page, onBackgroundUpdated)
                    }
                    return CachedResult(diskEntry.data, CacheSource.DISK)
                }
            }
        } catch (_: Throwable) {
            // 忽略磁盘读取错误，继续网络流程
        }

        // 如果已有 in-flight 请求，复用之；此时说明网络已经在进行
        inFlightRequests[key]?.let { ongoing ->
            onNetworkStarted?.invoke(true)
            return try {
                ongoing.await()
            } catch (_: Exception) {
                null
            }
        }

        // 否则创建 CompletableDeferred 并注册到 inFlightRequests
        val deferred = CompletableDeferred<CachedResult<NewsListBean>?>()
        val prev = inFlightRequests.putIfAbsent(key, deferred)
        if (prev != null) {
            onNetworkStarted?.invoke(true)
            return try {
                prev.await()
            } catch (_: Exception) {
                null
            }
        }

        // 将发起网络请求，通知回调
        onNetworkStarted?.invoke(true)

        // 发起网络请求（在 repoScope），完成后写入内存和磁盘缓存
        repoScope.launch {
            try {
                val resp = newsList(typeId, page.toString()).execute()
                val news = resp?.data()
                if (news is NewsListBean) {
                    val entry = CacheEntry(news, System.currentTimeMillis())
                    if (page <= perTabMemoryPages) memoryCache.put(key, entry)
                    if (page == diskCachePage) {
                        try {
                            diskKey.mmkvPutObject(entry)
                        } catch (e: Throwable) {
                            e.printStackTrace()
                            BuglyCrashManager.postCatchException(e)
                        }
                    }
                    deferred.complete(CachedResult(news, CacheSource.NETWORK))
                } else {
                    deferred.complete(null)
                }
            } catch (e: Exception) {
                deferred.completeExceptionally(e)
            } finally {
                inFlightRequests.remove(key)
            }
        }

        return try {
            deferred.await()
        } catch (_: Exception) {
            null
        }
    }

    // 启动后台刷新（stale-while-revalidate），不触发 onNetworkStarted
    private fun backgroundRevalidate(
        key: String,
        typeId: String,
        page: Int,
        onBackgroundUpdated: ((CachedResult<NewsListBean>?) -> Unit)?
    ) {
        // 如果已有 in-flight 请求，复用并在完成后回调
        inFlightRequests[key]?.let { existing ->
            repoScope.launch {
                try {
                    val res = existing.await()
                    onBackgroundUpdated?.let {
                        withContext(Dispatchers.Main) { it(res) }
                    }
                } catch (_: Exception) {
                    onBackgroundUpdated?.let { withContext(Dispatchers.Main) { it(null) } }
                }
            }
            return
        }

        val deferred = CompletableDeferred<CachedResult<NewsListBean>?>()
        val prev = inFlightRequests.putIfAbsent(key, deferred)
        if (prev != null) {
            repoScope.launch {
                try {
                    val res = prev.await()
                    onBackgroundUpdated?.let { withContext(Dispatchers.Main) { it(res) } }
                } catch (_: Exception) {
                    onBackgroundUpdated?.let { withContext(Dispatchers.Main) { it(null) } }
                }
            }
            return
        }

        // 发起静默网络请求并在完成后更新缓存与回调
        repoScope.launch {
            try {
                val resp = newsList(typeId, page.toString()).execute()
                val news = resp?.data()
                if (news is NewsListBean) {
                    val entry = CacheEntry(news, System.currentTimeMillis())
                    if (page <= perTabMemoryPages) memoryCache.put(key, entry)
                    if (page == diskCachePage) {
                        try {
                            ("featured:$key").mmkvPutObject(entry)
                        } catch (_: Throwable) {
                        }
                    }
                    deferred.complete(CachedResult(news, CacheSource.NETWORK))
                    onBackgroundUpdated?.let {
                        withContext(Dispatchers.Main) {
                            it(
                                CachedResult(
                                    news,
                                    CacheSource.NETWORK
                                )
                            )
                        }
                    }
                } else {
                    deferred.complete(null)
                    onBackgroundUpdated?.let { withContext(Dispatchers.Main) { it(null) } }
                }
            } catch (_: Exception) {
                deferred.completeExceptionally(Exception("background fetch failed"))
                onBackgroundUpdated?.let { withContext(Dispatchers.Main) { it(null) } }
            } finally {
                inFlightRequests.remove(key)
            }
        }
    }
}