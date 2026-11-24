package com.hash.video.player

/** 通知栏两侧媒体操作的只读快照；不包含任何页面或 View 引用。 */
internal data class VideoNotificationControls(
    val isAppInForeground: Boolean = true,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
) {
    val usesPlaylistNavigation: Boolean
        get() = !isAppInForeground
}
