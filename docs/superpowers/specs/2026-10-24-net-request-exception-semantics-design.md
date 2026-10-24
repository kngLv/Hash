# Net 请求异常语义设计

## 目标

消除 Net 模块对取消异常和业务回调异常的吞没行为，并让网络、协议、响应解析失败以可观察且可区分的方式交给调用方处理。

## 异常边界

| 来源 | 处理方式 | 业务可见性 |
| --- | --- | --- |
| 协程取消 | 重新抛出 `CancellationException` | 由调用协程取消，不触发网络错误回调 |
| 传输、HTTP、协议 code 失败 | 转为 `ResultState.ErrorState` 或 `CodeErrorState` | `enqueue` 调用 `onError` 或 `onErrorCode` |
| `IResponse.data()` 解析失败 | 转为 `ErrorState` | `enqueue` 调用 `onError`，`onEnd(false)` |
| `onBody`、`onBodyOf`、`onErrorCode` 业务回调失败 | 不由网络层捕获 | 原样抛回注册该回调的协程作用域 |
| 全局错误 handler 失败 | 隔离并记录，保留原始网络异常 | 局部错误回调仍接收原始异常 |

## 接口设计

- 保留 `execute(): T?`，避免破坏现有调用方；其现有 null 语义不扩展。
- 新增 `executeState(): ResultState<T>`，供需要区分成功空值、业务 code 失败与异常失败的挂起调用方使用。
- `enqueue` 保持链式 API：传输和解析失败调用 `onError`；业务回调异常直接交给调用 scope 的异常处理机制。

## 流程

1. `LvRequest` 在 `Dispatchers.IO` 执行请求，立即重新抛出取消异常。
2. 对 `IResponse` 先校验业务 code，再读取 body；读取失败生成 `ErrorState`。
3. `RequestActionImpl` 根据 `ResultState` 调用相应回调；只保护生命周期／全局 handler，不吞没业务回调异常。
4. `onEnd` 的布尔值仅描述网络与响应解析是否成功；业务回调异常不被重写为网络结果。

## 测试

- 取消请求时不调用 `onError`，并向上抛出取消异常。
- `IResponse.data()` 抛异常时调用一次 `onError` 且 `onEnd(false)`。
- `onBodyOf` 业务回调抛异常时不调用 `onError`，异常由调用协程接收。
- `executeState()` 分别保留成功、code 错误与异常错误状态。
