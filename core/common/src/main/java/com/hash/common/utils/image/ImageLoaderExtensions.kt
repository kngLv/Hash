@file:Suppress("unused")

package com.hash.common.utils.image

import android.widget.ImageView
import com.hash.common.config.GlideApp

/**
 * Extension helpers for ImageLoader.
 */
fun ImageView.load(model: Any?, options: LoadOptions = LoadOptions()) {
    ImageLoader.load(this, model, options)
}

fun ImageView.load(model: Any?, builderBlock: LoadOptions.Builder.() -> Unit) {
    val opts = LoadOptions.Builder().apply(builderBlock).build()
    ImageLoader.load(this, model, opts)
}

fun ImageView.cancelLoad() {
    GlideApp.with(this).clear(this)
}

