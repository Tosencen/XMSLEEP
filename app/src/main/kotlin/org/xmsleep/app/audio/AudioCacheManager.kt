package org.xmsleep.app.audio

import android.content.Context
import org.xmsleep.app.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.xmsleep.app.utils.NetworkClient
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
        private const val MAX_CACHE_SIZE = 500 * 1024 * 1024L // 500MB
        private const val MAX_CACHE_FILES = 200 // 最多缓存200个文件
        private const val MAX_RETRY_COUNT = 3  // 最大重试次数
        private const val INITIAL_RETRY_DELAY = 500L  // 初始重试延迟（毫秒）
        
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
    private val okHttpClient = NetworkClient.newBuilder()
        .readTimeout(90, TimeUnit.SECONDS)    // 音频文件较大，使用更长的读取超时
        .build()
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * 获取缓存的音频文件
     * 支持任意扩展名：查找 cacheDir 下以 soundId. 开头的文件
     */
    fun getCachedFile(soundId: String): File? {
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("$soundId.") && file.exists() && file.length() > 0) {
                return file
            }
        }
        return null
    }
    
    /**
     * 下载音频文件（带智能回退和重试机制）
     * 先尝试jsDelivr CDN，失败后回退到GitHub原始URL
     */
    suspend fun downloadAudio(url: String, soundId: String): Result<File> {
        // 检测URL类型并获取URL对
        val urlPair = if (url.contains("cdn.jsdelivr.net")) {
            // 如果是jsDelivr URL，尝试提取原始GitHub URL
            val githubUrl = extractGithubUrlFromJsDelivr(url)
            if (githubUrl != null) {
                RemoteAudioLoader.UrlPair(url, githubUrl)
            } else {
                // 无法提取，只使用当前URL
                RemoteAudioLoader.UrlPair(url, url)
            }
        } else {
            // 如果是GitHub URL，转换为jsDelivr
            val jsDelivrUrl = convertToJsDelivrUrl(url)
            RemoteAudioLoader.UrlPair(jsDelivrUrl, url)
        }
        
        // 先尝试jsDelivr URL
        val jsDelivrResult = downloadAudioWithUrl(urlPair.jsDelivrUrl, soundId, "jsDelivr")
        if (jsDelivrResult.isSuccess) {
            return jsDelivrResult
        }
        
        // jsDelivr失败，回退到GitHub原始URL
        Logger.w(TAG, "jsDelivr下载失败，回退到GitHub原始URL: ${urlPair.githubUrl}")
        return downloadAudioWithUrl(urlPair.githubUrl, soundId, "GitHub")
    }
    
    /**
     * 从jsDelivr URL提取GitHub原始URL
     */
    private fun extractGithubUrlFromJsDelivr(jsDelivrUrl: String): String? {
        return try {
            // jsDelivr格式: https://cdn.jsdelivr.net/gh/owner/repo@branch/path
            // GitHub格式: https://raw.githubusercontent.com/owner/repo/branch/path
            val pattern = Regex("https://cdn\\.jsdelivr\\.net/gh/([^/]+)/([^/]+)@([^/]+)/(.+)")
            val match = pattern.find(jsDelivrUrl)
            if (match != null) {
                val (owner, repo, branch, path) = match.destructured
                "https://raw.githubusercontent.com/$owner/$repo/$branch/$path"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将GitHub URL转换为jsDelivr URL
     */
    private fun convertToJsDelivrUrl(githubUrl: String): String {
        return try {
            val pattern = Regex("https://raw\\.githubusercontent\\.com/([^/]+)/([^/]+)/([^/]+)/(.+)")
            val match = pattern.find(githubUrl)
            if (match != null) {
                val (owner, repo, branch, path) = match.destructured
                "https://cdn.jsdelivr.net/gh/$owner/$repo@$branch/$path"
            } else {
                githubUrl
            }
        } catch (e: Exception) {
            githubUrl
        }
    }
    
    /**
     * 使用指定URL下载音频文件（带重试机制）
     */
    private suspend fun downloadAudioWithUrl(url: String, soundId: String, source: String): Result<File> {
        return withContext(Dispatchers.IO) {
            // 确保缓存目录存在
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // 检查缓存
            getCachedFile(soundId)?.let { file ->
                return@withContext Result.success(file)
            }
            
            // 获取文件扩展名
            val extension = url.substringAfterLast('.', "mp3")
            val file = File(cacheDir, "$soundId.$extension")
            
            // 带重试的下载
            var lastException: Exception? = null
            for (attempt in 1..MAX_RETRY_COUNT) {
                try {
                    // 下载文件
                    val request = Request.Builder()
                        .url(url)
                        .build()
                    
                    val response = okHttpClient.newCall(request).execute()
                    
                    if (!response.isSuccessful) {
                        throw IOException("下载失败: HTTP ${response.code}")
                    }
                    
                    val body = response.body ?: throw IOException("响应体为空")
                    
                    // 保存到缓存
                    body.byteStream().use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    return@withContext Result.success(file)
                } catch (e: Exception) {
                    lastException = e
                    Logger.w(TAG, "下载音频失败 (来源: $source, 尝试 $attempt/$MAX_RETRY_COUNT): ${e.message}")
                    
                    // 如果不是最后一次尝试，等待后重试
                    if (attempt < MAX_RETRY_COUNT) {
                        val delay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                        kotlinx.coroutines.delay(delay)
                    }
                }
            }
            
            // 所有重试都失败
            Logger.e(TAG, "下载音频失败 (来源: $source)，已重试 $MAX_RETRY_COUNT 次: ${lastException?.message}")
            Result.failure(lastException ?: IOException("下载失败"))
        }
    }
    
    /**
     * 下载音频文件（带进度回调、智能回退和重试机制）
     * 先尝试jsDelivr CDN，失败后回退到GitHub原始URL
     */
    fun downloadAudioWithProgress(
        url: String,
        soundId: String
    ): Flow<DownloadProgress> = flow {
        // 检测URL类型并获取URL对
        val urlPair = if (url.contains("cdn.jsdelivr.net")) {
            // 如果是jsDelivr URL，尝试提取原始GitHub URL
            val githubUrl = extractGithubUrlFromJsDelivr(url)
            if (githubUrl != null) {
                RemoteAudioLoader.UrlPair(url, githubUrl)
            } else {
                // 无法提取，只使用当前URL
                RemoteAudioLoader.UrlPair(url, url)
            }
        } else {
            // 如果是GitHub URL，转换为jsDelivr
            val jsDelivrUrl = convertToJsDelivrUrl(url)
            RemoteAudioLoader.UrlPair(jsDelivrUrl, url)
        }
        
        // 先尝试jsDelivr URL
        var jsDelivrSuccess = false
        var shouldFallback = false
        
        downloadAudioWithProgressAndUrl(urlPair.jsDelivrUrl, soundId, "jsDelivr").collect { progress ->
            when (progress) {
                is DownloadProgress.Success -> {
                    jsDelivrSuccess = true
                    emit(progress)
                }
                is DownloadProgress.Error -> {
                    // jsDelivr失败，标记需要回退
                    if (!jsDelivrSuccess) {
                        shouldFallback = true
                    } else {
                        emit(progress)
                    }
                }
                else -> emit(progress)
            }
        }
        
        // 如果jsDelivr失败，回退到GitHub原始URL
        if (shouldFallback && urlPair.jsDelivrUrl != urlPair.githubUrl) {
            Logger.w(TAG, "jsDelivr下载失败，回退到GitHub原始URL: ${urlPair.githubUrl}")
            downloadAudioWithProgressAndUrl(urlPair.githubUrl, soundId, "GitHub").collect { fallbackProgress ->
                emit(fallbackProgress)
            }
        }
    }
    
    /**
     * 使用指定URL下载音频文件（带进度回调和重试机制）
     * 对于 403/404 错误，立即失败不重试；对于网络错误，会重试
     */
    private fun downloadAudioWithProgressAndUrl(
        url: String,
        soundId: String,
        source: String
    ): Flow<DownloadProgress> = flow {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        
        // 检查缓存
        getCachedFile(soundId)?.let { file ->
            emit(DownloadProgress.Success(file))
            return@flow
        }
        
        // 获取文件扩展名
        val extension = url.substringAfterLast('.', "mp3")
        val file = File(cacheDir, "$soundId.$extension")
        
        // 带重试的下载
        var lastException: Exception? = null
        for (attempt in 1..MAX_RETRY_COUNT) {
            try {
                // 下载文件
                val request = Request.Builder()
                    .url(url)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                
                if (!response.isSuccessful) {
                    val httpCode = response.code
                    val errorMsg = "下载失败: HTTP $httpCode"
                    Logger.w(TAG, "$errorMsg (来源: $source, URL: $url)")
                    
                    // 对于 403/404 错误，立即失败，不重试（这些错误重试也没用）
                    if (httpCode == 403 || httpCode == 404) {
                        Logger.w(TAG, "HTTP $httpCode 错误，立即失败，不回退重试")
                        throw NonRetryableException(errorMsg, httpCode)
                    }
                    
                    throw IOException(errorMsg)
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
                
                Logger.d(TAG, "音频下载成功: $soundId (来源: $source, 尝试 $attempt/$MAX_RETRY_COUNT)")
                emit(DownloadProgress.Success(file))
                return@flow
            } catch (e: NonRetryableException) {
                // 不可重试的错误（403/404），立即失败
                Logger.e(TAG, "不可重试的错误 (来源: $source): ${e.message}")
                emit(DownloadProgress.Error(e))
                return@flow
            } catch (e: Exception) {
                lastException = e
                Logger.w(TAG, "下载音频失败 (来源: $source, 尝试 $attempt/$MAX_RETRY_COUNT): ${e.message}")
                
                // 如果不是最后一次尝试，等待后重试（仅对可重试的错误）
                if (attempt < MAX_RETRY_COUNT) {
                    val delay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                    delay(delay)
                    Logger.d(TAG, "等待 ${delay}ms 后重试...")
                }
            }
        }
        
        // 所有重试都失败
        Logger.e(TAG, "下载音频失败 (来源: $source)，已重试 $MAX_RETRY_COUNT 次: ${lastException?.message}")
        emit(DownloadProgress.Error(lastException ?: IOException("下载失败")))
    }.flowOn(Dispatchers.IO)
    
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
     * 支持任意扩展名
     */
    fun deleteCache(soundId: String) {
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("$soundId.") && file.exists()) {
                file.delete()
            }
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

/**
 * 不可重试的异常（如 403/404 错误）
 */
class NonRetryableException(message: String, val httpCode: Int) : IOException(message)

