@file:Suppress("unused")

package com.hash.common.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.Transformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.BitmapTransitionOptions
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.bitmap.GranularRoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.load.resource.gif.GifDrawable
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition
import com.bumptech.glide.request.transition.DrawableCrossFadeFactory
import com.hash.common.config.GlideApp
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.RequestManager

/**
 * ImageLoader 核心实现：封装了对 Glide 的调用逻辑，负责根据传入的 LoadOptions 路由到 GIF/Bitmap/Drawable 的不同加载路径。
 */
object ImageLoader {

    enum class ExpectedListenerType { ANY, BITMAP, GIF, DRAWABLE }

    // LoadOptions 在 LoadOptions.kt 中定义

    // 构建通用 RequestOptions（供外部复用）
    private fun buildRequestOptions(ctx: Context, options: LoadOptions, applyBitmapTransforms: Boolean): RequestOptions {
        var ro = RequestOptions().diskCacheStrategy(options.diskCacheStrategy)
        if (options.skipMemoryCache) ro = ro.skipMemoryCache(true)
        options.placeholderRes?.let { ro = ro.placeholder(it).error(it) }
        options.errorRes?.let { ro = ro.error(it) }
        options.placeholderDrawable?.let { ro = ro.placeholder(it).error(it) }
        options.errorDrawable?.let { ro = ro.error(it) }

        // override 尺寸
        options.overrideWidth?.let { w -> options.overrideHeight?.let { h -> ro = ro.override(w, h) } }
        // 优先级
        options.priority?.let { p -> ro = ro.priority(p) }

        // 只有在允许 bitmap 转换时才应用圆角/centerCrop 等基于 Bitmap 的 transforms
        if (applyBitmapTransforms) {
            val transformations = mutableListOf<Transformation<Bitmap>>()
            if (options.centerCrop) transformations.add(CenterCrop())
            // 如果提供了按角度的圆角，则优先使用
            options.granularRoundedRadiusDp?.let {
                if (it.size == 4) {
                    val tl = dp2px(ctx, it[0].toFloat()).toFloat()
                    val tr = dp2px(ctx, it[1].toFloat()).toFloat()
                    val br = dp2px(ctx, it[2].toFloat()).toFloat()
                    val bl = dp2px(ctx, it[3].toFloat()).toFloat()
                    transformations.add(GranularRoundedCorners(tl, tr, br, bl))
                }
            } ?: run {
                options.roundedRadiusDp?.let { transformations.add(RoundedCorners(dp2px(ctx, it.toFloat()))) }
            }
            if (transformations.isNotEmpty()) ro = ro.transform(*transformations.toTypedArray())
        } else {
            // 对 GIF 路径只应用 centerCrop（若请求），避免对 GifDrawable 应用不兼容的 Bitmap 转换
            if (options.centerCrop) ro = ro.centerCrop()
        }

        if (options.circleCrop) ro = ro.circleCrop()

        return ro
    }

    // 将 transition 与 listener 附加到 Bitmap RequestBuilder（类型安全）
    private fun attachTransitionAndListenerBitmap(rb: RequestBuilder<Bitmap>, options: LoadOptions): RequestBuilder<Bitmap> {
        var builder = rb
        if (options.crossFade) {
            options.transitionDurationMs?.let { duration ->
                val factory = DrawableCrossFadeFactory.Builder(duration).setCrossFadeEnabled(true).build()
                builder = builder.transition(BitmapTransitionOptions.withCrossFade(factory))
            } ?: run {
                builder = builder.transition(BitmapTransitionOptions.withCrossFade())
            }
        }
        // 优先使用类型化的 listener
        options.bitmapListener?.let { builder = builder.listener(it) }
        return builder
    }

    private fun attachTransitionAndListenerDrawable(rb: RequestBuilder<Drawable>, options: LoadOptions): RequestBuilder<Drawable> {
        var builder = rb
        if (options.crossFade) {
            options.transitionDurationMs?.let { duration ->
                val factory = DrawableCrossFadeFactory.Builder(duration).setCrossFadeEnabled(true).build()
                builder = builder.transition(DrawableTransitionOptions.withCrossFade(factory))
            } ?: run {
                builder = builder.transition(DrawableTransitionOptions.withCrossFade())
            }
        }
        options.drawableListener?.let { builder = builder.listener(it) }
        return builder
    }

