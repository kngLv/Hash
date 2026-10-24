# Hash 户型 Flutter 插件基础实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建独立的 `hash_floorplan` Flutter Plugin，使任何 Flutter/Android 宿主均可本地嵌入户型编辑器，并为 Hash 的 CloudBase Ticket 同步预留稳定接口。

**Architecture:** 插件的公开层只包含配置、同步模式、状态和 `HashFloorplanView`。本地模式使用 `shared_preferences` 保存完整 `planJson`，WebView 通过 JavaScript Channel 读取和写入该 JSON。云同步不在本计划中直接调用 PostgreSQL；本版本仅以 `FloorplanTicketProvider` 固定宿主认证边界，避免猜测 CloudBase PostgreSQL 的非公开客户端接口。

**Tech Stack:** Flutter Plugin、Dart、`webview_flutter`、`shared_preferences`、Flutter Test、Android MethodChannel。

---

## 文件结构

独立项目根目录为 `/Users/lvkang/StudioProjects/hash-floorplan-flutter`：

| 文件 | 职责 |
| --- | --- |
| `pubspec.yaml` | 声明插件、Android/iOS 平台与依赖。 |
| `lib/hash_floorplan.dart` | 唯一公开入口。 |
| `lib/src/model/floorplan_config.dart` | 同步模式与组件配置。 |
| `lib/src/model/floorplan_sync_state.dart` | 对宿主暴露的状态。 |
| `lib/src/storage/floorplan_local_store.dart` | 本地 JSON 存储抽象。 |
| `lib/src/storage/shared_preferences_floorplan_store.dart` | SharedPreferences 实现。 |
| `lib/src/bridge/floorplan_web_message.dart` | 网页与 Dart 间 JSON 消息协议。 |
| `lib/src/presentation/hash_floorplan_view.dart` | WebView 容器和本地读写编排。 |
| `lib/src/auth/floorplan_ticket_provider.dart` | 云同步 Ticket 抽象；本阶段不绑定 Hash。 |
| `lib/src/auth/method_channel_ticket_provider.dart` | Android 宿主的 MethodChannel 实现。 |
| `test/...` | 相应单元和 Widget 测试。 |
| `example/` | 手工验证用 Flutter 示例 App。 |

### Task 1：创建独立插件工程并锁定公开模型

**Files:**
- Create: `/Users/lvkang/StudioProjects/hash-floorplan-flutter/`
- Create: `/Users/lvkang/StudioProjects/hash-floorplan-flutter/lib/src/model/floorplan_config.dart`
- Create: `/Users/lvkang/StudioProjects/hash-floorplan-flutter/lib/src/model/floorplan_sync_state.dart`
- Create: `/Users/lvkang/StudioProjects/hash-floorplan-flutter/test/floorplan_config_test.dart`

- [ ] **Step 1: 创建 Flutter Plugin 工程。**

Run:

```bash
cd /Users/lvkang/StudioProjects
flutter create --template=plugin --platforms=android,ios --org com.hash hash-floorplan-flutter
cd hash-floorplan-flutter
```

Expected: 根目录包含 `pubspec.yaml`、`lib/`、`android/`、`ios/`、`example/` 和 `test/`。

- [ ] **Step 2: 写失败测试，固定本地模式无需 Ticket 的配置。**

```dart
import 'package:flutter_test/flutter_test.dart';
import 'package:hash_floorplan/hash_floorplan.dart';

void main() {
  test('本地模式不要求 Ticket 提供者', () {
    final config = FloorplanConfig(planId: 'default');

    expect(config.syncMode, FloorplanSyncMode.localOnly);
    expect(config.ticketProvider, isNull);
  });

  test('云同步模式必须提供 Ticket 提供者', () {
    expect(
      () => FloorplanConfig(
        planId: 'default',
        syncMode: FloorplanSyncMode.cloudSync,
      ),
      throwsArgumentError,
    );
  });
}
```

- [ ] **Step 3: 运行测试确认失败。**

Run: `flutter test test/floorplan_config_test.dart`

Expected: FAIL，原因是 `FloorplanConfig` 和 `FloorplanSyncMode` 尚不存在。

- [ ] **Step 4: 实现最小公开配置模型。**

