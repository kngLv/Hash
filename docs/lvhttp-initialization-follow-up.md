# LvHttp 进程重建初始化待修复项

状态：待后续修复。

## 问题

`LvHttp` 目前由 `SplashActivity` 在首帧绘制后手动初始化。前台服务异常导致进程被系统杀死后，系统可能直接恢复已有任务中的 `MainActivity`，而不会再次进入已结束的 `SplashActivity`。此时静态单例会重新创建，但 `LvHttp.Builder(...).build()` 尚未执行，任何 API 请求都会失败。

错误处理路径还会读取未初始化的 `LvHttp.p`，使根因被 `lateinit property p has not been initialized` 的二次异常遮蔽。

## 后续方案

1. 将 `NetInitializer` 移至 `Application.onCreate()` 同步初始化，或恢复 AndroidX Startup 自动初始化。
2. 为 `LvHttp.getErrorDispose()`、`getCode()` 等读取配置的方法增加未初始化兜底，避免错误处理本身再次抛异常。
3. 覆盖进程被杀后从最近任务、通知点击和深链恢复的验证场景。
