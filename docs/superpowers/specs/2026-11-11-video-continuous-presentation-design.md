# 视频连续呈现与横竖屏播放设计

## 背景与目标

当前视频模块在内嵌播放器进入全屏时启动 `VideoFullscreenActivity`。虽然全局只有一个
`ExoPlayer`，音频通常不会中断，但新 Activity 的窗口创建、布局及首帧输出会造成画面突然
切换或短暂黑屏；返回详情页时也可能出现同样的问题。

本次改造的目标是：

- 内嵌视频进入横屏时，画面以旋转、放大动画连续过渡，播放不中断。
- 横屏返回内嵌时，以反向动画连续过渡，播放不中断。
- 支持任意页面直接进入横屏播放，无需预先存在内嵌播放器。
- 支持竖屏全屏视频流，并为后续上下切换视频提供基础。
- 保持全局只有一个真实播放器，避免多个视频同时占用解码与网络资源。

不在本次范围：应用外悬浮窗、画中画、弹幕、清晰度切换和完整的推荐视频流数据源。

## 已确认的产品规则

- 横屏与竖屏都使用视频覆盖层，而不是默认跳转独立播放 Activity。
- 竖屏视频流退出后，详情页不跟随竖屏当前视频。
- 若竖屏流已切换到其他视频，退出时详情页恢复进入竖屏前的原视频及其保存的进度，并按进入前
  的播放状态继续或暂停。

最后一条意味着：竖屏流已经切换到另一条视频后，详情原视频无法在后台同时继续播放，因为模块
只有一个 `ExoPlayer`。退出竖屏时需要将播放器切回原视频，因此该场景只能做平滑淡入，不能把
“另一条视频的画面”连续变形为详情原视频的画面。未切换视频时则可以完整执行连续回迁动画。

## 方案比较

### 方案一：保留独立全屏 Activity

通过共享元素或截图模拟内嵌与全屏之间的动画。

- 优点：横屏页面拥有独立返回栈，实现直观。
- 缺点：真实视频输出跨窗口迁移仍有首帧与 Surface 切换间隙；截图无法可靠覆盖
  `SurfaceView`，不能稳定实现连续画面。

不采用。

### 方案二：多个 PlayerView 之间使用 switchTargetView

保留当前每个播放器 View 自带 `PlayerView` 的结构，只优化迁移时机。

- 优点：改动较小。
- 缺点：仍然是旧目标停止输出、新目标开始输出，无法让“同一画面”做位置、尺寸与旋转动画。

不采用。

### 方案三：唯一可迁移 TextureView 渲染视图与同 Activity 覆盖层

`VideoPresentationCoordinator` 持有唯一的 `PlayerView`，其内部使用 `TextureView` 渲染；
各个具体播放器只提供画面插槽。呈现切换时，将同一个 `PlayerView` 在插槽和覆盖层之间重新挂载。

- 优点：视频输出对象不变，可对同一画面执行缩放、平移、圆角及旋转动画；不依赖跨窗口首帧；
  横屏、竖屏流、小窗可复用同一个呈现能力。
- 代价：`TextureView` 比默认 `SurfaceView` 多一层 GPU 合成；需要重构现有多 `PlayerView` 的
  呈现协调代码。

采用本方案。

## 核心结构

```text
VideoController / MediaSession / ExoPlayer（唯一）
                 │
                 ▼
VideoRenderCoordinator
├── 唯一 PlayerView（TextureView）
├── 当前画面插槽
└── 过渡状态与回迁信息

VideoPresentationHost（安装到当前 Activity 根视图）
├── 内嵌画面插槽
├── 转场层 VideoTransitionLayer
├── 横屏覆盖层 LandscapeVideoOverlay
└── 竖屏覆盖层 PortraitVideoFeedOverlay
```

### 画面插槽

`BaseHashVideoPlayerView` 不再创建或持有自己的 `PlayerView`，改为提供一个内部
`VideoRenderSlot`。它只负责：

- 告知协调器自己是否可承载画面；
- 提供用于回迁的窗口坐标、尺寸与圆角；
- 渲染各自的控制栏、Loading、错误和手势 UI。

只有 `VideoRenderCoordinator` 可以把唯一的 `PlayerView` 添加到或移出插槽。

### 渲染视图

唯一 `PlayerView` 必须使用 `TextureView`。原因是 `TextureView` 支持常规 View 的 alpha、平移、
缩放、旋转和裁剪动画；默认 `SurfaceView` 不适合作为这类连续转场的动画对象。

## 横屏呈现流程

### 从内嵌进入横屏

1. 内嵌控件请求 `LANDSCAPE` 呈现，并提供自己的画面插槽作为来源。
2. 协调器记录来源媒体、当前进度、播放状态、来源插槽弱引用及其窗口矩形。
3. 将同一个 `PlayerView` 从来源插槽移入根布局的转场层，初始位置、尺寸和圆角与来源完全一致。
4. 请求横屏。视频承载 Activity 必须处理 `orientation|screenSize` 配置变化，避免自身被重建。
5. 配置变化完成且横屏覆盖层布局可用后，`PlayerView` 在转场层中旋转、放大到目标区域；控制栏
   在动画后段淡入。