```dart
enum FloorplanSyncMode { localOnly, cloudSync }

abstract interface class FloorplanTicketProvider {
  Future<String> issueTicket();
}

class FloorplanConfig {
  factory FloorplanConfig({
    required String planId,
    FloorplanSyncMode syncMode = FloorplanSyncMode.localOnly,
    FloorplanTicketProvider? ticketProvider,
  }) {
    if (syncMode == FloorplanSyncMode.cloudSync && ticketProvider == null) {
      throw ArgumentError('cloudSync 模式必须提供 ticketProvider');
    }
    return FloorplanConfig._(
      planId: planId,
      syncMode: syncMode,
      ticketProvider: ticketProvider,
    );
  }

  const FloorplanConfig._({
    required this.planId,
    required this.syncMode,
    required this.ticketProvider,
  });

  final String planId;
  final FloorplanSyncMode syncMode;
  final FloorplanTicketProvider? ticketProvider;
}
```

使用工厂构造或运行时检查替代 `assert`，确保 release 构建的 `cloudSync` 模式缺少 Ticket 时抛出 `ArgumentError`。

- [ ] **Step 5: 运行测试确认通过。**

Run: `flutter test test/floorplan_config_test.dart`

Expected: PASS，2 个测试均通过。

- [ ] **Step 6: 提交。**

```bash
git add pubspec.yaml lib test
git commit -m "feat: 初始化户型 Flutter 插件接口"
```

### Task 2：实现本地户型 JSON 存储

**Files:**
- Create: `lib/src/storage/floorplan_local_store.dart`
- Create: `lib/src/storage/shared_preferences_floorplan_store.dart`
- Create: `test/shared_preferences_floorplan_store_test.dart`
- Modify: `pubspec.yaml`
- Modify: `lib/hash_floorplan.dart`

- [ ] **Step 1: 添加失败测试，描述按 planId 隔离保存。**

```dart
test('保存的数据只会被相同 planId 读取', () async {
  final store = MemoryFloorplanLocalStore();
  await store.save('living-room', '{"rooms":3}');

  expect(await store.load('living-room'), '{"rooms":3}');
  expect(await store.load('bedroom'), isNull);
});
```

测试文件内部实现一个仅测试使用的 `MemoryFloorplanLocalStore`，其接口与生产存储一致。

- [ ] **Step 2: 运行测试确认失败。**

Run: `flutter test test/shared_preferences_floorplan_store_test.dart`

Expected: FAIL，原因是 `FloorplanLocalStore` 不存在。

- [ ] **Step 3: 添加依赖并实现存储接口。**

在 `pubspec.yaml` 添加：

```yaml
dependencies:
  shared_preferences: ^2.5.3
```

实现：

```dart
abstract interface class FloorplanLocalStore {
  Future<String?> load(String planId);
  Future<void> save(String planId, String planJson);
}

class SharedPreferencesFloorplanStore implements FloorplanLocalStore {
  SharedPreferencesFloorplanStore(this._preferences);
  final SharedPreferencesAsync _preferences;

  static String _key(String planId) => 'hash_floorplan.plan.$planId';

  @override
  Future<String?> load(String planId) => _preferences.getString(_key(planId));

  @override
  Future<void> save(String planId, String planJson) =>
      _preferences.setString(_key(planId), planJson);
}
```

- [ ] **Step 4: 运行测试确认通过。**

Run: `flutter test test/shared_preferences_floorplan_store_test.dart`

Expected: PASS。

- [ ] **Step 5: 提交。**

```bash
git add pubspec.yaml lib test
git commit -m "feat: 保存本地户型数据"
```

### Task 3：定义 WebView 消息协议

**Files:**
- Create: `lib/src/bridge/floorplan_web_message.dart`
- Create: `test/floorplan_web_message_test.dart`
- Modify: `lib/hash_floorplan.dart`

- [ ] **Step 1: 写失败测试，要求网页保存事件只接受完整字符串 JSON。**

```dart
test('解析网页保存消息', () {
  final message = FloorplanWebMessage.parse(
    '{"type":"savePlan","planJson":"{\\\"rooms\\\":3}"}',
  );

  expect(message, const FloorplanWebMessage.savePlan('{"rooms":3}'));
});

test('未知或缺失 planJson 的消息被拒绝', () {
  expect(() => FloorplanWebMessage.parse('{"type":"other"}'), throwsFormatException);
  expect(() => FloorplanWebMessage.parse('{"type":"savePlan"}'), throwsFormatException);
});
```

- [ ] **Step 2: 运行测试确认失败。**

Run: `flutter test test/floorplan_web_message_test.dart`

