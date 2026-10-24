# Hash Android 户型模块接入说明

Hash 通过 Flutter Add-to-App 加载户型编辑器，Android 7.0（API 24）及以上可用。

## 构建 Flutter AAR

先进入 Flutter Module 工程并生成 Maven 产物：

```bash
cd /Users/lvkang/StudioProjects/hash-floorplan-module
flutter build aar
```

默认产物目录为：

```text
/Users/lvkang/StudioProjects/hash-floorplan-module/build/host/outputs/repo
```

Hash 默认从其相邻目录读取这个 Maven 仓库。若 Module 存放在其他路径，构建 Hash 时提供属性：

```bash
./gradlew -PfloorplanAarRepo=/绝对路径/outputs/repo :app:assembleDebug
```

Flutter Engine 的运行时依赖由 `https://storage.googleapis.com/download.flutter.io` 解析；首次构建需要能访问该地址或已存在 Gradle 缓存。

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

当前已完成 Hash Debug 编译验证。构建 Preview 或 Release 前，需确保 Flutter Module 的对应 AAR 变体已完整生成。
