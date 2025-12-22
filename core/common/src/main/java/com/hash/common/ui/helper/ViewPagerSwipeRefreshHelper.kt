package com.hash.common.ui.helper

import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * (中文说明)
 *
 * 问题（Problem）：
 * 在包含 `AppBarLayout + CollapsingToolbarLayout + ViewPager2` 的页面中，
 * 当 `SwipeRefreshLayout` 包裹整个页面时，会与 `ViewPager2` 的横向滑动及子页面的纵向滚动产生滑动冲突，常见症状：
 *  - 快速横向滑动切页时误触发下拉刷新；
 *  - 在 AppBar 完全折叠（即 ViewPager 占满屏幕）时，不希望下拉手势触发刷新，但当 SwipeRefreshLayout 是根视图时会被误触发；
 *  - 子页面（RecyclerView/ScrollView）在非顶部时，下拉应滚动子视图而不是触发刷新。
 *
 * 解决（What this helper solves）：
 *  - 避免在横向滑动（明显的水平手势）时触发下拉刷新；
 *  - 当 AppBar 完全折叠（ViewPager 占满屏幕）时禁止刷新；
 *  - 在子视图未滚动到顶部时禁止刷新（通过回调获取当前页的可滚动子视图并判断是否能向上滚动）。
 *
 * 原理（How it works）：
 * 1) 监听 AppBarLayout 的偏移，判断是否已接近完全折叠（isAppBarFullyCollapsed），如果是则禁止刷新。
 * 2) 通过 `SwipeRefreshLayout.setOnChildScrollUpCallback` 提供自定义逻辑：优先检查 AppBar 折叠状态，再检查当前页面的第一个可滚动子视图是否能向上滚动（若能向上滚，说明内容在中间/底部，应禁止刷新）。
 * 3) 给 `ViewPager2` 的内部承载视图（其 RecyclerView）添加触摸监听：
 *    - 基于 pointerId 跟踪活动手指位置，支持多指场景；
 *    - 计算 dx/dy，若 dx > dy（明显水平滑动）则禁用刷新，避免切页时被下拉打断；
 *    - 垂直滑动时，结合 AppBar 折叠和子视图滚动位置决定是否允许刷新。
 *
 * 使用方式：
 *   val helper = ViewPagerSwipeRefreshHelper(swipeRefresh, viewPager, appBar) { findCurrentScrollable() }
 *   helper.install()
 *   helper.uninstall()
 *
 * 备注：该类为视图层的交互协调工具，保持轻量，不依赖具体页面逻辑；页面需要实现 `findCurrentScrollable()` 回调来定位当前页的可滚动视图。
 */
