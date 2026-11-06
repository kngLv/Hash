package com.hash.video.api

/** 与业务模型解耦的视频播放输入。 */
data class VideoMedia(
    val id: String,
    val url: String,
    val title: String = "",
    val coverUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
)
