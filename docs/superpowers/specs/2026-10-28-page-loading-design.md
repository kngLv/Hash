# 页面级 Loading 方案

## 目标

为页面提供可复用、状态驱动的 Loading 能力，减少 ViewModel 中重复的 `try/finally` 与页面中重复的 `showLoading`、`dismissLoading` 观察代码。网络模块不参与 Loading，也不依赖 Android UI。

## 范围与非目标

- 覆盖 Activity 全屏 Loading 与 Fragment 局部 Loading。
- 支持同一页面多个并发操作，避免先完成的操作提前关闭 Loading。
- 支持用户手动关闭后仅隐藏当前轮的界面，不取消网络请求。
- 不把 Loading 自动绑定到 `net` 请求；预加载、轮询等网络请求默认不会影响 UI。
- 不试图用一个 Loading 替代下拉刷新、加载更多、按钮提交状态或骨架屏。

## 方案比较

### 方案一：网络层自动 Loading

由 `net` 在每个请求生命周期内自动显示和关闭 Loading。

优点是调用短；缺点是网络层不知道请求所属页面、是否为静默请求、页面是否切换，也无法合理处理并发与用户手动关闭。因此不采用。

### 方案二：全局 Loading 调度中心

按应用全局请求计数，在当前前台 Activity 显示 Loading。

无需传递页面对象，但会把 A 页的请求显示到 B 页，也会让后台请求干扰当前用户操作；全局语义与绝大多数页面请求不匹配。因此不采用。

### 方案三：页面状态驱动（采用）

ViewModel 持有页面 Loading 状态；Activity 或 Fragment 基类根据该状态展示自己的 Loading。页面仍自行创建 ViewModel，基类不创建也不持有具体 ViewModel 类型。

优点是作用域明确、页面销毁安全、可测试，且可通过统一状态容器与基类观察减少模板代码。

## 结构与职责

```text
core:common
├─ base/action/LoadingAction
│  └─ showLoading / dismissLoading：仅表示 UI 展示能力
├─ loading/PageLoadingState
│  └─ 维护当前页面的活跃操作数与“本轮已被用户隐藏”状态
├─ loading/LoadingStateOwner
│  └─ ViewModel 暴露 LoadingState 的轻量契约
├─ base/activity/BaseAppActivity
│  └─ Activity 级 WaitDialog
├─ base/fragment/BaseAppFragment
│  └─ Fragment 根 View 的局部覆盖层
└─ base/*Binding*
   └─ 可选地观察页面声明的 LoadingStateOwner

feature:*
└─ ViewModel
   └─ 通过统一的 launchLoading 包装需要遮罩的页面操作
```

`InitAction` 只保留初始化流程；`showLoading`、`dismissLoading` 从其中移至 `LoadingAction`。

## ViewModel 侧

不要求所有 ViewModel 继承新的 `BaseViewModel`。先提供独立、可组合的页面 Loading 状态容器；需要时再由轻量基类或委托减少声明。

页面的 ViewModel 实现 `LoadingStateOwner`，并使用统一包装执行需要遮罩的操作：

```kotlin
fun submit() = launchLoading {
    repository.submit()
}
```

包装负责递增/递减活跃操作数，并在 `finally` 中回收，因此成功、错误、协程取消和业务回调抛异常都不会遗留 Loading。业务异常不在这里吞掉，仍维持原有传播或状态上报逻辑。

状态容器按页面实例隔离，而不是应用全局计数；多个操作并发时，仅当活跃数归零才结束本轮 Loading。

## 页面侧

页面仍使用现有的 `by viewModels()`、`activityViewModels()` 等方式创建自己的 ViewModel。基类不创建 ViewModel。

需要自动绑定的页面覆写一个可选的 Loading 状态来源；基类通过 `repeatOnLifecycle` 观察它：

```kotlin
override val loadingStateOwner: LoadingStateOwner
    get() = viewModel
```

没有 Loading 需求的页面无需声明，也不受影响。

## Activity 与 Fragment 的边界

- `BaseAppActivity` 实现 `LoadingAction`，沿用 `WaitDialog`，用于登录、全屏提交等 Activity 级场景。
- `BaseAppFragment` 实现 `LoadingAction`，仅在 Fragment 根布局之上添加覆盖层，不委托给宿主 Activity。
- Fragment 需要 Activity 级遮罩时必须显式调用宿主 Activity，例如 `(activity as? LoadingAction)?.showLoading()`；不提供自动透传。
- Fragment 覆盖层随 `viewLifecycleOwner` 销毁自动移除，不会覆盖相邻 Tab 或后续页面。

## 手动关闭规则

默认 Loading 可由用户关闭，但关闭只隐藏 UI，不取消请求或协程。

```text
当前页面仍有活跃操作
  → 用户关闭 Loading
  → 标记本轮为“已隐藏”，后续操作结束不会重新弹出
  → 活跃操作归零，结束本轮并清除隐藏标记
  → 下一轮操作允许正常展示
```

关键操作是否允许关闭由页面层在后续 API 中选择；该选择不下沉到网络层。

## 展示策略

- 默认延迟约 300ms 展示，快速完成的操作不显示。
- Activity 的 WaitDialog 与 Fragment 覆盖层都根据同一状态变化显示/隐藏。
- 页面进入非活跃或 Fragment View 销毁时停止 UI 观察并清理局部覆盖层。
- 下拉刷新、分页加载和按钮级提交优先使用各自的局部状态，不接入页面遮罩。

## 迁移顺序

1. 新增 `LoadingAction`，将 Loading 方法从 `InitAction` 移出，并保留 Activity 现有行为。
2. 新增页面 Loading 状态容器、ViewModel 操作包装和状态来源接口。
3. 为 `BaseBindingActivity`、`BaseBindingFragment` 增加可选状态观察；实现 Fragment 局部覆盖层。
4. 以 `LoginActivity` 为首个 Activity 示例，以首页一个 Fragment 为首个局部覆盖层示例。
5. 逐步迁移需要页面遮罩的交互；不批量改造无实际 Loading 需求的页面。

## 验证

- 同页两个并发操作：第一个完成后 Loading 仍保持，最后一个完成才关闭。
- 用户手动关闭：请求继续完成，但本轮不重新显示。
- Fragment Tab 切换与销毁：局部覆盖层不残留到其他 Fragment。
- Activity 级登录 Loading：保留现有行为。
- 请求失败、取消、回调异常：Loading 均能回收，异常原有行为不被吞掉。
