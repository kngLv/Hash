# Hash 户型 Flutter 插件设计

## 目标

创建独立 Git 项目 `hash-floorplan-flutter`，其中的 Flutter Plugin 包名为 `hash_floorplan`。它向 Flutter 宿主与 Android 宿主提供可嵌入的户型编辑能力，并使用 CloudBase 在同一 Hash 用户的设备间同步户型和家具数据。

首个接入方是 Hash Android。Hash 仅负责提供已登录用户的 CloudBase Ticket；插件不保存 WanAndroid Cookie、密码或 CloudBase 私钥。

## 发布与依赖

第一版采用私有 Git 仓库 + Git tag 依赖：

```yaml
dependencies:
  hash_floorplan:
    git:
      url: git@github.com:LvKang-insist/hash-floorplan-flutter.git
      ref: v0.1.0
```

Flutter 宿主直接引用该包。Android 原生宿主采用 Flutter Add-to-App，构建并引用插件产生的 Android 产物。稳定后可按同一公开 API 迁移至私有 Pub 包，不改变宿主调用方式。

## 对外 API

插件对 Flutter 提供一个 `HashFloorplanView`，调用方传入：

- `planId`：当前户型标识；首版使用固定 `default`，每个 CloudBase 用户各自拥有一条数据。
- `syncMode`：`localOnly` 或 `cloudSync`，默认 `localOnly`。
- `ticketProvider`：仅 `cloudSync` 模式需要；异步返回 CloudBase 自定义登录 Ticket 的函数。
- `onSyncStateChanged`：可选的同步状态回调。

Android Add-to-App 场景中，插件通过 `MethodChannel` 调用宿主方法 `issueCloudBaseTicket`。Hash Android 的实现复用 `FloorplanIdentityProvider`：成功时返回 Ticket；未登录时返回错误码 `HASH_NOT_LOGGED_IN`；网络或服务异常返回 `HASH_TICKET_UNAVAILABLE`。

插件不要求所有宿主都使用 Hash 登录。`localOnly` 模式可被任何 Flutter 或 Android 宿主直接使用，并只在设备本地保存户型；`cloudSync` 模式才要求宿主提供其自身认证体系签发的、可登录 CloudBase 的 Ticket。Hash 只是第一种 `ticketProvider` 实现。

## 内部组件

```text
HashFloorplanView
 ├─ HashTicketAuth       获取 Ticket、登录 CloudBase、刷新失效登录态
 ├─ FloorplanRepository  读取与写入 floorplans 表
 ├─ FloorplanSyncController
 │   ├─ 进入页面时拉取云端 plan_json
 │   └─ 接收网页保存事件后防抖写入云端
 └─ FloorplanWebView     加载 assets 中的 floorplan-3d 网页
     └─ JavaScriptChannel 与网页互传 plan_json
```

网页仍然负责户型绘制、3D 展示与家具编辑。Flutter 不解析家具结构，只保存并透传 `plan_json`，从而保持现有网页编辑能力。

## 数据流

1. 宿主打开 `HashFloorplanView`。
2. `localOnly` 模式从本地存储读取 `plan_json` 并注入 WebView；`cloudSync` 模式向宿主请求 CloudBase Ticket。
3. `cloudSync` 模式通过 CloudBase 自定义登录建立当前用户会话，再按 `planId` 查询该用户的 `floorplans` 记录并注入 WebView。
4. 网页编辑完成后向 JavaScript Channel 发送完整 JSON。
5. `localOnly` 模式写入本地存储；`cloudSync` 模式更新或新增该用户、该 `planId` 对应的数据，并回调同步状态。

`floorplans` 表已有 RLS 策略；任何请求只能读写其 CloudBase 用户身份所拥有的记录。

## 冲突与离线规则

首版采取最后保存者覆盖：云端以最新 `updated_at` 为准。每次写入保留完整 JSON；网络失败时在本地保留待上传内容，并在下次页面打开或网络恢复后重试。首版不支持多人协作合并。

## 错误处理

- 宿主未登录：抛出 `HASH_NOT_LOGGED_IN`，由 Hash 跳转已有登录页。
- Ticket 无法获取：展示可重试状态，不泄露 Cookie 或网络响应细节。
- CloudBase 登录失败：清除插件内短期会话并重新请求 Ticket。
- 云端写入失败：网页编辑不丢失，展示“等待同步”，并在本地排队重试。

## 测试范围

- Dart 单测：Ticket 登录、未登录映射、云端 JSON 读写、失败重试和最后写入优先规则。
- Flutter Widget 测试：加载、同步状态和未登录提示。
- Android 集成测试：MethodChannel 正确调用 `FloorplanIdentityProvider`，且不向 Flutter 返回 Cookie。
- 手工验收：同一 Hash 账号的两台设备可看到相同户型与家具；不同账号互不可见。

## 非目标

- 不在插件中实现第二套手机号、密码或短信登录。
- 不将 CloudBase 私钥、WanAndroid Cookie 或密码写入插件代码、WebView 或仓库。
- 首版不实现多人实时协作、冲突合并或订阅推送。
