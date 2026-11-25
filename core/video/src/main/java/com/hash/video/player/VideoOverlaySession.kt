package com.hash.video.player

/**
 * 系统悬浮窗持有的播放会话凭据。
 *
 * 它只由引擎创建，外部不能任意替换操作凭证；恢复或关闭必须携带同一会话实例。
 */
internal class VideoOverlaySession internal constructor(
    internal val token: VideoPlaybackControlToken,
)
