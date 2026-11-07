package com.hash.video.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.hash.video.player.VideoController
import java.lang.ref.WeakReference

/**
 * 统一管理唯一播放器在不同界面宿主之间的视频画面输出。
 *
 * 播放器本身仍由 [VideoController] 对应的媒体服务持有；该类只迁移 PlayerView 的渲染目标，
 * 不会准备媒体、改变播放进度或触发播放。
 */
@OptIn(UnstableApi::class)
internal class VideoPresentationCoordinator(
    private val controller: VideoController,
) {

    /**
     * 最新请求接管视频画面的宿主。
     *
     * MediaController 的连接和 Player 操作均可能异步执行，因此它代表“最终期望显示画面的 View”；
     * 较早请求对应的回调完成时，会通过它识别并丢弃过期的绑定操作。
     */
    private var requestedHost: WeakReference<BaseHashVideoPlayerView>? = null

    /**
     * 当前实际已绑定 Player、正在接收视频画面输出的宿主。
     *
     * 它用于 `switchTargetView` 的旧目标。刚发起接管请求但尚未完成绑定时，
     * [requestedHost] 可以已经是新宿主，而该字段仍保留旧宿主。
     */
    private var boundHost: WeakReference<BaseHashVideoPlayerView>? = null

    /** 登记宿主；没有当前画面宿主时，首个可用宿主承接画面。 */
    fun register(host: BaseHashVideoPlayerView) {
        if (requestedHost() == null || requestedHost() === host) {
            requestHost(host)
        }
    }

    /** 显式请求当前宿主接管视频画面，供全屏、小窗等展示形态切换使用。 */
    fun takeOver(host: BaseHashVideoPlayerView) {
        requestHost(host)
    }

    /**
     * 注销宿主。
     *
     * 非当前宿主（例如全屏打开后仍存在的详情页）注销时不做任何操作，避免错误清空新宿主。
     */
    fun unregister(host: BaseHashVideoPlayerView) {
        if (requestedHost() !== host) return
        requestedHost = null
        controller.withPlayer { player ->
            if (requestedHost() == null && boundHost() === host) {
                PlayerView.switchTargetView(player, host.presentationPlayerView, null)
                boundHost = null
            }
        }
    }

    private fun requestHost(host: BaseHashVideoPlayerView) {
        requestedHost = WeakReference(host)
        controller.withPlayer { player ->
            // MediaController 连接是异步的；只允许最后一次请求的、仍在窗口中的宿主接管画面。
            if (requestedHost() !== host || !host.isPresentationAvailable) return@withPlayer

            val previousHost = boundHost()
            if (previousHost === host) return@withPlayer

            PlayerView.switchTargetView(
                player,
                previousHost?.presentationPlayerView,
                host.presentationPlayerView,
            )
            boundHost = WeakReference(host)
        }
    }

    private fun requestedHost(): BaseHashVideoPlayerView? = requestedHost?.get()

    private fun boundHost(): BaseHashVideoPlayerView? = boundHost?.get()
}
