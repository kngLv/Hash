# Loading 生命周期收集实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让 Loading 状态收集器跨前后台切换持续存在，避免重新订阅误关闭全局 LoadingDialog。

**Architecture:** 保留 `PageLoadingState` 的可空状态快照；BaseActivity 与 BaseFragment 在生命周期作用域内持续收集，不因 `STARTED` 状态取消、重建。Dialog 的创建和去重逻辑保留在 `BaseAppActivity`。

**Tech Stack:** Kotlin、Kotlin Flow、AndroidX Lifecycle、JUnit 4。

**Spec:** `docs/superpowers/specs/2026-10-29-loading-command-design.md`

## Global Constraints

- 保持 `BaseViewModel`、Activity、Fragment 现有 `showLoading()` / `dismissLoading()` 的业务调用形式。
- 不增加指令流、活跃计数、页面状态标记或 Loading 归属方。
- 清除本次定位加入的 `HashLoadingTrace` 临时日志。

## Review Focus

- 前后台切换不能重建 Loading 状态收集器。
- 延迟参数仍需归一化为非负值。
- Activity/Fragment 切到后台后收集器不因 `STARTED` 状态而重建。
- 重复显示仍由 `BaseAppActivity` 复用现有 Dialog。

### Task 1: 生命周期消费与清理

**Files:**
- Modify: `core/common/src/main/java/com/hash/common/base/activity/BaseAppActivity.kt`
- Modify: `core/common/src/main/java/com/hash/common/base/fragment/BaseAppFragment.kt`

**Interfaces:**
- Consumes: `LoadingStateOwner.loadingState.config`。

- [ ] 保留 `StateFlow` 状态分支，并移除 `repeatOnLifecycle(STARTED)`。
- [ ] 删除 `HashLoadingTrace` 临时日志与监听器。
- [ ] 运行 common 单测与 `:feature:mine:compileDebugKotlin`。
