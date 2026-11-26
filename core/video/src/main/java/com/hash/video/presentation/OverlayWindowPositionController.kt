package com.hash.video.presentation

import android.animation.ValueAnimator
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import java.lang.ref.WeakReference
import kotlin.math.roundToInt

/** 管理系统悬浮窗的位置、屏幕边界和松手后的边缘吸附。 */
internal class OverlayWindowPositionController(
    windowManager: WindowManager,
    view: View,
    private val params: WindowManager.LayoutParams,
    private val onWindowError: (Throwable) -> Unit,
) {
    private var windowManager: WindowManager? = windowManager
    private var viewRef: WeakReference<View>? = WeakReference(view)
    private var x = params.x.toFloat()
    private var y = params.y.toFloat()
    private var snapAnimator: ValueAnimator? = null

    fun moveBy(deltaX: Float, deltaY: Float) {
        snapAnimator?.cancel()
        snapAnimator = null
        val maxX = maxX() ?: return
        val maxY = maxY() ?: return
        x = (x - deltaX).coerceIn(0f, maxX)
        y = (y + deltaY).coerceIn(0f, maxY)
        updateLayout()
    }

    /** 松手后吸附到距离更近的左侧或右侧边缘。 */
    fun onDragEnd() {
        val maxX = maxX() ?: return
        if (maxX <= 0f) return
        val targetX = if (x <= maxX / 2f) 0f else maxX
        if (x == targetX) return
        snapAnimator?.cancel()
        snapAnimator = ValueAnimator.ofFloat(x, targetX).apply {
            duration = SNAP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                x = animation.animatedValue as Float
                updateLayout()
            }
            start()
        }
    }

    /** 悬浮窗移除后主动清空窗口和 View 引用。 */
    fun release() {
        snapAnimator?.cancel()
        snapAnimator = null
        windowManager = null
        viewRef = null
    }

    private fun maxX(): Float? {
        val view = viewRef?.get() ?: return null
        return (view.resources.displayMetrics.widthPixels - params.width).coerceAtLeast(0).toFloat()
    }

    private fun maxY(): Float? {
        val view = viewRef?.get() ?: return null
        return (view.resources.displayMetrics.heightPixels - params.height).coerceAtLeast(0).toFloat()
    }

    private fun updateLayout() {
        val view = viewRef?.get() ?: return
        val manager = windowManager ?: return
        params.x = x.roundToInt()
        params.y = y.roundToInt()
        runCatching { manager.updateViewLayout(view, params) }
            .onFailure { error ->
                release()
                onWindowError(error)
            }
    }

    private companion object {
        const val SNAP_DURATION_MS = 180L
    }
}
