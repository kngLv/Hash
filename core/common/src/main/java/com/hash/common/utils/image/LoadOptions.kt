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
 * LoadOptions: image loading options and a Builder for convenient construction.
 * Moved out of the monolithic file to keep responsibilities separated.
 */
data class LoadOptions(
    val placeholderRes: Int? = null,
    val errorRes: Int? = null,
    val placeholderDrawable: Drawable? = null,
    val errorDrawable: Drawable? = null,
    val circleCrop: Boolean = false,
    val roundedRadiusDp: Int? = null,
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
    // typed listeners (prefer these when present)
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

        // typed listener storage
        private var bitmapListener: RequestListener<Bitmap>? = null
        private var gifListener: RequestListener<GifDrawable>? = null
        private var drawableListener: RequestListener<Drawable>? = null

        fun placeholderRes(@DrawableRes res: Int) = apply { this.placeholderRes = res }
        fun errorRes(@DrawableRes res: Int) = apply { this.errorRes = res }
        fun placeholderDrawable(drawable: Drawable) = apply { this.placeholderDrawable = drawable }
        fun errorDrawable(drawable: Drawable) = apply { this.errorDrawable = drawable }
        fun circleCrop() = apply { this.circleCrop = true }
        fun rounded(radiusDp: Int) = apply { this.roundedRadiusDp = radiusDp }
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
}
