# Loading 生命周期收集设计

## 目标

修复应用从后台回到前台时，未加载页面的 `null` 状态误关闭全局 LoadingDialog 的问题，同时保持业务侧 `showLoading()`、`dismissLoading()` 的调用方式不变。

## 根因

`PageLoadingState` 使用 `StateFlow<LoadingConfig?>`：其 `null` 是“当前未加载”的状态快照，而界面层却将其解释成“关闭 Dialog”的指令。`repeatOnLifecycle(STARTED)` 重建收集器时，`StateFlow` 会立即重放当前的 `null`，造成误关闭。

## 方案

- 保留 `PageLoadingState` 的 `StateFlow<LoadingConfig?>` 接口与现有调用方式。
- Activity 与 Fragment 在各自生命周期作用域中从创建持续收集到销毁，不使用 `repeatOnLifecycle(STARTED)`。
- 因后台/前台切换不会重建收集器，`StateFlow` 不会向同一收集器重放 `null`；Dialog 仍由 `BaseAppActivity` 唯一持有。

## 边界

- 普通后台/前台切换不丢失当前 Dialog，也不会因重新订阅重放 `null`。
- 若 Activity 或 Fragment 的视图确实被销毁，Dialog 随宿主销毁；网络 Loading 不跨宿主重建恢复。
- 不引入指令流、计数器、每页面的显示标记或归属方仲裁。
