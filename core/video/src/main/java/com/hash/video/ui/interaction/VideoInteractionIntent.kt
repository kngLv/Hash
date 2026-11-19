package com.hash.video.ui.interaction

/** 交互协调器输出的用户意图；实际执行始终由播放器宿主统一完成。 */
sealed interface VideoInteractionIntent {
    /** 播放器空白区域被单击，用于通知外部处理点击事件。 */
    data object PlayerClicked : VideoInteractionIntent

    /** 请求切换当前 View 绑定媒体的播放或暂停状态。 */
    data object TogglePlayback : VideoInteractionIntent

    /** 请求跳转当前 View 绑定媒体的播放位置。 */
    data class SeekTo(val positionMs: Long) : VideoInteractionIntent
}
