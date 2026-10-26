package com.hash.net.launch

import com.hash.net.download.DownloadActionImpl
import com.hash.net.request.DownloadBlockObject
import com.hash.net.request.LvRequest
import com.hash.net.request.RequestActionImpl
import com.hash.net.request.RequestBlockObject
import com.hash.net.request.ResultState
import com.hash.net.response.IResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import okhttp3.ResponseBody
import retrofit2.Response

/**
 * @name ILaunch
 * @package com.www.net
 * @author 345 QQ:1831712732
 * @time 2020/6/23 21:33
 * @description 请求发起的扩展函数
 */

/**
 * 将一个挂起函数封装为请求动作对象，用于延迟执行或链式调用。
 *
 * @param T 请求返回的数据类型
 * @param block 要封装的挂起函数，不接受参数，返回类型为 T
 * @return RequestActionImpl<T> 封装了请求代码块的请求动作实现对象
 *
 * 示例：
 * ```kotlin
 * val data = request { api.homeList(0) }.execute()
 *
 * // 或者使用链式调用
 * request { api.homeList(0) }
 *     .onBody { body ->
 *         // 处理返回的 body
 *     }
 *     ....more handlers...
 *     .onError { e ->
 *         // 处理错误
 *     }
 *     .enqueue(viewModelScope)
 * ```
 */
fun <T> request(block: suspend () -> IResponse<T>) = RequestActionImpl(RequestBlockObject(block))

/**
 * 将一个“下载请求”的挂起函数封装为可链式调用的下载动作对象。
 *
 * @param T 响应体类型，必须是 OkHttp 的 [ResponseBody] 或其子类
 * @param block 具体的下载逻辑，入参为本地已下载的字节数 existingLength：
 *  - existingLength == 0       ：本地还没有该文件，可以发起全量下载；
 *  - existingLength > 0        ：本地已经有部分内容，可根据该值构造 Range 头实现断点续传。
 *
 * 典型用法（支持断点续传）：
 * ```kotlin
 * // Retrofit API 示例
 * @Streaming
 * @GET("...")
 * suspend fun downFile(@Header("Range") range: String? = null): Response<ResponseBody>
 *
 * // 调用处
 * download { existingLength ->
 *     val range = if (existingLength > 0) "bytes=$existingLength-" else null
 *     api.downFile(range)
 * }
 * .onBegin { total, isResume -> /* total 为已下载 + 本次长度，isResume 表示是否断点续传 */ }
 * .onProgress { /* 更新进度 */ }
 * .onError { /* 处理异常（包括 HttpException 416 等）*/ }
 * .onEnd { /* 下载结束（无论成功/失败都会回调）*/ }
 * .enqueue(dir, fileName, scope, enableBreakpointResume = true)
 * ```
 *
 * 若服务端不支持 Range 或暂时不需要断点续传：
 * - 推荐：在 block 中忽略 existingLength，并在 enqueue 时将 enableBreakpointResume 设为 false，
 *   这样每次都会从 0 开始重新下载并覆盖本地文件，不会触发本地 append；
 * - 仅仅忽略 existingLength 而仍然使用 enableBreakpointResume = true 时，
 *   若本地已存在部分文件，框架仍会按“断点续写”方式追加写入，可能导致文件内容错误，不建议这么用。
 *
 * 若支持断点续传 Range：
 * - 当 existingLength 超出服务端可用范围时，服务端通常会返回 HTTP 416；
 * - 本框架不会将 416 自动视为“已下载完成”，而是通过 onError 原样抛出给调用方处理。
 * - 若需要更严谨地判断“文件是否已经完整下载”，推荐：
 *   1）先请求文件元信息（总大小 + MD5 等）；
 *   2）比较本地文件长度与总大小是否一致，小于则继续下载；
 *   3）下载完后必要时再比对 MD5；
 *   这些逻辑更适合作为业务层的下载管理/校验策略，而非由本类强行判定。
 */
fun <T : ResponseBody> download(block: suspend (existingLength: Long) -> Response<T>) =
    DownloadActionImpl(DownloadBlockObject(block))


/**
 * 同步并发请求
 *
 * @receiver CoroutineScope 用于启动并发协程的作用域（例如 Activity.lifecycleScope 或 ViewModel.viewModelScope）
 * @param T 列表中每个请求返回的数据类型
 * @param block 要并发执行的挂起函数列表；每个元素是一个不接受参数并返回 T? 的挂起函数
 * @return List<T?> 返回与传入请求列表顺序相对应的结果列表；若某个请求返回 null，则对应位置为 null
 * @throws CancellationException 当协程作用域被取消时，所有正在运行的请求会被取消并抛出 CancellationException
 *
 * 说明：函数内部使用 `async` 启动多个并发任务并通过 `awaitAll` 等待所有任务完成，
 * 返回结果列表的顺序与传入的 `block` 列表顺序一致。
 *
 * 示例：
 * ```kotlin
 * // 在 Activity 中
 * lifecycleScope.launch {
 *     val calls = listOf<suspend () -> HomeListBean?>(
 *         { api.getHome(1) },
 *         { api.getHome(2) }
 *     )
 *     val results: List<HomeListBean?> = zipSyncHttp(calls)
 * }
 * ```
 */
@Suppress("unused")
suspend fun <T> CoroutineScope.zipSyncHttp(block: List<suspend () -> T?>): List<T?> {
    val list = arrayListOf<Deferred<T?>>()
    block.forEach { it ->
        list.add(async { executeRaw(it) })
    }
    return list.awaitAll()
}

private suspend fun <T> executeRaw(block: suspend () -> T?): T? = when (
    val state = LvRequest().request(RequestBlockObject(block))
) {
    is ResultState.SuccessState -> state.body
    is ResultState.CodeErrorState -> null
    is ResultState.ErrorState -> null
}
