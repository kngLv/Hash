package com.hash.home.ui.featured.detail.video

import android.content.Context
import android.util.AttributeSet
import com.hash.video.ui.player.FullscreenVideoPlayerView

/** 新闻详情的竖屏沉浸播放器，复用通用全屏 UI。 */
@Suppress("UnsafeOptInUsageError")
class NewsDetailPortraitVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FullscreenVideoPlayerView(context, attrs, defStyleAttr)
