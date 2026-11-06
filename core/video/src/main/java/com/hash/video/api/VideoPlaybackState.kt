package com.hash.video.api

/** 业务层可观察的简化播放状态，不暴露 Media3 类型。 */
data class VideoPlaybackState(
    val media: VideoMedia? = null,
    val status: VideoPlaybackStatus = VideoPlaybackStatus.IDLE,
    val presentation: VideoPresentation = VideoPresentation.EMBEDDED,
    val errorMessage: String? = null,
)

/** 播放器当前的稳定状态，供界面根据状态渲染播放控件。 */
enum class VideoPlaybackStatus {
    /** 尚未加载媒体，或播放器已被重置。 */
    IDLE,
    /** 已接收播放请求，正在创建或准备媒体资源。 */
    PREPARING,
    /** 播放过程中正在等待更多媒体数据。 */
    BUFFERING,
    /** 正在播放媒体内容。 */
    PLAYING,
    /** 媒体已准备完成，但当前暂停播放。 */
    PAUSED,
    /** 已播放至媒体末尾。 */
    ENDED,
    /** 播放过程中发生不可恢复的错误。 */
    ERROR,
}