Expected: FAIL，原因是 `FloorplanWebMessage` 不存在。

- [ ] **Step 3: 实现最小不可变消息模型。**

```dart
sealed class FloorplanWebMessage {
  const FloorplanWebMessage();
  const factory FloorplanWebMessage.savePlan(String planJson) = SavePlanMessage;

  factory FloorplanWebMessage.parse(String raw) {
    final value = jsonDecode(raw);
    if (value is! Map<String, dynamic> || value['type'] != 'savePlan') {
      throw const FormatException('不支持的户型网页消息');
    }
    final planJson = value['planJson'];
    if (planJson is! String || planJson.isEmpty) {
      throw const FormatException('户型网页未提供 planJson');
    }
    return FloorplanWebMessage.savePlan(planJson);
  }
}
```

- [ ] **Step 4: 运行测试确认通过。**

Run: `flutter test test/floorplan_web_message_test.dart`

Expected: PASS，3 个测试通过。

- [ ] **Step 5: 提交。**

```bash
git add lib test
git commit -m "feat: 定义户型网页通信协议"
```

### Task 4：实现本地模式的 WebView 容器

**Files:**
- Create: `lib/src/presentation/hash_floorplan_view.dart`
- Create: `assets/floorplan/index.html`
- Create: `test/hash_floorplan_view_test.dart`
- Modify: `pubspec.yaml`
- Modify: `lib/hash_floorplan.dart`

- [ ] **Step 1: 写失败 Widget 测试，描述 WebView 保存消息会写入本地存储并上报状态。**

```dart
testWidgets('网页保存事件写入本地并通知已同步', (tester) async {
  final store = MemoryFloorplanLocalStore();
  final states = <FloorplanSyncState>[];
  final controller = FakeFloorplanWebController();

  await tester.pumpWidget(
    MaterialApp(
      home: HashFloorplanView(
        config: FloorplanConfig(planId: 'default'),
        localStore: store,
        webControllerFactory: () => controller,
        onSyncStateChanged: states.add,
      ),
    ),
  );
  await controller.emit('{"type":"savePlan","planJson":"{\\\"rooms\\\":3}"}');

  expect(await store.load('default'), '{"rooms":3}');
  expect(states.last, FloorplanSyncState.localSaved);
});
```

- [ ] **Step 2: 运行测试确认失败。**

Run: `flutter test test/hash_floorplan_view_test.dart`

Expected: FAIL，原因是 `HashFloorplanView` 不存在。

- [ ] **Step 3: 添加 WebView 依赖并实现容器。**

在 `pubspec.yaml` 添加：

```yaml
dependencies:
  webview_flutter: ^4.13.0
flutter:
  assets:
    - assets/floorplan/
```

`HashFloorplanView` 创建 `WebViewController`，注册 `JavaScriptChannel` 名称 `HashFloorplan`，接收 `savePlan` 消息后先调用 `localStore.save(config.planId, planJson)`，再调用 `onSyncStateChanged(FloorplanSyncState.localSaved)`。页面载入完成后读取 `localStore.load(config.planId)`，以 `window.HashFloorplan.loadPlan(<JSON 字符串>)` 注入；没有本地数据时不执行脚本。

`assets/floorplan/index.html` 首版只提供桥接契约：定义 `window.HashFloorplan.loadPlan` 并使用 `HashFloorplan.postMessage(JSON.stringify({type:'savePlan', planJson}))` 发送保存事件。后续替换为迁入的 `floorplan-3d` 构建产物。

- [ ] **Step 4: 运行 Widget 测试确认通过。**

Run: `flutter test test/hash_floorplan_view_test.dart`

Expected: PASS。

- [ ] **Step 5: 提交。**

```bash
git add pubspec.yaml assets lib test
git commit -m "feat: 提供本地户型编辑容器"
```

### Task 5：实现 Hash Android Ticket 适配边界

**Files:**
- Create: `lib/src/auth/method_channel_ticket_provider.dart`
- Create: `test/method_channel_ticket_provider_test.dart`
- Modify: `lib/hash_floorplan.dart`
- Modify: `/Users/lvkang/StudioProjects/Hash/core/router/src/main/java/com/hash/router/FloorplanIdentityProvider.kt`
- Create: `/Users/lvkang/StudioProjects/Hash/docs/hash-floorplan-android-embedding.md`

- [ ] **Step 1: 写失败测试，描述 MethodChannel 的成功和未登录映射。**

