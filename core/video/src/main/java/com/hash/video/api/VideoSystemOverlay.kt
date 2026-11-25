package com.hash.video.api

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import com.hash.video.player.VideoPlaybackControlToken
import com.hash.video.presentation.SystemOverlayHost

/** 系统级视频悬浮窗入口。 */
object VideoSystemOverlay {
    /** 当前设备是否已授予悬浮窗权限。 */
    fun canDraw(context: Context): Boolean = SystemOverlayHost.canDraw(context)

    /** 将 [token] 正在播放的视频转交给系统悬浮窗。 */
    fun enter(context: Context, token: VideoPlaybackControlToken, returnIntent: PendingIntent?) =
        SystemOverlayHost.enter(context.applicationContext as Application, token, returnIntent)

    /** 页面重新出现时，如有待恢复的悬浮窗播放则转交回 [token]。 */
    fun restoreIfRequested(token: VideoPlaybackControlToken): Boolean = SystemOverlayHost.restoreTo(token)
}
