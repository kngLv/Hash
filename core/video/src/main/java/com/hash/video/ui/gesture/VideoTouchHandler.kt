package com.hash.video.ui.gesture

import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.ui.player.BaseHashVideoPlayerView

/**
 * 统一处理视频画面的触摸序列，并向播放器 View 输出语义化的手势事件。
 *
 * 该类负责单双击判定、单击延迟、拖动过程以及手势取消，不执行播放、跳转或系统设置；这些行为
 * 仍由 [BaseHashVideoPlayerView] 根据 [VideoGestureEvent] 统一处理。
 *
 * @param touchSlopPx 识别滑动所需的最小距离。
 * @param doubleTapSlopPx 两次点击可识别为双击的最大间距。
 * @param postDelayed 延迟执行待确认的单击。
 * @param removeCallbacks 取消尚未执行的单击。
 * @param onGesture 输出识别完成的手势事件。
 */
internal class VideoTouchHandler(
    touchSlopPx: Float,
    doubleTapSlopPx: Float = touchSlopPx * 4f,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeCallbacks: (Runnable) -> Unit,
    private val onGesture: (VideoGestureEvent) -> Unit,
) {
    private val gestureDetector = VideoGestureDetector(touchSlopPx, doubleTapSlopPx)
    private val pendingSingleTap = Runnable {
        onGesture(VideoGestureEvent.Click)
    }

    fun handle(
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

            Action.MOVE -> dispatch(
                gestureDetector.onMove(event.x, event.y, event.pointerCount, config),
                config,
            )

            Action.UP -> dispatch(
                gestureDetector.onUp(event.x, event.y, event.eventTimeMs, config),
                config,
            )

            Action.CANCEL, Action.POINTER_DOWN -> cancel(config)
        }
        return gestureDetector.currentDirection()
    }

    /** 取消当前手势及尚未确认的单击，供多指触摸和 View 生命周期结束时调用。 */
    fun cancel(config: VideoGestureConfig) {
        cancelPendingSingleTap()
        dispatch(gestureDetector.cancel(config), config)
    }

    private fun dispatch(event: VideoGestureEvent?, config: VideoGestureConfig) {
        when (event) {
            null -> Unit
            VideoGestureEvent.Click -> dispatchSingleTap(config)

            VideoGestureEvent.DoubleClick -> {
                //取消之前的单击，直接回调双击事件
                cancelPendingSingleTap()
                onGesture(event)
            }

            else -> onGesture(event)
        }
    }

    /** 单击启用双击时延迟确认；否则立即通知界面切换控制栏。 */
    private fun dispatchSingleTap(config: VideoGestureConfig) {
        //没有启用双击，直接回调单击事件
        if (!config.doubleTapEnabled) {
            onGesture(VideoGestureEvent.Click)
            return
        }
        //取消尚未确认的单击，延时执行单击，后续收到双击时会取消该单击
        cancelPendingSingleTap()
        postDelayed(pendingSingleTap, VideoGestureDetector.DOUBLE_TAP_TIMEOUT_MS)
    }

    /** 取消尚未确认的单击，避免与双击或手势取消同时生效。 */
    private fun cancelPendingSingleTap() {
        removeCallbacks(pendingSingleTap)
    }

    /** 与 Android MotionEvent 解耦的触摸快照，便于独立验证手势时序。 */
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
