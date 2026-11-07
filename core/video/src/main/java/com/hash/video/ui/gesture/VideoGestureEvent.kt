package com.hash.video.ui.gesture

/**
 * 已完成坐标与方向判断的视频手势事件。
 *
 * Base 仅分发语义化事件；具体播放器或宿主决定是否显示提示、修改系统亮度或调整媒体音量。
 */
sealed interface VideoGestureEvent {
    /** 用户单击视频画面，通常用于切换控制栏显示状态。 */
    data object ToggleControls : VideoGestureEvent

    /** 用户双击视频画面，请求切换播放和暂停。 */
    data object TogglePlayback : VideoGestureEvent

    /** 用户横向拖动时的目标播放位置预览，单位为毫秒。 */
    data class SeekPreview(
        val positionMs: Long,
        val durationMs: Long,
    ) : VideoGestureEvent

    /** 用户结束横向拖动后提交的目标播放位置，单位为毫秒。 */
    data class SeekCommitted(val positionMs: Long) : VideoGestureEvent

    /** 横向拖动被系统或多指触摸取消，界面应放弃尚未提交的跳转预览。 */
    data object SeekCancelled : VideoGestureEvent

    /** 左侧纵向拖动的亮度变化；变化量范围为 -1 到 1，阶段用于管理宿主的亮度基准。 */
    data class BrightnessAdjustment(
        val delta: Float,
        val phase: VideoGesturePhase,
    ) : VideoGestureEvent

    /** 右侧纵向拖动的媒体音量变化；变化量范围为 -1 到 1，阶段用于管理宿主的音量基准。 */
    data class VolumeAdjustment(
        val delta: Float,
        val phase: VideoGesturePhase,
    ) : VideoGestureEvent
}

/** 连续调节手势的生命周期：移动更新、正常结束或被系统取消。 */
enum class VideoGesturePhase {
    UPDATE,
    END,
    CANCEL,
}
