package com.hash.common.bindingAdapter

import android.view.View
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.databinding.BindingAdapter


@BindingAdapter("binding_onSelect")
fun View.setSelect(isSelect: Boolean) {
    isSelected = isSelect
}

@BindingAdapter("binding_visibleOrGone")
fun View.setVisibleOrGone(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.GONE
}

@BindingAdapter("binding_visibleOrInvisible")
fun View.setVisibleOrInvisible(visible: Boolean) {
    this.visibility = if (visible) View.VISIBLE else View.INVISIBLE
}

@BindingAdapter("binding_setColor")
fun TextView.setColor(color: Int) {
    setTextColor(ResourcesCompat.getColor(resources, color, null))
}

@BindingAdapter("binding_setText")
fun TextView.setTextNullable(text: String?) {
    // support nullable input and be defensive against unexpected errors
    try {
        this.text = text ?: ""
    } catch (_: Exception) {
        // fallback to empty string if anything goes wrong
        this.text = ""
    }
}