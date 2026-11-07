package com.hash.video.ui.gesture

import kotlin.math.abs

/**
 * 不依赖 Android View 的视频画面手势计算器。
 *
 * 只完成触点、方向和时间的判断；View 层负责将 MotionEvent 转成这里的输入，并执行最终命令。
 */
internal class VideoGestureDetector(
    private val touchSlopPx: Float,
    private val doubleTapSlopPx: Float = touchSlopPx * DEFAULT_DOUBLE_TAP_SLOP_MULTIPLIER,
    private val doubleTapTimeoutMs: Long = DOUBLE_TAP_TIMEOUT_MS,
) {
    private var downX = 0f
    private var downY = 0f
    private var viewWidth = 0
    private var viewHeight = 0
    private var initialPositionMs = 0L
    private var durationMs = 0L
    private var direction = Direction.NONE
    private var cancelled = false
    private var lastTapTimeMs: Long? = null
    private var lastTapX = 0f
    private var lastTapY = 0f
    private var latestX = 0f
    private var latestY = 0f

    /** 记录本次触摸起点和播放信息，为后续手势计算初始化状态。 */
    fun onDown(
        x: Float,
        y: Float,
        viewWidth: Int,
        viewHeight: Int,
        currentPositionMs: Long,
        durationMs: Long,
    ) {
        downX = x
        downY = y
        latestX = x
        latestY = y
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight
        initialPositionMs = currentPositionMs
        this.durationMs = durationMs
        direction = Direction.NONE
        cancelled = false
    }

    /** 根据移动方向生成跳转、亮度或音量的过程事件。 */
    fun onMove(
        x: Float,
        y: Float,
        pointerCount: Int,
        config: VideoGestureConfig
    ): VideoGestureEvent? {
        if (pointerCount != 1) {
            cancelled = true
            return null
        }
        if (cancelled) return null

        latestX = x
        latestY = y
        lockDirectionIfNeeded(x, y)
        return when (direction) {
            Direction.HORIZONTAL -> if (config.seekEnabled && durationMs > 0L) {
                VideoGestureEvent.SeekPreview(targetPositionMs(x), durationMs)
            } else null

            Direction.VERTICAL -> verticalGesture(x, y, config, VideoGesturePhase.UPDATE)
            Direction.NONE -> null
        }
    }

    /** 结束当前手势：提交调节结果，或识别单击、双击。 */
    fun onUp(
        x: Float,
        y: Float,
        eventTimeMs: Long,
        config: VideoGestureConfig
    ): VideoGestureEvent? {
        if (cancelled) return null

        lockDirectionIfNeeded(x, y)
        if (direction == Direction.HORIZONTAL) {
            return if (config.seekEnabled && durationMs > 0L) {
                VideoGestureEvent.SeekCommitted(targetPositionMs(x))
            } else {
                null
            }
        }
        if (direction == Direction.VERTICAL) {
            return verticalGesture(x, y, config, VideoGesturePhase.END)
        }

        val previousTapTimeMs = lastTapTimeMs
        val isWithinDoubleTapArea = previousTapTimeMs != null &&
                (x - lastTapX) * (x - lastTapX) + (y - lastTapY) * (y - lastTapY) <=
                doubleTapSlopPx * doubleTapSlopPx
        lastTapTimeMs = eventTimeMs
        lastTapX = x
        lastTapY = y
        if (
            config.doubleTapEnabled &&
            previousTapTimeMs != null &&
            eventTimeMs - previousTapTimeMs <= doubleTapTimeoutMs &&
            isWithinDoubleTapArea
        ) {
            lastTapTimeMs = null
            return VideoGestureEvent.TogglePlayback
        }
        return VideoGestureEvent.ToggleControls.takeIf { config.singleTapEnabled }
    }

    /** 取消当前手势，并返回界面需要收尾的取消事件。 */
    fun cancel(config: VideoGestureConfig): VideoGestureEvent? {
        val cancelEvent = when (direction) {
            Direction.HORIZONTAL -> VideoGestureEvent.SeekCancelled.takeIf { config.seekEnabled }
            Direction.VERTICAL -> verticalGesture(
                latestX,
                latestY,
                config,
                VideoGesturePhase.CANCEL
            )

            Direction.NONE -> null
        }
        cancelled = true
        direction = Direction.NONE
        lastTapTimeMs = null
        return cancelEvent
    }

    /** 首次超过触摸阈值时锁定横向或纵向手势方向。 */
    private fun lockDirectionIfNeeded(x: Float, y: Float) {
        if (direction != Direction.NONE) return
        val deltaX = x - downX
        val deltaY = y - downY
        if (maxOf(abs(deltaX), abs(deltaY)) < touchSlopPx) return
        direction = if (abs(deltaX) >= abs(deltaY)) Direction.HORIZONTAL else Direction.VERTICAL
    }

    /** 将横向滑动距离换算为限定范围内的目标播放进度。 */
    private fun targetPositionMs(x: Float): Long {
        if (viewWidth <= 0 || durationMs <= 0L) return initialPositionMs
        val deltaMs = ((x - downX) / viewWidth * durationMs).toLong()
        return (initialPositionMs + deltaMs).coerceIn(0L, durationMs)
    }

    /** 根据触点所在半屏生成亮度或音量调节事件。 */
    private fun verticalGesture(
        x: Float,
        y: Float,
        config: VideoGestureConfig,
        phase: VideoGesturePhase,
    ): VideoGestureEvent? {
        if (viewHeight <= 0) return null
        val delta = ((downY - y) / viewHeight).coerceIn(-1f, 1f)
        return if (downX < viewWidth / 2f) {
            VideoGestureEvent.BrightnessAdjustment(delta, phase).takeIf { config.brightnessEnabled }
        } else {
            VideoGestureEvent.VolumeAdjustment(delta, phase).takeIf { config.volumeEnabled }
        }
    }

    private enum class Direction { NONE, HORIZONTAL, VERTICAL }

    companion object {
        /** 第一次点击在此时间内等到第二击时，两个点击会被识别为一次双击。 */
        const val DOUBLE_TAP_TIMEOUT_MS = 300L
        private const val DEFAULT_DOUBLE_TAP_SLOP_MULTIPLIER = 4f
    }
}
