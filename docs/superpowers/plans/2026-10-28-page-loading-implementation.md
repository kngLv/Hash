# 页面级 Loading 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 Activity 和 Fragment 提供与页面生命周期绑定的、状态驱动的 Loading 能力，同时消除业务 ViewModel 中重复的展示状态收尾代码。

**Architecture:** `core:common` 新增纯状态的页面 Loading 容器及轻量 `BaseViewModel`，由该基类提供 `launchLoading`。`BaseAppActivity` 和 `BaseAppFragment` 通过可选的 `LoadingStateOwner` 观察状态；前者展示现有 `WaitDialog`，后者在自身根布局展示局部覆盖层。网络模块保持无感知。

**Tech Stack:** Kotlin、Kotlin Coroutines、StateFlow、AndroidX Lifecycle、Data Binding、JUnit 4。

**Spec:** `docs/superpowers/specs/2026-10-28-page-loading-design.md`

## 全局约束

- `net` 不引用 Loading、Activity、Dialog 或任何 Android UI 类型。
- 页面继续通过 `by viewModels()` 等既有方式创建 ViewModel；基类不创建具体 ViewModel。
- Fragment Loading 只遮住自身根布局，不隐式委托到宿主 Activity。
- 用户手动关闭仅隐藏本轮 UI，不取消请求或协程。
- 默认展示延迟为 300ms；快速操作不显示 Loading。
- 下拉刷新、分页与按钮级 Loading 不接入页面遮罩。

## 审查重点

- 两个并发操作：第一个结束后必须保持 Loading，最后一个结束才关闭（任务 1）。
- 延迟展示期间操作结束：不得产生短暂闪烁（任务 1、3、4）。
- 用户手动关闭后仍有操作：请求完成前不得再次展示（任务 1）。
- Fragment 切换或销毁 View：覆盖层不得保留到其他 Tab 或 Activity（任务 4）。
- 协程取消和异常：`finally` 必须回收状态且不吞掉原始异常（任务 2）。

---

### 任务 1：页面 Loading 状态容器

**文件：**
- 新建：`core/common/src/main/java/com/hash/common/loading/PageLoadingState.kt`
- 新建：`core/common/src/test/java/com/hash/common/loading/PageLoadingStateTest.kt`

**接口：**
- 产出：`class PageLoadingState`，暴露 `StateFlow<PageLoadingUiState>`、`begin(): LoadingHandle`、`dismissByUser()`。
- 产出：`interface LoadingHandle { fun finish() }`，重复调用 `finish()` 不重复扣减计数。
- 产出：`data class PageLoadingUiState(val isVisible: Boolean, val isDismissible: Boolean)`。

- [ ] **步骤 1：编写并发与手动关闭的失败测试**

```kotlin
@Test fun first_handle_finish_keeps_loading_visible() { /* 两个 handle，仅结束一个 */ }
@Test fun user_dismiss_hides_current_round_until_all_handles_finish() { /* 本轮不重显 */ }
@Test fun finishing_same_handle_twice_does_not_close_other_operation() { /* 幂等 */ }
```

- [ ] **步骤 2：运行单元测试，确认因类型不存在而失败**

运行：`./gradlew :core:common:testDebugUnitTest --tests '*PageLoadingStateTest'`

预期：失败，提示 `PageLoadingState` 不存在。

- [ ] **步骤 3：实现 `PageLoadingState`**

状态由活跃 handle 数量和“当前轮已手动隐藏”标记推导；当最后一个 handle 结束时清除手动隐藏标记。状态容器仅使用 Kotlin/Coroutines，不依赖 Android View 或网络层。

- [ ] **步骤 4：运行状态容器测试**

运行：`./gradlew :core:common:testDebugUnitTest --tests '*PageLoadingStateTest'`

预期：全部通过。

- [ ] **步骤 5：提交状态容器**

