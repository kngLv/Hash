# LoadingDialog 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**目标：** 用带 Hash 品牌动画的 `LoadingDialog` 替换旧 `WaitDialog`。

**架构：** `HashLoadingView` 只负责基于 `ValueAnimator` 绘制与驱动四段连续流动的 Hash 线条；`LoadingDialog.Builder` 只负责 Dialog 配置和可选文案。`BaseAppActivity` 保持既有延时、取消和复用机制，仅替换 Dialog 类型。

**技术栈：** Kotlin、Android View Canvas、ValueAnimator、AndroidX、JUnit 4。

**设计说明：** `docs/superpowers/specs/2026-10-29-loading-dialog-design.md`

## 全局约束

- 不新增第三方依赖，不再使用 `progress.json` Lottie 动画。
- 保持无页面遮罩、300ms 默认延时和可点击外部取消的现有 Loading 行为。
- 仅有 `setMessage` 时显示辅助文字；默认仅显示动画。
- 删除旧 `WaitDialog` 及只被其使用的资源，不保留兼容别名。

## 审查重点

- `HashLoadingView` 从窗口移除后必须取消无限动画，防止 Activity 销毁后泄漏。
- 四条 Hash 线必须以错峰波形连续改变颜色和线宽，不得出现逐帧跳变。
- `LoadingDialog.Builder.setMessage(null)` 必须隐藏文本，不影响动画容器尺寸。
- `BaseAppActivity` 只替换构造类型，既有延时取消和 Dialog 复用行为不变。
- 旧 `WaitDialog`、`wait_dialog.xml`、`progress.json` 必须不存在遗留引用。

---

### 任务 1：Hash 动画视图与 LoadingDialog

**文件：**
- 新建：`core/common/src/main/java/com/hash/common/base/dialog/HashLoadingView.kt`
- 新建：`core/common/src/main/java/com/hash/common/base/dialog/LoadingDialog.kt`
- 新建：`core/common/src/main/res/layout/loading_dialog.xml`
- 新建：`core/common/src/main/res/drawable/loading_dialog_background.xml`
- 新建：`core/common/src/test/java/com/hash/common/base/dialog/HashLoadingViewTest.kt`

**接口：**
- 产出：`class HashLoadingView(context: Context, attrs: AttributeSet? = null) : View`。
- 产出：`class LoadingDialog`，其中 `class Builder(context: Context) : BaseDialog.Builder<Builder>` 提供 `setMessage(@StringRes)` 与 `setMessage(CharSequence?)`。

- [ ] **步骤 1：编写失败测试**

```kotlin
@Test
fun segment_intensity_flows_smoothly_between_hash_strokes() {
    assertEquals(1f, HashLoadingView.segmentIntensityAt(0f, 0), 0.001f)
    assertEquals(1f, HashLoadingView.segmentIntensityAt(0.25f, 1), 0.001f)
}
```

- [ ] **步骤 2：运行测试，确认因 `HashLoadingView` 不存在而失败**

运行：`JAVA_HOME='…/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :core:common:testDebugUnitTest --tests 'com.hash.common.base.dialog.HashLoadingViewTest'`

预期：失败，错误指向缺少 `HashLoadingView`。

- [ ] **步骤 3：实现 `HashLoadingView` 与 `LoadingDialog`**

`HashLoadingView.segmentIntensityAt(progress: Float, segment: Int): Float` 使用错峰余弦波计算每条线的连续强度；绘制时据此过渡颜色与线宽。`onAttachedToWindow` 启动 1200ms 无限 `ValueAnimator`，`onDetachedFromWindow` 取消。`LoadingDialog.Builder` 使用缩放动画、无背景遮罩和新的圆形布局。

- [ ] **步骤 4：运行任务测试，确认通过**

运行：同步骤 2。

预期：通过。

### 任务 2：接入与旧资源清理

**文件：**
- 修改：`core/common/src/main/java/com/hash/common/base/activity/BaseAppActivity.kt`
- 删除：`core/common/src/main/java/com/hash/common/base/dialog/WaitDialog.kt`
- 删除：`core/common/src/main/res/layout/wait_dialog.xml`
- 删除：`core/common/src/main/res/raw/progress.json`

**接口：**
- 消费：`LoadingDialog.Builder`。
- 产出：`BaseAppActivity.showLoading` 与 `dismissLoading` 的现有公开签名和行为保持不变。

- [ ] **步骤 1：编译失败前检查**

运行：`rg -n 'WaitDialog|wait_dialog|progress' core/common/src/main app/src/main --glob '*.*'`

预期：仅存在待替换的 `BaseAppActivity` 与旧资源引用。

- [ ] **步骤 2：替换创建逻辑并移除旧文件**

在 `BaseAppActivity` 中使用 `LoadingDialog.Builder`；删除不再使用的类和资源，并确保没有新依赖或行为变更。

- [ ] **步骤 3：运行相关验证**

运行：`JAVA_HOME='…/Android Studio.app/Contents/jbr/Contents/Home' ./gradlew :core:common:testDebugUnitTest :feature:login:compileDebugKotlin`

预期：通过。

- [ ] **步骤 4：静态残留检查**

运行：`rg -n 'WaitDialog|wait_dialog|progress' core/common/src/main app/src/main --glob '*.*' && exit 1 || true`

预期：无输出。

- [ ] **步骤 5：提交**

```bash
git add core/common docs/superpowers/specs/2026-10-29-loading-dialog-design.md docs/superpowers/plans/2026-10-29-loading-dialog-implementation.md
git commit -m "重构品牌加载对话框"
```
