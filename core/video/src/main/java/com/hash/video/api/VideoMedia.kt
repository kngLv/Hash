package com.hash.video.api

/** 与业务模型解耦的视频播放输入。 */
data class VideoMedia(
    val id: String,
    val url: String,
    val title: String = "",
    val coverUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

/** 标题和封面只影响展示；播放源由业务标识、地址和请求头共同确定。 */
internal fun VideoMedia.isSameSourceAs(other: VideoMedia?): Boolean =
    other != null && id == other.id && url == other.url && headers == other.headers
