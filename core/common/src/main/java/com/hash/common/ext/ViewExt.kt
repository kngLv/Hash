package com.hash.common.ext

import android.view.View


/** 切换 View 的选中状态。 */
fun View.selectToggle() {
    isSelected = !isSelected
}

