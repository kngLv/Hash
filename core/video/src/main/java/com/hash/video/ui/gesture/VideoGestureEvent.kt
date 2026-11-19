package com.hash.video.ui.gesture

/**
 * 已完成坐标与方向判断的视频手势事件。
 *
 * Base 仅分发通用的播放手势事件。
 */
sealed interface VideoGestureEvent {
    /** 用户单击视频画面；具体播放器可决定显示或隐藏控制栏等行为。 */
    data object Click : VideoGestureEvent

    /** 用户双击视频画面；基类默认切换播放状态，具体播放器也可追加展示效果。 */
    data object DoubleClick : VideoGestureEvent

    /** 用户横向拖动时的目标播放位置预览，单位为毫秒。 */
    data class SeekPreview(val positionMs: Long, val durationMs: Long) : VideoGestureEvent

    /** 用户正常结束横向拖动后的最终预览位置，单位为毫秒。 */
    data class SeekPreviewEnd(val positionMs: Long) : VideoGestureEvent

    /** 横向拖动被系统或多指触摸取消，界面应放弃尚未提交的跳转预览。 */
    data object SeekPreviewCancel : VideoGestureEvent
}
