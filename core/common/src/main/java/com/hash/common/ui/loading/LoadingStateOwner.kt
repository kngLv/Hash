package com.hash.common.ui.loading

/** 向页面基类提供 Loading 状态的 ViewModel 契约。 */
interface LoadingStateOwner {
    val loadingState: PageLoadingState
}
