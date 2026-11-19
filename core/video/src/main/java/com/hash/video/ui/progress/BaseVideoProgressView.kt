package com.hash.video.ui.progress

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import com.hash.video.api.VideoPlaybackProgress

/** 进度 UI 基类：统一处理真实进度和用户拖动预览。 */
abstract class BaseVideoProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    /** 拖动期间的临时展示位置。 */
    private var seekPreviewPositionMs: Long? = null

    /** 跳转后等待播放器返回目标位置。 */
    private var isAwaitingSeekCompletion = false
    /** 最近一次真实进度。 */
    private var lastPlaybackProgress = VideoPlaybackProgress()

    /** 用户确认跳转的位置。 */
    private var seekListener: SeekListener? = null

    fun setSeekListener(listener: SeekListener?) {
        seekListener = listener
    }

    /** 更新真实播放进度。 */
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

    /** 显示横滑等手势的目标位置，不立即跳转。 */
    fun renderSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        isAwaitingSeekCompletion = false
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
        renderProgress(ProgressDisplayState(positionMs = seekPreviewPositionMs!!, durationMs = durationMs))
    }

    /** 手势结束后保留预览，直到播放器跳到目标位置。 */
    fun endSeekPreview() {
        isAwaitingSeekCompletion = seekPreviewPositionMs != null
    }

    /** 取消预览并恢复真实进度。 */
    fun cancelSeekPreview() {
        clearPreview()
        renderLastPlaybackProgress()
    }

    /** 记录用户正在拖动的位置。 */
    protected fun dispatchUserSeekPreview(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        isAwaitingSeekCompletion = false
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
    }

    /** 用户松开进度条后发送跳转请求。 */
    protected fun dispatchUserSeek(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        seekPreviewPositionMs = positionMs.coerceIn(0L, durationMs)
        endSeekPreview()
        seekListener?.onSeek(seekPreviewPositionMs!!)
    }

    /** 子类只负责显示计算后的进度。 */
    protected abstract fun renderProgress(state: ProgressDisplayState)

    /** 已合并真实进度和预览进度的显示数据。 */
    protected data class ProgressDisplayState(
        /** 当前应展示的播放位置，单位为毫秒。 */
        val positionMs: Long = 0L,
        /** 已缓冲到的位置，单位为毫秒。 */
        val bufferedPositionMs: Long = 0L,
        /** 媒体总时长，未知或直播流时为 `0`。 */
        val durationMs: Long = 0L,
    ) {
        /** 是否可以显示可跳转的进度。 */
        val isAvailable: Boolean
            get() = durationMs > 0L
    }

    private fun clearPreview() {
        seekPreviewPositionMs = null
        isAwaitingSeekCompletion = false
    }

    /** 取消预览后立即恢复真实进度。 */
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
        /** 认为跳转完成时允许的位置误差。 */
        const val SEEK_CONFIRM_TOLERANCE_MS = 500L
    }

    fun interface SeekListener {
        fun onSeek(positionMs: Long)
    }
}
