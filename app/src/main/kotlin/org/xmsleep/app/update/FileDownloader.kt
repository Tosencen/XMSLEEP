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
        .readTimeout(90, TimeUnit.SECONDS)    // APK 文件较大，使用更长读取超时
        .build()
    
    companion object {
        private const val MAX_RETRY_COUNT = 2  // 单个下载源最大重试次数
        private const val INITIAL_RETRY_DELAY = 500L  // 初始重试延迟（毫秒）
    }
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state
    
    /**
     * 下载文件（多下载源依次回退 + 重试机制）
     * 依次尝试 downloadUrls 中的每个地址，某个失败则自动切换到下一个，
     * 全部失败后才上报最终失败。
     * @param urls 下载源列表（按优先级排序）
     * @param destinationFile 目标文件
     * @return 成功返回文件，失败返回 null
     */
    suspend fun download(urls: List<String>, destinationFile: File): File? = withContext(Dispatchers.IO) {
        // 确保父目录存在
        destinationFile.parentFile?.mkdirs()
        
        val uniqueUrls = urls.distinct()
        if (uniqueUrls.isEmpty()) {
            _state.value = DownloadState.Failed("没有可用的下载地址")
            return@withContext null
        }
        
        var lastError: String? = null
        uniqueUrls.forEachIndexed { index, url ->
            Logger.d("FileDownloader", "尝试下载源 ($index/${uniqueUrls.size}): $url")
            val result = downloadWithUrl(url, destinationFile)
            if (result != null) {
                return@withContext result
            }
            Logger.w("FileDownloader", "下载源失败，切换到下一个: $url")
        }
        
        _state.value = DownloadState.Failed(lastError ?: "所有下载源均失败")
        null
    }
    
    /**
     * 使用指定URL下载文件（带重试机制）
     * 注意：单个下载源失败时不会设置全局 Failed 状态，由外层 download() 在所有源
     * 耗尽后才上报失败，避免中途误触发 UI 的失败提示。
     */
    private suspend fun downloadWithUrl(url: String, destinationFile: File): File? = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        for (attempt in 1..MAX_RETRY_COUNT) {
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
                
                val totalBytes = body.contentLength()
                var downloadedBytes = 0L
                
                FileOutputStream(destinationFile).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            
                            val progressValue = if (totalBytes > 0) {
                                downloadedBytes.toFloat() / totalBytes
                            } else {
                                0f
                            }
                            
                            _progress.value = progressValue
                            _state.value = DownloadState.Downloading(progressValue)
                        }
                    }
                }
                
                _state.value = DownloadState.Success(destinationFile)
                _progress.value = 1f
                Logger.d("FileDownloader", "下载成功 (尝试 $attempt/$MAX_RETRY_COUNT)")
                return@withContext destinationFile
                
            } catch (e: Exception) {
                lastException = e
                Logger.w("FileDownloader", "下载失败 (尝试 $attempt/$MAX_RETRY_COUNT): ${e.message}")
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < MAX_RETRY_COUNT) {
                    val retryDelay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                    delay(retryDelay)
                    Logger.d("FileDownloader", "等待 ${retryDelay}ms 后重试...")
                    _state.value = DownloadState.Downloading(0f) // 重置状态
                } else {
                    // 单个下载源最终失败，仅记录，不设置全局 Failed
                    Logger.e("FileDownloader", "下载源最终失败，已重试 $MAX_RETRY_COUNT 次: ${e.message}")
                }
            }
        }
        
        null
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
