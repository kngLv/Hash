package com.hash.common.base.fragment

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
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

}
