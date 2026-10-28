package com.hash.home.data.repository

import android.content.ComponentCallbacks2
import android.util.LruCache
import com.hash.common.cache.CacheTrimManager
import com.hash.common.const.AppConst
import com.hash.common.ext.mmkvGetTypeObject
import com.hash.common.ext.mmkvPutObject
import com.hash.home.data.api.rollApi
import com.hash.home.data.model.NewsListBean
import com.hash.net.launch.request
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/** 精选分类新闻的数据来源，负责请求以及短期页缓存。 */
class FeaturedTabRepository(
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
) {

    suspend fun loadPage(
        typeId: String,
        page: Int,
        /** 为 true 时跳过内存、磁盘缓存并直接请求网络。 */
        bypassCache: Boolean,
    ): NewsListBean {
        val key = "$typeId:$page"
        if (!bypassCache) {
            readMemory(key)?.let { return it }
            readDisk(key, page)?.let { entry ->
                memoryCache.put(key, entry)
                return entry.data
            }
        }

        return requestPage(key, typeId, page)
    }

    private suspend fun requestPage(key: String, typeId: String, page: Int): NewsListBean {
        inFlightRequests[key]?.let { return it.await() }

        val deferred = CompletableDeferred<NewsListBean>()
        val existing = inFlightRequests.putIfAbsent(key, deferred)
        if (existing != null) return existing.await()

        try {
            val news = requestNews(typeId, page)
            val entry = CacheEntry(news, System.currentTimeMillis())
            memoryCache.put(key, entry)
            if (page == FIRST_PAGE) {
                withContext(Dispatchers.IO) { diskKey(key).mmkvPutObject(entry) }
            }
            return news.also(deferred::complete)
        } catch (cancellation: CancellationException) {
            deferred.cancel(cancellation)
            throw cancellation
        } catch (error: Exception) {
            deferred.completeExceptionally(error)
            throw error
        } finally {
            inFlightRequests.remove(key, deferred)
        }
    }

    private suspend fun requestNews(typeId: String, page: Int): NewsListBean {
        var requestError: Exception? = null
        val response = request {
            rollApi.newsList(
                appId = AppConst.rollAppId,
                appSecret = AppConst.rollAppSecret,
                type = typeId,
                page = page.toString(),
            )
        }
            .onError { requestError = it }
            .onErrorCode { code, errorResponse ->
                requestError = IllegalStateException(errorResponse?.message() ?: "请求失败，错误码：$code")
            }
            .execute()

        return response?.data()
            ?: throw requestError
            ?: IllegalStateException("新闻列表为空")
    }

    private fun readMemory(key: String): NewsListBean? {
        val entry = memoryCache.get(key) ?: return null
        return entry.takeIf { !it.isExpired() }?.data
    }

    private suspend fun readDisk(key: String, page: Int): CacheEntry? {
        if (page != FIRST_PAGE) return null
        val entry = withContext(Dispatchers.IO) {
            diskKey(key).mmkvGetTypeObject<CacheEntry>()
        } ?: return null
        return entry.takeIf { !it.isExpired() }
    }

    private fun CacheEntry.isExpired(): Boolean =
        System.currentTimeMillis() - timestampMs > cacheTtlMs

    data class CacheEntry(
        val data: NewsListBean,
        val timestampMs: Long,
    )

    companion object {
        private const val DEFAULT_CACHE_TTL_MS = 120_000L
        private const val FIRST_PAGE = 1
        private const val MAX_MEMORY_ENTRIES = 50

        private val memoryCache = object : LruCache<String, CacheEntry>(MAX_MEMORY_ENTRIES) {
            override fun sizeOf(key: String?, value: CacheEntry?): Int = 1
        }
        private val inFlightRequests = ConcurrentHashMap<String, CompletableDeferred<NewsListBean>>()

        init {
            CacheTrimManager.register(object : CacheTrimManager.CacheTrimHandler {
                override fun clear() = memoryCache.evictAll()

                override fun trim(level: Int) {
                    when (level) {
                        ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
                        ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW,
                        ComponentCallbacks2.TRIM_MEMORY_COMPLETE,
                        ComponentCallbacks2.TRIM_MEMORY_BACKGROUND,
                        ComponentCallbacks2.TRIM_MEMORY_MODERATE -> memoryCache.evictAll()

                        ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> memoryCache.trimToSize(MAX_MEMORY_ENTRIES / 2)
                    }
                }
            })
        }

        private fun diskKey(key: String): String = "featured:$key"
    }
}
