package com.hash.common.ui.loading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Loading 的展示参数。 */
data class LoadingConfig(
    val delayMillis: Long = DEFAULT_DELAY_MILLIS,
    val cancelOnTouchOutside: Boolean = true,
) {
    companion object {
        /** 避免极短请求造成 Loading 闪烁的默认延时。 */
        const val DEFAULT_DELAY_MILLIS = 300L
    }
}

/** 按页面实例维护 Loading 的展示意图和参数，由调用方自行控制开关时机。 */
class PageLoadingState {

    private val mutableConfig = MutableStateFlow<LoadingConfig?>(null)

    /** `null` 表示隐藏，非空值表示使用该配置展示 Loading。 */
    val config: StateFlow<LoadingConfig?> = mutableConfig.asStateFlow()

    fun show(
        delayMillis: Long = LoadingConfig.DEFAULT_DELAY_MILLIS,
        cancelOnTouchOutside: Boolean = true,
    ) {
        mutableConfig.value = LoadingConfig(
            delayMillis = delayMillis.coerceAtLeast(0L),
            cancelOnTouchOutside = cancelOnTouchOutside,
        )
    }

    fun dismiss() {
        mutableConfig.value = null
    }
}
