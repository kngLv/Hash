# CloudBase 自定义登录桥接实施计划

> **面向执行智能体：** 必须使用 `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按任务逐项执行。所有步骤均使用复选框跟踪。

**目标：** 复用 Hash 已登录的 WanAndroid Cookie，为户型 Flutter 模块签发 CloudBase 自定义登录 Ticket，从而让同一 WanAndroid 账号在多设备同步自己的户型数据。

**架构：** Android 客户端不保存 CloudBase 私钥；它从 `UserInfoStore` 读取已有 Cookie，并通过 HTTPS 调用 CloudBase HTTP 云函数。云函数先请求 WanAndroid 的用户信息接口校验 Cookie，取得稳定用户 ID 后生成 `wan_<id>`，再用仅存放在云函数中的 CloudBase 私钥签发 Ticket。Flutter 模块只接收 Ticket 并调用 `signInWithCustomTicket`，不接触 WanAndroid Cookie 或私钥。

**技术栈：** Android/Kotlin、MMKV、CloudBase Node.js 云函数、`@cloudbase/node-sdk`、Node 内置 `node:test`、Flutter CloudBase SDK。

---

## 约束与人工控制台操作

- CloudBase 环境：`hash-kng-d1geb954se5651c73`。
- 在「身份认证 → 登录方式」启用“自定义登录”，下载 `tcb_custom_login.json`。
- 私钥只放入 `cloudfunctions/issue-floorplan-ticket/` 的部署包，绝不放入 Android、Flutter、WebView、本仓库 Git 历史或日志。
- 将 HTTP 云函数网关配置为仅接受 HTTPS `POST`；函数实际入口 URL 在部署后写入 Android 的本地开发配置，不提交含用户 Cookie 的调试日志。
- PostgreSQL 的户型表通过 RLS 以 CloudBase `auth.uid()` 作为所有者条件；客户端不得写入任意 `owner_id`。

## 文件结构

| 文件 | 责任 |
| --- | --- |
| `cloudfunctions/issue-floorplan-ticket/index.js` | 校验 WanAndroid Cookie、构造 CloudBase 自定义 UID、签发 Ticket。 |
| `cloudfunctions/issue-floorplan-ticket/package.json` | 锁定云函数 Node 依赖和测试命令。 |
| `cloudfunctions/issue-floorplan-ticket/.gitignore` | 排除私钥和 `node_modules`。 |
| `cloudfunctions/issue-floorplan-ticket/test/index.test.js` | 验证 Cookie 缺失、无效 Cookie、有效 Cookie 三种结果。 |
| `core/repository/src/main/java/com/hash/repository/floorplan/FloorplanTicketRepository.kt` | 从已有登录缓存读取 Cookie，请求云函数，向 Flutter 宿主提供 Ticket。 |
| `core/repository/src/test/java/com/hash/repository/floorplan/FloorplanTicketRepositoryTest.kt` | 验证未登录拒绝、请求头携带 Cookie、失败信息不泄露 Cookie。 |
| `core/router/src/main/java/com/hash/router/FloorplanIdentityProvider.kt` | 定义稳定的宿主接口，供未来 Flutter Add-to-App 的 MethodChannel 适配层调用。 |
| `docs/cloudbase-户型同步配置.md` | 记录控制台配置、部署步骤、表结构和撤销私钥流程。 |

### Task 1：建立可测试的云函数身份映射

**文件：**

- 新建：`cloudfunctions/issue-floorplan-ticket/package.json`
- 新建：`cloudfunctions/issue-floorplan-ticket/index.js`
- 新建：`cloudfunctions/issue-floorplan-ticket/test/index.test.js`
- 新建：`cloudfunctions/issue-floorplan-ticket/.gitignore`

- [ ] **步骤 1：编写失败测试，描述合法 WanAndroid 用户到固定自定义 UID 的映射。**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { createIssueTicketHandler } from '../index.js';

test('有效 Cookie 会为 WanAndroid 用户签发稳定 Ticket', async () => {
  const handler = createIssueTicketHandler({
    fetchWanAndroidUser: async (cookie) => {
      assert.equal(cookie, 'loginUserName=test; token_pass=test');
      return { id: 12345 };
    },
    createTicket: async (uid) => {
      assert.equal(uid, 'wan_12345');
      return 'signed-ticket';
    },
  });

  assert.deepEqual(
    await handler({ body: JSON.stringify({ cookie: 'loginUserName=test; token_pass=test' }) }),
    { statusCode: 200, body: JSON.stringify({ ticket: 'signed-ticket' }) },
  );
});
```

