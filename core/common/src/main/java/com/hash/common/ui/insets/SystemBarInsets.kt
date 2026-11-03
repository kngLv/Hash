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
fun View.applyTopBarInsets() = applyTopBarInsets(this)

/**
 * 让顶部固定栏使用 [insetsSource] 提供的 Insets 避开状态栏和显示缺口。
 *
 * 当当前 Toolbar 位于某个会截断 Insets 分发的容器内部时使用。例如可传入页面根容器、父容器
 * 或其他能直接收到窗口 Insets 的 View。Insets 仍应用到当前 View，不会修改 [insetsSource]
 * 的 padding 或高度。
 *
 * 示例：
 * ```kotlin
 * binding.toolbar.applyTopBarInsets(binding.root)
 * ```
 *
 * 注意：[insetsSource] 会注册 Insets 监听器。一个来源 View 若还需处理其他 Insets 逻辑，应将
 * 它们合并在同一个监听器中，避免后注册的监听器覆盖前一个。
 */
fun View.applyTopBarInsets(insetsSource: View) {
    // 记录调用扩展前的尺寸。之后无论 Insets 回调多少次，始终从这份基准重新计算，
    // 而不是在当前 padding / height 上继续累加。
    val initialMetrics = initialMetrics(topBarInsetsTagKey)
    ViewCompat.setOnApplyWindowInsetsListener(insetsSource) { _, windowInsets ->
        applyTopInset(initialMetrics, windowInsets.systemBarAndCutoutInsets().top)
        windowInsets
    }
    requestInsetsWhenAttached(insetsSource)
}

/**
 * 将顶部安全区加到初始布局数据上，确保重复分发 Insets 时不累计 padding 或高度。
 */
private fun View.applyTopInset(initialMetrics: InitialViewMetrics, topInset: Int) {
    // 例如原 Toolbar 高度为 40dp、状态栏为 24dp：
    // - paddingTop = 原 paddingTop + 24dp，子控件会整体落到状态栏下方；
    // - height = 40dp + 24dp，仍保留 40dp 的原始内容区域。
    updatePadding(
        left = initialMetrics.paddingLeft,
        top = initialMetrics.paddingTop + topInset,
        right = initialMetrics.paddingRight,
        bottom = initialMetrics.paddingBottom,
    )
    if (initialMetrics.height > 0) {
        layoutParams = layoutParams.apply {
            height = initialMetrics.height + topInset
        }
    }
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
        val bottomInset = view.rootOrDispatchedInsets(windowInsets).systemBarAndCutoutInsets().bottom
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
        val bottomInset = view.rootOrDispatchedInsets(windowInsets).systemBarAndCutoutInsets().bottom
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
        val insets = view.rootOrDispatchedInsets(windowInsets)
        val imeBottom = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
        val navigationBottom = insets.systemBarAndCutoutInsets().bottom
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
 * 尝试从根窗口读取 Insets，避免中间容器消费后子 View 只能收到剩余的 0 Insets。
 *
 * 读取顺序：Android 原始根窗口 Insets → AndroidX 根 View Insets → 当前回调参数。
 * 前两者不可用时才退回回调参数。用于底栏、滚动内容和输入栏等需要兼容中间容器消费
 * Insets 的场景。
 */
private fun View.rootOrDispatchedInsets(dispatchedInsets: WindowInsetsCompat): WindowInsetsCompat =
    rootWindowInsets?.let { WindowInsetsCompat.toWindowInsetsCompat(it, this) }
        ?: ViewCompat.getRootWindowInsets(this)
        ?: dispatchedInsets

/**
 * 在 View 已附着时立即请求 Insets；尚未附着时等待首次附着后请求一次。
 *
 * 这样扩展既可以在 `onCreate` / `initView` 中调用，也可以在 View 已显示后调用。
 */
private fun View.requestInsetsWhenAttached(insetsTarget: View = this) {
    fun requestInsets() {
        ViewCompat.requestApplyInsets(insetsTarget)
    }
    if (isAttachedToWindow) {
        requestInsets()
    } else {
        addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(view: View) {
                view.removeOnAttachStateChangeListener(this)
                requestInsets()
            }

            override fun onViewDetachedFromWindow(view: View) = Unit
        })
    }
}
