package com.hash.video.player

import android.content.Context
import android.util.Log
import com.hash.video.ui.player.VideoPresentationCoordinator

/** 视频模块的唯一入口，保证整个 App 复用同一个播放控制器。 */
object HashVideo {

    @Volatile
    private var controller: VideoController? = null

    @Volatile
    private var presentationCoordinator: VideoPresentationCoordinator? = null

    fun controller(context: Context): VideoController {
        return synchronized(this) {
            controller ?: VideoController(context.applicationContext).also {
                controller = it
                Log.d(LOG_TAG, "HashVideo 已创建 VideoController")
            }
        }
    }

    /** 返回全局唯一的画面宿主协调器，避免多个 View 直接竞争同一个播放器输出。 */
    internal fun presentationCoordinator(context: Context): VideoPresentationCoordinator {
        return synchronized(this) {
            presentationCoordinator ?: VideoPresentationCoordinator(controller(context)).also {
                presentationCoordinator = it
            }
        }
    }

    /** 结束当前会话并释放全局播放器；下次调用 [controller] 时会按需创建新实例。 */
    fun release() {
        val releasedController = synchronized(this) {
            presentationCoordinator = null
            controller.also { controller = null }
        }
        Log.d(
            LOG_TAG,
            "HashVideo 已请求释放：controllerExists=${releasedController != null}",
        )
        releasedController?.release()
    }

    private const val LOG_TAG = "HashVideoLifecycle"
}
