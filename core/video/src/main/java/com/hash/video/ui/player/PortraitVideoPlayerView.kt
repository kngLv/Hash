package com.hash.video.ui.player

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

/**
 * 竖屏沉浸播放的标准 UI 容器。
 *
 * 它复用全屏播放器的手势与控制栏，方向、导航与画面迁移仍由业务页面控制。
 */
@OptIn(UnstableApi::class)
open class PortraitVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FullscreenVideoPlayerView(context, attrs, defStyleAttr) {

    /** 宿主负责退出竖屏沉浸播放。 */
    var onExitPortraitRequested: (() -> Unit)? = null

    init {
        onExitFullscreenRequested = { onExitPortraitRequested?.invoke() }
    }
}
