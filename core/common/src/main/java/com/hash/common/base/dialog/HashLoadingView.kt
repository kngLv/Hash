package com.hash.common.base.dialog

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.hash.common.R
import kotlin.math.min
import kotlin.math.cos
import kotlin.math.PI

/** 以逐段点亮效果展示的 Hash 加载动画。 */
class HashLoadingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }
    private val activeColor = ContextCompat.getColor(context, R.color.loadingHashActive)
    private val inactiveColor = ContextCompat.getColor(context, R.color.loadingHashInactive)
    private var progress = 0f

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = ANIMATION_DURATION_MILLIS
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            progress = it.animatedValue as Float
            invalidate()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) {
            animator.start()
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return

        val centerX = width / 2f
        val centerY = height / 2f
        val lineInset = size * 0.23f
        val lineHalfLength = size * 0.30f
        val strokeWidth = size * 0.13f
        drawSegment(canvas, 0, strokeWidth) {
            drawLine(centerX - lineInset, centerY - lineHalfLength, centerX - lineInset, centerY + lineHalfLength, paint)
        }
        drawSegment(canvas, 1, strokeWidth) {
            drawLine(centerX + lineInset, centerY - lineHalfLength, centerX + lineInset, centerY + lineHalfLength, paint)
        }
        drawSegment(canvas, 2, strokeWidth) {
            drawLine(centerX - lineHalfLength, centerY - lineInset, centerX + lineHalfLength, centerY - lineInset, paint)
        }
        drawSegment(canvas, 3, strokeWidth) {
            drawLine(centerX - lineHalfLength, centerY + lineInset, centerX + lineHalfLength, centerY + lineInset, paint)
        }
    }

    private inline fun drawSegment(
        canvas: Canvas,
        segment: Int,
        strokeWidth: Float,
        draw: Canvas.() -> Unit,
    ) {
        val intensity = segmentIntensityAt(progress, segment)
        paint.color = ColorUtils.blendARGB(inactiveColor, activeColor, intensity)
        paint.strokeWidth = strokeWidth * (0.84f + intensity * 0.36f)
        canvas.draw()
    }

    companion object {
        private const val ANIMATION_DURATION_MILLIS = 1200L
        private const val SEGMENT_COUNT = 4
        private const val MINIMUM_INTENSITY = 0.04f

        /** 返回指定 Hash 线段在当前动画进度中的连续强调强度。 */
        fun segmentIntensityAt(progress: Float, segment: Int): Float {
            require(segment in 0 until SEGMENT_COUNT) { "segment 必须在 0 到 3 之间" }
            val normalizedProgress = ((progress % 1f) + 1f) % 1f
            val phase = normalizedProgress - segment.toFloat() / SEGMENT_COUNT
            val wave = ((cos(phase * 2f * PI) + 1.0) / 2.0).toFloat()
            return MINIMUM_INTENSITY + (1f - MINIMUM_INTENSITY) * wave
        }
    }
}
