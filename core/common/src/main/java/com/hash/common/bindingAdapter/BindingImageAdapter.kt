package com.hash.common.bindingAdapter

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.hash.common.utils.image.load


@BindingAdapter("binding_res", requireAll = false)
fun ImageView.bindingRes(
    res: Int
) {
    if (res == 0) return
    load(res)
//    setImageResource(res)
}

// 支持可选的 placeholder 和 error 资源
@BindingAdapter(value = ["binding_url", "binding_placeholder", "binding_error"], requireAll = false)
fun ImageView.bindingUrl(
    url: String?,
    placeholderRes: Int?,
    errorRes: Int?
) {
    load(url) {
        placeholderRes?.let { placeholderRes(it) }
        errorRes?.let { errorRes(it) }
    }
}
