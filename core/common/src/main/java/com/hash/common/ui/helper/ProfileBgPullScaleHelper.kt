package com.hash.common.ui.helper

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlin.math.abs

/**
 * ProfileBgPullScaleHelper
 *
 * 将对某个背景（通常是 ImageView）的下拉->放大/上滑->缩小的交互抽离出来，便于在多个 Fragment/Activity 中复用。
 *
 * 行为：
 * - 当用户在指定的 SwipeRefreshLayout 上下拉/上滑时，目标 view 的 scale 将根据从按下点开始到当前手指的垂直位移实时计算并设置。
 * - 支持多指场景（跟踪 activePointerId），并在 pointer 切换时尽量保持平滑过渡。
 * - 松手/取消时，会以动画将 scale 恢复到 1.0。
 *
 * 构造参数：
 * @param swipeRefreshLayout 被监听的 SwipeRefreshLayout（不会修改其刷新逻辑，本 helper 仅绑定触摸以驱动 targetView 的 scale）
 * @param targetView 需要被缩放的 View（一般是背景 ImageView）
 * @param maxPullDistanceDp 对应于达到最大缩放所需的下拉距离（dp 单位），默认 180dp
 * @param maxProfileScale 最大缩放值，默认 2.0（即从 1.0 到 2.0）
 */
@SuppressLint("ClickableViewAccessibility")
class ProfileBgPullScaleHelper(
    private val swipeRefreshLayout: SwipeRefreshLayout,
    private val targetView: View,
    maxPullDistanceDp: Int = 180,
    private val maxProfileScale: Float = 2f
) {

    private val density = targetView.resources.displayMetrics.density
    private val maxPullDistancePx: Float = maxPullDistanceDp * density

    // 活动指针与起始 Y
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var pullStartY: Float = 0f

    // 内部记录的 touch listener 以便卸载
    private var touchListener: View.OnTouchListener? = null

    /**
     * 安装 touch listener，使 targetView 根据手势实时缩放
     */
    fun install() {
        uninstall()

        touchListener = View.OnTouchListener { v, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val idx = event.actionIndex
                    activePointerId = event.getPointerId(idx)
                    pullStartY = event.getY(idx)
                    targetView.animate().cancel()
                }
                MotionEvent.ACTION_MOVE -> {
                    // 找到当前活动指针的位置；若找不到，回退到 pointer 0 并重置基准
                    var pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex == -1) {
                        pointerIndex = 0
                        activePointerId = event.getPointerId(0)
                        pullStartY = event.getY(0)
                    }
                    val currentY = event.getY(pointerIndex)
                    val dyFromStart = currentY - pullStartY
                    val effectiveMaxPull = if (maxPullDistancePx <= 0f) 1f else maxPullDistancePx
                    val scalePerPx = (maxProfileScale - 1f) / effectiveMaxPull
                    val targetScale = (1f + dyFromStart * scalePerPx).coerceIn(1f, maxProfileScale)
                    targetView.scaleX = targetScale
                    targetView.scaleY = targetScale
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val idx = event.actionIndex
                    activePointerId = event.getPointerId(idx)
                    pullStartY = event.getY(idx)
                }
                MotionEvent.ACTION_POINTER_UP -> {
                    val pointerIndex = event.actionIndex
                    val pointerId = event.getPointerId(pointerIndex)
                    if (pointerId == activePointerId) {
                        val newIndex = if (pointerIndex == 0) 1 else 0
                        if (event.pointerCount > newIndex) {
                            activePointerId = event.getPointerId(newIndex)
                            pullStartY = event.getY(newIndex)
                        } else {
                            activePointerId = MotionEvent.INVALID_POINTER_ID
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    targetView.animate().cancel()
                    targetView.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(300L)
                        .start()

                    // 如果是轻点则调用 performClick 以满足无障碍
                    val moved = abs(event.y - pullStartY)
                    val slop = ViewConfiguration.get(targetView.context).scaledTouchSlop.toFloat()
                    if (moved <= slop) {
                        v.performClick()
                    }

                    activePointerId = MotionEvent.INVALID_POINTER_ID
                }
            }
            // 返回 false，允许 SwipeRefreshLayout 自己处理拖拽/刷新逻辑
            false
        }

        // 将 Listener 安装到 SwipeRefreshLayout（监听上层触摸以获得手指位置）
        swipeRefreshLayout.setOnTouchListener(touchListener)
    }

    /** 卸载 listener 并恢复 targetView 的状态（不会修改 targetView 的 scale） */
    fun uninstall() {
        touchListener?.let { swipeRefreshLayout.setOnTouchListener(null) }
        touchListener = null
        activePointerId = MotionEvent.INVALID_POINTER_ID
    }
}
