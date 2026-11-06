package com.hash.video.player

import android.content.Context
import com.hash.video.ui.VideoPresentationCoordinator

/** 视频模块的唯一入口，保证整个 App 复用同一个播放控制器。 */
object HashVideo {

    @Volatile
    private var controller: VideoController? = null

    @Volatile
    private var presentationCoordinator: VideoPresentationCoordinator? = null

    fun controller(context: Context): VideoController {
        controller?.let { return it }
        return synchronized(this) {
            controller ?: VideoController(context.applicationContext).also { controller = it }
        }
    }

    /** 返回全局唯一的画面宿主协调器，避免多个 View 直接竞争同一个播放器输出。 */
    internal fun presentationCoordinator(context: Context): VideoPresentationCoordinator {
        presentationCoordinator?.let { return it }
        return synchronized(this) {
            presentationCoordinator ?: VideoPresentationCoordinator(controller(context)).also {
                presentationCoordinator = it
            }
        }
    }
}
