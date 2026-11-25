package com.hash.video.ui.player

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.hash.video.ui.interaction.VideoInteractionIntent
import com.hash.video.ui.interaction.FloatingDragGestureHandler
import com.hash.video.ui.interaction.FloatingDragListener
import com.hash.video.ui.panel.FloatingVideoControlPanel
import com.hash.video.api.VideoPlaybackState

/** 系统悬浮窗播放器，复用基础播放器，仅保留轻触、双击和紧凑控制栏。 */
class FloatingVideoPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : BaseHashVideoPlayerView(context, attrs, defStyleAttr) {
    private val panel = FloatingVideoControlPanel(context)
    /** 覆盖视频区域的独立拖动层；控制图标位于它的上方。 */
    private val dragSurface = View(context)
    private var onClose: (() -> Unit)? = null
    private var onRestore: (() -> Unit)? = null
    private var dragListener: FloatingDragListener? = null
    private val dragGestureHandler = FloatingDragGestureHandler(object : FloatingDragListener {
        override fun onDragBy(deltaX: Float, deltaY: Float) {
            dragListener?.onDragBy(deltaX, deltaY)
        }

        override fun onDragEnd() {
            dragListener?.onDragEnd()
        }
    })
    private val gestureDetector = GestureDetector(context, DragSurfaceGestureListener())
    private val hideControlsRunnable = Runnable { panel.hideControls() }

    init {
        background = GradientDrawable().apply {
            cornerRadius = dp(CORNER_RADIUS_DP).toFloat()
            setColor(Color.BLACK)
            setStroke(dp(BORDER_WIDTH_DP), Color.WHITE)
        }
        clipToOutline = true
        addView(dragSurface, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        addView(panel, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        panel.hideControls()
        panel.setIntentHandler(::handleInteractionIntent)
        dragSurface.setOnTouchListener(::handleDragTouch)
    }

    fun setCallbacks(onClose: () -> Unit, onRestore: () -> Unit) {
        this.onClose = onClose
        this.onRestore = onRestore
    }

    internal fun setDragListener(listener: FloatingDragListener) {
        dragListener = listener
    }

    override fun handleInteractionIntent(intent: VideoInteractionIntent) {
        when (intent) {
            VideoInteractionIntent.RestoreFromOverlay -> onRestore?.invoke()
            VideoInteractionIntent.CloseOverlay -> onClose?.invoke()
            else -> super.handleInteractionIntent(intent)
        }
    }

    override fun onPlaybackStateChanged(state: VideoPlaybackState) {
        panel.render(state, media != null)
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(hideControlsRunnable)
        super.onDetachedFromWindow()
    }

    /** 拖动层直接处理视频区域移动，不需要从播放器手势层中途拦截事件。 */
    private fun handleDragTouch(view: View, event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        dragGestureHandler.onTouch(event)
        return true
    }

    private inner class DragSurfaceGestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(event: MotionEvent) = true

        override fun onSingleTapConfirmed(event: MotionEvent): Boolean {
            if (!dragGestureHandler.hasDragged) {
                showControls()
                performClick()
            }
            return true
        }

        override fun onDoubleTap(event: MotionEvent): Boolean {
            if (!dragGestureHandler.hasDragged) handleInteractionIntent(VideoInteractionIntent.TogglePlayback)
            return true
        }
    }

    /** 显示控制层，并从最后一次点击开始重新计时自动隐藏。 */
    private fun showControls() {
        panel.showControls()
        removeCallbacks(hideControlsRunnable)
        postDelayed(hideControlsRunnable, CONTROLS_AUTO_HIDE_DELAY_MS)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val CORNER_RADIUS_DP = 12
        const val BORDER_WIDTH_DP = 1
        const val CONTROLS_AUTO_HIDE_DELAY_MS = 5_000L
    }
}
