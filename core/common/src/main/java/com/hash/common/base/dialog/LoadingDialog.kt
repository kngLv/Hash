package com.hash.common.base.dialog

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.annotation.StyleRes
import com.hash.common.R
import com.hash.common.base.action.AnimAction

/** 使用 Hash 品牌动画的通用加载对话框。 */
class LoadingDialog {

    class Builder(context: Context) : BaseDialog.Builder<Builder>(context) {

        private val messageView: TextView? by lazy { findViewById(R.id.tv_loading_message) }

        init {
            setContentView(R.layout.loading_dialog)
            setAnimStyle(AnimAction.ANIM_EMPTY)
            setBackgroundDimEnabled(false)
            setCancelable(false)
            setOnCreateListener(object : BaseDialog.OnCreateListener {
                override fun onCreate(dialog: BaseDialog?) {
                    dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            })
        }

        override fun createDialog(context: Context, @StyleRes themeId: Int): BaseDialog {
            return ContentAnimatedLoadingDialog(context, themeId)
        }

        fun setMessage(@StringRes id: Int): Builder = apply {
            setMessage(getString(id))
        }

        fun setMessage(text: CharSequence?): Builder = apply {
            messageView?.text = text
            messageView?.visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    /** 对已完成布局的内容根视图执行退出动画，确保缩放原点就是视觉中心。 */
    private class ContentAnimatedLoadingDialog(
        context: Context,
        @StyleRes themeId: Int,
    ) : BaseDialog(context, themeId) {

        private var isDismissing = false

        override fun dismiss() {
            if (!isShowing || isDismissing) {
                return
            }
            val contentView = getContentView()
            if (contentView == null || contentView.width == 0 || contentView.height == 0) {
                super.dismiss()
                return
            }
            isDismissing = true
            contentView.pivotX = contentView.width / 2f
            contentView.pivotY = contentView.height / 2f
            contentView.animate()
                .scaleX(EXIT_SCALE)
                .scaleY(EXIT_SCALE)
                .alpha(0f)
                .setDuration(EXIT_ANIMATION_DURATION_MILLIS)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    contentView.scaleX = 1f
                    contentView.scaleY = 1f
                    contentView.alpha = 1f
                    isDismissing = false
                    dismissAfterContentAnimation()
                }
                .start()
        }

        private fun dismissAfterContentAnimation() {
            super.dismiss()
        }

        private companion object {
            const val EXIT_SCALE = 0.6f
            const val EXIT_ANIMATION_DURATION_MILLIS = 150L
        }
    }
}
