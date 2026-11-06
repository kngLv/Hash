package com.hash.video.cache

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/** 统一管理边播边缓存，避免页面各自持有缓存目录或创建多个缓存实例。 */
@OptIn(UnstableApi::class)
object VideoCacheProvider {

    private const val CACHE_DIRECTORY_NAME = "hash_video_cache"
    private const val MAX_CACHE_BYTES = 512L * 1024L * 1024L

    @Volatile
    private var cache: SimpleCache? = null

    fun getDataSourceFactory(context: Context): CacheDataSource.Factory {
        val applicationContext = context.applicationContext
        return CacheDataSource.Factory()
            .setCache(getCache(applicationContext))
            .setUpstreamDataSourceFactory(DefaultHttpDataSource.Factory())
    }

    private fun getCache(context: Context): SimpleCache {
        cache?.let { return it }
        return synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, CACHE_DIRECTORY_NAME),
                LeastRecentlyUsedCacheEvictor(MAX_CACHE_BYTES),
                StandaloneDatabaseProvider(context),
            ).also { cache = it }
        }
    }
}
