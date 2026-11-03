package com.hash.common.ui.insets

import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * 系统栏安全区适配工具。
 *
 * `enableEdgeToEdge()` 只负责让页面背景延伸至状态栏和导航栏下方；本文件的扩展用于
 * 让标题、底栏、滚动内容和输入栏等可交互内容避开系统安全区。
 *
 * 使用原则：
 * - 顶部固定栏调用 [applyTopBarInsets]；
 * - 底部固定导航或操作栏调用 [applyBottomBarInsets]；
 * - 列表、滚动容器调用 [applyScrollContentInsets]；
 * - 会被软键盘遮挡的固定输入栏调用 [applyImeInsets]。
 *
 * 每个扩展会保存调用时的初始 padding 和固定高度，后续系统栏尺寸变化时均从初始值重新
 * 计算，不会重复累加。一个 View 只能调用一种扩展，因为每种扩展都会设置该 View 的
 * Insets 监听器。
 */
internal object SystemBarInsetsPolicy {

    /** 计算输入栏底部应避让的距离，键盘与导航栏同时存在时取较大值。 */
    fun resolveImeBottom(imeBottom: Int, navigationBottom: Int): Int =
        maxOf(imeBottom, navigationBottom)
}

/** View 在首次应用 Insets 前的布局数据，用于避免多次分发时重复叠加。 */
private data class InitialViewMetrics(
    val paddingLeft: Int,
    val paddingTop: Int,
    val paddingRight: Int,
    val paddingBottom: Int,
    val height: Int,
)

/**
 * Insets 内部 keyed tag。
 *
 * `View.setTag(key, value)` 要求 key 的高位为应用资源包范围，`0x99999` 这类普通整数会
 * 被系统拒绝。这里预留 `0x7f0f10xx` 范围作为 Insets 内部键，不占用布局中的 `android:id`。
 */
private const val topBarInsetsTagKey = 0x7f0f1001
private const val bottomBarInsetsTagKey = 0x7f0f1002
private const val scrollContentInsetsTagKey = 0x7f0f1003
private const val imeInsetsTagKey = 0x7f0f1004

/**
 * 让贴顶固定栏避开状态栏和显示缺口，同时保留原有内容高度。
 *
 * 适用场景：页面顶部的 Toolbar、返回栏、一级 Tab 等。不要用于嵌套在其他顶部栏下方的
 * 二级 Tab，否则会重复让出状态栏区域。
 *
 * 示例：
 * ```kotlin
 * binding.toolbar.applyTopBarInsets()
 * ```
 *
 * 固定高度 View 会在原高度基础上增加顶部安全区，因此原本的 56dp Toolbar 内容区不会被
 * 状态栏压缩；`match_parent` 或 `wrap_content` View 仅增加顶部 padding。
 */
fun View.applyTopBarInsets() {
    val initialMetrics = initialMetrics(topBarInsetsTagKey)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val topInset = windowInsets.systemBarAndCutoutInsets().top
        view.updatePadding(
            left = initialMetrics.paddingLeft,
            top = initialMetrics.paddingTop + topInset,
            right = initialMetrics.paddingRight,
            bottom = initialMetrics.paddingBottom,
        )
        if (initialMetrics.height > 0) {
            view.layoutParams = view.layoutParams.apply {
                height = initialMetrics.height + topInset
            }
        }
        windowInsets
    }
    requestInsetsWhenAttached()
}

/**
 * 让固定底栏的内容避开导航栏，底栏背景仍可延伸至屏幕底部。
 *
 * 适用场景：首页 Tab、底部操作栏、固定按钮容器。应对底栏的外层背景容器调用，而不是
 * 直接对内部图标或按钮调用。
 *
 * 示例：
 * ```kotlin
 * binding.bottomNavigationContainer.applyBottomBarInsets()
 * ```
 *
 * 手势导航时会避开底部手势区域；三键导航时会避开三键区域。
 */
