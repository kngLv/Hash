package com.hash.video.ui.interaction

/** 交互协调器输出的用户意图；实际执行始终由播放器宿主统一完成。 */
sealed interface VideoInteractionIntent {
    data object PlayerClicked : VideoInteractionIntent
    data object TogglePlayback : VideoInteractionIntent
    data class SeekTo(val positionMs: Long) : VideoInteractionIntent
}
