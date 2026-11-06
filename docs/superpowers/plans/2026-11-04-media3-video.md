# Media3 视频播放器实施计划

> **给执行者：** 必须逐任务执行，并在每个任务完成后运行相应验证；使用复选框记录进度。

**目标：** 新增一个基于 Media3 的公共视频模块，并将新闻详情的视频从外部播放器替换为可迁移、可缓存、可后台播放的应用内播放器。

**架构：** `core:video` 以 `MediaSessionService` 持有唯一 ExoPlayer；所有 UI 只通过 `MediaController` 连接它。`HashVideoPlayerView` 是业务唯一需要嵌入页面的 View，展示迁移由 `VideoPresentationCoordinator` 统一管理，避免页面各自操作播放器和 WindowManager。

**技术栈：** Kotlin、AndroidX Media3 ExoPlayer/UI/Session、`SimpleCache`、`CacheDataSource`、`MediaSessionService`、WindowManager、StateFlow。

**方案：** [Media3 视频播放器方案](../specs/2026-11-04-media3-video.md)

## 全局约束

- 最低 SDK 维持项目现有 `minSdk 24`。
- 不引入 GSYVideoPlayer、IJKPlayer、AndroidVideoCache 或业务模块依赖。
- 首期仅实现新闻详情单视频；不创建列表自动播放、弹幕或离线下载的占位实现。
- 一个 App 同时只保留一个播放会话和一个实际 ExoPlayer。
- 系统悬浮窗只在用户主动操作后申请权限或展示；无权限时不可自动跳转设置页。
- 不提交代码，直到用户审阅并确认该计划。

## 审查重点

- 视频 URL 为空、非法或 HTTP 请求失败时，页面显示可重试错误态，不能崩溃或跳转外部播放器。
- Activity 重建、全屏返回、View 被 RecyclerView 或页面销毁时，播放器不得持有旧 Activity 或 View。
- 无悬浮窗权限、授权被拒绝、从设置页返回时，播放器状态和错误提示应明确且可恢复。
- 缓存目录被占用、已满或不可写时，必须降级为正常网络播放而非阻断播放。
- 用户返回桌面时，只有已开启自动悬浮且授权成功才显示系统悬浮窗；其他情况保持符合后台播放配置的通知/音频行为。

---

## 文件结构

| 路径 | 职责 |
|---|---|
| `settings.gradle.kts` | 注册 `:core:video` 模块。 |
| `gradle/libs.versions.toml` | 统一声明 Media3 版本与 ExoPlayer、UI、Session、Datasource 依赖。 |
| `core/video/build.gradle.kts` | 视频模块插件、Android 配置和依赖。 |
| `core/video/src/main/AndroidManifest.xml` | 媒体前台服务、悬浮窗相关权限和全屏 Activity 注册。 |
| `core/video/src/main/java/com/hash/video/api/VideoMedia.kt` | 与业务无关的视频输入模型。 |
| `core/video/src/main/java/com/hash/video/api/VideoPlaybackState.kt` | 只读播放和展示状态模型。 |
| `core/video/src/main/java/com/hash/video/cache/VideoCacheProvider.kt` | 单例缓存及 `CacheDataSource.Factory`。 |
| `core/video/src/main/java/com/hash/video/service/HashVideoPlaybackService.kt` | 唯一 ExoPlayer、MediaSession 和后台通知。 |
| `core/video/src/main/java/com/hash/video/player/VideoController.kt` | 服务控制器连接、播放命令与状态转换。 |
| `core/video/src/main/java/com/hash/video/ui/HashVideoPlayerView.kt` | 业务嵌入的播放器 View 与基础控制 UI。 |
| `core/video/src/main/java/com/hash/video/presentation/VideoPresentationCoordinator.kt` | 播放器 Surface 在四种展示形态间迁移。 |
| `core/video/src/main/java/com/hash/video/presentation/VideoFullscreenActivity.kt` | 横屏全屏承载页面。 |
| `core/video/src/main/java/com/hash/video/presentation/InAppFloatHost.kt` | 当前 Activity 内可拖动小窗。 |
| `core/video/src/main/java/com/hash/video/presentation/SystemOverlayHost.kt` | 跨应用悬浮窗、权限检测和移除。 |
| `feature/home/src/main/java/com/hash/home/ui/featured/detail/NewsDetailActivity.kt` | 删除外部 `ACTION_VIEW`，接入页内播放器。 |
| `feature/home/src/main/java/com/hash/home/ui/featured/detail/adapter/NewsDetailAdapter.kt` | 视频条目绑定 `HashVideoPlayerView`，不再由 Adapter 直接启动 Intent。 |
| `feature/home/src/main/res/layout/*news_detail*.xml` | 视频条目展示布局及无障碍文案。 |

