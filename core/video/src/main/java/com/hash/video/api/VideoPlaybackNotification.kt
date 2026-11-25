package com.hash.video.api

import android.app.Application
import android.app.PendingIntent
import android.content.Context
import com.hash.video.player.VideoPlayerEngine

/** 系统媒体通知的页面返回入口配置。 */
object VideoPlaybackNotification {
    /** 设置点击通知后返回应用的 Intent。 */
    fun setActivity(context: Context, pendingIntent: PendingIntent?) {
        VideoPlayerEngine.setNotificationActivity(context.applicationContext as Application, pendingIntent)
    }
}
