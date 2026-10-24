# CloudBase 户型同步配置

## 已配置资源

- CloudBase 环境：`hash-kng-d1geb954se5651c73`
- 自定义登录：已启用。
- 云函数：`issue-floorplan-ticket-v2`，Node.js 20，监听 `9000` 端口。
- HTTP 网关：`POST https://hash-kng-d1geb954se5651c73-1258197985.ap-shanghai.app.tcloudbase.com/floorplan-ticket`
- PostgreSQL 表：`public.floorplans`。

网关不启用 CloudBase 身份认证，因为首次换取 Ticket 前尚未拥有 CloudBase 身份。请求体中的 Hash Cookie 会在云函数内通过 WanAndroid 用户信息接口校验；无效或缺失 Cookie 统一返回 `401`，不会回传 Cookie 或私钥。

## 数据访问规则

`floorplans` 表字段如下：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | `uuid` | 户型记录标识，自动生成。 |
| `owner_uid` | `text` | CloudBase 当前用户标识，默认使用 `auth.uid()`。 |
| `plan_json` | `jsonb` | 户型、家具与视角等业务数据。 |
| `updated_at` | `timestamptz` | 最近写入时间。 |

表已启用 RLS，并配置 `floorplans_owner_only` 策略：读取、插入、更新和删除均要求 `owner_uid = auth.uid()::text`。客户端不应传入或修改其他用户的 `owner_uid`。

建表 SQL：

```sql
create table if not exists public.floorplans (
  id uuid primary key default gen_random_uuid(),
  owner_uid text not null default auth.uid()::text,
  plan_json jsonb not null,
  updated_at timestamptz not null default now()
);

alter table public.floorplans enable row level security;

create policy floorplans_owner_only on public.floorplans
  for all
  using (owner_uid = auth.uid()::text)
  with check (owner_uid = auth.uid()::text);
```

## 客户端接入约定

1. Hash Android 宿主从 `UserInfoStore.getCookie()` 读取已有 WanAndroid 登录态。
2. 宿主向上述 HTTP 网关发送 JSON：`{"cookie":"..."}`。
3. 云函数校验登录态后签发 CloudBase 自定义登录 Ticket。
4. Flutter 户型组件仅通过 MethodChannel 请求 Ticket，并调用 CloudBase SDK 的 `signInWithCustomTicket` 登录。
5. Flutter 组件使用已登录的 CloudBase 身份读写 `floorplans`；不得自行保存 WanAndroid Cookie、账户密码或 CloudBase 私钥。

## 私钥与撤销

`tcb_custom_login.json` 仅存在于云函数部署包中，并被 Git 忽略。它不能复制到 Android、Flutter、WebView 或提交到 Git。

若怀疑私钥泄露，在 CloudBase 控制台的自定义登录配置中重新生成凭据，再重新部署云函数。旧凭据应立即失效。
