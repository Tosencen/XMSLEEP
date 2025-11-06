package org.xmsleep.app.update

import android.util.Log
import kotlinx.coroutines.Dispatchers
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
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val _progress = MutableStateFlow(0f)
    val progress: StateFlow<Float> = _progress
    
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state
    
    /**
     * 下载文件
     * @param url 下载地址
     * @param destinationFile 目标文件
     * @return 成功返回文件，失败返回 null
     */
    suspend fun download(url: String, destinationFile: File): File? = withContext(Dispatchers.IO) {
        try {
            _state.value = DownloadState.Downloading(0f)
            _progress.value = 0f
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                _state.value = DownloadState.Failed("HTTP ${response.code}")
                return@withContext null
            }
            
            val body = response.body ?: run {
                _state.value = DownloadState.Failed("Response body is null")
                return@withContext null
            }
            
            val totalBytes = body.contentLength()
            var downloadedBytes = 0L
            
            // 确保父目录存在
            destinationFile.parentFile?.mkdirs()
            
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
            destinationFile
            
        } catch (e: IOException) {
            Log.e("FileDownloader", "Download failed", e)
            _state.value = DownloadState.Failed(e.message ?: "Unknown error")
            null
        } catch (e: Exception) {
            Log.e("FileDownloader", "Download failed", e)
            _state.value = DownloadState.Failed(e.message ?: "Unknown error")
            null
        }
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