    private fun attachTransitionAndListenerGif(rb: RequestBuilder<GifDrawable>, options: LoadOptions): RequestBuilder<GifDrawable> {
        var builder = rb
        if (options.crossFade) {
            options.transitionDurationMs?.let { duration ->
                val factory = DrawableCrossFadeFactory.Builder(duration).setCrossFadeEnabled(true).build()
                builder = builder.transition(DrawableTransitionOptions.withCrossFade(factory))
            } ?: run {
                builder = builder.transition(DrawableTransitionOptions.withCrossFade())
            }
        }
        options.gifListener?.let { builder = builder.listener(it) }
        return builder
    }

    /**
     * 统一入口：根据 options 路由到对应的加载流程（Drawable / Bitmap / Gif）。
     */
    fun load(imageView: ImageView, model: Any?, options: LoadOptions = LoadOptions()) {
        val ctx = imageView.context
        if (model == null || (model is String && model.isBlank())) {
            if (options.placeholderDrawable != null) imageView.setImageDrawable(options.placeholderDrawable)
            else if (options.placeholderRes != null) imageView.setImageResource(options.placeholderRes)
            else if (options.clearOnLoadFailed) GlideApp.with(imageView).clear(imageView)
            return
        }

        // 路由：优先 asGif -> asBitmap/Bitmap listener -> Drawable
        if (options.asGif) {
            // GIF 路径
            var rb: RequestBuilder<GifDrawable> = GlideApp.with(imageView).asGif().load(model)
            var ro = buildRequestOptions(ctx, options, applyBitmapTransforms = false)
            if (options.useViewSizeAsOverride && (options.overrideWidth == null || options.overrideHeight == null) && imageView.width > 0 && imageView.height > 0) {
                ro = ro.override(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1))
            }
            rb = rb.apply(ro)
            rb = attachTransitionAndListenerGif(rb, options)
            rb.into(imageView)
            return
        }

