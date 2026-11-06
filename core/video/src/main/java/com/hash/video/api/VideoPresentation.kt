package com.hash.video.api

/** 视频画面当前的承载形态。 */
enum class VideoPresentation {
    /** 嵌入业务页面中的普通播放器视图。 */
    EMBEDDED,
    /** 在应用内横屏或沉浸式展示的视频全屏页面。 */
    FULLSCREEN,
    /** 仅在应用内显示、可自由拖动的小窗。 */
    IN_APP_FLOAT,
    /** 覆盖其他应用显示的系统级悬浮窗，需要用户授予悬浮窗权限。 */
    SYSTEM_OVERLAY,
}
