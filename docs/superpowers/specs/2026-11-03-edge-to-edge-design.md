# Hash 全局系统栏适配设计

## 目标与背景

Hash 的 `targetSdk` 为 36，Android 已将 Edge-to-Edge 作为默认窗口行为。项目当前仍通过 `ImmersionBar` 分别控制 Activity、Fragment 和标题栏，首页自定义底部导航也没有统一处理导航栏安全区。此次迁移以官方 `enableEdgeToEdge()` 与 `WindowInsetsCompat` 为唯一系统栏方案，保证手势导航、三键导航、刘海、深色模式和输入法下的可用性。

成功标准是：背景可以延伸到状态栏和导航栏下方；标题、按钮、底部 Tab、列表最后一项和输入控件不会被系统栏遮挡；页面不出现重复 padding、跳动或深浅图标对比不足。

## 范围

### 目标

- 在 `BaseActivity` 统一启用 Edge-to-Edge。
- 用 Common 中按 UI 语义划分的 Insets 工具，替代 `ImmersionBar` 和固定系统栏高度计算。
- 将 `HashToolbar`、首页底部导航、新闻详情及所有固定底部交互控件迁移到统一工具。
- 保留启动页的独立品牌窗口外观，但不让其影响普通页面。
- 完成迁移后移除 `ImmersionBar` 及其依赖。

### 非目标

- 不更改页面业务逻辑、导航逻辑和现有视觉稿。
- 不为了适配而给所有 View 统一增加 padding；只有可能被系统栏遮挡的内容才处理。
- 不继续兼容 `fitsSystemWindows` 作为新页面的适配方案。

## 已考虑的方案

| 方案 | 取舍 |
|---|---|
| 保持现有 `ImmersionBar 3.2.2` | 改动小，但仍要自行修复自定义底栏；与 Android 15/16 的系统窗口策略双重控制，后续维护成本最高。 |
| 升级并保留 `ImmersionBar` | 能获得其新版本兼容修复，但仍保留额外的状态管理和兼容视图层，项目会持续依赖该库。 |
| 官方 Edge-to-Edge（采用） | 由 `enableEdgeToEdge()` 统一窗口行为、`WindowInsetsCompat` 决定内容避让位置，契合 `targetSdk = 36`，职责清晰且长期稳定。 |

选择第三种方案。它不改变应用最终可达到的视觉效果，但移除了窗口层的重复控制来源。

## 架构与规则

### 1. 窗口层

`BaseActivity` 在创建内容视图前启用 `enableEdgeToEdge()`。`BaseAppActivity` 仅保留“当前页面使用深色还是浅色系统栏图标”的可覆写策略，并通过 `WindowInsetsControllerCompat` 设置图标外观；不再设置固定状态栏高度、颜色或 `fitsSystemWindows`。

同一个窗口不得混用 `ImmersionBar` 和新的 Insets 机制。启动页由于需要固定品牌背景，可以保留独立主题/窗口背景，但不参与普通页面基类的状态栏控制。

### 2. 内容层

在 Common 提供四类明确的 View 扩展，所有扩展均记录 View 的初始 padding/高度，并在每次 Insets 分发时从初始值计算，避免旋转、主题切换或重新附着后重复累加：

- `applyTopBarInsets()`：状态栏与刘海安全区，供贴顶工具栏使用。
- `applyBottomBarInsets()`：导航栏安全区，供固定底部 Tab/操作栏使用；背景可铺至底部，内容区位于安全区之上。
- `applyScrollContentInsets()`：供 RecyclerView、ScrollView 使用；保持 `clipToPadding = false`，使最后内容可滚动至导航栏上方。
- `applyImeInsets()`：供输入框/提交栏使用；底部采用 IME 与导航栏二者中较大的 inset。

监听器不消费 Insets，避免父子 View 无法继续取得系统栏信息。

### 3. 组件与页面职责

- `HashToolbar` 增加可选的顶部安全区支持。只有位于页面最顶部的 toolbar 显式开启；普通嵌入式 toolbar 不受影响。
- 首页的自定义 RecyclerView 底部导航外包一层底栏容器，容器适配导航栏 inset，内部 Tab 保持现有 50dp 内容高度。
- 详情页和其他滚动页面为内容列表追加可滚动的底部安全区，而不是改变每个 item 的布局。
- 普通居中内容、非贴边卡片和没有固定底部操作的页面不做无意义适配。

## 分阶段实施与验收

### 阶段一：建立基础能力，不移除库

1. 在 `BaseActivity` 启用官方 Edge-to-Edge。
2. 新增 Common Insets 工具与单元测试，覆盖初始值不累计、导航栏/IME 取值规则。
3. 将 `BaseAppActivity`、`BaseAppFragment` 停止调用 `ImmersionBar`，但此阶段保留 Gradle 依赖，以便出现遗漏引用时能清楚定位。

验收：首页、详情页能启动；手势/三键导航、深浅模式下系统栏图标可见；无重复 padding。

### 阶段二：迁移公共 UI 骨架

1. 给 `HashToolbar` 增加可选顶部 inset。
2. 迁移首页底部导航容器，保证手势小白条与三键导航均不覆盖 Tab。
3. 迁移新闻详情等固定顶部栏和滚动内容页面。

验收：顶部返回按钮和标题不进入刘海/状态栏；底部 Tab 与详情最后内容均可触达。

### 阶段三：按页面类型收尾

1. 审计所有贴底的输入栏、提交栏和播放/操作栏，分别使用底栏或 IME 工具。
2. 审计滚动容器和全屏页面，避免遗漏或多重适配。
3. 在至少一台手势导航设备和一台三键导航设备上验证首页、详情、登录、我的页面；同时验证深色模式和软键盘。

验收：切换导航方式、后台前台、页面跳转后不出现内容被遮挡或边距叠加。

### 阶段四：删除旧依赖并回归

1. 用全仓检索确认无 `ImmersionBar`、`fitsSystemWindows` 和手工系统栏高度读取残留。
2. 删除 Common 的 `ImmersionBar` 依赖与版本目录声明。
3. 构建 Debug、Release，并检查差异不包含构建产物和 baseline profile 文件。

验收：`assembleDebug`、`assembleRelease` 成功，且所有系统栏适配只通过官方 API 与 Common Insets 工具完成。

## 风险控制

- 每个阶段独立构建并真机验证，避免一次迁移后无法判断问题来源。
- 首先处理基类与公共组件，再处理业务页面；业务页面不自行计算状态栏高度。
- 不把系统栏 inset 消费掉，避免嵌套页面或子 View 收不到 Insets。
- 对仍需特殊品牌背景的启动页保留独立主题，避免将品牌需求混入通用窗口规则。
