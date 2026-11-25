package com.hash.home.ui.featured.detail.video

import android.app.PendingIntent
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.chad.library.adapter4.BaseQuickAdapter
import com.hash.home.ui.featured.detail.adapter.NewsDetailAdapter
import com.hash.home.ui.featured.detail.adapter.VideoHost
import com.hash.video.api.VideoMedia
import com.hash.video.api.VideoPlaybackState
import com.hash.video.player.VideoController
import com.hash.video.player.VideoPlaybackOwner
import com.hash.video.ui.player.EmbeddedVideoPlayerView
import java.lang.ref.WeakReference
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 将新闻详情播放队列的当前位置同步到对应卡片播放器。
 *
 * 只保存已附着 View 的弱引用；卡片离屏或被 RecyclerView 复用时会立即注销，因此不会把
 * View、Holder 或 Activity 保留在进程级播放器会话中。
 */
class NewsDetailVideoPresentationCoordinator(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val recyclerView: RecyclerView,
    private val adapter: NewsDetailAdapter,
    playbackOwner: VideoPlaybackOwner,
) {

    /** 详情页专用的轻量控制入口，仅用于队列配置和播放状态观察。 */
    private val controller = VideoController(context).apply {
        this.playbackOwner = playbackOwner
    }

    /**
     * 当前已附着到 RecyclerView 窗口的视频卡片：队列索引 -> 该卡片的播放器 View。
     *
     * 它不保存离屏/已回收卡片，也不保存尚未创建的卡片；RecyclerView 的预加载使“已附着”
     * 不一定严格等于完全可见。唯一 ExoPlayer 的画面最终只会挂到其中一个 View。
     */
    private val hosts = mutableMapOf<Int, WeakReference<EmbeddedVideoPlayerView>>()
    private val adapterPositions = mutableMapOf<Int, Int>()
    private var stateJob: Job? = null

    /** 当前由新闻详情内嵌卡片承载画面的队列位置；用于在切项时由本页主动迁移画面。 */
    private var presentedInlineIndex: Int? = null

    /** 最近一次已按播放状态处理过的队列位置；进度刷新不能再次触发 RecyclerView 定位。 */
    private var lastPresentedPlaylistIndex: Int? = null

    /**
     * release() 后置为 true：Adapter 可能仍派发已经排队的 attach/detach 回调，所有入口
     * 都必须停止修改宿主映射或操作 View，避免详情页销毁后发生越界渲染与引用泄漏。
     */
    private var released = false

    /**
     * BRVAH 在整个 Item 进入或离开 RecyclerView 时回调；集中监听即可，不需要每个
     * VideoHolder 再向页面层传递监听器。
     */
    private val hostAttachListener = object : BaseQuickAdapter.OnViewAttachStateChangeListener {
        override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
            adapter.videoHostOf(holder)?.let(::registerHost)
        }

        override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
            adapter.videoHostOf(holder)?.let(::unregisterHost)
        }
    }

    fun start() {
        if (stateJob != null || released) return
        adapter.addOnViewAttachStateChangeListener(hostAttachListener)
        registerAttachedHosts()
        stateJob = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 监听播放状态变化，更新当前卡片的渲染画面。
                controller.playbackState.collectLatest(::renderPlaybackState)
            }
        }
    }

    /**
     * 替换详情数据时更新可选队列及索引到 Adapter 位置的映射。
     *
     * 单视频不保留队列；多视频队列供 ExoPlayer 自动续播和后台通知上一条、下一条使用。
     */
    fun updatePlaybackQueue(
        items: List<VideoMedia>,
        positions: Map<Int, Int>,
    ) {
        if (released) return
        adapterPositions.clear()
        adapterPositions.putAll(positions)
        // submitList 可能复用一个仍在屏幕上的 Holder；旧队列位置不能继续指向它。
        hosts.clear()
        lastPresentedPlaylistIndex = null
        if (items.size > 1) controller.setPlaybackQueue(items) else controller.clearPlaybackQueue()
    }

    /** 设置本详情页播放会话对应的通知点击目标，与具体播放器 View 无关。 */
    fun setNotificationActivity(pendingIntent: PendingIntent?) {
        if (!released) controller.setNotificationActivity(pendingIntent)
    }

    /** 列表 Diff 提交完成后调用，补齐被复用且仍在屏幕内的 Holder。 */
    fun onAdapterListCommitted() {
        if (released) return
        registerAttachedHosts()
        controller.playbackState.value.playlistIndex?.let(::presentCurrentHost)
    }

    private fun registerHost(host: VideoHost) {
        // NO_POSITION 表示 Holder 已移除、正被复用或尚未完成布局，不能作为有效卡片定位。
        if (released || host.adapterPosition == RecyclerView.NO_POSITION) return
        hosts[host.playlistIndex] = WeakReference(host.playerView)
        adapterPositions[host.playlistIndex] = host.adapterPosition
        val state = controller.playbackState.value
        if (state.playlistIndex == host.playlistIndex)
            attachToInlineHost(host.playlistIndex, host.playerView)
    }

    private fun unregisterHost(host: VideoHost) {
        // Holder 复用、Diff 刷新时索引可能已经改变，按 View 清理比按索引更安全。
        hosts.entries.removeAll { (_, value) -> value.get() === host.playerView }
        if (presentedInlineIndex == host.playlistIndex) presentedInlineIndex = null
    }

    private fun renderPlaybackState(state: VideoPlaybackState) {
        if (released) return
        val currentIndex = state.playlistIndex ?: return
        // ExoPlayer 只发布新的媒体和队列索引；新闻多卡片页面在此决定是否迁移唯一画面。
        // 同一媒体的进度每 500ms 都会更新，不能因此把用户刚刚滑走的卡片再滚回屏幕。
        if (lastPresentedPlaylistIndex == currentIndex) return
        lastPresentedPlaylistIndex = currentIndex
        presentCurrentHost(currentIndex)
    }

    private fun presentCurrentHost(index: Int) {
        if (presentedInlineIndex != index) {
            // 这是新闻详情“每个视频一张卡片”的页面策略：自动或手动切项后，旧卡片不应
            // 继续显示新视频。单播放器、选集等页面不使用本协调器，因而不会被强制摘除。
            presentedInlineIndex?.let(::hostAt)?.detachRender()
            presentedInlineIndex = null
        }
        hostAt(index)?.let { host ->
            if (host.isAttachedToWindow)
                attachToInlineHost(index, host)
            else
                requestTargetHost(index)
        } ?: findAttachedHost(index)?.let(::registerHost) ?: requestTargetHost(index)
    }

    /** 将唯一渲染 View 迁入目标新闻卡片，不改变 ExoPlayer 的媒体和播放状态。 */
    private fun attachToInlineHost(index: Int, host: EmbeddedVideoPlayerView) {
        host.attachRender()
        presentedInlineIndex = index
    }

    private fun requestTargetHost(index: Int) {
        val position = adapterPositions[index] ?: return
        recyclerView.scrollToPosition(position)
    }

    private fun hostAt(index: Int): EmbeddedVideoPlayerView? = hosts[index]?.get()?.also { host ->
        if (!host.isAttachedToWindow) hosts.remove(index)
    }

    /** start() 晚于首帧布局时，也能补齐已经附着的视频卡片。 */
    private fun registerAttachedHosts() {
        repeat(recyclerView.childCount) { childIndex ->
            adapter.videoHostOf(recyclerView.getChildViewHolder(recyclerView.getChildAt(childIndex)))
                ?.let(::registerHost)
        }
    }

    private fun findAttachedHost(index: Int): VideoHost? {
        val position = adapterPositions[index] ?: return null
        return recyclerView.findViewHolderForAdapterPosition(position)
            ?.let(adapter::videoHostOf)
            ?.takeIf { it.playlistIndex == index }
    }

    /** 由 Activity 的 onDestroy 调用，注销 Adapter 监听并清除所有 View 引用。 */
    fun release() {
        if (released) return
        released = true
        adapter.removeOnViewAttachStateChangeListener(hostAttachListener)
        stateJob?.cancel()
        stateJob = null
        hosts.clear()
        adapterPositions.clear()
        presentedInlineIndex = null
        lastPresentedPlaylistIndex = null
    }
}
