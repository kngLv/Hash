package com.hash.net.download

import com.hash.net.request.AbstractRequestAction
import com.hash.net.request.DownloadBlockObject
import com.hash.net.request.LvRequest
import com.hash.net.request.ResultState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

class DownloadActionImpl<T : ResponseBody>(
    val block: DownloadBlockObject<T>
) : AbstractRequestAction<T>() {


    private var downLoadBegin: ((Long, Boolean) -> Unit)? = null

    private var downLoadEnd : ((String?) -> Unit)? = null

    private var progress: ((Float) -> Unit)? = null

    /**
     * 注册：开始下载的回调
     * @param block (totalBytes, isResume) -> Unit
     *  - totalBytes: 预计总大小（已下载 + 本次要下载），如果拿不���则为已下载大小
     *  - isResume: 是否为断点续传（true 表示之前已下载了一部分，这次继续）
     */
    fun onBegin(block: (Long, Boolean) -> Unit): DownloadActionImpl<T> = apply {
        this.downLoadBegin = block
    }

    /**
     * 注册：下载进度的回调
     */
    fun onProgress(block: (Float) -> Unit): DownloadActionImpl<T> = apply { this.progress = block }

    /**
     * 注册：当请求抛出异常时的回调
     */
    fun onError(block: (Exception) -> Unit): DownloadActionImpl<T> = apply { this.error = block }

    /**
     * 注册：请求结束的回调
     * @param block (filePath) -> Unit，filePath 为最终下载完成的文件绝对路径；
     *               若下载失败，可能为 null。
     */
    fun onEnd(block: (String?) -> Unit): DownloadActionImpl<T> = apply { this.downLoadEnd = block }


    /**
     * 发起下载。
     *
     * @param dir 下载目录（最终文件会保存在此目录下）
     * @param fileName 逻辑文件名（不带 .temp，如 "xxx.apk"），用于决定最终文件/副本名
     * @param scope 协程作用域（建议 viewModelScope / lifecycleScope）
     * @param enableBreakpointResume 是否开启断点续传：
     *  - true  且 allowDuplicateFileName=false：仅使用原始 fileName 及其 .temp 做续传；
     *  - true  且 allowDuplicateFileName=true ：在源文件及其副本(1)(2)...及各自 .temp 中选择续传目标；
     *  - false：总是从 0 开始下载，会删除旧 .temp。
     * @param allowDuplicateFileName 目标文件已存在时的处理方式：
     *  - true ：生成不覆盖原文件的新文件名（test(1).apk、test(2).apk ...）；
     *  - false：始终使用传入的 fileName，可能覆盖已有文件。
     *
     * @return 可用于取消的 Job
     */
    fun enqueue(
        dir: String,
        fileName: String,
        scope: CoroutineScope,
        enableBreakpointResume: Boolean = false,
        allowDuplicateFileName: Boolean = false,
    ): Job {
        return scope.launch(Dispatchers.IO) {
            try {
                val dirFile = File(dir)
                if (!dirFile.exists()) {
                    dirFile.mkdirs()
                }

                var targetFile: File // 最终目标文件（无 .temp）
                var tempFile: File  // 下载缓存文件（带 .temp）
                var existingLength: Long // 本地已下载长度

                if (enableBreakpointResume) {
                    val originalTargetFile = File(dirFile, fileName)
                    if (!allowDuplicateFileName) {
                        // 断点续传 + 不生成副本：仅使用原始 fileName 及其 .temp
                        targetFile = originalTargetFile
                        tempFile = File(dirFile, targetFile.name + ".temp")
                        existingLength = if (tempFile.exists()) tempFile.length() else 0L
                    } else {
                        // 断点续传 + 允许副本：在源文件及其副本(1)(2)...中，结合各自 .temp 寻找续传点
                        val (t, temp, len) = findBreakpointCopyBaseFile(dirFile, fileName)
                        targetFile = t
                        tempFile = temp
                        existingLength = len
                    }
                } else {
                    // 非断点续传：只根据 allowDuplicateFileName 决定是否生成副本名，不使用 .temp 长度做续传
                    val originalTargetFile = File(dirFile, fileName)
                    targetFile = if (!allowDuplicateFileName) {
                        originalTargetFile
                    } else {
                        generateNonConflictFile(originalTargetFile)
                    }
                    tempFile = File(dirFile, targetFile.name + ".temp")

                    // 非断点续传场景不保留旧 temp
                    if (tempFile.exists()) tempFile.delete()
                    existingLength = 0L
                }

                // 告知调用方本地已下载长度，供其构造 Range 头
                block.existingLength = existingLength

                val state = LvRequest().request(block)
                when (state) {
                    is ResultState.SuccessState -> {
                        val finalFile = dispatchResponseBody(
                            state.body,
                            targetFile,
                            tempFile,
                            existingLength
                        )
                        dispatchEnd(finalFile?.absolutePath)
                    }

                    is ResultState.CodeErrorState -> {
                        dispatchError(Exception("Code error: code=${state.code}"))
                        dispatchEnd(null)
                    }

                    is ResultState.ErrorState -> {
                        dispatchError(state.error)
                        dispatchEnd(null)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                dispatchError(e)
                dispatchEnd(null)
            }
        }
    }

    /**
     * 启用断点续传时，根据“源文件名 + 副本(1)(2)... + 对应 .temp”规则，
     * 找到当前应该操作的目标文件(target)与其缓存文件(temp)，并计算 existingLength。
     *
     * 返回值含义：Triple<targetFile, tempFile, existingLength>
     */
    private fun findBreakpointCopyBaseFile(
        dirFile: File,
        fileName: String
    ): Triple<File, File, Long> {
        var index = 0
        while (true) {
            val candidateTarget = if (index == 0) {
                File(dirFile, fileName)
            } else {
                val base = File(dirFile, fileName)
                val candidate = generateIndexedFile(base, index)
                candidate
            }
            val candidateTemp = File(candidateTarget.parentFile, candidateTarget.name + ".temp")

            val targetExists = candidateTarget.exists()
            val tempExists = candidateTemp.exists()
            val tempLength = if (tempExists) candidateTemp.length() else 0L

            // 情况 A：当前副本文件不存在 -> 首次下载/全新副本
            if (!targetExists) {
                val existingLength = if (tempExists && tempLength > 0L) tempLength else 0L
                // 如果 temp 存在但长度为 0，也视为从 0 开始，调用方会根据 existingLength 决定是否 Range
                return Triple(candidateTarget, candidateTemp, existingLength)
            }

            // 情况 B：当前副本文件存在，且 temp 存在且有内容 -> 未完成下载的副本，继续续传
            if (tempExists && tempLength > 0L) {
                return Triple(candidateTarget, candidateTemp, tempLength)
            }

            // 情况 C：当前副本文件存在，但 temp 不存在或长度为 0 -> 认为该副本已完整存在，尝试下一个副本
            index++
        }
    }

    /**
     * 为已存在的目标文件生成不冲突的新文件。
     * 例：
     *  - 输入   test.apk    且存在，则依次尝试 test(1).apk、test(2).apk ...
     *  - 输入   test        且存在，则依次尝试 test(1)、test(2) ...
     */
    private fun generateNonConflictFile(original: File): File {
        if (!original.exists()) return original

        var index = 1
        var candidate: File
        do {
            candidate = generateIndexedFile(original, index)
            index++
        } while (candidate.exists())

        return candidate
    }

    /**
     * 为 original 生成带 (index) 的文件名，例如：
     *  - original = /a/test.apk, index = 1 -> /a/test(1).apk
     *  - original = /a/test,     index = 2 -> /a/test(2)
     */
    private fun generateIndexedFile(original: File, index: Int): File {
        val name = original.name
        val dotIndex = name.lastIndexOf('.')
        val baseName: String
        val ext: String
        if (dotIndex <= 0) {
            baseName = name
            ext = ""
        } else {
            baseName = name.substring(0, dotIndex)
            ext = name.substring(dotIndex) // 包含点，例如 .apk
        }
        val newName = "$baseName($index)$ext"
        return File(original.parentFile, newName)
    }

    private suspend fun dispatchResponseBody(
        body: ResponseBody?,
        targetFile: File,
        tempFile: File,
        existingLength: Long
    ): File? {
        if (body == null) {
            dispatchError(Exception("Response body is null"))
            return null
        }
        try {
            // 是否以续传方式写入：仅当 .temp 存在且长度与 existingLength 一致时才追加
            val append =
                existingLength > 0L && tempFile.exists() && tempFile.length() == existingLength
            val baseLength = if (append) existingLength else 0L

            // 若无法安全续传，则删除旧的 temp 文件重下
            if (!append && tempFile.exists()) {
                tempFile.delete()
            }

            val contentLength = body.contentLength()
            val totalSize = if (contentLength > 0) baseLength + contentLength else baseLength

            body.byteStream().use { input ->
                FileOutputStream(tempFile, append).use { output ->
                    download(input, output, baseLength, totalSize, baseLength > 0L)
                }
            }

            if (tempFile.exists()) {
                if (targetFile.exists()) targetFile.delete()
                if (tempFile.renameTo(targetFile)) {
                    return targetFile
                }
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            dispatchError(e)
        }
        return null
    }


    private suspend fun download(
        input: InputStream,
        output: OutputStream,
        alreadyBytes: Long,
        totalSize: Long,
        isResume: Boolean
    ) {
        currentCoroutineContext().ensureActive()
        var preProgress = 0f
        // 先分发开始回调：告知总大小和是否断点续传
        dispatchBegin(totalSize, isResume)

        val hasLength = totalSize > 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var bytesCopied = alreadyBytes
        var bytes = input.read(buffer)
        // 如果已经有部分下载，先回调一次当前进度
        if (hasLength && alreadyBytes > 0) {
            val initProgress = alreadyBytes.toFloat() * 100 / totalSize
            dispatchProcess(initProgress)
            preProgress = initProgress
        }

        while (bytes >= 0) {
            currentCoroutineContext().ensureActive()
            output.write(buffer, 0, bytes)
            bytesCopied += bytes
            if (hasLength) {
                val progress = bytesCopied.toFloat() * 100 / totalSize
                // 控制进度回调频率，这里每增加 1% 通知一次
                if (progress - preProgress >= 1f) {
                    dispatchProcess(progress)
                    preProgress = progress
                }
            }

            currentCoroutineContext().ensureActive()
            bytes = input.read(buffer)
        }
        currentCoroutineContext().ensureActive()
        output.flush()
        // 无论是否有 totalSize，最后都回调 100%，表示完成
        dispatchProcess(100f)
    }

    private suspend fun dispatchBegin(totalSize: Long, isResume: Boolean) {
        try {
            withMain { downLoadBegin?.invoke(totalSize, isResume) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun dispatchProcess(progress: Float) {
        try {
            withMain { this.progress?.invoke(progress) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun dispatchEnd(filePath: String?) {
        try {
            withMain { downLoadEnd?.invoke(filePath) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun dispatchError(t: Exception) {
        // 使用 AbstractRequestAction 中的默认实现（打印日志）作为兜底
        withMain { this.error.invoke(t) }
    }

    private suspend inline fun <R> withMain(crossinline block: () -> R) =
        withContext(Dispatchers.Main) { block() }

    private fun executeResponseBody(
        body: ResponseBody?,
        targetFile: File,
        tempFile: File,
        existingLength: Long
    ): File? {
        if (body == null) {
            throw Exception("Response body is null")
        }
        // 与 dispatchResponseBody 基本一致，但为同步版本，只返回最终文件
        val append =
            existingLength > 0L && tempFile.exists() && tempFile.length() == existingLength
        val baseLength = if (append) existingLength else 0L

        if (!append && tempFile.exists()) {
            tempFile.delete()
        }

        val contentLength = body.contentLength()
        val totalSize = if (contentLength > 0) baseLength + contentLength else baseLength

        body.byteStream().use { input ->
            FileOutputStream(tempFile, append).use { output ->
                // 这里直接阻塞写入，若你希望同步下也分发进度，可以复用 download 方法
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesCopied = baseLength
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    output.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = input.read(buffer)
                }
                output.flush()
            }
        }

        if (tempFile.exists()) {
            if (targetFile.exists()) targetFile.delete()
            if (tempFile.renameTo(targetFile)) {
                return targetFile
            }
        }
        return null
    }
}
