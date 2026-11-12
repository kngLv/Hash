package com.hash.video.ui.render

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.hash.video.R
import com.hash.video.player.VideoController
import java.lang.ref.WeakReference

/**
 * 持有唯一的视频渲染视图，并在调用方提供的容器之间迁移它。
 *
 * 该类不改变媒体、进度和播放状态；[VideoController] 仍是唯一的播放控制入口。
 */
@OptIn(UnstableApi::class)
internal class VideoRenderCoordinator(
    context: Context,
    controller: VideoController? = null,
) {

    /** 唯一的 Media3 画面渲染 View，会在不同业务容器之间迁移。 */
    private val renderView = LayoutInflater.from(context).inflate(
        R.layout.view_video_render,
        null,
        false,
    ) as PlayerView
    /** 当前承载 [renderView] 的业务容器；弱引用避免页面退出后被渲染器持有。 */
    private var renderContainer: WeakReference<ViewGroup>? = null

    init {
        controller?.withPlayer { player -> renderView.player = player }
    }

    /** 将唯一渲染视图迁移至目标容器；迁移不会修改媒体或播放状态。 */
    fun attachTo(container: ViewGroup) {
        if (currentContainer() === container) return
        (renderView.parent as? ViewGroup)?.removeView(renderView)
        container.addView(
            renderView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        renderContainer = WeakReference(container)
    }

    /** 仅当指定容器仍承载画面时解除挂载，避免旧页面误清理新页面的画面。 */
    fun detachFrom(container: ViewGroup): Boolean {
        if (currentContainer() !== container) return false
        clear()
        return true
    }

    /** 清空当前画面容器，但不停止或释放播放器。 */
    fun clear() {
        renderContainer = null
        (renderView.parent as? ViewGroup)?.removeView(renderView)
    }

    /** 当前实际承载唯一渲染视图的容器；容器被外部移除后自动视为未挂载。 */
    fun currentContainer(): ViewGroup? {
        val container = renderContainer?.get() ?: return null
        return container.takeIf { renderView.parent === it }
    }
}