```bash
git add core/common/src/main/java/com/hash/common/loading/PageLoadingState.kt core/common/src/test/java/com/hash/common/loading/PageLoadingStateTest.kt
git commit -m "新增页面加载状态容器"
```

### 任务 2：ViewModel 请求包装与 Loading 契约

**文件：**
- 新建：`core/common/src/main/java/com/hash/common/base/viewmodel/BaseViewModel.kt`
- 新建：`core/common/src/main/java/com/hash/common/loading/LoadingStateOwner.kt`
- 新建：`core/common/src/test/java/com/hash/common/base/viewmodel/BaseViewModelTest.kt`

**接口：**
- 产出：`interface LoadingStateOwner { val loadingState: PageLoadingState }`。
- 产出：`abstract class BaseViewModel : ViewModel(), LoadingStateOwner`。
- 产出：`protected fun launchLoading(block: suspend CoroutineScope.() -> Unit): Job`。

- [ ] **步骤 1：编写异常和取消时回收状态的失败测试**

```kotlin
@Test fun exception_finishes_loading_and_is_not_swallowed() { /* 异常继续传播到 Job */ }
@Test fun cancellation_finishes_loading() { /* cancel 后状态归零 */ }
```

- [ ] **步骤 2：运行测试，确认失败**

运行：`./gradlew :core:common:testDebugUnitTest --tests '*BaseViewModelTest'`

预期：失败，提示 `BaseViewModel` 不存在。

- [ ] **步骤 3：实现 `BaseViewModel.launchLoading`**

在 `viewModelScope` 启动协程并在 `finally` 调用本次 `LoadingHandle.finish()`；不捕获并转换异常。`loadingState` 作为每个 ViewModel 独立实例，避免跨页面共享。

- [ ] **步骤 4：运行 ViewModel 测试**

运行：`./gradlew :core:common:testDebugUnitTest --tests '*BaseViewModelTest'`

预期：全部通过。

- [ ] **步骤 5：提交 ViewModel 基础设施**

```bash
git add core/common/src/main/java/com/hash/common/base/viewmodel/BaseViewModel.kt core/common/src/main/java/com/hash/common/loading/LoadingStateOwner.kt core/common/src/test/java/com/hash/common/base/viewmodel/BaseViewModelTest.kt
git commit -m "新增页面加载 ViewModel 基础能力"
```

### 任务 3：拆分 UI Loading Action 并接入 Activity

**文件：**
- 新建：`core/common/src/main/java/com/hash/common/base/action/LoadingAction.kt`
- 修改：`core/common/src/main/java/com/hash/common/base/InitAction.kt`
- 修改：`core/common/src/main/java/com/hash/common/base/activity/BaseAppActivity.kt`

**接口：**
- 产出：`interface LoadingAction { fun showLoading(); fun dismissLoading() }`。
- 消费：`LoadingStateOwner.loadingState`。
- 产出：`protected open val loadingStateOwner: LoadingStateOwner? = null`。

- [ ] **步骤 1：将 `showLoading` 与 `dismissLoading` 从 `InitAction` 移至 `LoadingAction`**

`BaseAppActivity` 实现 `LoadingAction`；保持既有 `WaitDialog`、300ms 延迟与同 Activity 计数行为，避免改动当前登录交互。

- [ ] **步骤 2：在 `BaseAppActivity` 中观察可选状态来源**

以 `repeatOnLifecycle(Lifecycle.State.STARTED)` 收集 `loadingStateOwner`；仅在 `isVisible` 变化时显示或关闭 Dialog。页面未覆写状态来源时不注册观察。

- [ ] **步骤 3：编译 common 与登录模块**

运行：`./gradlew :core:common:compileDebugKotlin :feature:login:compileDebugKotlin`

预期：通过；`LoginActivity` 现有显式调用无行为变化。

- [ ] **步骤 4：提交 Action 与 Activity 适配**

```bash
git add core/common/src/main/java/com/hash/common/base/action/LoadingAction.kt core/common/src/main/java/com/hash/common/base/InitAction.kt core/common/src/main/java/com/hash/common/base/activity/BaseAppActivity.kt
git commit -m "拆分加载界面能力"
```