6. 动画结束，将 `PlayerView` 挂入横屏覆盖层的插槽。

### 从横屏返回内嵌

1. 若来源详情页的插槽仍附着、媒体仍一致，则将唯一 `PlayerView` 放回转场层。
2. 恢复竖屏；待原页面完成新尺寸布局后，读取来源插槽的新窗口矩形。
3. 执行旋转、缩小和圆角恢复动画。
4. 动画结束，将 `PlayerView` 放回详情页插槽，恢复系统栏。
5. 若来源插槽已销毁或不再匹配媒体，则关闭横屏覆盖层并以淡出收尾，不强行回迁。

整个过程中不重新 `prepare` 媒体，也不重新绑定另一个 `PlayerView`，因此视频和音频可以连续。

### 直接进入横屏

新增统一入口，来源插槽可选：

```kotlin
HashVideo.present(
    activity = activity,
    media = media,
    mode = VideoPresentationMode.LANDSCAPE,
    sourceSlot = null,
)
```

没有来源插槽时，播放器直接挂入横屏覆盖层并淡入；存在来源插槽时才执行共享位置动画。

## 竖屏视频流

`PortraitVideoFeedOverlay` 是同 Activity 的全屏覆盖层，内部使用垂直 `ViewPager2`：

- 当前页提供可接管唯一 `PlayerView` 的画面插槽；
- 相邻页只显示封面，避免每个条目创建播放器；
- 切页后协调器将唯一画面迁移到新当前页，并由 `VideoController` 切换媒体；
- 初次从详情进入时，当前视频从详情插槽放大至竖屏全屏区域。

退出规则：

- 当前仍是原视频：将同一个 `PlayerView` 动画回迁到详情插槽，连续播放。
- 当前已切换为其他视频：保存当前条目状态，恢复进入竖屏前记录的原视频与进度；详情页面拿回
  原视频画面。媒体已发生切换，无法进行真实画面回迁，使用淡入过渡而非伪造连续动画。

## 生命周期与系统方向

- `VideoPresentationHost` 由调用 `HashVideo.present` 的 `ComponentActivity` 按需安装到其根视图。
- Host 在 Activity 销毁时取消动画、释放引用，但不直接释放全局播放器；播放器释放仍由现有播放
  会话规则决定。
- 横屏期间 Host 保存原始方向；退出时恢复 `SCREEN_ORIENTATION_UNSPECIFIED`。
- 需要支持横屏视频的 Activity 在 Manifest 中声明处理 `orientation|screenSize`，并将
  `onConfigurationChanged` 交由 Host 重算覆盖层和回迁目标。
- 返回键优先关闭当前视频覆盖层；没有覆盖层时交还 Activity 默认返回逻辑。

## API 草案

```kotlin
enum class VideoPresentationMode {
    LANDSCAPE,
    PORTRAIT_FEED,
}

data class VideoPresentationRequest(
    val media: VideoMedia,
    val mode: VideoPresentationMode,
    val playlist: List<VideoMedia> = listOf(media),
    val sourceSlot: VideoRenderSlot? = null,
)

object HashVideo {
    fun present(activity: ComponentActivity, request: VideoPresentationRequest)
    fun dismissPresentation(activity: ComponentActivity)
}
```

`EmbeddedVideoPlayerView` 对外仍保留 `enterFullscreen()`，内部改为构造 `LANDSCAPE` 请求。业务页如需
直接打开大屏或竖屏流，调用 `HashVideo.present(...)` 即可。

## 分步实施

1. 重构渲染所有权：唯一 TextureView `PlayerView`、画面插槽和新的协调器单元测试。
2. 建立 `VideoPresentationHost` 与横屏覆盖层，先实现无动画的连续接管和返回。
3. 加入内嵌与横屏之间的转场层、尺寸/圆角/旋转动画以及方向处理。
4. 将新闻详情的全屏入口接入 Host，移除默认 `VideoFullscreenActivity` 路径。
5. 增加直接横屏播放 API。
6. 实现竖屏单视频覆盖层。
7. 实现竖屏 `ViewPager2` 视频流及“切换其他视频后恢复详情原视频”的规则。
8. 覆盖单元测试、界面测试和真机验收。

## 验收标准

- 内嵌进入横屏和返回内嵌时，声音和视频时间轴连续，不出现黑屏或重新缓冲。
- 过渡中的视频画面随容器连续缩放和旋转，而非突然替换为另一个画面。
- 直接打开横屏可正常播放指定媒体。
- 竖屏流切页时只有当前页持有真实画面输出。
- 竖屏未切换媒体时退出可连续回迁；切换媒体后退出能恢复详情原视频与保存进度。
- Activity 旋转不触发详情接口重复请求，也不销毁视频播放会话。
