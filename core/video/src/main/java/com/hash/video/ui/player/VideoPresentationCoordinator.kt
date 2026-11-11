package com.hash.video.ui.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.hash.video.api.VideoPresentation
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

    /** 仍处于 Window 中的候选宿主；当前宿主退出时按最近登记顺序恢复上一宿主。 */
    private val attachedHosts = mutableListOf<WeakReference<BaseHashVideoPlayerView>>()

    /**
     * 最新明确激活、期望接收视频画面的宿主。
     *
     * MediaController 的连接和 Player 操作均可能异步执行，因此它代表“最终期望显示画面的 View”；
     * 较早请求对应的回调完成时，会通过它识别并丢弃过期的绑定操作。
     */
    private var activeHost: WeakReference<BaseHashVideoPlayerView>? = null

    /**
     * 当前实际已绑定 Player、正在接收视频画面输出的宿主。
     *
     * 它用于 `switchTargetView` 的旧目标。刚激活新宿主但尚未完成绑定时，
     * [activeHost] 已是新宿主，而该字段仍保留旧宿主。
     */
    private var renderedHost: WeakReference<BaseHashVideoPlayerView>? = null

    /** 登记可用宿主，不迁移播放器画面，也不修改当前承载状态。 */
    fun attach(host: BaseHashVideoPlayerView) {
        remember(host)
    }

    /** 显式激活宿主并迁移视频画面，供内嵌、全屏、小窗等形态切换使用。 */
    fun activate(host: BaseHashVideoPlayerView) {
        remember(host)
        activeHost = WeakReference(host)
        controller.updatePresentation(host.presentation)
        controller.withPlayer { player ->
            // MediaController 连接是异步的；只允许最后一次请求的、仍在窗口中的宿主接管画面。
            if (activeHost() !== host || !host.isPresentationAvailable) return@withPlayer

            val previousHost = renderedHost()
            if (previousHost === host) return@withPlayer

            PlayerView.switchTargetView(
                player,
                previousHost?.presentationPlayerView,
                host.presentationPlayerView,
            )
            renderedHost = WeakReference(host)
        }
    }

    /**
     * 注销宿主。
     *
     * 非当前宿主（例如全屏打开后仍存在的详情页）注销时不做任何操作，避免错误清空新宿主。
     */
    fun detach(host: BaseHashVideoPlayerView) {
        forget(host)
        if (activeHost() !== host) return

        val fallbackHost = latestAvailableHost()
        if (fallbackHost != null) {
            activate(fallbackHost)
            return
        }

        activeHost = null
        controller.updatePresentation(VideoPresentation.EMBEDDED)
        controller.withPlayer { player ->
            if (activeHost() == null && renderedHost() === host) {
                PlayerView.switchTargetView(player, host.presentationPlayerView, null)
                renderedHost = null
            }
        }
    }

    private fun activeHost(): BaseHashVideoPlayerView? = activeHost?.get()

    private fun renderedHost(): BaseHashVideoPlayerView? = renderedHost?.get()

    /**
     * 将当前仍存活的宿主加入候选列表。
     *
     * 列表使用弱引用，不持有页面或 View；同一宿主只登记一次，登记顺序用于当前宿主退出后的回退。
     */
    private fun remember(host: BaseHashVideoPlayerView) {
        forgetCollectedHosts()
        if (attachedHosts.none { it.get() === host }) {
            attachedHosts += WeakReference(host)
        }
    }

    /** 移除已分离的指定宿主，并顺带清理已被回收的弱引用。 */
    private fun forget(host: BaseHashVideoPlayerView) {
        attachedHosts.removeAll { it.get() == null || it.get() === host }
    }

    /** 清理弱引用已失效的宿主，避免候选列表保留无效条目。 */
    private fun forgetCollectedHosts() {
        attachedHosts.removeAll { it.get() == null }
    }

    /** 返回最后登记且仍附着到 Window 的宿主，作为当前画面退出后的回退目标。 */
    private fun latestAvailableHost(): BaseHashVideoPlayerView? {
        forgetCollectedHosts()
        return attachedHosts.asReversed()
            .firstNotNullOfOrNull { it.get()?.takeIf(BaseHashVideoPlayerView::isPresentationAvailable) }
    }
}
