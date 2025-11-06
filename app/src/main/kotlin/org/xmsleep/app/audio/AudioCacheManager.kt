package org.xmsleep.app.audio

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 音频缓存管理器
 * 负责网络音频的下载和缓存管理
 */
class AudioCacheManager private constructor(context: Context) {
    
    companion object {
        private const val TAG = "AudioCacheManager"
        private const val CACHE_DIR_NAME = "audio_cache"
        private const val MAX_CACHE_SIZE = 100 * 1024 * 1024L // 100MB
        private const val MAX_CACHE_FILES = 50 // 最多缓存50个文件
        
        @Volatile
        private var instance: AudioCacheManager? = null
        
        fun getInstance(context: Context): AudioCacheManager {
            return instance ?: synchronized(this) {
                instance ?: AudioCacheManager(context.applicationContext).also { 
                    instance = it 
                }
            }
        }
    }
    
    private val appContext: Context = context.applicationContext
    private val cacheDir: File = File(appContext.cacheDir, CACHE_DIR_NAME)
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * 获取缓存的音频文件
     */
    fun getCachedFile(soundId: String): File? {
        val file = File(cacheDir, "$soundId.mp3") // 默认mp3格式
        return if (file.exists() && file.length() > 0) {
            file
        } else {
            // 尝试其他格式
            val wavFile = File(cacheDir, "$soundId.wav")
            if (wavFile.exists() && wavFile.length() > 0) {
                wavFile
            } else {
                null
            }
        }
    }
    
    /**
     * 下载音频文件
     */
    suspend fun downloadAudio(url: String, soundId: String): Result<File> {
        return withContext(Dispatchers.IO) {
            try {
                // 检查缓存
                getCachedFile(soundId)?.let { file ->
                    return@withContext Result.success(file)
                }
                
                // 检查缓存空间
                ensureCacheSpace()
                
                // 获取文件扩展名
                val extension = url.substringAfterLast('.', "mp3")
                val file = File(cacheDir, "$soundId.$extension")
                
                // 下载文件
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code}")
                }
                
                val body = response.body ?: throw IOException("响应体为空")
                
                // 保存到缓存
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                
                Log.d(TAG, "音频下载成功: $soundId")
                Result.success(file)
            } catch (e: Exception) {
                Log.e(TAG, "下载音频失败: ${e.message}")
                Result.failure(e)
            }
        }
    }
    
    /**
     * 下载音频文件（带进度回调）
     */
    fun downloadAudioWithProgress(
        url: String,
        soundId: String
    ): Flow<DownloadProgress> = flow {
        withContext(Dispatchers.IO) {
            try {
                // 检查缓存
                getCachedFile(soundId)?.let { file ->
                    emit(DownloadProgress.Success(file))
                    return@withContext
                }
                
                // 检查缓存空间
                ensureCacheSpace()
                
                // 获取文件扩展名
                val extension = url.substringAfterLast('.', "mp3")
                val file = File(cacheDir, "$soundId.$extension")
                
                // 下载文件
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    throw IOException("下载失败: ${response.code}")
                }
                
                val body = response.body ?: throw IOException("响应体为空")
                val contentLength = body.contentLength()
                
                // 保存到缓存
                body.byteStream().use { input ->
                    file.outputStream().use { output ->
                        var totalBytesRead = 0L
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytesRead += bytesRead
                            
                            // 更新进度
                            if (contentLength > 0) {
                                emit(DownloadProgress.Progress(totalBytesRead, contentLength))
                            }
                        }
                    }
                }
                
                Log.d(TAG, "音频下载成功: $soundId")
                emit(DownloadProgress.Success(file))
            } catch (e: Exception) {
                Log.e(TAG, "下载音频失败: ${e.message}")
                emit(DownloadProgress.Error(e))
            }
        }
    }
    
    /**
     * 确保缓存空间足够
     */
    private fun ensureCacheSpace() {
        val files = cacheDir.listFiles() ?: return
        
        // 按最后修改时间排序（LRU策略）
        val sortedFiles = files.sortedBy { it.lastModified() }.toMutableList()
        
        // 计算当前缓存大小
        var currentSize = sortedFiles.sumOf { it.length() }
        
        // 如果超过最大缓存大小或文件数，删除最旧的文件
        while ((currentSize > MAX_CACHE_SIZE || sortedFiles.size > MAX_CACHE_FILES) 
                && sortedFiles.isNotEmpty()) {
            val oldestFile = sortedFiles.removeFirst()
            currentSize -= oldestFile.length()
            oldestFile.delete()
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        val files = cacheDir.listFiles() ?: return 0L
        return files.sumOf { it.length() }
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        val files = cacheDir.listFiles() ?: return
        files.forEach { it.delete() }
    }
    
    /**
     * 删除指定音频的缓存
     */
    fun deleteCache(soundId: String) {
        val file = File(cacheDir, "$soundId.mp3")
        if (file.exists()) {
            file.delete()
        }
        val wavFile = File(cacheDir, "$soundId.wav")
        if (wavFile.exists()) {
            wavFile.delete()
        }
    }
}

/**
 * 下载进度
 */
sealed class DownloadProgress {
    data class Progress(val bytesRead: Long, val contentLength: Long) : DownloadProgress()
    data class Success(val file: File) : DownloadProgress()
    data class Error(val exception: Exception) : DownloadProgress()
}

