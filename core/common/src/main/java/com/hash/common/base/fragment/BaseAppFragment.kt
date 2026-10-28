package com.hash.common.base.fragment

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gyf.immersionbar.ImmersionBar
import com.hash.common.R
import com.hash.common.base.activity.BaseActivity
import com.hash.common.ui.loading.LoadingStateOwner
import kotlinx.coroutines.launch

/**
 * @name BaseAppFragment
 * @package com.hash.common.base.fragment
 * @author 345 QQ:1831712732
 * @time 2024/12/14 00:21
 * @description
 */
abstract class BaseAppFragment : BaseFragment() {

    private var loadingOverlay: View? = null
    private var showLoadingRunnable: Runnable? = null

    protected open val loadingStateOwner: LoadingStateOwner? = null

    /**
     * 将当前 Fragment 的局部 Loading 状态切换为显示。
     *
     * 覆盖层由状态观察者在当前 Fragment 根布局上统一渲染。
     */
    protected fun showLoading() {
        loadingStateOwner?.loadingState?.show()
    }

    /**
     * 将当前 Fragment 的局部 Loading 状态切换为隐藏。
     *
     * 覆盖层由状态观察者从当前 Fragment 根布局中移除。
     */
    protected fun dismissLoading() {
        loadingStateOwner?.loadingState?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingStateOwner?.let { owner ->
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    owner.loadingState.shouldShowLoading.collect { shouldShow ->
                        if (shouldShow) showLoadingOverlay() else dismissLoadingOverlay()
                    }
                }
            }
        }
    }

    private fun showLoadingOverlay() {
        if (loadingOverlay != null || showLoadingRunnable != null) return
        val root = view as? ViewGroup ?: return
        showLoadingRunnable = Runnable {
            showLoadingRunnable = null
            if (!isAdded || loadingOverlay != null) return@Runnable
            loadingOverlay = FrameLayout(requireContext()).apply {
                background = ColorDrawable(Color.parseColor("#33000000"))
                isClickable = true
                setOnClickListener { loadingStateOwner?.loadingState?.dismiss() }
                addView(ProgressBar(context), FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER,
                ))
            }
            root.addView(loadingOverlay, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ))
        }
        root.postDelayed(showLoadingRunnable, 300)
    }

    private fun dismissLoadingOverlay() {
        val root = view as? ViewGroup
        showLoadingRunnable?.let { root?.removeCallbacks(it) }
        showLoadingRunnable = null
        loadingOverlay?.let { root?.removeView(it) }
        loadingOverlay = null
    }

    override fun onDestroyView() {
        dismissLoadingOverlay()
        super.onDestroyView()
    }


    override fun onResume() {
        initStatusBar()
        super.onResume()
    }


    private fun initStatusBar() {
        if (isStatusBarEnable()) {
            ImmersionBar
                .with(this)
                .statusBarColor(statusBarColor())
                .fitsSystemWindows(fitsSystemWindows())
                .statusBarDarkFont(isStatusDarkFont())
                .init()
            getTitleBar()?.let {
                ImmersionBar.setTitleBar(this, it)
            } ?: kotlin.run {
                view?.findViewById<View>(R.id.toolbar)?.let {
                    ImmersionBar.setTitleBar(this, it)
                }
            }
        }
    }

    /** 是否启用沉浸式状态栏 */
    open fun isStatusBarEnable(): Boolean = true

    /** 状态栏字体深色模式 */
    open fun isStatusDarkFont(): Boolean = true

    open fun getTitleBar(): View? = null

    open fun fitsSystemWindows(): Boolean = false

    /** 状态栏颜色 */
    open fun statusBarColor(): Int = R.color.transparent
}
