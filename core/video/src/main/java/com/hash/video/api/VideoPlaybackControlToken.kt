package com.hash.video.api

/**
 * 全局视频播放器的操作凭证。
 *
 * 引擎只接受当前凭证发起的操作，避免旧页面误操作新页面或悬浮窗正在使用的播放器。
 * 单播放器 View 会自行创建凭证；队列页面可复用某个播放器的凭证，或将一个凭证分配给多个
 * 播放器 View。
 */
class VideoPlaybackControlToken
