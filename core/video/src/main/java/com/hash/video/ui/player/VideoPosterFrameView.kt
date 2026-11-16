package com.hash.video.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Color
import android.util.AttributeSet
import android.widget.ImageView
import com.bumptech.glide.Glide

/**
 * 视频真实画面不可用时的占位视图。
 *
 * 只负责封面、暂停帧和隐藏三种展示状态；播放状态和截图时机由外层播放器决定。
 */
internal class VideoPosterFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ImageView(context, attrs, defStyleAttr) {

    private var coverUrl: String? = null
    private var contentType = ContentType.HIDDEN
    private var frozenFrame: Bitmap? = null

    init {
        setBackgroundColor(Color.BLACK)
        isClickable = false
        isFocusable = false
        visibility = GONE
    }

    /** 绑定新媒体封面，并立即显示封面。 */
    fun setCover(coverUrl: String?) {
        this.coverUrl = coverUrl
        showCover(forceReload = true)
    }

    /** 显示已绑定的封面。 */
    fun showCover() = showCover(forceReload = false)

    /** 显示播放器迁移前保存的暂停帧或结束帧。 */
    fun showFrozenFrame(frame: Bitmap) {
        if (contentType == ContentType.FROZEN_FRAME && frozenFrame === frame) {
            visibility = VISIBLE
            return
        }
        clearImageRequest()
        scaleType = ScaleType.FIT_CENTER
        setImageBitmap(frame)
        frozenFrame = frame
        contentType = ContentType.FROZEN_FRAME
        visibility = VISIBLE
    }

    /** 隐藏当前画面；若当前是暂停帧，同时释放该 Bitmap 引用。 */
    fun hideFrozenFrame() {
        if (contentType != ContentType.FROZEN_FRAME) return
        setImageDrawable(null)
        frozenFrame = null
        contentType = ContentType.HIDDEN
        visibility = GONE
    }

    /** 隐藏当前内容，但保留封面请求，便于下一次展示时复用。 */
    fun hide() {
        if (contentType == ContentType.FROZEN_FRAME) {
            hideFrozenFrame()
            return
        }
        visibility = GONE
    }

    /** 取消封面请求并清除当前内容，供绑定新媒体或 View 销毁时调用。 */
    fun clear() {
        clearImageRequest()
        setImageDrawable(null)
        coverUrl = null
        frozenFrame = null
        contentType = ContentType.HIDDEN
        visibility = GONE
    }

    override fun onDetachedFromWindow() {
        clearImageRequest()
        setImageDrawable(null)
        frozenFrame = null
        contentType = ContentType.HIDDEN
        super.onDetachedFromWindow()
    }

    private fun showCover(forceReload: Boolean) {
        if (!forceReload && contentType == ContentType.COVER) {
            visibility = VISIBLE
            return
        }
        scaleType = ScaleType.CENTER_CROP
        frozenFrame = null
        contentType = ContentType.COVER
        visibility = VISIBLE
        val url = coverUrl
        if (url.isNullOrBlank()) {
            clearImageRequest()
            setImageDrawable(null)
        } else {
            if (!isHostDestroyed()) {
                Glide.with(this).load(url).into(this)
            }
        }
    }

    /** Activity 销毁中无需再启动或取消图片请求，Glide 会随该 Activity 的生命周期清理。 */
    private fun clearImageRequest() {
        if (!isHostDestroyed()) Glide.with(this).clear(this)
    }

    /** 查找当前 View 所属的 Activity，判断它是否已进入销毁状态。 */
    private fun isHostDestroyed(): Boolean {
        var currentContext: Context = context
        // View 的 Context 可能包了多层 ContextWrapper，需要逐层取出真正的 Activity。
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext.isDestroyed
            val baseContext = currentContext.baseContext
            if (baseContext === currentContext) break
            currentContext = baseContext
        }
        return false
    }

    private enum class ContentType {
        COVER, FROZEN_FRAME, HIDDEN,
    }
}
