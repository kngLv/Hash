# LoadingDialog 设计说明

## 目标

将视觉陈旧、风格不统一的 `WaitDialog` 更名为 `LoadingDialog`，以 Hash 符号作为加载动画的核心，让通用请求 Loading 与应用的红粉品牌视觉一致。

## 采用方案

采用“Hash 符号能量流动”方案：四根圆角线条组成 Hash 图形，以错峰波形连续改变颜色与线宽，形成沿 Hash 流动的强调效果。动画使用 `ValueAnimator` 驱动自定义 View 绘制，不继续使用旧的 `progress.json` Lottie 文件。

## 视觉与交互

- Dialog 保持居中、无页面遮罩，沿用现有延时展示和外部点击可取消能力。
- Dialog 窗口与布局均使用透明背景，不显示容器、圆角或阴影。
- Hash 图形尺寸约 48dp：四条线始终可见，使用极浅粉至品牌红 `#E72A36` 的连续过渡，并以更明显的线宽变化突出流动。
- 单轮动画约 1200ms，无限循环；View 附着时启动、分离时停止，避免后台无意义动画。
- 默认不显示文案；调用 `setMessage` 时，在图形下方显示一行辅助文字，并由内容撑高卡片。
- Dialog 出入场改用现有缩放动画，替换 Toast 风格动画。

## 代码结构

```text
base/dialog/
├─ LoadingDialog.kt        # 对外 Builder，替代 WaitDialog
└─ HashLoadingView.kt      # 自定义 Hash 逐段点亮动画

res/layout/loading_dialog.xml
res/drawable/loading_dialog_background.xml
```

`BaseAppActivity` 改为创建 `LoadingDialog`。旧的 `WaitDialog.kt`、`wait_dialog.xml` 与只被其使用的 `progress.json` 会一并删除，不保留兼容别名；项目内不存在其他调用点。

## 非目标

- 不改变 `PageLoadingState`、延时策略或 Loading 的页面作用域。
- 不增加 Lottie、Compose 或新的第三方依赖。
- 不为每个业务页面提供独立的 Loading 主题。

## 验证

- 编译 `core:common` 与依赖它的登录模块。
- 手工检查：快速请求不闪现；普通请求中 Hash 动画连续；点击外部可关闭；Activity 销毁后 Dialog 和动画不残留；带文案和无文案布局均正常。
