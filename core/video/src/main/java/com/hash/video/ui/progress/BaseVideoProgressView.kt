package com.hash.video.ui.progress

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.hash.video.api.VideoPlaybackProgress

/**
 * 视频进度组件的状态基类。
 *
 * 负责合并播放器进度与用户拖动预览，不决定任何控件外观；具体场景只需实现
 * [renderProgress]，即可使用不同的进度条、时间文本或拖动交互。
 */
abstract class BaseVideoProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    /** 拖动期间的临时展示位置。 */
    private var seekPreviewPositionMs: Long? = null

    /** 拖动进度条时，播放器尚未返回真实位置前的占位状态。 */
    private var isAwaitingSeekCompletion = false
    /** 最近一次由播放器发布的真实进度，用于取消预览后立即恢复画面。 */
    private var lastPlaybackProgress = VideoPlaybackProgress()

    /** 用户拖动时的预览通知，可用于展示额外的时间气泡。 */
    var onSeekPreviewChanged: ((Long) -> Unit)? = null

    /** 用户确认拖动后的跳转请求，由外层控制栏或播放器执行。 */
    var onSeekRequested: ((Long) -> Unit)? = null

    /** 输入播放器的真实进度，基类计算当前 UI 应显示的进度。 */
    fun renderProgress(progress: VideoPlaybackProgress) {
        lastPlaybackProgress = progress
        if (!progress.isAvailable) {
            clearPreview()
            renderProgress(ProgressDisplayState())
            return
        }

        val previewPositionMs = seekPreviewPositionMs
        if (isAwaitingSeekCompletion && previewPositionMs != null &&
            progress.isNear(previewPositionMs, SEEK_CONFIRM_TOLERANCE_MS)
        ) {
            seekPreviewPositionMs = null
            isAwaitingSeekCompletion = false
        }

        val displayedPositionMs = seekPreviewPositionMs ?: progress.positionMs
        renderProgress(
            ProgressDisplayState(
                positionMs = displayedPositionMs,
                bufferedPositionMs = progress.bufferedPositionMs,
                durationMs = progress.durationMs,
            ),
        )
    }

    /** 画面横滑等外部手势拖动时，立即渲染目标位置但不触发真实跳转。 */
    fun renderSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
        renderProgress(ProgressDisplayState(positionMs = seekPreviewPositionMs!!, durationMs = durationMs))
    }

    /** 外部手势已结束并将要执行 seek，等待播放器返回真实位置前继续保留预览。 */
    fun endSeekPreview() {
        isAwaitingSeekCompletion = seekPreviewPositionMs != null
    }

    /** 外部手势被取消时丢弃预览并等待下一次真实进度刷新。 */
    fun cancelSeekPreview() {
        clearPreview()
        renderLastPlaybackProgress()
    }

    /** 具体 UI 将用户拖动过程的位置交给基类统一记录。 */
    protected fun dispatchUserSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
        onSeekPreviewChanged?.invoke(seekPreviewPositionMs!!)
    }

    /** 具体 UI 在用户松开进度条后调用，基类保留预览并分发跳转。 */
    protected fun dispatchUserSeek(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
        endSeekPreview()
        onSeekRequested?.invoke(seekPreviewPositionMs!!)
    }

    /** 子类只根据计算完成的展示状态渲染，不应维护额外进度状态。 */
    protected abstract fun renderProgress(state: ProgressDisplayState)

    /**
     * 基础进度状态机计算出的展示数据。
     *
     * [positionMs] 已优先合并用户预览和播放器进度；具体 UI 只需渲染该状态，
     * 不需要自行判断真实播放进度与预览进度的优先级。
     */
    protected data class ProgressDisplayState(
        /** 当前应展示的播放位置，单位为毫秒。 */
        val positionMs: Long = 0L,
        /** 已缓冲到的位置，单位为毫秒。 */
        val bufferedPositionMs: Long = 0L,
        /** 媒体总时长，未知或直播流时为 `0`。 */
        val durationMs: Long = 0L,
    ) {
        /** 是否有可供确定进度条渲染的有效时长。 */
        val isAvailable: Boolean
            get() = durationMs > 0L
    }

    private fun clearPreview() {
        seekPreviewPositionMs = null
        isAwaitingSeekCompletion = false
    }

    /** 取消预览后立即恢复最近一次播放器进度，避免等待下一轮轮询造成画面停留。 */
    private fun renderLastPlaybackProgress() {
        val progress = lastPlaybackProgress
        if (!progress.isAvailable) {
            renderProgress(ProgressDisplayState())
            return
        }
        renderProgress(
            ProgressDisplayState(
                positionMs = progress.positionMs,
                bufferedPositionMs = progress.bufferedPositionMs,
                durationMs = progress.durationMs,
            ),
        )
    }

    private companion object {
        /** seek 完成确认允许的时间误差，覆盖轮询间隔和解码位置的小幅偏移。 */
        const val SEEK_CONFIRM_TOLERANCE_MS = 500L
    }
}
