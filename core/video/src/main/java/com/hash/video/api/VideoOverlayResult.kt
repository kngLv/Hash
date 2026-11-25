package com.hash.video.api

/** 请求进入系统悬浮窗的结果。 */
enum class VideoOverlayResult {
    /** 悬浮窗已成功显示并接管当前播放会话。 */
    SHOWN,
    /** 未获得“显示在其他应用上层”权限，调用方可引导用户授权。 */
    PERMISSION_REQUIRED,
    /** 当前没有可迁移到悬浮窗的播放媒体。 */
    NO_ACTIVE_MEDIA,
    /** 添加系统窗口或迁移画面失败，当前页面仍保留原播放会话。 */
    FAILED,
}
