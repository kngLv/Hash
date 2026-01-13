package com.hash.common.ext

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs
import java.util.Collections
import java.util.WeakHashMap

// 使用 WeakHashMap 来保存为 ViewPager2 添加的 OnItemTouchListener，避免内存泄漏并支持 GC 回收
// 由于 WeakHashMap 的 key 是弱引用，当 ViewPager2 被回收时条目会被自动移除
private val listeners = Collections.synchronizedMap(WeakHashMap<ViewPager2, RecyclerView.OnItemTouchListener>())

/**
 * 启用内层水平 ViewPager2 的嵌套滑动支持，使其能够与父 ViewPager2（或其它会拦截水平滑动的父控件）共存。
 *
 * @param touchSlopPx 判定为水平/垂直滑动的像素阈值，默认使用系统的 scaledTouchSlop（更贴近系统行为），
 *                     如果传入大于 0 的值则使用该值。
 *
 * 用法: 在内层 ViewPager2 创建后调用 `innerViewPager.enableNestedPaging()`（例如在 onViewCreated 或
 * ViewHolder 初始化时）。该扩展会为 ViewPager2 的内部 RecyclerView 添加一个 OnItemTouchListener，
 * 用以在滑动时根据方向与边界决定是否阻止父控件拦截触摸事件。
 */
fun ViewPager2.enableNestedPaging(touchSlopPx: Int = -1) {
    // 防重复：如果已经添加过 listener 则不再重复添加
    if (listeners.containsKey(this)) return

    // ViewPager2 通常将内部的 RecyclerView 作为第一个子视图
    val rv = getChildAt(0) as? RecyclerView ?: return

    // 如果调用时未提供有效的阈值，则使用系统默认的 scaledTouchSlop
    val slop = if (touchSlopPx > 0) touchSlopPx else ViewConfiguration.get(context).scaledTouchSlop

    val listener = object : RecyclerView.OnItemTouchListener {
        var startX = 0f
        var startY = 0f

        override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    // 记录起点坐标
                    startX = e.x
                    startY = e.y
                    // 临时请求父控件不要拦截，后续在 MOVE 阶段根据方向决定是否恢复
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.x - startX
                    val dy = e.y - startY
                    val adx = abs(dx)
                    val ady = abs(dy)

                    // 判断是否为主要的水平滑动
                    if (adx > ady && adx > slop) {
                        val itemCount = adapter?.itemCount ?: 0
                        val atFirst = currentItem == 0
                        val atLast = currentItem == (itemCount - 1)

                        // 如果在第一页且向右滑（dx > 0），或在最后一页且向左滑（dx < 0），
                        // 允许父控件拦截以便外层 pager 接管；否则阻止父拦截，内层处理滑动。
                        if ((atFirst && dx > 0) || (atLast && dx < 0)) {
                            parent?.requestDisallowInterceptTouchEvent(false)
                        } else {
                            parent?.requestDisallowInterceptTouchEvent(true)
                        }
                    } else if (ady > adx && ady > slop) {
                        // 垂直滑动：允许父控件拦截（常见于纵向嵌套场景）
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 结束或取消时恢复父控件拦截权限，避免长时间锁死父控件
                    parent?.requestDisallowInterceptTouchEvent(false)
                }
            }
            // 返回 false，让 RecyclerView 继续其正常的触摸处理流程
            return false
        }

        override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
            // no-op
        }

        override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
            // no-op
        }
    }

    rv.addOnItemTouchListener(listener)
    // 记录 listener，便于将来移除
    listeners[this] = listener
}

/**
 * 移除之前为该 ViewPager2 添加的嵌套滑动支持（如果有）。
 * 调用场景：当你想在某些生命周期阶段手动移除 listener 或回收资源时使用。
 */
fun ViewPager2.disableNestedPaging() {
    val listener = listeners.remove(this) ?: return
    // 尝试从当前的内部 RecyclerView 中移除 listener
    val rv = getChildAt(0) as? RecyclerView
    rv?.removeOnItemTouchListener(listener)
}