fun View.applyBottomBarInsets() {
    val initialMetrics = initialMetrics(bottomBarInsetsTagKey)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bottomInset = windowInsets.systemBarAndCutoutInsets().bottom
        view.updatePadding(
            left = initialMetrics.paddingLeft,
            top = initialMetrics.paddingTop,
            right = initialMetrics.paddingRight,
            bottom = initialMetrics.paddingBottom + bottomInset,
        )
        windowInsets
    }
    requestInsetsWhenAttached()
}

/**
 * 让滚动内容可滚动至导航栏安全区上方。
 *
 * 适用场景：RecyclerView、NestedScrollView、ScrollView 等内容会延伸到页面底部的滚动
 * 容器。扩展会关闭 [ViewGroup.clipToPadding]，这样最后一项可滚到导航栏上方而不会突然
 * 被裁切。
 *
 * 示例：
 * ```kotlin
 * binding.contentRecycler.applyScrollContentInsets()
 * ```
 */
fun View.applyScrollContentInsets() {
    val initialMetrics = initialMetrics(scrollContentInsetsTagKey)
    if (this is ViewGroup) {
        clipToPadding = false
    }
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val bottomInset = windowInsets.systemBarAndCutoutInsets().bottom
        view.updatePadding(
            left = initialMetrics.paddingLeft,
            top = initialMetrics.paddingTop,
            right = initialMetrics.paddingRight,
            bottom = initialMetrics.paddingBottom + bottomInset,
        )
        windowInsets
    }
    requestInsetsWhenAttached()
}

/**
 * 让固定输入栏避开软键盘或导航栏，以两者中较大的底部安全区为准。
 *
 * 适用场景：聊天输入栏、评论输入栏、底部提交栏。不要与 [applyBottomBarInsets] 同时用于
 * 同一个 View。
 *
 * 示例：
 * ```kotlin
 * binding.commentInputBar.applyImeInsets()
 * ```
 */
fun View.applyImeInsets() {
    val initialMetrics = initialMetrics(imeInsetsTagKey)
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val imeBottom = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val navigationBottom = windowInsets.systemBarAndCutoutInsets().bottom
        view.updatePadding(
            left = initialMetrics.paddingLeft,
            top = initialMetrics.paddingTop,
            right = initialMetrics.paddingRight,
            bottom = initialMetrics.paddingBottom + SystemBarInsetsPolicy.resolveImeBottom(
                imeBottom = imeBottom,
                navigationBottom = navigationBottom,
            ),
        )
        windowInsets
    }
    requestInsetsWhenAttached()
}

/**
 * 获取或保存 View 在指定 Insets 语义下的初始布局数据，确保重复分发时不会累计 padding。
 */
private fun View.initialMetrics(tagKey: Int): InitialViewMetrics {
    return (getTag(tagKey) as? InitialViewMetrics) ?: InitialViewMetrics(
        paddingLeft = paddingLeft,
        paddingTop = paddingTop,
        paddingRight = paddingRight,
        paddingBottom = paddingBottom,
        height = layoutParams?.height ?: ViewGroup.LayoutParams.WRAP_CONTENT,
    ).also { setTag(tagKey, it) }
}

/** 合并状态栏、导航栏和显示缺口的当前可见安全区。 */
private fun WindowInsetsCompat.systemBarAndCutoutInsets(): Insets = getInsets(
    WindowInsetsCompat.Type.statusBars() or
        WindowInsetsCompat.Type.navigationBars() or
        WindowInsetsCompat.Type.displayCutout(),
)

/**
 * 在 View 已附着时立即请求 Insets；尚未附着时等待首次附着后请求一次。
 *
 * 这样扩展既可以在 `onCreate` / `initView` 中调用，也可以在 View 已显示后调用。
 */
private fun View.requestInsetsWhenAttached() {
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                ViewCompat.requestApplyInsets(view)
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}
