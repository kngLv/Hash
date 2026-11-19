package com.hash.video.ui.gesture

import com.hash.video.api.VideoPlaybackProgress
/** 把触摸序列识别为单击、双击、横滑或纵滑事件。 */
internal class VideoTouchHandler(
    touchSlopPx: Float,
    doubleTapSlopPx: Float = touchSlopPx * 4f,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    private val onGesture: (VideoGestureEvent) -> Unit,
) {
    private val gestureDetector = VideoGestureDetector(touchSlopPx, doubleTapSlopPx)
    private val singleTapRunnable = Runnable {
        onGesture(VideoGestureEvent.Click)
    }

    fun handleTouchEvent(
        event: TouchEvent,
        config: VideoGestureConfig,
        progress: VideoPlaybackProgress,
    ): VideoGestureDirection {
        when (event.action) {
            Action.DOWN -> gestureDetector.onDown(
                x = event.x,
                y = event.y,
                viewWidth = event.viewWidth,
                viewHeight = event.viewHeight,
                currentPositionMs = progress.positionMs,
                durationMs = progress.durationMs,
            )

            Action.MOVE -> dispatchGesture(
                gestureDetector.onMove(event.x, event.y, event.pointerCount, config),
                config,
            )

            Action.UP -> dispatchGesture(
                gestureDetector.onUp(event.x, event.y, event.eventTimeMs, config),
                config,
            )

            Action.CANCEL, Action.POINTER_DOWN -> cancel(config)
        }
        return gestureDetector.currentDirection()
    }

    /** 取消当前手势和未确认的单击。 */
    fun cancel(config: VideoGestureConfig) {
        cancelPendingSingleTap()
        dispatchGesture(gestureDetector.cancel(config), config)
    }

    /** 处理延迟单击，再分发已确认的手势。 */
    private fun dispatchGesture(event: VideoGestureEvent?, config: VideoGestureConfig) {
        when (event) {
            null -> Unit
            VideoGestureEvent.Click -> dispatchSingleTap(config)

            VideoGestureEvent.DoubleClick -> {
                // 双击成立，不再触发之前等待中的单击。
                cancelPendingSingleTap()
                onGesture(event)
            }

            else -> onGesture(event)
        }
    }

    /** 开启双击时先等待，否则立即发送单击。 */
    private fun dispatchSingleTap(config: VideoGestureConfig) {
        // 未开启双击，单击可以立即生效。
        if (!config.doubleTapEnabled) {
            onGesture(VideoGestureEvent.Click)
            return
        }
        // 等待双击窗口结束后，再确认这是单击。
        cancelPendingSingleTap()
        postDelayed(singleTapRunnable, VideoGestureDetector.DOUBLE_TAP_TIMEOUT_MS)
    }

    /** 取消等待中的单击。 */
    private fun cancelPendingSingleTap() {
        removeCallbacks(singleTapRunnable)
    }

    /** 与 Android View 解耦的触摸数据。 */
    data class TouchEvent(
        val action: Action,
        val x: Float,
        val y: Float,
        val pointerCount: Int,
        val eventTimeMs: Long,
        val viewWidth: Int,
        val viewHeight: Int,
    )

    enum class Action {
        DOWN,
        MOVE,
        UP,
        CANCEL,
        POINTER_DOWN,
    }
}