class ViewPagerSwipeRefreshHelper(
    private val swipeRefreshLayout: SwipeRefreshLayout,
    private val viewPager: ViewPager2,
    private val appBarLayout: AppBarLayout,
    /**
     * 可选的根 View（例如 Fragment 的 root view 或 Activity 的 content view）：
     * - 若传入，helper 会在该根 View 下递归查找第一个可滚动的子视图作为当前页面的滚动目标，适用于没有 FragmentManager 的场景；
     * - 使用弱引用保存，避免持有对 View 的强引用导致内存泄露。
     */
    rootView: View? = null,
    /**
     * 备用的回调（保留向后兼容）：如果未传入 rootView 或者查找失败，将调用此回调来定位当前页的可滚动视图。
     */
    private val findCurrentScrollable: (() -> View?)? = null
) {

    private val rootViewRef: WeakReference<View?>? = rootView?.let { WeakReference(it) }

    // 标记 AppBar 是否已经完全折叠（用于决定是否允许刷新）
    private var isAppBarFullyCollapsed: Boolean = false

    // 当前活动触摸指针 id（支持多指触控），以及按下时的起始坐标
    private var activePointerId: Int = MotionEvent.INVALID_POINTER_ID
    private var startX: Float = 0f
    private var startY: Float = 0f

    // 保存外部设置的触摸监听和 AppBar 偏移监听，便于卸载
    private var pagerTouchListener: View.OnTouchListener? = null
    private var appBarOffsetListener: AppBarLayout.OnOffsetChangedListener? = null

    /**
     * 安装 helper（详细说明）：
     *
     * 主要功能（按步骤）：
     * 1) 在 SwipeRefreshLayout 上注册 setOnChildScrollUpCallback：
     *    - 优先检查 AppBar 折叠状态（isAppBarFullyCollapsed），如果为 true 则直接返回 true（表示子视图已“向上滚动”，SwipeRefresh 不会拦截下拉）；
     *    - 否则通过调用传入的 findCurrentScrollable 回调获取当前页的可滚动子视图（如 RecyclerView、NestedScrollView 等），并调用其 canScrollVertically(-1)：
     *        - 若返回 true（子视图能向上滚），说明内容未滚动到顶部，应禁止触发刷新；
     *        - 若返回 false，说明已在顶部，可以触发刷新。
     *
     * 2) 在 AppBarLayout 上注册 OnOffsetChangedListener：
     *    - 监听偏移以维护 isAppBarFullyCollapsed 标志（带容差判断），用于快速判断“ViewPager 是否占满屏幕”这种场景；
     *    - 如果 AppBar 接近完全折叠，helper 会优先禁止刷新，以避免误触发。
     *
     * 3) 在 ViewPager2 的内部承载控件（通常是其 child RecyclerView，即 viewPager.getChildAt(0)）上注册触摸监听：
     *    - 使用 activePointerId 跟踪当前处理的触摸指针，支持多指场景；
     *    - 在 ACTION_MOVE 中计算水平/垂直位移 dx/dy：若 dx > dy（明显水平滑动），临时禁用 SwipeRefresh，避免横向切页时触发下拉刷新；
     *    - 垂直滑动时，结合 isAppBarFullyCollapsed 和当前子视图能否向上滚动来决定是否启用刷新；
     *    - 在 ACTION_UP/ACTION_CANCEL 时恢复 SwipeRefresh 的默认可用性，并在位移很小的情况下调用 performClick() 以满足无障碍和 lint 要求。
     *
     * 注意与边界情况：
     * - 该方法仅注册监听器，不改变视图层次；请在视图已创建且 viewPager 已 attach 后（例如 Fragment#onViewCreated）调用；
     * - 若在调用时 viewPager.getChildAt(0) 尚未创建（返回 null），触摸监听不会安装；可在稍后重试 install，或在页面初始化逻辑中保证时机；
     * - 回调 findCurrentScrollable 必须准确返回当前页面的可滚动视图，否则会影响刷新判定；此回调应尽量高效（避免在每次触摸中做大量遍历）。
     */
    fun install() {
        //return true（子视图能向上滚），说明内容未滚动到顶部，应禁止触发刷新；
        //return false，说明已在顶部，可以触发刷新。
        swipeRefreshLayout.setOnChildScrollUpCallback { _, _ ->
            // 已经完全折叠时禁止刷新
            if (isAppBarFullyCollapsed) return@setOnChildScrollUpCallback true
            // 否则检查当前页面的可滚动子视图
            val currentScrollable = getCurrentScrollable()
            // 检测子视图是否能滚动
            currentScrollable?.canScrollVertically(-1) ?: false
        }

        // 监听 AppBarLayout 偏移，更新 isAppBarFullyCollapsed 标志
        appBarOffsetListener = AppBarLayout.OnOffsetChangedListener { appBar, verticalOffset ->
            // 计算是否接近完全折叠
            val totalRange = appBar.totalScrollRange.toFloat()
            val absOffset = abs(verticalOffset).toFloat()
            // 带 1px 容差判断
            isAppBarFullyCollapsed = absOffset >= (totalRange - 1f)
        }
        appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)

        // 在 ViewPager2 的承载视图上注册触摸监听，处理横向滑动与刷新冲突
        val pagerTouchView = viewPager.getChildAt(0)
        if (pagerTouchView != null) {
            pagerTouchListener = View.OnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        activePointerId = event.getPointerId(0)
                        val idx = event.findPointerIndex(activePointerId).coerceAtLeast(0)
                        startX = event.getX(idx)
                        startY = event.getY(idx)
                        swipeRefreshLayout.isEnabled = !isAppBarFullyCollapsed
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        val newIndex = event.actionIndex
                        activePointerId = event.getPointerId(newIndex)
                        startX = event.getX(newIndex)
                        startY = event.getY(newIndex)
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val pointerIndex = event.findPointerIndex(activePointerId).coerceAtLeast(0)
                        val curX = event.getX(pointerIndex)
                        val curY = event.getY(pointerIndex)
                        val dx = abs(curX - startX)
                        val dy = abs(curY - startY)
                        if (dx > dy) {
                            swipeRefreshLayout.isEnabled = false
                        } else {
                            if (isAppBarFullyCollapsed) {
                                swipeRefreshLayout.isEnabled = false
                            } else {
                                val currentScrollable = getCurrentScrollable()
                                swipeRefreshLayout.isEnabled = (currentScrollable?.canScrollVertically(-1) == false)
                            }
                        }
                    }
                    MotionEvent.ACTION_POINTER_UP -> {
                        val pointerIndex = event.actionIndex
                        val pointerId = event.getPointerId(pointerIndex)
                        if (pointerId == activePointerId) {
                            val newIndex = if (pointerIndex == 0) 1 else 0
                            if (event.pointerCount > 1) {
                                val safeIndex = newIndex.coerceAtMost(event.pointerCount - 1)
                                activePointerId = event.getPointerId(safeIndex)
                                startX = event.getX(safeIndex)
                                startY = event.getY(safeIndex)
                            } else {
                                activePointerId = MotionEvent.INVALID_POINTER_ID
                            }
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        val pointerIndex = event.findPointerIndex(activePointerId).coerceAtLeast(0)
                        val upX = event.getX(pointerIndex)
                        val upY = event.getY(pointerIndex)
                        val totalDx = abs(upX - startX)
                        val totalDy = abs(upY - startY)
                        val clickThreshold = 10 * v.resources.displayMetrics.density
                        if (totalDx <= clickThreshold && totalDy <= clickThreshold) {
                            v?.performClick()
                        }
                        swipeRefreshLayout.isEnabled = !isAppBarFullyCollapsed
                        activePointerId = MotionEvent.INVALID_POINTER_ID
                    }
                }
                false
            }
            pagerTouchView.setOnTouchListener(pagerTouchListener)
        }
    }

    /**
     * 卸载 helper（详细说明）：
     *
     * 主要工作：
     * - 将 SwipeRefreshLayout 的 setOnChildScrollUpCallback 设为 null，以恢复默认行为（由子视图自身决定是否能向上滚动）；
     * - 从 AppBarLayout 移除此前注册的 OnOffsetChangedListener，避免持有对 view 的引用导致内存泄露或重复监听；
     * - 从 ViewPager 的承载视图上移除注册的 OnTouchListener，恢复原有触摸处理；
     * - 清理 helper 内部保存的监听器引用（pagerTouchListener、appBarOffsetListener）以便垃圾回收或重新安装时不会有残留。
     *
     * 调用时机建议：
     * - 在 Fragment#onDestroyView() 或 Activity#onDestroy() 中调用，以确保在视图销毁时移除所有监听器，避免内存泄露；
     * - 如果希望在页面恢复时重复使用 helper，可在适当时机再次调用 install()（此时建议先调用 uninstall() 做清理）。
     */
    fun uninstall() {
        // remove child scroll callback: set to default behavior (allow child to handle)
        swipeRefreshLayout.setOnChildScrollUpCallback(null)
        // remove appbar listener
        appBarOffsetListener?.let { appBarLayout.removeOnOffsetChangedListener(it) }
        appBarOffsetListener = null
        // remove touch listener
        val pagerTouchView = viewPager.getChildAt(0)
        if (pagerTouchView != null && pagerTouchListener != null) {
            pagerTouchView.setOnTouchListener(null)
        }
        pagerTouchListener = null
    }

    // 找到当前页面的第一个可滚动子视图（优先使用 rootView，失败时使用回调作为后备）
    private fun getCurrentScrollable(): View? {
        // 1) 尝试使用传入的根视图（rootView）进行查找
        rootViewRef?.get()?.let { root ->
            try {
                if (root.isAttachedToWindow) {
                    val found = findScrollableInView(root)
                    if (found != null) return found
                }
            } catch (_: Exception) {
                // 忽略并回退到回调
            }
        }

        // 2) 回退到外部回调（若提供）
        findCurrentScrollable?.let { cb ->
            return try { cb() } catch (_: Exception) { null }
        }

        // 3) 最后回退到 viewPager 的 child view
        val pagerTouchView = viewPager.getChildAt(0) ?: return null
        return findScrollableInView(pagerTouchView)
    }

    // 在 view 层级中递归查找第一个可滚动视图
    private fun findScrollableInView(v: View): View? {
        if (v is ViewGroup) {
            // 常见可滚动容器类型优先检查
            if (v is androidx.recyclerview.widget.RecyclerView || v is androidx.core.widget.NestedScrollView || v is android.widget.ScrollView || v is android.widget.AbsListView) return v
            // 任意 view 如果能向上滚动也可作为滚动目标
            if (v.canScrollVertically(-1)) return v
            for (i in 0 until v.childCount) {
                val child = v.getChildAt(i)
                val found = findScrollableInView(child)
                if (found != null) return found
            }
        } else {
            if (v.canScrollVertically(-1)) return v
        }
        return null
    }
}