- [ ] **步骤 2：运行测试并确认因模块不存在而失败。**

运行：`cd cloudfunctions/issue-floorplan-ticket && node --test test/index.test.js`

预期：失败信息指出无法导入 `../index.js`。

- [ ] **步骤 3：实现最小的可注入处理器和生产入口。**

```js
export function createIssueTicketHandler({ fetchWanAndroidUser, createTicket }) {
  return async (event = {}) => {
    const request = typeof event.body === 'string' ? JSON.parse(event.body) : event;
    if (typeof request.cookie !== 'string' || request.cookie.trim().length === 0) {
      return { statusCode: 401, body: JSON.stringify({ error: '未检测到登录态' }) };
    }
    try {
      const user = await fetchWanAndroidUser(request.cookie);
      if (!Number.isInteger(user?.id)) throw new Error('WanAndroid 用户无效');
      const ticket = await createTicket(`wan_${user.id}`);
      return { statusCode: 200, body: JSON.stringify({ ticket }) };
    } catch {
      return { statusCode: 401, body: JSON.stringify({ error: 'Hash 登录态已失效，请重新登录' }) };
    }
  };
}
```

生产入口使用 `fetch('https://www.wanandroid.com/user/lg/userinfo/json', { headers: { Cookie: cookie } })`，要求 HTTP 200 且响应 `errorCode === 0`，再把 `data.userInfo.id` 传给 `auth.createTicket`。初始化 CloudBase SDK 时仅从同目录的 `tcb_custom_login.json` 读取 `credentials`。

- [ ] **步骤 4：补充边界测试并跑绿。**

```js
test('未携带 Cookie 返回 401 且不签发 Ticket', async () => {
  const handler = createIssueTicketHandler({
    fetchWanAndroidUser: async () => { throw new Error('不应调用'); },
    createTicket: async () => { throw new Error('不应调用'); },
  });

  const response = await handler({ body: '{}' });
  assert.equal(response.statusCode, 401);
  assert.deepEqual(JSON.parse(response.body), { error: '未检测到登录态' });
});
```

运行：`cd cloudfunctions/issue-floorplan-ticket && node --test test/index.test.js`

预期：所有测试通过。

- [ ] **步骤 5：提交云函数及测试。**

```bash
git add cloudfunctions/issue-floorplan-ticket
git commit -m "feat(户型): 新增 CloudBase 登录票据云函数"
```

### Task 2：在 Hash 暴露不泄密的户型登录桥接接口

**文件：**

- 新建：`core/router/src/main/java/com/hash/router/FloorplanIdentityProvider.kt`
- 新建：`core/repository/src/main/java/com/hash/repository/floorplan/FloorplanTicketRepository.kt`
- 新建：`core/repository/src/test/java/com/hash/repository/floorplan/FloorplanTicketRepositoryTest.kt`
- 修改：`core/repository/build.gradle.kts`

- [ ] **步骤 1：先写失败测试，描述未登录不发起网络请求。**

```kotlin
@Test
fun `未登录时不请求云函数并返回未登录结果`() = runTest {
    var requested = false
    val repository = FloorplanTicketRepository(
        cookieProvider = { null },
        ticketRequester = { requested = true; error("不应请求") },
    )

    assertEquals(FloorplanTicketResult.NotLoggedIn, repository.issueTicket())
    assertFalse(requested)
}
```

- [ ] **步骤 2：运行测试并确认因类型不存在而失败。**

运行：`./gradlew :core:repository:testDebugUnitTest --tests '*FloorplanTicketRepositoryTest*'`

预期：编译失败，提示 `FloorplanTicketRepository` 未定义。

- [ ] **步骤 3：定义可供 Flutter 宿主调用的最小接口。**

```kotlin
interface FloorplanIdentityProvider {
    suspend fun issueCloudBaseTicket(): FloorplanTicketResult
}

sealed interface FloorplanTicketResult {
    data class Success(val ticket: String) : FloorplanTicketResult
    data object NotLoggedIn : FloorplanTicketResult
    data class Failed(val message: String) : FloorplanTicketResult
}
```

`FloorplanTicketRepository` 必须从 `UserInfoStore.getCookie()` 读取 Cookie；不得读取、保存或向调用方返回 `UserInfo.password`、WanAndroid `token`、完整 Cookie。网络请求将 Cookie 放在 HTTPS JSON 请求体的 `cookie` 字段，响应只解析 `ticket`。

- [ ] **步骤 4：补充有效登录态测试并实现 HTTP 请求适配。**

