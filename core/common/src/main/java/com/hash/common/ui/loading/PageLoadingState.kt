package com.hash.common.ui.loading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 按页面实例维护 Loading 的展示意图，由调用方自行控制开关时机。 */
class PageLoadingState {

    private val mutableShouldShowLoading = MutableStateFlow(false)

    val shouldShowLoading: StateFlow<Boolean> = mutableShouldShowLoading.asStateFlow()

    fun show() {
        mutableShouldShowLoading.value = true
    }

    fun dismiss() {
        mutableShouldShowLoading.value = false
    }
}
