# Hash Android 户型模块接入说明

Hash 通过 Flutter Add-to-App 加载户型编辑器，Android 7.0（API 24）及以上可用。当前 Debug 所需 AAR 与 Flutter Engine 已直接保存到 `feature/mine/libs/floorplan/`，Hash 构建不再依赖 Module 生成的本地 Maven 仓库。

## 更新内置 Flutter 文件

修改 Flutter Plugin 或 Module 后，先进入 Module 工程生成 AAR：

```bash
cd /Users/lvkang/StudioProjects/hash-floorplan-module
flutter build aar
```

将产物目录中的 Debug 主包、插件 AAR 替换到 Hash 的 `feature/mine/libs/floorplan/`；同时使用相同 Flutter SDK 版本替换 Flutter Engine 的 embedding 和 arm64 包。当前目录包含：

```text
flutter_debug-1.0.aar
hash_floorplan_debug-1.0.aar
shared_preferences_android_debug-1.0.aar
webview_flutter_android_debug-1.0.aar
flutter_embedding_debug.jar
flutter_arm64_v8a_debug.jar
```

其中 `flutter_arm64_v8a_debug.jar` 体积较大，但包含设备运行 Flutter 所需的 `libflutter.so`，不可遗漏。Hash Debug 当前仅构建 `arm64-v8a`，因此只内置 arm64 引擎。

> `flutter_arm64_v8a_debug.jar` 约 163MB，超过 GitHub 普通 Git 单文件 100MB 限制。若要将这些二进制文件推送到 GitHub，需要先启用 Git LFS；本地开发和 APK 构建不受影响。

## 打开户型页

Hash 注册的 ARouter 路径为：

```kotlin
ARouter.getInstance()
    .build(RouterActivityPath.Floorplan.EDITOR)
    .navigation(context)
```

页面是 `FloorplanFlutterActivity`。它在 `com.hash.floorplan/auth` 通道上实现 `issueCloudBaseTicket`：

- Hash 已登录：返回短期 CloudBase Ticket。
- 未登录：返回 `HASH_NOT_LOGGED_IN`。
- 网络或票据签发异常：返回 `HASH_TICKET_UNAVAILABLE`。

WanAndroid Cookie 和 CloudBase 私钥均不会传递给 Flutter。

## 验证顺序

1. 登录 Hash。
2. 通过上述路由进入户型页，添加或移动家具并保存。
3. 使用相同账号的另一台设备进入户型页，确认云端户型加载成功。
4. 断网编辑后退出，再联网进入户型页，确认本地待同步内容被补传。

当前已完成 Hash Debug APK 打包验证。此项目当前内置的是 Debug Flutter 产物；如需发布 Preview 或 Release，必须生成并替换对应模式的 Module、插件与 Flutter Engine 文件。
