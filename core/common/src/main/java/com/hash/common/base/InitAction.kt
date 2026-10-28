package com.hash.common.base

import androidx.annotation.LayoutRes

/**
 * @name BaseUnifiedFunction
 * @package com.hash.common.base
 * @author 345 QQ:1831712732
 * @time 2024/11/26 22:36
 * @description  统一activity 和 fragment
 */
interface InitAction {

    /** 布局 */
    @LayoutRes
    fun layoutId(): Int

    /** 初始化参数 */
    fun initParam() = Unit

    /** 初始化View */
    fun initView()

    /** 加载数据 */
    fun loadData() = Unit

    /** 监听 */
    fun listener() = Unit

    /** 观察 */
    fun observer() = Unit

    /** Br 数据 */
    fun getBrMap(): Map<Int, Any> = emptyMap()

}
