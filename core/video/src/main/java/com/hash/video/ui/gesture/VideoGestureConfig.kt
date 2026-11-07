package com.hash.video.ui.gesture

/**
 * 视频画面区域可选手势的开关。
 *
 * Base 默认不启用任何手势，由具体播放器按自身场景明确配置：内嵌播放器通常只开启单击、双击和
 * 横向跳转；全屏播放器可额外开启亮度和音量；列表播放器应谨慎启用滑动手势，避免和列表滚动冲突。
 * 手势仅作用于视频画面层，不会拦截控制栏按钮或进度条的触摸。
 */
data class VideoGestureConfig(
    /** 单击画面时分发控制栏显隐事件。 */
    val singleTapEnabled: Boolean = false,
    /** 双击画面时切换当前媒体的播放与暂停状态。 */
    val doubleTapEnabled: Boolean = false,
    /** 横向滑动时预览并在手指抬起后跳转播放位置；未知时长的媒体自动忽略。 */
    val seekEnabled: Boolean = false,
    /** 左侧纵向滑动时分发亮度变化事件；具体全屏宿主负责修改 Window 亮度。 */
    val brightnessEnabled: Boolean = false,
    /** 右侧纵向滑动时分发媒体音量变化事件；具体全屏宿主负责调整系统媒体音量。 */
    val volumeEnabled: Boolean = false,
)
