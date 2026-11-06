# 视频播放状态重构方案

## 目标

重构 `core:video` 的播放控制与播放器 View，使“切换视频”“恢复播放”“加载中”“播放结束”和“播放失败”拥有明确且唯一的状态来源；页面只根据真实播放状态渲染 UI。

同时保持一个全局播放会话，为后续详情页、横屏全屏、应用内小窗、系统悬浮窗与短视频列表复用同一个 ExoPlayer 做准备。

## 当前问题

- `HashVideoPlayerView` 只在点击时修改按钮文本，无法反映系统媒体控制、缓冲、错误或播放结束。
- `play(media)` 同时表示切换媒体和恢复播放；同一视频暂停后再次点击会重新设置 `MediaItem` 并从头准备。
- View 每次重新 attach 会重新调用 `play(media)`，可能导致非预期重播。
- `VideoPlaybackEvent`、Reducer 和 Mapper 仅作为内部中转，当前没有独立事件流消费者，增加了理解成本。

## 方案选择

### 保留事件与 Reducer

修正现有事件流并让 View 观察状态。改动较小，但单播放器场景仍需理解事件、Reducer 和状态三层概念。

### 采用状态驱动的控制器（采用）

删除仅作内部中转的 `VideoPlaybackEvent`、`VideoPlaybackReducer`，由 `VideoController` 将 Media3 回调直接收敛为 `VideoPlaybackState`。控制器负责判断同一媒体的恢复、重播和切换，View 只发起意图并渲染状态。

该方案保留一个全局播放会话，且不会将详情页或列表策略放入播放器内核；未来列表层只需决定哪个 `VideoMedia` 调用 `controller.play(media)`。

### 引入完整播放命令和宿主协调器

可严格管理多个展示容器，但当前没有列表、全屏或小窗的实际实现，过早引入会增加空抽象，因此不采用。

## API 与状态规则

`VideoController` 对业务层提供：

```kotlin
fun play(media: VideoMedia)
fun pause()
val playbackState: StateFlow<VideoPlaybackState>
```

`play(media)` 表示“确保该媒体正在播放”：

- 当前不是该媒体：替换 `MediaItem`，状态进入 `PREPARING`，然后 prepare 并播放。
- 当前是该媒体且暂停：直接恢复，不重新设置 `MediaItem`。
- 当前是该媒体且正在准备、缓冲或播放：忽略重复请求。
- 当前是该媒体且结束：回到开头并重播。
- 当前是该媒体且出错：重新 prepare 并播放。

`pause()` 只请求实际播放器暂停；最终状态由 Media3 回调确认。

为 `VideoPlaybackStatus` 提供少量只读扩展：`isLoading`、`isPlaying`、`isPaused`、`isFinished`。不提供模糊的 `canPlay` 等组合判断。

## View 规则

`HashVideoPlayerView` 仅负责：

- attach 时绑定 `PlayerView` 到全局 `MediaController`，detach 时解绑画面；不自动开始或重播。
- 页面显式调用 `play(media)` 时保存当前媒体并请求控制器播放。
- 观察 `playbackState`，且只渲染与自身当前媒体匹配的状态。
- `PREPARING`、`BUFFERING` 显示居中 loading；`PLAYING` 显示暂停；`PAUSED`、`ENDED`、`ERROR` 显示播放或重试。

View 的状态收集在 attach 时启动、detach 时取消，避免已移除 View 持有 Activity 或接收无效更新。

## 扩展边界

- `HashVideoPlaybackService` 继续是唯一 ExoPlayer 的所有者。
- 后续全屏和小窗只迁移 PlayerView 绑定，不创建播放器或改变播放状态语义。
- 后续短视频列表由独立的列表可见性策略决定当前播放的 `VideoMedia`；它复用 `play(media)` 与 `playbackState`，不需要为每个 item 创建播放器。
- 同一时间只能有一个实际视频输出宿主；宿主切换协调器在全屏、小窗或列表功能实际进入开发时新增，当前不预留空实现。

## 非目标

- 本次不实现全屏、小窗、系统悬浮窗或短视频列表。
- 本次不改变缓存、媒体服务或网络请求头透传策略。
- 本次不提供跨页面播放队列。
