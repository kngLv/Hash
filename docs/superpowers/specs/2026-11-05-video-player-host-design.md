# 视频播放宿主重构设计

## 目标

将当前集播放控制、画面绑定、详情页按钮和 Loading 样式于一体的 `HashVideoPlayerView` 重构为抽象的 `BaseHashVideoPlayerView`。首期仅交付详情页使用的嵌入式实现；全屏、列表、小窗复用相同基础能力，但不提前创建没有实际行为的空壳类。

## 背景与问题

播放器由 `HashVideoPlaybackService` 持有，整个 App 只有一个 `ExoPlayer`。当前每一个已 attach 的 `HashVideoPlayerView` 都会自行将该 Player 绑定给自己的 `PlayerView`。当详情页和全屏页短暂共存时，两者会竞争同一个视频输出目标；旧 View 销毁时也可能干扰新宿主。

## 选定方案

采用继承式 UI 宿主设计：

```text
VideoController（唯一播放控制与状态）
        │
VideoPresentationCoordinator（唯一画面宿主与迁移）
        │
BaseHashVideoPlayerView（抽象：媒体、生命周期、状态回调、播放 API）
        └─ EmbeddedVideoPlayerView（首期：详情页控件与样式）
```

后续的 `ListVideoPlayerView`、`FullscreenVideoPlayerView`、`FloatingVideoPlayerView` 都继承 `BaseHashVideoPlayerView`，由协调器在宿主之间迁移同一个 Player 的画面。迁移使用 Media3 的 `PlayerView.switchTargetView`；不会重复 `prepare()` 或重新播放。

## BaseHashVideoPlayerView 职责

- 保有唯一的视频画面容器 `PlayerView`，供协调器受控绑定。
- 提供 `setMedia`、`play`、`pause` 等通用 API。
- 在 attach / detach 时向协调器登记或注销自身，而不是直接绑定 Player。
- 收集当前媒体的播放状态，并通过受保护回调交给子类渲染 UI。
- 不创建 Loading、播放、全屏、小窗等任何场景控件，不决定布局或样式。

## VideoPresentationCoordinator 职责

- 维护当前实际接收视频画面的 `BaseHashVideoPlayerView`。
- 首个可见并登记的宿主成为画面目标。
- 新宿主显式请求接管时，通过 `PlayerView.switchTargetView` 原子迁移画面。
- 已不是当前目标的旧宿主 detach 时不得解绑当前宿主。
- 仅协调画面输出，不持有播放业务状态、不触发 `play()`。

## 首期范围

- 将现有详情页样式迁移为 `EmbeddedVideoPlayerView`。
- 保持现有 `setMedia`、`play`、`pause`、播放 / 暂停 / Loading / 重试按钮行为。
- 消除直接在 View attach 时绑定 Player 的行为。
- 为后续全屏、小窗、列表保留受控的“请求接管画面”入口。

## 非目标

- 本期不创建全屏 Activity、系统悬浮窗或小窗拖动逻辑。
- 本期不新增列表自动播放策略。
- 本期不修改 `VideoController` 的单播放器和播放状态职责。
