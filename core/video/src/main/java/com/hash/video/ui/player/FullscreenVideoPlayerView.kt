package com.hash.video.ui.player

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.ui.panel.FullscreenVideoControlPanel
import com.hash.video.ui.gesture.VideoGestureConfig

/** 应用内横屏全屏播放宿主，页面级呈现与控制层职责分离。 */
@OptIn(UnstableApi::class)
open class FullscreenVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseHashVideoPlayerView(context, attrs, defStyleAttr) {

    private val controlPanel = FullscreenVideoControlPanel(context)
    /** 当前播放器的控制面板；只暴露面板供业务配置按钮，不替换面板实例。 */
    val panel: FullscreenVideoControlPanel
        get() = controlPanel
    /** 全屏尺寸由外层覆盖容器决定，不能修改 ConstraintLayout 的全屏约束。 */
    override fun resolveDisplaySize(
        containerWidth: Int,
        containerHeight: Int,
        media: VideoMedia,
    ) = VideoDisplaySize(containerWidth, containerHeight)

    override fun applyDisplaySize(size: VideoDisplaySize) = Unit

    init {
        setBackgroundColor(Color.BLACK)
        installControlPanel(
            controlPanel,
            VideoGestureConfig(
                singleTapEnabled = true,
                doubleTapEnabled = true,
                seekEnabled = true,
            ),
        )
    }

    /** 全屏的加载、错误、进度和手势 UI 由控制层与可替换的 Panel 共同处理。 */
    override fun onPlaybackStateChanged(state: VideoPlaybackState) = Unit

}
