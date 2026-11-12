package com.hash.home.ui.featured.detail.video

import android.content.Context
import android.util.AttributeSet
import com.hash.video.ui.player.FullscreenVideoPlayerView

/** 新闻详情的竖屏沉浸播放器，复用通用全屏 UI，并将退出操作交给详情页。 */
@Suppress("UnsafeOptInUsageError")
class NewsDetailPortraitVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FullscreenVideoPlayerView(context, attrs, defStyleAttr) {

    /** 请求新闻详情页关闭竖屏沉浸展示。 */
    var onExitPortraitRequested: (() -> Unit)? = null

    init {
        onExitFullscreenRequested = { onExitPortraitRequested?.invoke() }
    }
}
