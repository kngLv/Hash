package com.hash.common.bindingAdapter

import android.widget.ImageView
import androidx.databinding.BindingAdapter


@BindingAdapter("binding_res", requireAll = false)
fun ImageView.bindingRes(
    res: Int
) {
    println("=======================> BindingAdapter bindingRes: $res")
    if (res == 0) return
    setImageResource(res)
}
