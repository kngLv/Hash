# 视频播放宿主重构 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立可在嵌入式、列表、全屏和小窗之间安全迁移同一播放器画面的抽象播放宿主，首期完成详情页嵌入式实现。

**Architecture:** `VideoController` 继续持有唯一播放会话；新增 `VideoPresentationCoordinator` 独占视频画面绑定与迁移。抽象 `BaseHashVideoPlayerView` 提供媒体和播放 API、生命周期登记及状态分发，`EmbeddedVideoPlayerView` 承载详情页现有控件。

**Tech Stack:** Kotlin、Android View、Media3 PlayerView、StateFlow、JUnit、AndroidX 测试。

**Spec:** `docs/superpowers/specs/2026-11-05-video-player-host-design.md`

## Global Constraints

- 全 App 仅复用 `HashVideoPlaybackService` 中的一个 ExoPlayer。
- View 之间迁移画面时不得重新 `prepare()` 或触发重复播放请求。
- 首期只实现嵌入式详情页 UI，不创建未使用的全屏、列表、小窗空壳类。
- 保持现有详情页播放按钮、Loading、全屏和悬浮入口行为。

## Review Focus

- 新宿主接管画面时，旧宿主仍 attach 不得继续持有 Player。
- 旧宿主在迁移后 detach 不得清空新宿主的画面。
- 首个 attach 的嵌入式宿主应正常展示已在播放的视频。
- 非当前媒体的列表宿主不得显示全局视频的 Loading 或暂停状态。
- 未连接 MediaController 时的宿主登记不得导致崩溃或重复绑定。

---

### Task 1: 画面宿主协调器

**Files:**
- Create: `core/video/src/main/java/com/hash/video/ui/VideoPresentationCoordinator.kt`
- Modify: `core/video/src/main/java/com/hash/video/player/VideoController.kt`
- Test: `core/video/src/test/java/com/hash/video/ui/VideoPresentationCoordinatorTest.kt`

**Interfaces:**
- Consumes: `VideoController.withPlayer(onReady: (Player) -> Unit)`。
- Produces: `register(host)`, `unregister(host)`, `takeOver(host)`；协调器仅从 `BaseHashVideoPlayerView` 接收宿主。

- [ ] **Step 1: 写出协调器的失败测试**

覆盖首个宿主绑定、接管时切换目标、旧宿主注销不影响新宿主三个场景。

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:video:testDebugUnitTest --tests '*VideoPresentationCoordinatorTest'`
Expected: FAIL，因为协调器尚不存在。

- [ ] **Step 3: 实现协调器与必要的受控 Player 获取接口**

协调器维护当前宿主，以 `PlayerView.switchTargetView` 完成迁移；不触发播放器业务操作。

- [ ] **Step 4: 运行测试确认通过**

Run: `./gradlew :core:video:testDebugUnitTest --tests '*VideoPresentationCoordinatorTest'`
Expected: PASS。

### Task 2: 抽象播放宿主

**Files:**
- Create: `core/video/src/main/java/com/hash/video/ui/BaseHashVideoPlayerView.kt`
- Modify: `core/video/src/main/java/com/hash/video/ui/HashVideoPlayerView.kt`
- Test: `core/video/src/androidTest/java/com/hash/video/ui/BaseHashVideoPlayerViewTest.kt`

**Interfaces:**
- Consumes: `VideoPresentationCoordinator.register/unregister/takeOver` 与 `VideoController.play/pause/playbackState`。
- Produces: `BaseHashVideoPlayerView`、`EmbeddedVideoPlayerView`；基类公开 `setMedia`、`play`、`pause`，向子类提供播放状态渲染回调。

- [ ] **Step 1: 写出嵌入式宿主的失败测试**

验证绑定媒体后状态只影响当前媒体，Preparing/Buffering 显示 Loading，Playing 显示暂停入口。

- [ ] **Step 2: 运行测试确认失败**

Run: `./gradlew :core:video:assembleDebugAndroidTest`
Expected: FAIL，因为抽象宿主和嵌入式实现尚不存在。

- [ ] **Step 3: 实现基类并迁移当前详情页 UI**

将当前 `HashVideoPlayerView` 更名为 `EmbeddedVideoPlayerView`，控件渲染留在该子类；基类不含场景按钮和样式。

- [ ] **Step 4: 运行 Android 测试编译确认通过**

Run: `./gradlew :core:video:assembleDebugAndroidTest`
Expected: BUILD SUCCESSFUL。

### Task 3: 接入与回归验证

**Files:**
- Modify: `feature/home/**` 中详情页对旧 `HashVideoPlayerView` 的引用。
- Modify: `core/video/src/test/java/com/hash/video/**` 中旧 View 名称引用。

**Interfaces:**
- Consumes: `EmbeddedVideoPlayerView` 的既有播放 API。
- Produces: 详情页使用新的嵌入式宿主，无旧类引用。

- [ ] **Step 1: 更新详情页布局与调用代码**

将类型替换为 `EmbeddedVideoPlayerView`，保持原有播放入口和全屏、悬浮回调。

- [ ] **Step 2: 执行模块回归验证**

Run: `./gradlew :core:video:testDebugUnitTest :core:video:assembleDebugAndroidTest :feature:home:assembleDebug`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 检查变更质量**

Run: `git diff --check`
Expected: 无输出。
