package com.hash.common.base.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.hash.common.R
import com.hash.common.base.activity.BaseActivity
import com.hash.common.base.activity.BaseAppActivity
import com.hash.common.ui.loading.LoadingConfig
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

    protected open val loadingStateOwner: LoadingStateOwner? = null

    /** Dialog 由宿主 Activity 统一持有和渲染。*/
    protected fun showLoading(
        delayMillis: Long = LoadingConfig.DEFAULT_DELAY_MILLIS,
        cancelOnTouchOutside: Boolean = true,
    ) {
        (activity as? BaseAppActivity)?.showLoading(delayMillis, cancelOnTouchOutside)
    }

    /** Dialog 由宿主 Activity 统一关闭。*/
    protected fun dismissLoading() {
        (activity as? BaseAppActivity)?.dismissLoading()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? BaseAppActivity)?.let(::observeLoadingState)
    }

    private fun observeLoadingState(hostActivity: BaseAppActivity) {
        val owner = loadingStateOwner ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            owner.loadingState.config.collect { config ->
                if (config == null) {
                    hostActivity.dismissLoading()
                } else {
                    hostActivity.showLoading(
                        delayMillis = config.delayMillis,
                        cancelOnTouchOutside = config.cancelOnTouchOutside,
                    )
                }
            }
        }
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
