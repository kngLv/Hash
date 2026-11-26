package com.hash.video.player

/** 通知栏两侧媒体操作的只读快照；不包含任何页面或 View 引用。 */
internal data class VideoNotificationControls(
    val isAppInForeground: Boolean = true,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
) {
    val usesPlaylistNavigation: Boolean
        get() = !isAppInForeground

    /** 后台队列中存在前一项时才显示上一条。 */
    val shouldShowPrevious: Boolean
        get() = usesPlaylistNavigation && hasPrevious

    /** 后台队列中存在后一项时才显示下一条。 */
    val shouldShowNext: Boolean
        get() = usesPlaylistNavigation && hasNext

    /** 后台队列存在任一相邻媒体时，通知展示上一条和下一条两个导航按钮。 */
    val shouldShowPlaylistNavigationButtons: Boolean
        get() = shouldShowPrevious || shouldShowNext
}
