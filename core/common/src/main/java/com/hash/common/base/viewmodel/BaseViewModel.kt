package com.hash.common.base.viewmodel

import androidx.lifecycle.ViewModel
import com.hash.common.ui.loading.LoadingConfig
import com.hash.common.ui.loading.LoadingStateOwner
import com.hash.common.ui.loading.PageLoadingState

/** 提供页面级 Loading 状态的 ViewModel 基类。 */
abstract class BaseViewModel : ViewModel(), LoadingStateOwner {
    override val loadingState = PageLoadingState()

    /** 将当前 ViewModel 的页面 Loading 状态切换为显示。 */
    protected fun showLoading(
        delayMillis: Long = LoadingConfig.DEFAULT_DELAY_MILLIS,
        cancelOnTouchOutside: Boolean = true,
    ) = loadingState.show(delayMillis, cancelOnTouchOutside)

    /** 将当前 ViewModel 的页面 Loading 状态切换为隐藏。 */
    protected fun dismissLoading() = loadingState.dismiss()
}
