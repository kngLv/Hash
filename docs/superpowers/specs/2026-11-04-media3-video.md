# Media3 视频播放器方案

## 目标

为 Hash 新增 `core:video` 公共模块，使用 AndroidX Media3 的 ExoPlayer 提供新闻详情页内播放、横屏全屏、应用内悬浮小窗、系统悬浮窗和后台持续播放能力；同时为后续短视频列表预加载、边播边缓存和弹幕预留清晰扩展点。

## 范围

首期必须交付：

- 新闻详情页视频在应用内播放，不再跳转外部播放器。
- 同一个视频可在内嵌、横屏全屏、应用内小窗、系统悬浮窗之间迁移，迁移时保持播放位置和状态。
- 使用 Media3 缓存实现边播边缓存，并限制磁盘占用。
- 使用 `MediaSessionService` 保障后台播放、系统通知和媒体控制。
- 用户主动点击悬浮播放时才申请系统悬浮窗权限；授权后可选择回桌面自动转为系统悬浮窗。

首期明确不实现：

- 抖音式 RecyclerView 自动播放、预加载策略及静音策略。
- 弹幕、滤镜、美颜、广告、水印和离线下载界面。
- 多路视频同时播放。

## 架构边界

`core:video` 不依赖 `feature:home`，不出现新闻模型、新闻接口或弹幕数据模型。对业务只暴露 `VideoMedia`、`HashVideoPlayerView` 和少量播放控制 API。

模块内部使用一个实际的 ExoPlayer，由 `HashVideoPlaybackService` 持有。每个展示形态只负责承载同一播放器画面：切换时先从旧 `PlayerView` 解绑，再绑定到新 `PlayerView`，绝不为同一个视频重复创建播放器。

缓存由 `VideoCacheProvider` 统一创建并以 LRU 策略淘汰。缓存键必须由稳定的媒体 ID、URL 和影响内容的请求头构成，缓存目录和 ExoPlayer 实例均不暴露给业务模块。

## 模块结构

```text
core:video
├── api/             业务可用模型与门面 API
├── player/          MediaController 连接、播放状态和播放器迁移
├── cache/           CacheDataSource 与 LRU 磁盘缓存
├── presentation/    内嵌、全屏、应用内/系统悬浮窗协调
├── service/         MediaSessionService 与媒体通知
└── ui/              HashVideoPlayerView 和控制层
```

首期保持一个 `core:video` 模块，不预先拆出 `video-ui`、`video-feed` 或 `video-danmaku`。当短视频流、弹幕的实际需求出现时，再创建可选模块，避免当前引入空抽象。

## 核心模型与 API

```kotlin
data class VideoMedia(
    val id: String,
    val url: String,
    val title: String = "",
    val coverUrl: String? = null,
    val headers: Map<String, String> = emptyMap(),
)

enum class VideoPresentation {
    EMBEDDED,
    FULLSCREEN,
    IN_APP_FLOAT,
    SYSTEM_OVERLAY,
}

class HashVideoPlayerView : FrameLayout {
    fun setMedia(media: VideoMedia)
    fun play()
    fun pause()
    fun enterFullscreen()
    fun enterInAppFloat()
    fun enterSystemOverlay()
    fun releaseHost()
}
```

`HashVideoPlayerView` 不向业务暴露 `ExoPlayer` 或 `MediaController`。播放器状态以只读 `StateFlow<VideoPlaybackState>` 提供；业务只订阅播放、暂停、缓冲、错误和展示形态。

## 展示与后台规则

- 内嵌：详情页面默认形态。
- 全屏：核心模块的 `VideoFullscreenActivity` 强制横屏，返回后恢复为内嵌形态。
- 应用内小窗：附着当前 Activity 的 decor view，无需悬浮窗权限，可拖动、关闭、回到详情。
- 系统悬浮窗：使用 `TYPE_APPLICATION_OVERLAY`，仅在用户已授予权限时启用。无权限时只返回可识别的权限缺失结果，页面负责引导用户授权。
- 回桌面：已开启“后台自动悬浮”且拥有权限时，转入系统悬浮窗；否则后台服务保留播放和通知，不自行弹出授权页。
- 当前只允许一个播放会话；新媒体播放时结束旧媒体会话。

## 可扩展性

未来短视频列表通过 `VideoFeedController` 使用相同的 `VideoMedia` 与全局播放器。它只负责“当前可播放 Item 的选择”和 `DefaultPreloadManager` 预加载，不改变详情页 API。

弹幕作为覆盖层插件：播放器 View 提供内容覆盖容器，未来 `core:video-danmaku` 只向该容器渲染，不参与播放器、缓存、服务或窗口迁移。

## 全局约束

- 最低 SDK 保持项目现有 `minSdk 24`。
- 使用 AndroidX Media3，不引入 GSYVideoPlayer、IJKPlayer 或 AndroidVideoCache。
- 所有用户可见文案放资源文件，不在代码中硬编码。
- 悬浮窗权限只在用户主动行为后引导，不在应用启动或首次播放时申请。
- 先完成详情页单视频流程；列表和弹幕不以占位代码形式进入首期。