        if (options.asBitmap || options.bitmapListener != null) {
            // Bitmap 路径
            var rb: RequestBuilder<Bitmap> = GlideApp.with(imageView).asBitmap().load(model)
            var ro = buildRequestOptions(ctx, options, applyBitmapTransforms = true)
            if (options.useViewSizeAsOverride && (options.overrideWidth == null || options.overrideHeight == null) && imageView.width > 0 && imageView.height > 0) {
                ro = ro.override(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1))
            }
            rb = rb.apply(ro)
            rb = attachTransitionAndListenerBitmap(rb, options)
            rb.into(imageView)
            return
        }

        // 默认 Drawable 路径
        var rb: RequestBuilder<Drawable> = GlideApp.with(imageView).load(model)
        var ro = buildRequestOptions(ctx, options, applyBitmapTransforms = true)
        if (options.useViewSizeAsOverride && (options.overrideWidth == null || options.overrideHeight == null) && imageView.width > 0 && imageView.height > 0) {
            ro = ro.override(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1))
        }
        rb = rb.apply(ro)
        rb = attachTransitionAndListenerDrawable(rb, options)
        rb.into(imageView)
    }

    /**
     * 公共接口：带可选的 lifecycleOwner 重载。
     * 如果传入了 lifecycleOwner，会尽量将请求与该生命周期绑定（优先尝试 Fragment/FragmentActivity/Activity）；
     * 否则会将请求与 ImageView 的视图上下文绑定。
     */
    fun load(imageView: ImageView, model: Any?, lifecycleOwner: LifecycleOwner? = null, options: LoadOptions = LoadOptions()) {
        // 延迟加载处理：如果 view 未被测量且调用方请求延迟加载，则在布局完成后再执行加载
        if (options.useDeferredLoad && options.useViewSizeAsOverride && (imageView.width == 0 || imageView.height == 0)) {
            imageView.post {
                // 布局完成后调用内部加载逻辑
                internalLoad(imageView, model, lifecycleOwner, options)
            }
            return
        }

        internalLoad(imageView, model, lifecycleOwner, options)
    }

    // 内部实现：执行实际的 Glide 请求；lifecycleOwner 可为 null
    private fun internalLoad(imageView: ImageView, model: Any?, lifecycleOwner: LifecycleOwner?, options: LoadOptions) {
        val ctx = imageView.context
        if (model == null || (model is String && model.isBlank())) {
            if (options.placeholderDrawable != null) imageView.setImageDrawable(options.placeholderDrawable)
            else if (options.placeholderRes != null) imageView.setImageResource(options.placeholderRes)
            else if (options.clearOnLoadFailed) GlideApp.with(imageView).clear(imageView)
            return
        }

        // 根据传入的 lifecycleOwner 智能选择 RequestManager
        // 优先支持 androidx.fragment.app.Fragment / FragmentActivity / android.app.Activity，其他情况回退到以 view 绑定
        val rmProvider: (ImageView) -> RequestManager = { view ->
            when (lifecycleOwner) {
                null -> GlideApp.with(view)
                is androidx.fragment.app.Fragment -> GlideApp.with(lifecycleOwner)
                is androidx.fragment.app.FragmentActivity -> GlideApp.with(lifecycleOwner)
                is android.app.Activity -> GlideApp.with(lifecycleOwner)
                else -> GlideApp.with(view)
            }
        }

        // 路由：优先 asGif -> asBitmap/Bitmap listener -> Drawable
        if (options.asGif) {
            var rb: RequestBuilder<GifDrawable> = rmProvider(imageView).asGif().load(model)
            var ro = buildRequestOptions(ctx, options, applyBitmapTransforms = false)
            if (options.useViewSizeAsOverride && (options.overrideWidth == null || options.overrideHeight == null) && imageView.width > 0 && imageView.height > 0) {
                ro = ro.override(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1))
            }
            rb = rb.apply(ro)
            rb = attachTransitionAndListenerGif(rb, options)
            rb.into(imageView)
            return
        }

        if (options.asBitmap || options.bitmapListener != null) {
            var rb: RequestBuilder<Bitmap> = rmProvider(imageView).asBitmap().load(model)
            var ro = buildRequestOptions(ctx, options, applyBitmapTransforms = true)
            if (options.useViewSizeAsOverride && (options.overrideWidth == null || options.overrideHeight == null) && imageView.width > 0 && imageView.height > 0) {
                ro = ro.override(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1))
            }
            rb = rb.apply(ro)
            rb = attachTransitionAndListenerBitmap(rb, options)
            rb.into(imageView)
            return
        }

        var rb: RequestBuilder<Drawable> = rmProvider(imageView).load(model)
        var ro = buildRequestOptions(ctx, options, applyBitmapTransforms = true)
        if (options.useViewSizeAsOverride && (options.overrideWidth == null || options.overrideHeight == null) && imageView.width > 0 && imageView.height > 0) {
            ro = ro.override(imageView.width.coerceAtLeast(1), imageView.height.coerceAtLeast(1))
        }
        rb = rb.apply(ro)
        rb = attachTransitionAndListenerDrawable(rb, options)
        rb.into(imageView)
    }

    /**
     * 清理磁盘缓存（必须在后台线程执行，Glide 要求）。
     */
    fun clearImageDiskCache(context: Context) {
        val appCtx = context.applicationContext
        Thread {
            try {
                GlideApp.get(appCtx).clearDiskCache()
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }.start()
    }

    /**
     * 清理内存缓存（必须在主线程执行）。方法会在非主线程时自动切换到主线程。
     */
    fun clearImageMemoryCache(context: Context) {
        val appCtx = context.applicationContext
        if (Looper.myLooper() == Looper.getMainLooper()) {
            GlideApp.get(appCtx).clearMemory()
        } else {
            Handler(Looper.getMainLooper()).post { GlideApp.get(appCtx).clearMemory() }
        }
    }

    /**
     * 加载需要更细粒度控制 view/资源回调的长图（使用 CustomViewTarget）。
     */
    fun loadLongImage(context: Context, url: String?, imageView: ImageView) {
        if (url == null || url.isBlank()) return
        GlideApp.with(imageView)
            .load(url)
            .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.RESOURCE))
            .into(object : CustomViewTarget<ImageView, Drawable>(imageView) {
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    imageView.setImageDrawable(errorDrawable)
                }

                override fun onResourceCleared(placeholder: Drawable?) {
                    imageView.setImageDrawable(placeholder)
                }

                override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                    imageView.setImageDrawable(resource)
                }
            })
    }

    private fun dp2px(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}
