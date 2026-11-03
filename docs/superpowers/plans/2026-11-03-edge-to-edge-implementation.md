# Hash 官方 Edge-to-Edge 迁移实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用 Android 官方 Edge-to-Edge 和 `WindowInsetsCompat` 统一系统栏适配，并彻底移除 `ImmersionBar`。

**Architecture:** `BaseActivity` 统一启用 Edge-to-Edge；Common 提供顶部栏、底部栏、滚动内容和输入法四类 Insets 扩展。页面用这些扩展避开安全区，背景始终可延伸到系统栏下方。

**Tech Stack:** AndroidX Activity 1.12.2、Core KTX、`WindowInsetsCompat`、ViewBinding、ConstraintLayout。

**Spec:** `docs/superpowers/specs/2026-11-03-edge-to-edge-design.md`

## Global Constraints

- `minSdk = 24`、`targetSdk = 36`；不使用固定系统栏高度或新增 `fitsSystemWindows`。
- 每次 Insets 分发均以 View 初始 padding/固定高度为基准，禁止累计。
- 同一窗口不混用 `ImmersionBar` 与官方 Insets 机制。
- 保留用户现有 `app/release/baselineProfiles/*.dm` 改动，不修改也不提交。
- 用户已授权在当前 `master` 工作区实施，不创建新分支或工作树。

## Review Focus

- 三键导航切换为手势导航后，首页底栏仍完整可点击。
- 深色模式下状态栏与导航栏图标保持足够对比度。
- 固定高度 Toolbar 应用顶部 Insets 后保留原有 56dp 内容区。
- RecyclerView 最后一项可滚动到导航栏上方，不会因重复分发不断增大 padding。
- 键盘弹出时，输入栏使用 IME 与导航栏中较大的底部 inset。

### Task 1: 建立可测试的 Insets 计算与 View 扩展

**Files:**
- Create: `core/common/src/main/java/com/hash/common/ui/insets/SystemBarInsets.kt`
- Create: `core/common/src/test/java/com/hash/common/ui/insets/SystemBarInsetsPolicyTest.kt`

**Interfaces:**
- Produces: `internal object SystemBarInsetsPolicy`，提供 `resolveImeBottom(imeBottom: Int, navigationBottom: Int): Int`。
- Produces: `fun View.applyTopBarInsets()`、`fun View.applyBottomBarInsets()`、`fun View.applyScrollContentInsets()`、`fun View.applyImeInsets()`。

- [ ] **Step 1: 写入失败单元测试**：覆盖 `resolveImeBottom` 在键盘高于导航栏、导航栏高于键盘、两者相等时返回较大值。
- [ ] **Step 2: 运行失败测试**：`./gradlew :core:common:testDebugUnitTest --tests com.hash.common.ui.insets.SystemBarInsetsPolicyTest`，预期因目标不存在失败。
- [ ] **Step 3: 实现策略与四个扩展**：顶部栏使用 `statusBars() + displayCutout()` 并对固定高度 View 保留内容高度；底部栏使用 `navigationBars()`；滚动内容关闭裁剪并追加底部安全区；输入栏使用策略计算。
- [ ] **Step 4: 验证**：`./gradlew :core:common:testDebugUnitTest --tests com.hash.common.ui.insets.SystemBarInsetsPolicyTest :core:common:compileDebugKotlin`，预期 BUILD SUCCESSFUL。

### Task 2: 迁移窗口基类并移除 ImmersionBar 调用

**Files:**
- Modify: `core/common/src/main/java/com/hash/common/base/activity/BaseActivity.kt`
- Modify: `core/common/src/main/java/com/hash/common/base/activity/BaseAppActivity.kt`
- Modify: `core/common/src/main/java/com/hash/common/base/fragment/BaseAppFragment.kt`
- Modify: `feature/home/src/main/java/com/hash/home/HomeFragment.kt`
- Modify: `feature/home/src/main/java/com/hash/home/ui/featured/detail/NewsDetailActivity.kt`

