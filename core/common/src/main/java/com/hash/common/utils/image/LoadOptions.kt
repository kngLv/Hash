@file:Suppress("unused")

package com.hash.common.utils.image

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestListener

/**
 * LoadOptions：图片加载选项以及方便构建的 Builder。
 * 把相关逻辑从单一文件中拆分出来以便维护。
 */
data class LoadOptions(
    val placeholderRes: Int? = null,
    val errorRes: Int? = null,
    val placeholderDrawable: Drawable? = null,
    val errorDrawable: Drawable? = null,
    val circleCrop: Boolean = false,
    val roundedRadiusDp: Int? = null,
    val granularRoundedRadiusDp: IntArray? = null, // {tl, tr, br, bl}
    val centerCrop: Boolean = false,
    val crossFade: Boolean = false,
    val asGif: Boolean = false,
    val asBitmap: Boolean = false,
    val overrideWidth: Int? = null,
    val overrideHeight: Int? = null,
    val priority: Priority? = null,
    val transitionDurationMs: Int? = null,
    val diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC,
    val skipMemoryCache: Boolean = false,
    val clearOnLoadFailed: Boolean = false,
    // 控制是否使用 ImageView 的测量尺寸作为 override（默认保留当前自动行为）
    val useViewSizeAsOverride: Boolean = true,
    // 若 true 且 imageView 尚未测量（宽/高为 0），当尝试使用 view 尺寸作为 override 时会延迟加载直到 view 布局完成
    val useDeferredLoad: Boolean = false,
    // typed listeners（优先使用这些 listener）
    val bitmapListener: RequestListener<Bitmap>? = null,
    val gifListener: RequestListener<GifDrawable>? = null,
    val drawableListener: RequestListener<Drawable>? = null
) {
    class Builder {
        private var placeholderRes: Int? = null
        private var errorRes: Int? = null
        private var placeholderDrawable: Drawable? = null
        private var errorDrawable: Drawable? = null
        private var circleCrop = false
        private var roundedRadiusDp: Int? = null
        private var granularRoundedRadiusDp: IntArray? = null
        private var centerCrop = false
        private var crossFade = false
        private var asGif = false
        private var asBitmap = false
        private var overrideWidth: Int? = null
        private var overrideHeight: Int? = null
        private var priority: Priority? = null
        private var transitionDurationMs: Int? = null
        private var diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC
        private var skipMemoryCache = false
        private var clearOnLoadFailed = false
        // 新增 flag
        private var useViewSizeAsOverride = true
        private var useDeferredLoad = false

        // typed listener 存储
        private var bitmapListener: RequestListener<Bitmap>? = null
        private var gifListener: RequestListener<GifDrawable>? = null
        private var drawableListener: RequestListener<Drawable>? = null

        fun placeholderRes(@DrawableRes res: Int) = apply { this.placeholderRes = res }
        fun errorRes(@DrawableRes res: Int) = apply { this.errorRes = res }
        fun placeholderDrawable(drawable: Drawable) = apply { this.placeholderDrawable = drawable }
        fun errorDrawable(drawable: Drawable) = apply { this.errorDrawable = drawable }
        fun circleCrop() = apply { this.circleCrop = true }
        fun rounded(radiusDp: Int) = apply { this.roundedRadiusDp = radiusDp }
        /** 设置统一圆角的同时清除分角圆角配置 */
        fun roundedRadiusDp(radiusDp: Int) = apply { this.roundedRadiusDp = radiusDp; this.granularRoundedRadiusDp = null }
        /**
         * 设置按角分别圆角，顺序为 top-left, top-right, bottom-right, bottom-left（单位 dp）
         */
        fun roundedCorners(tlDp: Int, trDp: Int, brDp: Int, blDp: Int) = apply {
            this.granularRoundedRadiusDp = intArrayOf(tlDp, trDp, brDp, blDp)
            this.roundedRadiusDp = null
        }
        fun centerCrop() = apply { this.centerCrop = true }
        fun crossFade() = apply { this.crossFade = true }
        fun asGif() = apply { this.asGif = true }
        fun asBitmap() = apply { this.asBitmap = true }
        fun override(width: Int, height: Int) = apply { this.overrideWidth = width; this.overrideHeight = height }
        fun priority(p: Priority) = apply { this.priority = p }
        fun transitionDurationMs(ms: Int) = apply { this.transitionDurationMs = ms }
        fun diskCacheStrategy(strategy: DiskCacheStrategy) = apply { this.diskCacheStrategy = strategy }
        fun skipMemoryCache(skip: Boolean) = apply { this.skipMemoryCache = skip }
        fun clearOnLoadFailed(clear: Boolean) = apply { this.clearOnLoadFailed = clear }
        /** 控制是否使用 ImageView 的测量尺寸作为 override */
        fun useViewSizeAsOverride(use: Boolean) = apply { this.useViewSizeAsOverride = use }
        /** 控制是否在 view 未测量时延迟加载直到布局完成 */
        fun useDeferredLoad(use: Boolean) = apply { this.useDeferredLoad = use }

        fun listenerBitmap(listener: RequestListener<Bitmap>) = apply { this.bitmapListener = listener }
        fun listenerGif(listener: RequestListener<GifDrawable>) = apply { this.gifListener = listener }
        fun listenerDrawable(listener: RequestListener<Drawable>) = apply { this.drawableListener = listener }

        fun build() = LoadOptions(
            placeholderRes,
            errorRes,
            placeholderDrawable,
            errorDrawable,
            circleCrop,
            roundedRadiusDp,
            granularRoundedRadiusDp,
            centerCrop,
            crossFade,
            asGif,
            asBitmap,
            overrideWidth,
            overrideHeight,
            priority,
            transitionDurationMs,
            diskCacheStrategy,
            skipMemoryCache,
            clearOnLoadFailed,
            useViewSizeAsOverride,
            useDeferredLoad,
            bitmapListener,
            gifListener,
            drawableListener
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LoadOptions

        if (placeholderRes != other.placeholderRes) return false
        if (errorRes != other.errorRes) return false
        if (circleCrop != other.circleCrop) return false
        if (roundedRadiusDp != other.roundedRadiusDp) return false
        if (centerCrop != other.centerCrop) return false
        if (crossFade != other.crossFade) return false
        if (asGif != other.asGif) return false
        if (asBitmap != other.asBitmap) return false
        if (overrideWidth != other.overrideWidth) return false
        if (overrideHeight != other.overrideHeight) return false
        if (transitionDurationMs != other.transitionDurationMs) return false
        if (skipMemoryCache != other.skipMemoryCache) return false
        if (clearOnLoadFailed != other.clearOnLoadFailed) return false
        if (useViewSizeAsOverride != other.useViewSizeAsOverride) return false
        if (useDeferredLoad != other.useDeferredLoad) return false
        if (placeholderDrawable != other.placeholderDrawable) return false
        if (errorDrawable != other.errorDrawable) return false
        if (!granularRoundedRadiusDp.contentEquals(other.granularRoundedRadiusDp)) return false
        if (priority != other.priority) return false
        if (diskCacheStrategy != other.diskCacheStrategy) return false
        if (bitmapListener != other.bitmapListener) return false
        if (gifListener != other.gifListener) return false
        if (drawableListener != other.drawableListener) return false

        return true
    }

    override fun hashCode(): Int {
        var result = placeholderRes ?: 0
        result = 31 * result + (errorRes ?: 0)
        result = 31 * result + circleCrop.hashCode()
        result = 31 * result + (roundedRadiusDp ?: 0)
        result = 31 * result + centerCrop.hashCode()
        result = 31 * result + crossFade.hashCode()
        result = 31 * result + asGif.hashCode()
        result = 31 * result + asBitmap.hashCode()
        result = 31 * result + (overrideWidth ?: 0)
        result = 31 * result + (overrideHeight ?: 0)
        result = 31 * result + (transitionDurationMs ?: 0)
        result = 31 * result + skipMemoryCache.hashCode()
        result = 31 * result + clearOnLoadFailed.hashCode()
        result = 31 * result + useViewSizeAsOverride.hashCode()
        result = 31 * result + useDeferredLoad.hashCode()
        result = 31 * result + (placeholderDrawable?.hashCode() ?: 0)
        result = 31 * result + (errorDrawable?.hashCode() ?: 0)
        result = 31 * result + (granularRoundedRadiusDp?.contentHashCode() ?: 0)
        result = 31 * result + (priority?.hashCode() ?: 0)
        result = 31 * result + diskCacheStrategy.hashCode()
        result = 31 * result + (bitmapListener?.hashCode() ?: 0)
        result = 31 * result + (gifListener?.hashCode() ?: 0)
        result = 31 * result + (drawableListener?.hashCode() ?: 0)
        return result
    }
}
