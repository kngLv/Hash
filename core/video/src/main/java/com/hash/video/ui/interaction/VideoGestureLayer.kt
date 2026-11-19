package com.hash.video.ui.interaction

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import com.hash.video.api.VideoPlaybackProgress
import com.hash.video.ui.gesture.VideoGestureConfig
import com.hash.video.ui.gesture.VideoGestureDirection
import com.hash.video.ui.gesture.VideoGestureEvent
import com.hash.video.ui.gesture.VideoTouchHandler

/** 透明触摸层，只识别手势，不直接控制播放器。 */
@SuppressLint("ViewConstructor")
internal class VideoGestureLayer(
    context: Context,
    private val onGesture: (VideoGestureEvent) -> Unit,
) : FrameLayout(context) {

    /** 用于确认单击或双击。 */
    private val handler = Handler(Looper.getMainLooper())

    /** 识别单击、双击、横滑和纵滑。 */
    private val touchHandler = VideoTouchHandler(
        touchSlopPx = ViewConfiguration.get(context).scaledTouchSlop.toFloat(),
        doubleTapSlopPx = ViewConfiguration.get(context).scaledDoubleTapSlop.toFloat(),
        postDelayed = handler::postDelayed,
        removeCallbacks = handler::removeCallbacks,
        onGesture = onGesture,
    )

    /** 横滑时计算目标位置所需的当前进度。 */
    private var latestProgress = VideoPlaybackProgress()

    /** 当前播放器的手势开关。 */
    var gestureConfig: VideoGestureConfig = VideoGestureConfig()

    init {
        bindTouchHandler()
    }

    /** 更新横滑计算所需的进度。 */
    fun renderProgress(progress: VideoPlaybackProgress) {
        latestProgress = progress
    }

    /** 取消未完成的手势和延迟单击。 */
    fun release() {
        touchHandler.cancel(gestureConfig)
        handler.removeCallbacksAndMessages(null)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun bindTouchHandler() {
        setOnTouchListener { _, event ->
            val direction = touchHandler.handleTouchEvent(
                event = event.toVideoTouchEvent(),
                config = gestureConfig,
                progress = latestProgress,
            )
            coordinateParentTouchInterception(event.actionMasked, direction)
            true
        }
    }

    private fun coordinateParentTouchInterception(action: Int, direction: VideoGestureDirection) {
        // 纵向滑动交给列表，横滑和点击由播放器处理。
        val shouldDisallowIntercept = when (action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> direction != VideoGestureDirection.VERTICAL
            else -> false
        }
        parent?.requestDisallowInterceptTouchEvent(shouldDisallowIntercept)
    }

    private fun MotionEvent.toVideoTouchEvent() = VideoTouchHandler.TouchEvent(
        action = when (actionMasked) {
            MotionEvent.ACTION_DOWN -> VideoTouchHandler.Action.DOWN
            MotionEvent.ACTION_MOVE -> VideoTouchHandler.Action.MOVE
            MotionEvent.ACTION_UP -> VideoTouchHandler.Action.UP
            MotionEvent.ACTION_POINTER_DOWN -> VideoTouchHandler.Action.POINTER_DOWN
            else -> VideoTouchHandler.Action.CANCEL
        },
        x = x,
        y = y,
        pointerCount = pointerCount,
        eventTimeMs = eventTime,
        viewWidth = width,
        viewHeight = height,
    )
}
