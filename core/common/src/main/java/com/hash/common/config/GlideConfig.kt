@file:Suppress("unused")

package com.hash.common.config

import android.content.Context
import android.util.Log
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.engine.bitmap_recycle.LruBitmapPool
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import java.io.File

/**
 * 应用级 Glide 配置（AppGlideModule）。
 * 将在注解处理阶段生成 GlideApp/GlideRequests 等类型安全的 API。
 * 注意：不要在库模块中依赖生成的 GlideApp，库应优先使用 Glide.with(...)；但在 app 层启用该 Module 可使用 GlideApp。
 */
@GlideModule
class GlideConfig : AppGlideModule() {
    companion object {

        /** 本地图片缓存文件最大值（Bytes） */
        private const val IMAGE_DISK_CACHE_MAX_SIZE: Long = 500L * 1024L * 1024L
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        // 使用应用内部缓存目录（context.cacheDir），不需要申请存储权限；
        // 若你想使用外部缓存请改为 context.externalCacheDir 并注意运行时权限与可用性。
        val diskCacheFile = File(context.cacheDir, "glide")
        // 若目标路径存在且是文件，尝试删除（保留目录）
        if (diskCacheFile.exists() && diskCacheFile.isFile) {
            diskCacheFile.delete()
        }

        // 确保目录存在；若创建失败则回退到 context.cacheDir
        val cacheDirToUse = if (diskCacheFile.exists() || diskCacheFile.mkdirs()) {
            diskCacheFile
        } else {
            // 如果无法创建自定义目录，回退并记录日志以便排查
            try {
                Log.w("GlideConfig", "无法创建 glide 缓存目录，回退到默认 cacheDir")
            } catch (_: Throwable) {}
            context.cacheDir
        }

        builder.setDiskCache {
            DiskLruCacheWrapper.create(cacheDirToUse, IMAGE_DISK_CACHE_MAX_SIZE)
        }
        val calculator: MemorySizeCalculator = MemorySizeCalculator.Builder(context).build()
        val defaultMemoryCacheSize: Int = calculator.memoryCacheSize
        val defaultBitmapPoolSize: Int = calculator.bitmapPoolSize
        val customMemoryCacheSize: Long = (1.2 * defaultMemoryCacheSize).toLong()
        val customBitmapPoolSize: Long = (1.2 * defaultBitmapPoolSize).toLong()
        builder.setMemoryCache(LruResourceCache(customMemoryCacheSize))
        builder.setBitmapPool(LruBitmapPool(customBitmapPoolSize))
        builder.setDefaultRequestOptions(
            RequestOptions()
//            // 设置默认加载中占位图
//            .placeholder(R.drawable.image_loading_ic)
//            // 设置默认加载出错占位图
//            .error(R.drawable.image_error_ic)
        )
    }

//    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
//        // Glide 默认使用的是 HttpURLConnection 来做网络请求，这里切换成更高效的 OkHttp
//        registry.replace(GlideUrl::class.java, InputStream::class.java, OkHttpLoader.Factory(EasyConfig.getInstance().client))
//    }

    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}
