package com.hash.video.player

import android.content.Context
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.util.Log
import android.view.ViewGroup

/** 视频模块的唯一入口，保证整个 App 复用同一个播放控制器。 */
object HashVideo {

    /**
     * 控制器仅在进程内复用；其内部强制持有 applicationContext，不会引用 Activity 或 View。
     * Lint 无法跨构造函数追踪该约束，因此在唯一的单例字段上精确抑制误报。
     */
    @SuppressLint("StaticFieldLeak")
    @Volatile
    private var controller: VideoController? = null

    fun controller(context: Context): VideoController {
        return synchronized(this) {
            controller ?: VideoController(context).also {
                controller = it
                Log.d(LOG_TAG, "HashVideo 已创建 VideoController")
            }
        }
    }

    /** 将唯一视频画面挂载到调用方提供的容器；旧容器会自动移除画面。 */
    fun attachRenderTo(container: ViewGroup) = controller(container.context).attachRenderTo(container)

    /** 仅当容器仍承载视频画面时解除挂载，不停止或释放播放会话。 */
    fun detachRenderFrom(container: ViewGroup): Boolean =
        controller(container.context).detachRenderFrom(container)

    /** 设置系统媒体通知点击后应恢复的页面；传入 null 时清理旧目标。 */
    fun setNotificationActivity(context: Context, pendingIntent: PendingIntent?) {
        controller(context).setNotificationActivity(pendingIntent)
    }

    /** 结束当前会话并释放全局播放器；下次调用 [controller] 时会按需创建新实例。 */
    fun release() {
        val releasedController = synchronized(this) {
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
