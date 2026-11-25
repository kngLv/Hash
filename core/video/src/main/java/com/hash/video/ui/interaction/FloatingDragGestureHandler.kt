package com.hash.video.ui.interaction

import android.view.MotionEvent

/** 将悬浮窗触摸事件转换为拖动位移和松手回调，不依赖 WindowManager。 */
internal class FloatingDragGestureHandler(
    private val listener: FloatingDragListener,
) {
    private var lastX = 0f
    private var lastY = 0f

    /** 当前这次触摸是否已经发生拖动，供点击、双击手势排除拖动场景。 */
    var hasDragged = false
        private set

    fun onTouch(event: MotionEvent) {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.rawX
                lastY = event.rawY
                hasDragged = false
            }

            MotionEvent.ACTION_MOVE -> {
                val deltaX = event.rawX - lastX
                val deltaY = event.rawY - lastY
                if (deltaX != 0f || deltaY != 0f) {
                    hasDragged = true
                    listener.onDragBy(deltaX, deltaY)
                }
                lastX = event.rawX
                lastY = event.rawY
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> if (hasDragged) listener.onDragEnd()
        }
    }
}

/** 悬浮窗拖动结果回调；窗口宿主可在松手时增加吸附等策略。 */
internal interface FloatingDragListener {
    fun onDragBy(deltaX: Float, deltaY: Float)
    fun onDragEnd()
}
