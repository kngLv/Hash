package com.hash.video.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hash.video.R
import com.hash.video.api.VideoPlaybackState
import com.hash.video.api.VideoPlaybackStatus
import com.hash.video.api.isFinished
import com.hash.video.api.isLoading
import com.hash.video.api.isPlaying

/** 详情页等普通页面使用的嵌入式播放器，承载播放、Loading、全屏和悬浮操作 UI。 */
@OptIn(UnstableApi::class)
class EmbeddedVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseHashVideoPlayerView(context, attrs, defStyleAttr) {

    private val loadingView = ProgressBar(context).apply {
        id = R.id.video_loading
        visibility = GONE
        isIndeterminate = true
    }
    private val playAction = createAction(R.string.video_play).apply {
        id = R.id.video_play_action
    }
    private val fullscreenAction = createAction(R.string.video_fullscreen).apply {
        tag = FULLSCREEN_ACTION_TAG
    }
    private val floatAction = createAction(R.string.video_float).apply {
        tag = FLOAT_ACTION_TAG
    }

    /** 宿主负责将当前画面迁移到横屏全屏容器。 */
    var onFullscreenRequested: (() -> Unit)? = null

    /** 宿主负责将当前画面迁移到悬浮容器。 */
    var onFloatRequested: (() -> Unit)? = null

    init {
        setBackgroundColor(Color.BLACK)
        addView(loadingView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(
            playAction,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        addView(fullscreenAction, actionLayoutParams(endMargin = 12))
        addView(floatAction, actionLayoutParams(endMargin = 68))
        playAction.setOnClickListener { togglePlayback() }
        fullscreenAction.setOnClickListener { onFullscreenRequested?.invoke() }
        floatAction.setOnClickListener { onFloatRequested?.invoke() }
    }

    override fun onPlaybackStateChanged(state: VideoPlaybackState) {
        val status = state.status
        loadingView.visibility = if (status.isLoading) VISIBLE else GONE
        playAction.visibility = if (status.isLoading) GONE else VISIBLE
        playAction.setText(
            when {
                status.isPlaying -> R.string.video_pause
                status.isFinished -> R.string.video_replay
                status == VideoPlaybackStatus.ERROR -> R.string.video_retry
                else -> R.string.video_play
            },
        )
    }

    private fun togglePlayback() {
        val currentMedia = media ?: return
        val state = currentPlaybackState
        if (state.media == currentMedia && state.status.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    private fun actionLayoutParams(endMargin: Int) = LayoutParams(
        LayoutParams.WRAP_CONTENT,
        LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM or Gravity.END,
    ).apply {
        marginEnd = context.dp(endMargin)
        bottomMargin = context.dp(12)
    }

    private fun createAction(textRes: Int): TextView = TextView(context).apply {
        setText(textRes)
        setTextColor(Color.WHITE)
        setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
        setBackgroundColor(0x66000000)
        isClickable = true
        isFocusable = true
    }

    private fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val FULLSCREEN_ACTION_TAG = "video_fullscreen_action"
        const val FLOAT_ACTION_TAG = "video_float_action"
    }
}
