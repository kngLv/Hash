package com.hash.common.base.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import com.hash.common.R
import com.hash.common.base.action.AnimAction

/** 使用 Hash 品牌动画的通用加载对话框。 */
class LoadingDialog {

    class Builder(context: Context) : BaseDialog.Builder<Builder>(context) {

        private val messageView: TextView? by lazy { findViewById(R.id.tv_loading_message) }

        init {
            setContentView(R.layout.loading_dialog)
            setAnimStyle(AnimAction.ANIM_SCALE)
            setBackgroundDimEnabled(false)
            setCancelable(false)
            setOnCreateListener(object : BaseDialog.OnCreateListener {
                override fun onCreate(dialog: BaseDialog?) {
                    dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            })
        }

        fun setMessage(@StringRes id: Int): Builder = apply {
            setMessage(getString(id))
        }

        fun setMessage(text: CharSequence?): Builder = apply {
            messageView?.text = text
            messageView?.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }
}
