package org.xmsleep.app.update

import org.xmsleep.app.utils.Logger
import org.xmsleep.app.utils.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 文件下载器
 */
class FileDownloader {
    private val client = NetworkClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)  // 连接超时，避免被墙节点长时间阻塞
        .readTimeout(90, TimeUnit.SECONDS)     // APK 文件较大，使用更长读取超时
        .callTimeout(300, TimeUnit.SECONDS)    // 单次请求总时长上限，防止龟速流无限拖
        .build()

    companion object {
        private const val MAX_RETRY_COUNT = 2  // 单个下载源最大重试次数
        private const val INITIAL_RETRY_DELAY = 500L  // 初始重试延迟（毫秒）
        // 低速判定：每下载 MIN_SPEED_CHECK_BYTES 统计一次平均速度，
        // 低于 MIN_SPEED_BPS 视为劣质代理源，立即放弃本源切换下一个（慢速流不会触发 readTimeout）
        private const val MIN_SPEED_CHECK_BYTES = 512L * 1024
        private const val MIN_SPEED_BPS = 50L * 1024
        private const val BUFFER_SIZE = 64 * 1024
        private const val PROGRESS_EMIT_INTERVAL_MS = 150L  // 进度上报节流，避免高频重组
    }

    /** 源速度过低，直接放弃本源（不做同源重试） */
    private class SlowSourceException(message: String) : IOException(message)

    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state

    /**
     * 下载文件（多下载源依次回退 + 重试机制）
     * 依次尝试 downloadUrls 中的每个地址，某个失败则自动切换到下一个，
     * 全部失败后才上报最终失败（附带每个源的失败原因）。
     * 完整性校验：下载字节数必须与 expectedBytes（Release 资产大小）一致，
     * 防止镜像返回 200 + 错误页被当成 APK 缓存安装。
     * @param urls 下载源列表（按优先级排序）
     * @param destinationFile 目标文件
     * @param expectedBytes 期望字节数（0=未知，此时仅按 HTTP 头校验）
     * @return 成功返回文件，失败返回 null
     */
    suspend fun download(urls: List<String>, destinationFile: File, expectedBytes: Long = 0L): File? = withContext(Dispatchers.IO) {
        // 确保父目录存在
        destinationFile.parentFile?.mkdirs()

        val uniqueUrls = urls.distinct()
        if (uniqueUrls.isEmpty()) {
            _state.value = DownloadState.Failed("没有可用的下载地址")
            return@withContext null
        }

        // 已下载完整文件直接复用（外层另有签名校验兜底）
        if (expectedBytes > 0 && destinationFile.isFile && destinationFile.length() == expectedBytes) {
            Logger.d("FileDownloader", "发现已下载的完整文件，直接复用: ${destinationFile.path}")
            _progress.value = 1f
            _state.value = DownloadState.Success(destinationFile)
            return@withContext destinationFile
        }

        val errors = mutableListOf<String>()
        uniqueUrls.forEachIndexed { index, url ->
            Logger.d("FileDownloader", "尝试下载源 ($index/${uniqueUrls.size}): $url")
            try {
                return@withContext downloadWithUrl(url, destinationFile, expectedBytes)
            } catch (e: Exception) {
                Logger.w("FileDownloader", "下载源失败，切换到下一个: $url (${e.message})")
                errors += "${shortenUrl(url)}: ${e.message}"
            }
        }

        val errorDetail = errors.joinToString("；")
        Logger.e("FileDownloader", "所有下载源均失败: $errorDetail")
        _state.value = DownloadState.Failed(if (errorDetail.isEmpty()) "所有下载源均失败" else "所有下载源均失败：$errorDetail")
        null
    }

    /**
     * 使用指定URL下载文件（带重试机制）
     * 下载先写入 .part 临时文件，字节数校验通过后才改名为目标文件，避免截断文件被误认为完整。
     * 失败时抛出最后一次异常，由外层 download() 汇总各源错误信息。
     * SlowSourceException（低速源）不做同源重试，直接抛给外层切换下一源。
     */
    private suspend fun downloadWithUrl(url: String, destinationFile: File, expectedBytes: Long): File = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (attempt in 1..MAX_RETRY_COUNT) {
            val partFile = File(destinationFile.parentFile, destinationFile.name + ".part")
            try {
                _state.value = DownloadState.Downloading(0f)
                _progress.value = 0f

                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("HTTP ${response.code}")
                }

                val body = response.body ?: throw IOException("Response body is null")

                // 优先以 HTTP 头为准，缺失时用 API 给的资产大小
                val contentLength = body.contentLength()
                val totalBytes = if (contentLength > 0) contentLength else expectedBytes
                var downloadedBytes = 0L
                var lastCheckBytes = 0L
                var lastCheckTime = System.currentTimeMillis()
                var lastEmitTime = 0L

                FileOutputStream(partFile).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // 进度节流上报（150ms），避免 8KB 级高频 StateFlow 触发重组
                            val now = System.currentTimeMillis()
                            if (now - lastEmitTime >= PROGRESS_EMIT_INTERVAL_MS) {
                                lastEmitTime = now
                                val progressValue = if (totalBytes > 0) {
                                    downloadedBytes.toFloat() / totalBytes
                                } else {
                                    0f
                                }
                                _progress.value = progressValue
                                _state.value = DownloadState.Downloading(progressValue)
                            }

                            // 低速检测：每积累 512KB 统计一次平均速度
                            if (downloadedBytes - lastCheckBytes >= MIN_SPEED_CHECK_BYTES) {
                                val elapsedSec = (now - lastCheckTime) / 1000.0
                                if (elapsedSec >= 1.0) {
                                    val speedBps = ((downloadedBytes - lastCheckBytes) / elapsedSec).toLong()
                                    if (speedBps < MIN_SPEED_BPS) {
                                        throw SlowSourceException("速度过低 ${speedBps / 1024} KB/s")
                                    }
                                    lastCheckBytes = downloadedBytes
                                    lastCheckTime = now
                                }
                            }
                        }
                    }
                }

                // 字节数校验：防截断下载 / 镜像 200 + 错误页被当 APK 安装
                val expected = if (totalBytes > 0) totalBytes else expectedBytes
                if (expected > 0 && downloadedBytes != expected) {
                    throw IOException("下载不完整：$downloadedBytes/$expected 字节")
                }

                // 校验通过，.part 改名为正式文件
                if (destinationFile.exists()) destinationFile.delete()
                if (!partFile.renameTo(destinationFile)) {
                    partFile.copyTo(destinationFile, overwrite = true)
                    partFile.delete()
                }

                _state.value = DownloadState.Success(destinationFile)
                _progress.value = 1f
                Logger.d("FileDownloader", "下载成功 (尝试 $attempt/$MAX_RETRY_COUNT, $downloadedBytes 字节)")
                return@withContext destinationFile

            } catch (e: Exception) {
                lastException = e
                partFile.delete()  // 清理半截文件，避免污染下次下载
                Logger.w("FileDownloader", "下载失败 (尝试 $attempt/$MAX_RETRY_COUNT): ${e.message}")

                if (e is SlowSourceException) {
                    // 低速源不重试，直接交给外层切换下一个源
                    break
                }

                // 如果不是最后一次尝试，等待后重试
                if (attempt < MAX_RETRY_COUNT) {
                    val retryDelay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                    delay(retryDelay)
                    Logger.d("FileDownloader", "等待 ${retryDelay}ms 后重试...")
                    _state.value = DownloadState.Downloading(0f) // 重置状态
                } else {
                    // 单个下载源最终失败，仅记录，由外层汇总
                    Logger.e("FileDownloader", "下载源最终失败，已重试 $MAX_RETRY_COUNT 次: ${e.message}")
                }
            }
        }

        throw lastException ?: IOException("未知错误")
    }

    /** URL 截短为 host+路径末段，避免错误信息过长 */
    private fun shortenUrl(url: String): String = try {
        val u = java.net.URI(url)
        val path = u.path.substringAfterLast('/')
        "${u.host}/…/$path"
    } catch (_: Exception) {
        url.take(60)
    }

    fun reset() {
        _progress.value = 0f
        _state.value = DownloadState.Idle
    }
}

sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    data class Success(val file: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}
