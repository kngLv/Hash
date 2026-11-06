# 视频播放状态重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 让视频控制器以单一状态流处理媒体切换、恢复、加载与播放结果，并让播放器 View 依据真实状态渲染。

**架构：** `VideoController` 直接把 Media3 状态映射为 `VideoPlaybackState`，移除内部事件和 Reducer 中转。`HashVideoPlayerView` 不再自动重播，改为绑定画面并观察控制器状态；同一媒体的播放请求由控制器决定恢复、重播或忽略。

**技术栈：** Kotlin、Coroutines StateFlow、AndroidX Media3、JUnit、Android Instrumentation Test。

**方案：** `docs/superpowers/specs/2026-11-05-video-playback-state.md`

## 全局约束

- `HashVideoPlaybackService` 仍是唯一 ExoPlayer 所有者。
- 不向业务模块暴露 Media3 的 `Player`、`MediaController` 或缓存实现。
- View 不得在 attach 时自动重播媒体。
- 不实现全屏、小窗、系统悬浮窗或列表自动播放。
- 本次不提交代码。

## 审查重点

- 同一视频暂停后再次调用 `play(media)` 必须恢复而非重新设置媒体。
- 结束视频再次调用 `play(media)` 必须从开头重播。
- 加载、缓冲、播放、暂停、结束与错误状态必须可预测地映射。
- 已 detach 的播放器 View 不得继续收集状态或持有 View 生命周期引用。
- 非当前媒体的状态更新不得错误显示为当前 View 的 loading 或暂停状态。

---

### Task 1: 简化播放状态模型

**文件：**
- Create: `core/video/src/main/java/com/hash/video/api/VideoPlaybackStatusExt.kt`
- Modify: `core/video/src/main/java/com/hash/video/player/VideoPlayerEventMapper.kt`
- Delete: `core/video/src/main/java/com/hash/video/api/VideoPlaybackEvent.kt`（如定义仍在状态文件中则拆除）
- Delete: `core/video/src/main/java/com/hash/video/player/VideoPlaybackReducer.kt`
- Modify: `core/video/src/test/java/com/hash/video/player/VideoPlaybackReducerTest.kt`
- Modify: `core/video/src/test/java/com/hash/video/player/VideoPlayerEventMapperTest.kt`

**接口：**
- 产出：`VideoPlaybackStatus.isLoading`、`isPlaying`、`isPaused`、`isFinished`。
- 产出：`VideoPlayerEventMapper.from(playbackState: Int, isPlaying: Boolean): VideoPlaybackStatus`。

- [ ] 编写失败测试，覆盖状态扩展和 `STATE_IDLE`、缓冲、就绪、结束的准确映射。
- [ ] 运行测试，确认现有事件映射无法满足新的状态返回类型。
- [ ] 实现状态扩展与状态映射，移除事件/Reducer 及其旧测试。
- [ ] 运行视频模块单元测试，确认通过。

### Task 2: 重构控制器播放语义

**文件：**
- Modify: `core/video/src/main/java/com/hash/video/player/VideoController.kt`
- Test: `core/video/src/test/java/com/hash/video/player/VideoPlaybackReducerTest.kt`（替换为控制器可测试的播放决策测试）

**接口：**
- 消费：Task 1 的 `VideoPlaybackStatus` 扩展与状态映射。
- 产出：`VideoController.play(media)` 的“确保正在播放”语义，以及 `StateFlow<VideoPlaybackState>` 的直接更新。

- [ ] 编写失败测试，覆盖同媒体暂停恢复、加载时重复请求忽略、结束后重播、不同媒体切换四种决策。
- [ ] 运行测试，确认现有 `play(media)` 总是切换媒体，测试失败。
- [ ] 将播放决策收敛到可单测的内部逻辑；控制器直接更新状态，并将 `withPlayer` 收窄为模块内部能力。
- [ ] 运行视频模块单元测试，确认通过。

### Task 3: 状态驱动播放器 View

**文件：**
- Modify: `core/video/src/main/java/com/hash/video/ui/HashVideoPlayerView.kt`
- Modify: `core/video/src/main/res/values/strings.xml`
- Modify: `core/video/src/androidTest/java/com/hash/video/ui/HashVideoPlayerViewTest.kt`

**接口：**
- 消费：Task 1 的状态扩展与 Task 2 的控制器语义。
- 产出：显式 `play(media)`、真实状态渲染、attach/detach 生命周期安全的收集逻辑。

- [ ] 编写失败 instrumentation 测试，覆盖 View attach 不自动播放、loading 状态展示与 detach 后停止收集。
- [ ] 在连接设备上运行测试，确认当前 View 缺少状态渲染而失败。
- [ ] 添加 loading View，使用生命周期安全的收集更新按钮与 loading；移除点击时直接修改 UI、attach 时自动播放和对外 `resume()` 依赖。
- [ ] 运行 `core:video` 单元测试与 instrumentation 测试，确认通过。

### Task 4: 清理与整体验证

**文件：**
- Modify: 受上述任务影响的测试与导入。

- [ ] 检查并删除事件/Reducer 的全部引用。
- [ ] 执行 `git diff --check`。
- [ ] 在可用 JDK 21 与设备环境执行 `:core:video:testDebugUnitTest`、`:core:video:connectedDebugAndroidTest`。
