package org.xmsleep.app.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 文件下载器
 */
class FileDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)  // 增加连接超时时间
        .readTimeout(90, TimeUnit.SECONDS)    // 增加读取超时时间（APK文件较大）
        .retryOnConnectionFailure(true)        // 启用连接失败重试
        .build()
    
    companion object {
        private const val MAX_RETRY_COUNT = 3  // 最大重试次数
        private const val INITIAL_RETRY_DELAY = 500L  // 初始重试延迟（毫秒）
    }
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state
    
    /**
     * 下载文件（带智能回退和重试机制）
     * 先尝试jsDelivr CDN，失败后回退到GitHub原始URL
     * @param url 下载地址（优先使用）
     * @param fallbackUrl 回退地址（可选）
     * @param destinationFile 目标文件
     * @return 成功返回文件，失败返回 null
     */
    suspend fun download(url: String, destinationFile: File, fallbackUrl: String? = null): File? = withContext(Dispatchers.IO) {
        // 确保父目录存在
        destinationFile.parentFile?.mkdirs()
        
        // 先尝试主URL（通常是jsDelivr）
        val mainResult = downloadWithUrl(url, destinationFile, "主URL")
        if (mainResult != null) {
            return@withContext mainResult
        }
        
        // 主URL失败，如果有回退URL，尝试回退URL
        if (fallbackUrl != null && fallbackUrl != url) {
            Log.w("FileDownloader", "主URL下载失败，回退到备用URL: $fallbackUrl")
            val fallbackResult = downloadWithUrl(fallbackUrl, destinationFile, "备用URL")
            if (fallbackResult != null) {
                return@withContext fallbackResult
            }
        }
        
        null
    }
    
    /**
     * 使用指定URL下载文件（带重试机制）
     */
    private suspend fun downloadWithUrl(url: String, destinationFile: File, source: String): File? = withContext(Dispatchers.IO) {
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
                Log.d("FileDownloader", "下载成功 (来源: $source, 尝试 $attempt/$MAX_RETRY_COUNT)")
                return@withContext destinationFile
                
            } catch (e: Exception) {
                lastException = e
                Log.w("FileDownloader", "下载失败 (来源: $source, 尝试 $attempt/$MAX_RETRY_COUNT): ${e.message}")
                
                // 如果不是最后一次尝试，等待后重试
                if (attempt < MAX_RETRY_COUNT) {
                    val retryDelay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                    delay(retryDelay)
                    Log.d("FileDownloader", "等待 ${retryDelay}ms 后重试...")
                    _state.value = DownloadState.Downloading(0f) // 重置状态
                } else {
                    // 最后一次尝试失败
                    Log.e("FileDownloader", "下载失败 (来源: $source)，已重试 $MAX_RETRY_COUNT 次: ${e.message}")
                    _state.value = DownloadState.Failed(e.message ?: "Unknown error")
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