### 任务 4：实现 Fragment 局部覆盖层与最小接入示例

**文件：**
- 新建：`core/common/src/main/java/com/hash/common/loading/FragmentLoadingOverlay.kt`
- 修改：`core/common/src/main/java/com/hash/common/base/fragment/BaseAppFragment.kt`
- 修改：`core/common/src/main/java/com/hash/common/base/fragment/BaseBindingFragment.kt`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/vm/FeaturedTabViewModel.kt`
- 修改：`feature/home/src/main/java/com/hash/home/ui/featured/FeaturedTabFragment.kt`

**接口：**
- 消费：`LoadingAction`、`LoadingStateOwner`、`PageLoadingState`。
- 产出：`BaseAppFragment.showLoading()` 与 `dismissLoading()`，仅操作本 Fragment 根 View 的覆盖层。
- 产出：`BaseBindingFragment` 中可选的 `loadingStateOwner` 观察入口。

- [ ] **步骤 1：实现可移除的 `FragmentLoadingOverlay`**

使用根 `ViewGroup` 添加全尺寸遮罩与居中进度指示；`show` 幂等，`dismiss` 移除 View。若根布局不是 `ViewGroup`，安全降级为空操作并保留日志提示，不崩溃。

- [ ] **步骤 2：让 `BaseAppFragment` 实现局部 `LoadingAction`**

在 `onViewCreated` 建立状态收集，在 `onDestroyView` 无条件移除覆盖层。不得调用宿主 Activity 的 `showLoading` 或 `dismissLoading`。

- [ ] **步骤 3：将 `FeaturedTabViewModel` 迁移为 `BaseViewModel`，并让 `FeaturedTabFragment` 声明其 Loading 状态来源**

仅将首次加载/用户主动刷新中需要页面遮罩的操作包装为 `launchLoading`；保持缓存先显示、刷新与分页的既有逻辑不变，分页不展示页面遮罩。

- [ ] **步骤 4：编译首页模块并运行现有首页单元测试**

运行：`./gradlew :core:common:testDebugUnitTest :feature:home:testDebugUnitTest :feature:home:compileDebugKotlin`

预期：通过。

- [ ] **步骤 5：手工验证 Fragment 作用域**

验证：首次加载显示局部遮罩；切换一级/二级 Tab 后遮罩不覆盖其他 Fragment；刷新/分页行为与改造前一致。

- [ ] **步骤 6：提交 Fragment 接入**

```bash
git add core/common/src/main/java/com/hash/common/loading/FragmentLoadingOverlay.kt core/common/src/main/java/com/hash/common/base/fragment/BaseAppFragment.kt core/common/src/main/java/com/hash/common/base/fragment/BaseBindingFragment.kt feature/home/src/main/java/com/hash/home/ui/featured/vm/FeaturedTabViewModel.kt feature/home/src/main/java/com/hash/home/ui/featured/FeaturedTabFragment.kt
git commit -m "支持 Fragment 局部加载状态"
```

### 任务 5：回归验证与文档同步

**文件：**
- 修改：`docs/superpowers/specs/2026-10-28-page-loading-design.md`（仅在实现与设计存在必要差异时）

- [ ] **步骤 1：检查工作区差异与格式**

运行：`git diff --check && git status --short`

预期：无空白错误；仅包含本计划涉及文件。

- [ ] **步骤 2：运行完整相关模块验证**

运行：`./gradlew :core:common:testDebugUnitTest :feature:home:testDebugUnitTest :feature:login:compileDebugKotlin :feature:home:compileDebugKotlin`

预期：全部通过。

- [ ] **步骤 3：提交必要的设计同步并报告验证结果**

```bash
git add docs/superpowers/specs/2026-10-28-page-loading-design.md
git commit -m "同步页面加载设计说明"
```

若设计文档无需变化，不创建空提交。
