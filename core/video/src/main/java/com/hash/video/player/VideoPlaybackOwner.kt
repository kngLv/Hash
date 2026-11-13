package com.hash.video.player

import android.app.Application
import android.content.Context

/**
 * 一个页面或信息流协调器的播放会话所有权。
 *
 * 引擎只接受当前 Owner 的释放请求，避免旧页面销毁时误释放新页面正在使用的全局播放器。
 */
class VideoPlaybackOwner {

    /** 结束该 Owner 当前持有的播放会话；非当前 Owner 的调用会被忽略。 */
    fun release(context: Context) {
        VideoPlayerEngine.release(context.applicationContext as Application, this)
    }
}
