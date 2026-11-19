package com.hash.video.ui.player

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isSameSourceAs
import com.hash.video.ui.panel.EmbeddedVideoControlPanel
import com.hash.video.ui.gesture.VideoGestureConfig

/** 详情页等普通页面使用的嵌入式播放器，承载播放、加载和通用控制 UI。 */
@OptIn(UnstableApi::class)
open class EmbeddedVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseHashVideoPlayerView(context, attrs, defStyleAttr) {

    private val controlPanel = EmbeddedVideoControlPanel(context)
    /** 当前播放器的控制面板；只暴露面板供业务配置按钮，不替换面板实例。 */
    val panel: EmbeddedVideoControlPanel
        get() = controlPanel
    /** 当前媒体已经出现过真实视频画面后，暂停或短暂缓冲都不应再回退到封面。 */
    private var hasDisplayedPlaybackFrame = false

    init {
        setBackgroundColor(Color.BLACK)
        installControlPanel(
            panel = controlPanel,
            gestureConfig = VideoGestureConfig(
                singleTapEnabled = true,
                doubleTapEnabled = true,
                seekEnabled = true,
            ),
        )
    }

    override fun onMediaChanged(media: VideoMedia) {
        hasDisplayedPlaybackFrame = false
        setPosterCover(media)
    }

    override fun onPlaybackStateChanged(state: VideoPlaybackState) {
        renderCover(state)
    }

    /** 非当前播放项展示暂停帧或封面；当前项保留真实暂停画面，不被封面覆盖。 */
    private fun renderCover(state: VideoPlaybackState) {
        val boundMedia = media ?: return
        // 正在播放的不是当前视频源，检查是否需要展示暂停帧或封面
        if (!boundMedia.isSameSourceAs(state.media)) {
            pausedFrameFor(boundMedia)?.let { frame ->
                showPosterFrame(frame)
            } ?: showPosterCover()
            return
        }
        when (state.status) {
            VideoPlaybackStatus.PLAYING,
            VideoPlaybackStatus.PAUSED,
            VideoPlaybackStatus.ENDED,
                -> hasDisplayedPlaybackFrame = true

            else -> Unit
        }
        if (hasDisplayedPlaybackFrame) hidePoster() else showPosterCover()
    }

}