## 任务 1：建立模块与依赖边界

**文件：**
- 修改：`settings.gradle.kts`
- 修改：`gradle/libs.versions.toml`
- 创建：`core/video/build.gradle.kts`
- 创建：`core/video/src/main/AndroidManifest.xml`

**接口：**
- 产出：模块 `:core:video`；供 `feature:home` 通过 `implementation(project(":core:video"))` 使用。公共类型在任务 2 创建。

- [ ] 新增 Media3 版本目录项，以及 ExoPlayer、UI、Session、Common 和 Datasource 依赖别名。
- [ ] 注册 `:core:video`，创建 Android Library 模块，使用项目一致的 Kotlin、DataBinding 与 JUnit 配置。
- [ ] 在模块 Manifest 声明 `HashVideoPlaybackService`（`mediaPlayback` 前台服务类型）、横屏 `VideoFullscreenActivity`，以及后台播放和系统悬浮窗所需权限。
- [ ] 运行 `./gradlew :core:video:assembleDebug`，预期通过。
- [ ] 提交：`新增 Media3 视频模块基础配置`。

## 任务 2：定义稳定的业务 API 与播放状态机

**文件：**
- 创建：`core/video/src/main/java/com/hash/video/api/VideoMedia.kt`
- 创建：`core/video/src/main/java/com/hash/video/api/VideoPresentation.kt`
- 创建：`core/video/src/main/java/com/hash/video/api/VideoPlaybackState.kt`
- 创建：`core/video/src/main/java/com/hash/video/player/VideoPlaybackReducer.kt`
- 创建：`core/video/src/test/java/com/hash/video/player/VideoPlaybackReducerTest.kt`

**接口：**
- 消费：任务 1 的视频模块。
- 产出：`VideoMedia`、`VideoPresentation`、`VideoPlaybackState`、`VideoPlaybackReducer.reduce(previous, event)`；后续 UI 和服务均以该状态模型通信。

- [ ] 先编写 `VideoPlaybackReducerTest`，覆盖初始、缓冲、播放、暂停、播放错误、展示形态变更和媒体切换时旧状态清理。
- [ ] 运行该测试，预期因类型与实现不存在而失败。
- [ ] 实现不可变 `VideoMedia`、展示枚举、播放状态和纯 Kotlin reducer；禁止在 API 中暴露 Media3 类型。
- [ ] 运行 `./gradlew :core:video:testDebugUnitTest --tests '*VideoPlaybackReducerTest'`，预期通过。
- [ ] 提交：`定义视频播放公共状态模型`。

## 任务 3：实现边播边缓存与媒体服务

**文件：**
- 创建：`core/video/src/main/java/com/hash/video/cache/VideoCacheProvider.kt`
- 创建：`core/video/src/main/java/com/hash/video/cache/VideoCacheKeyFactory.kt`
- 创建：`core/video/src/main/java/com/hash/video/service/HashVideoPlaybackService.kt`
- 创建：`core/video/src/test/java/com/hash/video/cache/VideoCacheKeyFactoryTest.kt`

**接口：**
- 消费：任务 2 的 `VideoMedia`。
- 产出：`VideoCacheProvider.getDataSourceFactory(context)`、`VideoCacheKeyFactory.create(media)`、由服务提供的唯一 `MediaSession`。

- [ ] 编写缓存键测试：相同 `id/url/headers` 必须得到同一键；变化的 URL 或影响内容的请求头必须得到不同键；Map 顺序不能影响结果。
- [ ] 运行测试，预期失败。
- [ ] 实现带固定容量和 LRU 淘汰的 `SimpleCache`，将 `CacheDataSource.Factory` 注入 ExoPlayer 的媒体源工厂；缓存初始化失败时记录原因并降级为网络数据源。
- [ ] 实现服务：创建并持有唯一 ExoPlayer 与 MediaSession，在任务被移除、播放停止和服务销毁时按 Media3 生命周期释放资源。
- [ ] 运行 `./gradlew :core:video:testDebugUnitTest --tests '*VideoCacheKeyFactoryTest' :core:video:assembleDebug`，预期通过。
- [ ] 使用真机手工验证：播放同一 URL 两次，第二次网络读取明显减少且缓存超上限后旧内容被淘汰。
- [ ] 提交：`实现视频缓存与后台播放服务`。

