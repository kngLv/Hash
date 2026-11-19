package com.hash.video.ui.gesture

/** 当前播放器启用哪些画面手势。 */
data class VideoGestureConfig(
    /** 单击显示或隐藏控制栏。 */
    val singleTapEnabled: Boolean = false,
    /** 双击切换播放和暂停。 */
    val doubleTapEnabled: Boolean = false,
    /** 横滑预览并跳转进度。 */
    val seekEnabled: Boolean = false,
)
