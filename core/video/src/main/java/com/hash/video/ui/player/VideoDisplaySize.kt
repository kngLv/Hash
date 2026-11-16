package com.hash.video.ui.player

/** 已确定的视频展示尺寸；同一媒体会一直复用，直到业务主动刷新。 */
data class VideoDisplaySize(
    val width: Int,
    val height: Int,
)