## 任务 4：实现播放器控制器与内嵌 View

**文件：**
- 创建：`core/video/src/main/java/com/hash/video/player/VideoController.kt`
- 创建：`core/video/src/main/java/com/hash/video/ui/HashVideoPlayerView.kt`
- 创建：`core/video/src/main/res/layout/view_hash_video_player.xml`
- 创建：`core/video/src/main/res/drawable/*`
- 创建：`core/video/src/main/res/values/strings.xml`
- 创建：`core/video/src/test/java/com/hash/video/player/VideoControllerReducerBridgeTest.kt`

**接口：**
- 消费：任务 2 状态模型、任务 3 服务和缓存。
- 产出：`HashVideoPlayerView.setMedia(media)`、`play()`、`pause()`、`releaseHost()` 和只读 `playbackState`。

- [ ] 编写控制器状态桥接测试：接收到播放器缓冲/就绪/结束/错误事件时，输出状态必须符合任务 2 reducer。
- [ ] 运行测试，预期失败。
- [ ] 实现通过 `MediaController` 连接服务的 `VideoController`；View 只在 attach 时绑定控制器，detach 时清理 View 引用而不释放全局播放服务。
- [ ] 实现 `HashVideoPlayerView`：封面、缓冲、错误重试、播放控制、全屏和悬浮按钮；按钮文案、内容描述放资源文件。
- [ ] 运行 `./gradlew :core:video:testDebugUnitTest :core:video:assembleDebug`，预期通过。
- [ ] 使用真机手工验证：旋转、返回、页面重建后无旧 Activity 泄漏，且播放器不会出现两个声音源。
- [ ] 提交：`新增可复用的视频播放器视图`。

## 任务 5：实现全屏与应用内小窗迁移

**文件：**
- 创建：`core/video/src/main/java/com/hash/video/presentation/VideoPresentationCoordinator.kt`
- 创建：`core/video/src/main/java/com/hash/video/presentation/VideoFullscreenActivity.kt`
- 创建：`core/video/src/main/java/com/hash/video/presentation/InAppFloatHost.kt`
- 创建：`core/video/src/test/java/com/hash/video/presentation/VideoPresentationCoordinatorTest.kt`
- 创建：`core/video/src/androidTest/java/com/hash/video/presentation/VideoPresentationLifecycleTest.kt`

**接口：**
- 消费：任务 4 的 `HashVideoPlayerView` 与 `VideoController`。
- 产出：`moveTo(target: VideoPresentation)`、`restoreEmbedded()`；所有 Surface 迁移只能经协调器执行。

- [ ] 编写协调器单元测试，覆盖内嵌→全屏→内嵌、内嵌→应用内小窗→内嵌，以及迁移失败时旧宿主仍可播放。
- [ ] 运行单元测试，预期失败。
- [ ] 实现迁移顺序：保存旧宿主与位置、解绑旧 `PlayerView`、绑定新 `PlayerView`、最后更新状态；任何失败均回滚到旧宿主。
- [ ] 实现横屏全屏 Activity：强制横屏、隐藏控制外壳、返回时恢复原详情位置和系统栏状态。
- [ ] 实现应用内拖动小窗：添加到 Activity decor view，边界限制、关闭、恢复详情，不请求系统悬浮窗权限。
- [ ] 运行 `./gradlew :core:video:testDebugUnitTest :core:video:connectedDebugAndroidTest`，预期通过；在真机验证横屏返回和小窗拖动。
- [ ] 提交：`实现视频全屏与应用内小窗`。

## 任务 6：实现系统悬浮窗与回桌面策略

**文件：**
- 创建：`core/video/src/main/java/com/hash/video/presentation/SystemOverlayHost.kt`
- 创建：`core/video/src/main/java/com/hash/video/presentation/OverlayPermissionState.kt`
- 创建：`core/video/src/main/java/com/hash/video/player/VideoBackgroundPolicy.kt`
- 创建：`core/video/src/test/java/com/hash/video/presentation/OverlayPermissionPolicyTest.kt`
- 修改：`core/video/src/main/java/com/hash/video/presentation/VideoPresentationCoordinator.kt`

