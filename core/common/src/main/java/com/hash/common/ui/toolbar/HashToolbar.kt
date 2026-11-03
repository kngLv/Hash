package com.hash.common.ui.toolbar

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.hash.common.R

class HashToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    enum class TitleGravity {
        CENTER,
        START,
        END,
    }

    internal data class TitleBounds(
        val left: Int,
        val right: Int,
    ) {
        val width: Int
            get() = right - left
    }

    internal companion object {
        fun calculateTitleBounds(
            parentWidth: Int,
            titleWidth: Int,
            startAreaWidth: Int,
            endAreaWidth: Int,
            gravity: TitleGravity,
            edgePadding: Int = 0,
        ): TitleBounds {
            val resolvedPadding = if (gravity == TitleGravity.CENTER) {
                0
            } else {
                edgePadding.coerceAtLeast(0)
            }
            val availableWidth = when (gravity) {
                TitleGravity.CENTER -> parentWidth - 2 * maxOf(startAreaWidth, endAreaWidth)
                TitleGravity.START,
                TitleGravity.END,
                -> parentWidth - startAreaWidth - endAreaWidth
            }.minus(resolvedPadding).coerceAtLeast(0)
            val resolvedWidth = minOf(titleWidth, availableWidth)
            val left = when (gravity) {
                TitleGravity.CENTER -> (parentWidth - resolvedWidth) / 2
                TitleGravity.START -> startAreaWidth + resolvedPadding
                TitleGravity.END -> parentWidth - endAreaWidth - resolvedPadding - resolvedWidth
            }.coerceAtLeast(0)

            return TitleBounds(left, (left + resolvedWidth).coerceAtMost(parentWidth))
        }
    }

    private val backButton = AppCompatImageButton(context).apply {
        id = R.id.back
        setImageResource(R.drawable.ic_toolbar_back)
        contentDescription = context.getString(R.string.common_back)
        background = selectableItemBackground()
    }
    private val titleView = AppCompatTextView(context).apply {
        ellipsize = android.text.TextUtils.TruncateAt.END
        maxLines = 1
        setTextColor(ContextCompat.getColor(context, R.color.textTitle))
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 17f)
        setTypeface(typeface, android.graphics.Typeface.BOLD)
    }
    private val defaultAction = AppCompatImageButton(context).apply {
        visibility = GONE
        background = selectableItemBackground()
        contentDescription = context.getString(R.string.common_action)
    }
    private val endActions = LinearLayout(context).apply {
        gravity = Gravity.CENTER_VERTICAL
        orientation = LinearLayout.HORIZONTAL
        addView(defaultAction, LinearLayout.LayoutParams(dp(48), LayoutParams.MATCH_PARENT))
    }
    private var titleGravity = TitleGravity.CENTER
    private var titleLayoutWidth = -1
    private var titleLayoutStartWidth = -1
    private var titleLayoutEndWidth = -1
    private var titleEdgePadding = 0

    init {
        ImageViewCompat.setImageTintList(
            backButton,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textTitle)),
        )
        addView(
            backButton,
            LayoutParams(dp(48), dp(48), Gravity.START or Gravity.CENTER_VERTICAL),
        )
        addView(titleView, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
        addView(
            endActions,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT, Gravity.END),
        )

        context.obtainStyledAttributes(attrs, R.styleable.HashToolbar, defStyleAttr, 0).apply {
            setTitle(getString(R.styleable.HashToolbar_hashTitle))
            setTitleGravity(
                TitleGravity.entries.getOrElse(
                    getInt(
                        R.styleable.HashToolbar_hashTitleGravity,
                        TitleGravity.CENTER.ordinal,
                    ),
                ) { TitleGravity.CENTER },
            )
            setTitleEdgePadding(
                getDimensionPixelSize(R.styleable.HashToolbar_hashTitlePadding, 0),
            )
            backButton.visibility = if (
                getBoolean(R.styleable.HashToolbar_showBackButton, true)
            ) {
                VISIBLE
            } else {
                GONE
            }
            getDrawable(R.styleable.HashToolbar_backIcon)?.let(::setBackIcon)
            setBackIconSize(
                getDimensionPixelSize(
                    R.styleable.HashToolbar_backIconSize,
                    context.resources.getDimensionPixelSize(R.dimen.hash_toolbar_back_icon_size),
                ),
            )
            getResourceId(R.styleable.HashToolbar_actionIcon, 0).takeIf { it != 0 }?.let {
                setDefaultActionIcon(it)
            }
            setDefaultActionIconSize(
                getDimensionPixelSize(
                    R.styleable.HashToolbar_actionIconSize,
                    context.resources.getDimensionPixelSize(R.dimen.hash_toolbar_action_icon_size),
                ),
            )
            getString(R.styleable.HashToolbar_actionContentDescription)?.let {
                defaultAction.contentDescription = it
            }
            recycle()
        }
    }

    fun setTitle(title: CharSequence?) {
        titleView.text = title ?: ""
        titleView.visibility = if (title.isNullOrBlank()) INVISIBLE else VISIBLE
        titleView.maxWidth = Int.MAX_VALUE
        invalidateTitleBounds()
    }

    fun setTitleGravity(gravity: TitleGravity) {
        if (titleGravity == gravity) return
        titleGravity = gravity
        invalidateTitleBounds()
    }

    fun setTitleEdgePadding(padding: Int) {
        val resolvedPadding = padding.coerceAtLeast(0)
        if (titleEdgePadding == resolvedPadding) return
        titleEdgePadding = resolvedPadding
        invalidateTitleBounds()
    }

    fun setBackClickListener(listener: OnClickListener?) {
        backButton.setOnClickListener(listener)
    }

    fun setBackIcon(icon: Drawable) {
        backButton.setImageDrawable(icon)
    }

    fun setBackButtonVisible(visible: Boolean) {
        backButton.visibility = if (visible) VISIBLE else GONE
        invalidateTitleBounds()
    }

    fun setDefaultActionIcon(@DrawableRes iconRes: Int) {
        defaultAction.setImageResource(iconRes)
        defaultAction.visibility = VISIBLE
        ImageViewCompat.setImageTintList(
            defaultAction,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textTitle)),
        )
        invalidateTitleBounds()
    }

    fun setDefaultActionClickListener(listener: OnClickListener?) {
        defaultAction.setOnClickListener(listener)
    }

    fun setDefaultActionVisible(visible: Boolean) {
        defaultAction.visibility = if (visible) VISIBLE else GONE
        invalidateTitleBounds()
    }

    fun clearAddedActions() {
        endActions.removeViews(0, (endActions.childCount - 1).coerceAtLeast(0))
        invalidateTitleBounds()
    }

    fun addAction(action: View): View {
        endActions.addView(
            action,
            (endActions.childCount - 1).coerceAtLeast(0),
            LinearLayout.LayoutParams(dp(48), LayoutParams.MATCH_PARENT),
        )
        invalidateTitleBounds()
        return action
    }

    fun createTextAction(
        text: CharSequence,
        contentDescription: CharSequence = text,
        onClick: OnClickListener,
    ): AppCompatTextView = AppCompatTextView(context).apply {
        gravity = Gravity.CENTER
        this.text = text
        this.contentDescription = contentDescription
        setTextColor(ContextCompat.getColor(context, R.color.textTitle))
        textSize = 16f
        background = selectableItemBackground()
        setOnClickListener(onClick)
    }

    fun createIconAction(
        @DrawableRes iconRes: Int,
        contentDescription: CharSequence,
        onClick: OnClickListener,
    ): AppCompatImageButton = AppCompatImageButton(context).apply {
        setImageResource(iconRes)
        this.contentDescription = contentDescription
        ImageViewCompat.setImageTintList(
            this,
            ColorStateList.valueOf(ContextCompat.getColor(context, R.color.textTitle)),
        )
        background = selectableItemBackground()
        setPadding(dp(14), dp(14), dp(14), dp(14))
        setOnClickListener(onClick)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = measuredWidth
        val startWidth = if (backButton.visibility == VISIBLE) backButton.measuredWidth else 0
        val endWidth = endActions.measuredWidth
        if (
            width != titleLayoutWidth ||
            startWidth != titleLayoutStartWidth ||
            endWidth != titleLayoutEndWidth
        ) {
            val bounds = calculateTitleBounds(
                parentWidth = width,
                titleWidth = titleView.measuredWidth,
                startAreaWidth = startWidth,
                endAreaWidth = endWidth,
                gravity = titleGravity,
                edgePadding = titleEdgePadding,
            )
            titleView.maxWidth = bounds.width
            (titleView.layoutParams as LayoutParams).apply {
                gravity = when (titleGravity) {
                    TitleGravity.CENTER -> Gravity.CENTER
                    TitleGravity.START -> Gravity.START or Gravity.CENTER_VERTICAL
                    TitleGravity.END -> Gravity.END or Gravity.CENTER_VERTICAL
                }
                leftMargin = if (titleGravity == TitleGravity.START) bounds.left else 0
                rightMargin = if (titleGravity == TitleGravity.END) width - bounds.right else 0
            }.also { titleView.layoutParams = it }
            titleLayoutWidth = width
            titleLayoutStartWidth = startWidth
            titleLayoutEndWidth = endWidth
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    private fun setBackIconSize(size: Int) {
        val padding = (dp(48) - size).coerceAtLeast(0) / 2
        backButton.setPadding(padding, padding, padding, padding)
    }

    private fun setDefaultActionIconSize(size: Int) {
        val padding = (dp(48) - size).coerceAtLeast(0) / 2
        defaultAction.setPadding(padding, padding, padding, padding)
    }

    private fun selectableItemBackground(): Drawable? {
        val attributes = context.obtainStyledAttributes(
            intArrayOf(android.R.attr.selectableItemBackgroundBorderless),
        )
        return try {
            attributes.getDrawable(0)
        } finally {
            attributes.recycle()
        }
    }

    private fun invalidateTitleBounds() {
        titleLayoutWidth = -1
        titleLayoutStartWidth = -1
        titleLayoutEndWidth = -1
        requestLayout()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
