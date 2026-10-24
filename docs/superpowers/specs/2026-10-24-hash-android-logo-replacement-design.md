# Hash Android 图标替换设计

## 目标

将用户提供的 `hash-icon-android` 图标素材替换为 Hash 应用的系统启动图标，并让启动页显示同一套新版 Logo。

## 范围

- 更新 `core/common/src/main/res` 中各密度目录的 `ic_launcher`、`ic_launcher_round`、`ic_launcher_foreground` 资源。
- 更新 Android 8.0 及以上设备使用的自适应图标背景与前景资源，使标准和圆形遮罩都从同一套素材生成。
- 保持 `app/src/main/AndroidManifest.xml` 的 `@mipmap/ic_launcher` 与 `@mipmap/ic_launcher_round` 引用不变。
- 启动页 `activity_splash.xml` 已引用 `@mipmap/ic_launcher_round`，因此无需新增界面逻辑即可同步显示新 Logo。

## 不在范围内

- 不修改应用名称、主题颜色、包名或签名配置。
- 不改动非启动图标的界面图片。
- 不将 `.superpowers/` 下的预览文件纳入版本控制。

## 资源映射

| 新素材 | 目标位置 | 用途 |
| --- | --- | --- |
| `mipmap-*/ic_launcher_background.png` | `mipmap-*/ic_launcher.webp` | 标准启动图标底图 |
| `mipmap-*/ic_launcher_round_background.png` | `mipmap-*/ic_launcher_round.webp` | 圆形启动图标底图 |
| `mipmap-*/ic_launcher_foreground.png` | `mipmap-*/ic_launcher_foreground.webp` | Android 8+ 标准自适应图标前景 |
| `mipmap-*/ic_launcher_round_foreground.png` | 仅在需要独立圆形前景时使用 | 保留素材对应关系 |
| `mipmap-anydpi-v26` XML | 保持既有文件名与引用 | 将背景和前景组合为自适应图标 |

## 验证

1. 检查每个 `mdpi` 至 `xxxhdpi` 密度目录的资源文件均存在且可解码。
2. 执行 `:app:processDebugResources` 或等效 Android 资源处理任务，确认资源引用与格式均可通过编译。
3. 检查启动页仍引用 `@mipmap/ic_launcher_round`，以确认 Logo 与桌面圆形图标一致。
