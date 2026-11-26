package com.hash.video.api

/** 队列开始播放时的通知展示配置。 */
data class VideoQueuePlaybackOptions(
    val showNotification: Boolean = true,
    val showNotificationArtwork: Boolean = true,
)
