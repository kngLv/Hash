package com.hash.video.api

/** 业务层可观察的简化播放状态，不暴露 Media3 类型。 */
data class VideoPlaybackState(
    /** 当前播放会话对应的业务媒体，空表示没有可恢复的当前媒体。 */
    val media: VideoMedia? = null,
    /** 播放器的稳定播放阶段。 */
    val status: VideoPlaybackStatus = VideoPlaybackStatus.IDLE,
    /** 当前画面的承载形态。 */
    val presentation: VideoPresentation = VideoPresentation.EMBEDDED,
    /** 当前媒体的播放位置、缓冲位置与总时长。 */
    val progress: VideoPlaybackProgress = VideoPlaybackProgress(),
    /** 从媒体流解析出的真实视频尺寸，未知时为空。 */
    val contentSize: VideoContentSize? = null,
    /** 最近一次不可恢复播放错误的说明。 */
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