**Interfaces:**
- Consumes: Task 1 的 `applyTopBarInsets()`。
- Produces: 普通 Activity 内容创建前启用官方 Edge-to-Edge；基类不再导入或调用 `ImmersionBar`。

- [ ] **Step 1: 写入失败编译检查**：加入基类预期使用的 AndroidX 窗口 API 引用，运行 Common 编译，确认旧基类不满足新调用。
- [ ] **Step 2: 迁移 Activity 基类**：`BaseActivity.onCreate` 在内容视图创建前调用 `enableEdgeToEdge()`；`BaseAppActivity` 通过 `WindowInsetsControllerCompat` 按 `isStatusDarkFont()` 设置状态栏和导航栏图标明暗，删除 ImmersionBar、`isStatusBarEnable()`、`getTitleBar()`。
- [ ] **Step 3: 迁移 Fragment 基类与旧覆写点**：删除 `BaseAppFragment` 的 ImmersionBar 初始化、`fitsSystemWindows()`、`statusBarColor()` 和标题栏钩子，删除 Home 和详情的无效 `getTitleBar()` 覆写。
- [ ] **Step 4: 验证**：`./gradlew :core:common:compileDebugKotlin :feature:home:compileDebugKotlin`，预期 BUILD SUCCESSFUL。

### Task 3: 迁移通用 Toolbar 与首页底部导航

**Files:**
- Modify: `core/common/src/main/java/com/hash/common/ui/toolbar/HashToolbar.kt`
- Modify: `core/common/src/main/res/values/attrs.xml`
- Modify: `feature/main/src/main/res/layout/activity_main.xml`
- Modify: `feature/main/src/main/java/com/hash/main/MainActivity.kt`
- Modify: `feature/home/src/main/res/layout/activity_news_detail.xml`
- Modify: `feature/home/src/main/java/com/hash/home/ui/featured/detail/NewsDetailActivity.kt`

**Interfaces:**
- Consumes: Task 1 的顶部、底部和滚动内容扩展。
- Produces: XML 属性 `applyTopBarInsets`、首页底部导航容器、详情页安全区适配。

- [ ] **Step 1: Toolbar 顶部安全区开关**：新增默认关闭的 XML 属性 `applyTopBarInsets`；开启后仅追加状态栏/刘海区域，标题和按钮仍在原 56dp 内容区居中。
- [ ] **Step 2: 详情迁移**：详情 XML 开启 Toolbar Insets，Activity 为 `contentRecycler` 应用滚动内容 Insets。
- [ ] **Step 3: 首页迁移**：将 `main_navigation` 放入背景为 `surface` 的 `main_navigation_container`，RecyclerView 保持 50dp；MainActivity 为容器应用底部栏 Insets，ViewPager 约束到容器上方。
- [ ] **Step 4: 验证**：`./gradlew :core:common:testDebugUnitTest :feature:main:assembleDebug :feature:home:assembleDebug`，预期 BUILD SUCCESSFUL。

### Task 4: 清理依赖并全量回归

**Files:**
- Modify: `core/common/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

**Interfaces:**
- Consumes: Tasks 1-3 完成后的无 `ImmersionBar` 源码引用状态。
- Produces: 不再包含 `ImmersionBar` 的构建依赖。

- [ ] **Step 1: 残留检索**：`rg -n "ImmersionBar|immersionBar|fitsSystemWindows|status_bar_height|navigation_bar_height" --glob '*.kt' --glob '*.xml' --glob '*.kts' .`，预期生产代码与 Gradle 中无残留；启动页独立的 `navigationBarColor` 人工确认保留。
- [ ] **Step 2: 删除依赖**：删除 Version Catalog 的三个 library 条目、版本条目与 Common 依赖声明，不修改启动页品牌背景逻辑。
- [ ] **Step 3: 全量验证**：`./gradlew :app:assembleDebug :app:assembleRelease && git diff --check`，预期两个构建成功且 diff 检查无输出。
- [ ] **Step 4: 真机验收**：验证首页、详情、登录、我的页面在手势/三键导航、深色模式、横屏/刘海和键盘场景的安全区表现。
