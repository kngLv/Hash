package com.hash.video.player

/**
 * 全局视频播放器的操作凭证。
 *
 * 引擎只接受当前凭证发起的操作，避免旧页面误操作新页面或悬浮窗正在使用的播放器。
 * 单播放器 View 会自行创建凭证；多个播放器共用同一播放会话时，再由页面创建并分配给它们。
 */
class VideoPlaybackControlToken