**接口：**
- 消费：任务 5 协调器。
- 产出：`overlayPermissionState(context)`、`requestOverlayPermission(activity)`、`setAutoEnterSystemOverlay(enabled)`。

- [ ] 编写权限策略测试：未授权时返回“需要授权”结果但不自动发起设置 Intent；授权后可迁移；关闭自动悬浮时进入后台不创建 overlay。
- [ ] 运行测试，预期失败。
- [ ] 实现 `TYPE_APPLICATION_OVERLAY` 宿主，支持拖动、关闭和回到应用；移除悬浮窗时必须清理 WindowManager 引用。
- [ ] 实现仅由用户点击触发的设置页引导，以及回桌面时基于用户开关和权限的自动迁移；无权限时保留后台媒体服务与通知，不显示视频窗口。
- [ ] 运行 `./gradlew :core:video:testDebugUnitTest :core:video:assembleDebug`，预期通过。
- [ ] 在 Android 12、Android 14+ 真机手工验证授权、拒绝授权、回桌面、从悬浮窗回应用、结束播放和进程重启。
- [ ] 提交：`支持系统悬浮视频播放`。

## 任务 7：接入新闻详情与清理外部播放逻辑

**文件：**
- 修改：`feature/home/build.gradle.kts`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/detail/NewsDetailActivity.kt`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/detail/adapter/NewsDetailAdapter.kt`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/detail/adapter/NewsDetailPageItem.kt`
- 修改：对应新闻详情视频 Item 布局
- 修改：`feature/home/src/main/res/values/strings.xml`
- 创建：`feature/home/src/test/java/com/hash/home/ui/featured/detail/NewsDetailVideoMapperTest.kt`

**接口：**
- 消费：任务 4 的 `HashVideoPlayerView`，任务 5、6 的展示控制。
- 产出：新闻详情视频条目在应用内播放，Adapter 不再创建外部播放 Intent。

- [ ] 编写 `NewsDetailVideoMapperTest`，断言接口的 `videoUrl`、封面和新闻标题可转换成 `VideoMedia`；空 URL 条目不生成可播放媒体。
- [ ] 运行测试，预期失败。
- [ ] 为视频条目布局嵌入 `HashVideoPlayerView`，将页面项绑定为 `VideoMedia`；删除 `NewsDetailActivity.openVideo()` 的 `ACTION_VIEW` 与相关不可用提示逻辑。
- [ ] 保持详情 RecyclerView 图片、文字和视频项的既有 Diff 与点击行为；视频播放/暂停不触发整页列表刷新。
- [ ] 运行 `./gradlew :feature:home:testDebugUnitTest :feature:home:assembleDebug`，预期通过。
- [ ] 真机验收：详情视频页内播放、横屏、应用内小窗、系统悬浮窗、回桌面和返回详情均保持同一播放进度。
- [ ] 提交：`新闻详情接入应用内视频播放`。

## 任务 8：回归、内存与发布验证

**文件：**
- 修改：必要的 ProGuard / Consumer ProGuard 文件（仅在 R8 验证发现 Media3 或反射问题时）
- 创建：`docs/video-manual-test-checklist.md`

**接口：**
- 消费：任务 1 至任务 7 的完整功能。
- 产出：可重复执行的真机验收清单和 Release 验证结果。

- [ ] 编写真机检查清单，覆盖网络切换、缓存满、无权限、授权拒绝、Activity 重建、全屏来回切换、后台、系统悬浮窗、音频焦点和进程重启。
- [ ] 运行 `./gradlew :app:assembleDebug :app:assembleRelease`，预期构建成功。
- [ ] 在 Release 包验证 R8 后可播放、缓存、前台服务通知和悬浮窗权限流程。
- [ ] 使用 LeakCanary 或 Android Studio Profiler 验证退出详情、关闭小窗、关闭系统悬浮窗后没有残留 Activity、View、WindowManager 或播放器实例。
- [ ] 提交：`完善视频播放发布验证`。

## 计划自检

- 详情页内播放、缓存、后台服务、横屏、两种小窗均有对应任务。
- 短视频流和弹幕只定义扩展边界，未预埋无效实现。
- 所有公开 API 在任务 2 定义，后续任务不引入业务模型或泄漏 ExoPlayer 类型。
- 权限、缓存故障、窗口迁移、Activity 重建和 Release/R8 均纳入测试或真机验证。
