package com.hash.video.api

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import com.hash.video.presentation.SystemOverlayHost

/** 系统级视频悬浮窗入口。 */
object VideoSystemOverlay {
    /** 当前设备是否已授予悬浮窗权限。 */
    fun canDraw(context: Context): Boolean = SystemOverlayHost.canDraw(context)

    /** 返回当前悬浮窗正在播放的媒体；用于业务页面快速判断是否可接管。 */
    fun currentMediaOrNull(): VideoMedia? = SystemOverlayHost.currentMediaOrNull()

    /** 将 [token] 正在播放的视频转交给系统悬浮窗。 */
    fun enter(context: Context, token: VideoPlaybackControlToken, returnIntent: PendingIntent?) =
        SystemOverlayHost.enter(context.applicationContext as Application, token, returnIntent)

    /**
     * 页面重新出现时，仅当当前悬浮窗媒体满足 [matches] 时才将播放会话转交回 [token]。
     *
     * 调用方应以自己的内容标识判断，避免错误接管其他页面的悬浮窗播放。
     */
    fun restoreIfMatches(
        token: VideoPlaybackControlToken,
        matches: (VideoMedia) -> Boolean,
    ): Boolean = SystemOverlayHost.restoreTo(token, matches)

    /** 关闭当前系统悬浮窗，并结束其播放会话。 */
    fun close() = SystemOverlayHost.close()
}