```kotlin
@Test
fun `有效登录态只将 Ticket 返回给调用方`() = runTest {
    val repository = FloorplanTicketRepository(
        cookieProvider = { "loginUserName=test; token_pass=test" },
        ticketRequester = { cookie ->
            assertEquals("loginUserName=test; token_pass=test", cookie)
            "signed-ticket"
        },
    )

    assertEquals(
        FloorplanTicketResult.Success("signed-ticket"),
        repository.issueTicket(),
    )
}
```

运行：`./gradlew :core:repository:testDebugUnitTest --tests '*FloorplanTicketRepositoryTest*'`

预期：所有 `FloorplanTicketRepositoryTest` 通过。

- [ ] **步骤 5：提交 Android 身份桥接。**

```bash
git add core/router/src/main/java/com/hash/router/FloorplanIdentityProvider.kt core/repository/src/main/java/com/hash/repository/floorplan core/repository/build.gradle.kts
git commit -m "feat(户型): 复用 Hash 登录态获取同步票据"
```

### Task 3：接入 Flutter 户型模块并限制数据访问

**文件：**

- 修改：未来户型 Flutter 包中的 `lib/src/auth/hash_ticket_auth.dart`
- 修改：未来户型 Flutter 包中的 `android/src/main/kotlin/.../HashFloorplanPlugin.kt`
- 新建：`docs/cloudbase-户型同步配置.md`

- [ ] **步骤 1：在 Flutter 侧写失败测试，描述 Ticket 由宿主提供而非组件自行登录。**

```dart
test('使用宿主提供的 Ticket 登录 CloudBase', () async {
  final gateway = FakeTicketGateway('signed-ticket');
  final auth = FakeCloudBaseAuth();

  await HashTicketAuth(gateway, auth).ensureSignedIn();

  expect(auth.receivedTicket, 'signed-ticket');
});
```

- [ ] **步骤 2：运行失败测试。**

运行：`flutter test test/hash_ticket_auth_test.dart`

预期：失败，提示 `HashTicketAuth` 未定义。

- [ ] **步骤 3：实现 MethodChannel 与 CloudBase 登录。**

Flutter 侧只调用宿主 MethodChannel 的 `issueCloudBaseTicket`，再调用：

```dart
await auth.signInWithCustomTicket(() async => ticket);
```

Android 侧将 `FloorplanIdentityProvider.issueCloudBaseTicket()` 的成功 Ticket 返回给 MethodChannel。`NotLoggedIn` 必须返回 `HASH_NOT_LOGGED_IN`，让宿主跳回现有登录页；不得显示第二套 CloudBase 用户名密码登录页。

- [ ] **步骤 4：在 CloudBase PostgreSQL 执行最小表和 RLS 配置。**

```sql
create table if not exists floorplans (
  id uuid primary key default gen_random_uuid(),
  owner_uid text not null default auth.uid()::text,
  plan_json jsonb not null,
  updated_at timestamptz not null default now()
);

alter table floorplans enable row level security;

create policy floorplans_owner_only on floorplans
  for all
  using (owner_uid = auth.uid()::text)
  with check (owner_uid = auth.uid()::text);
```

执行前需在控制台确认 `auth.uid()` 在当前 PG 环境可用；若控制台的 SQL 方言使用 `request.jwt.claim.sub`，按 CloudBase PG 文档改写为同等的当前用户声明，并记录最终语句到文档。

- [ ] **步骤 5：验证 Flutter 认证、Android 测试和首次同步。**

运行：

```bash
./gradlew :core:repository:testDebugUnitTest
flutter test test/hash_ticket_auth_test.dart
```

人工验证：在设备 A 登录 Hash、保存一件家具；在设备 B 使用同一 WanAndroid 账号登录 Hash，进入户型页并确认出现同一家具；使用另一 WanAndroid 账号时确认看不到该家具。

- [ ] **步骤 6：提交 Flutter 桥接与配置文档。**

```bash
git add docs/cloudbase-户型同步配置.md
git commit -m "feat(户型): 接入跨设备同步登录桥接"
```

## 自检结果

- 范围覆盖：云函数签发、Hash 登录态复用、Flutter Ticket 登录、PG 数据隔离、多设备人工验证均有对应任务。
- 私钥处理：仅在云函数部署目录，已纳入 `.gitignore`，没有进入 Android/Flutter 客户端。
- 占位检查：没有使用 TBD、TODO 或“后续补充”作为实现步骤。
- 类型一致性：Android 方法统一为 `issueCloudBaseTicket()`，Ticket 结果统一为 `FloorplanTicketResult`，Flutter 方法统一为 `ensureSignedIn()`。