```dart
test('宿主成功时返回 Ticket', () async {
  const channel = MethodChannel('com.hash.floorplan/auth');
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, (call) async => 'ticket-1');

  expect(await MethodChannelTicketProvider(channel).issueTicket(), 'ticket-1');
});

test('宿主未登录映射为 FloorplanNotLoggedInException', () async {
  const channel = MethodChannel('com.hash.floorplan/auth');
  TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
      .setMockMethodCallHandler(channel, (call) async => throw PlatformException(code: 'HASH_NOT_LOGGED_IN'));

  expect(
    () => MethodChannelTicketProvider(channel).issueTicket(),
    throwsA(isA<FloorplanNotLoggedInException>()),
  );
});
```

- [ ] **Step 2: 运行测试确认失败。**

Run: `flutter test test/method_channel_ticket_provider_test.dart`

Expected: FAIL，原因是 `MethodChannelTicketProvider` 不存在。

- [ ] **Step 3: 实现 Dart 映射与 Android 端约定。**

`MethodChannelTicketProvider` 调用 `issueCloudBaseTicket`，只接受非空字符串结果。捕获 `PlatformException`：`HASH_NOT_LOGGED_IN` 转换为 `FloorplanNotLoggedInException`，其他异常转换为 `FloorplanTicketUnavailableException`。任何错误消息中不得包含 Cookie。

Hash Android 的 Flutter Engine 配置处注册同名 channel，协程调用 `FloorplanIdentityProvider.issueCloudBaseTicket()`；`Success` 返回 Ticket、`NotLoggedIn` 用错误码 `HASH_NOT_LOGGED_IN` 完成、`Failed` 用 `HASH_TICKET_UNAVAILABLE` 完成。

- [ ] **Step 4: 运行 Dart 测试确认通过。**

Run: `flutter test test/method_channel_ticket_provider_test.dart`

Expected: PASS。

- [ ] **Step 5: 编写嵌入说明。**

文档必须包括：构建 Flutter module/AAR 的命令、Hash Engine 的 channel 注册位置、`FloorplanIdentityProvider` 的实现来源、未登录时回到 Hash 已有登录页的责任归属，以及模块绝不接收 Cookie 的约束。

- [ ] **Step 6: 提交。**

在插件仓库提交 Dart 适配器；在 Hash 仓库单独提交 Android channel 与嵌入说明：

```bash
git add lib test
git commit -m "feat: 定义宿主同步凭据接口"

cd /Users/lvkang/StudioProjects/Hash
git add core/router docs/hash-floorplan-android-embedding.md
git commit -m "feat(户型): 接入 Flutter 凭据通道"
```

### Task 6：发布首个可引用版本

**Files:**
- Create: `README.md`
- Create: `CHANGELOG.md`
- Modify: `pubspec.yaml`

- [ ] **Step 1: 写 README 使用示例。**

示例必须覆盖本地模式：

```dart
HashFloorplanView(
  config: FloorplanConfig(planId: 'my-home'),
)
```

和需要同步的模式：

```dart
HashFloorplanView(
  config: FloorplanConfig(
    planId: 'my-home',
    syncMode: FloorplanSyncMode.cloudSync,
    ticketProvider: MethodChannelTicketProvider(),
  ),
)
```

- [ ] **Step 2: 运行完整检查。**

Run:

```bash
flutter analyze
flutter test
```

Expected: 两条命令均以退出码 `0` 结束。

- [ ] **Step 3: 创建 `v0.1.0` tag 并推送独立仓库。**

Run:

```bash
git add README.md CHANGELOG.md pubspec.yaml
git commit -m "docs: 发布户型插件使用说明"
git tag v0.1.0
git push origin main --tags
```

Expected: GitHub 仓库 `LvKang-insist/hash-floorplan-flutter` 有可被 Git 依赖引用的 `v0.1.0` tag。

## 自检

- 本地模式没有任何 Hash 或 CloudBase 依赖。
- 云同步接口是可替换的抽象，不会向 Dart/WebView 暴露 Cookie。
- `cloudSync` 缺少 Ticket 提供者会立即失败，不会降级为匿名同步。
- 首版的 WebView 页面是通信契约；迁入完整 `floorplan-3d` 产物独立于插件基础代码。
- CloudBase PostgreSQL 真实客户端写入放在下一阶段，待验证官方 Flutter SDK 的 PostgreSQL API 后实现，避免猜测非公开接口。
