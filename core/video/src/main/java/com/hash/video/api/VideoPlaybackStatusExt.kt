package com.hash.video.api

/** 是否正在准备媒体或等待更多数据。 */
val VideoPlaybackStatus.isLoading: Boolean
    get() = this == VideoPlaybackStatus.PREPARING || this == VideoPlaybackStatus.BUFFERING

/** 是否正在实际输出媒体内容。 */
val VideoPlaybackStatus.isPlaying: Boolean
    get() = this == VideoPlaybackStatus.PLAYING

/** 是否已加载媒体但被暂停。 */
val VideoPlaybackStatus.isPaused: Boolean
    get() = this == VideoPlaybackStatus.PAUSED

/** 是否已播放至媒体末尾。 */
val VideoPlaybackStatus.isFinished: Boolean
    get() = this == VideoPlaybackStatus.ENDED
