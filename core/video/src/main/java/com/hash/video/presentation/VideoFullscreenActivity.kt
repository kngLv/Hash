package com.hash.video.presentation

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hash.video.player.HashVideo
import com.hash.video.ui.player.FullscreenVideoPlayerView

/** 承载全局播放会话的横屏全屏页面，不创建或重新准备媒体。 */
class VideoFullscreenActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        val media = HashVideo.controller(this).playbackState.value.media
        if (media == null) {
            finish()
            return
        }

        val playerView = FullscreenVideoPlayerView(this).apply {
            setMedia(media)
            onExitFullscreenRequested = ::finish
        }
        setContentView(
            playerView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(startIntent(context))
        }

        internal fun startIntent(context: Context): Intent =
            Intent(context, VideoFullscreenActivity::class.java).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
    }
}
