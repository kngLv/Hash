package com.hash.video.ui.player

import android.content.Context
import com.hash.video.player.VideoPlaybackOwner
import java.lang.ref.WeakReference

/**
 * 页面级的唯一画面协调器。
 *
 * RecyclerView、横向列表和 ViewPager2 只需在选中目标变化时调用 [activate]；本类不依赖
 * 具体滚动组件，也不保存页面或 View 的强引用。
 */
class VideoPresentationCoordinator(context: Context) {

    private val applicationContext = context.applicationContext
    val playbackOwner = VideoPlaybackOwner()
    var showNotification: Boolean = false
    private var activePlayer: WeakReference<BaseHashVideoPlayerView>? = null

    /** 激活指定 View；它应已经绑定媒体。 */
    fun activate(player: BaseHashVideoPlayerView) {
        activePlayer?.get()?.takeIf { it !== player }?.detachRender()
        player.playbackOwner = playbackOwner
        player.showNotification = showNotification
        player.play()
        activePlayer = WeakReference(player)
    }

    /** 将当前播放画面无缝迁移到另一个已绑定同媒体的宿主。 */
    fun transferTo(player: BaseHashVideoPlayerView) {
        player.playbackOwner = playbackOwner
        player.showNotification = showNotification
        player.attachRender()
        activePlayer = WeakReference(player)
    }

    /** 当前页面没有可播放目标时暂停并移除画面。 */
    fun deactivate() {
        activePlayer?.get()?.let {
            it.pause()
            it.detachRender()
        }
        activePlayer = null
    }

    /** 页面真正退出时释放其仍拥有的全局播放会话。 */
    fun release() {
        activePlayer?.get()?.detachRender()
        activePlayer = null
        playbackOwner.release(applicationContext)
    }
}
