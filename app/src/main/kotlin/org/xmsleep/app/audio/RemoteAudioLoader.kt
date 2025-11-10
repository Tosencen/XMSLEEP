package org.xmsleep.app.audio

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmsleep.app.audio.model.SoundsManifest
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 网络音频加载器
 * 负责从GitHub加载音频清单和音频文件
 * 使用jsDelivr CDN以提高国内访问速度
 */
class RemoteAudioLoader(private val context: Context) {
    
    companion object {
        private const val TAG = "RemoteAudioLoader"
        // 使用jsDelivr CDN，国内可访问，无需VPN
        private const val REMOTE_MANIFEST_URL = 
            "https://cdn.jsdelivr.net/gh/Tosencen/XMSLEEP@main/sounds_remote.json"
        
        // GitHub raw URL模式（用于URL转换）
        private const val GITHUB_RAW_PATTERN = 
            "https://raw.githubusercontent.com/([^/]+)/([^/]+)/([^/]+)/(.+)"
        
        private const val MAX_RETRY_COUNT = 3  // 最大重试次数
        private const val INITIAL_RETRY_DELAY = 500L  // 初始重试延迟（毫秒）
    }
    
    private val gson = Gson()
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)  // 增加连接超时时间
        .readTimeout(60, TimeUnit.SECONDS)     // 增加读取超时时间
        .retryOnConnectionFailure(true)        // 启用连接失败重试
        .build()
    
    /**
     * 将GitHub raw URL转换为jsDelivr CDN URL
     * 例如: https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/audio/nature/river.mp3
     * 转换为: https://cdn.jsdelivr.net/gh/Tosencen/XMSLEEP@main/audio/nature/river.mp3
     */
    private fun convertToJsDelivrUrl(githubUrl: String): String {
        return try {
            // 匹配GitHub raw URL模式
            val regex = Regex(GITHUB_RAW_PATTERN)
            val matchResult = regex.find(githubUrl)
            
            if (matchResult != null) {
                val (username, repo, branch, path) = matchResult.destructured
                val jsDelivrUrl = "https://cdn.jsdelivr.net/gh/$username/$repo@$branch/$path"
                Log.d(TAG, "URL转换: $githubUrl -> $jsDelivrUrl")
                jsDelivrUrl
            } else {
                // 如果无法匹配，返回原URL
                Log.w(TAG, "无法转换URL，使用原URL: $githubUrl")
                githubUrl
            }
        } catch (e: Exception) {
            Log.e(TAG, "URL转换失败: ${e.message}", e)
            githubUrl
        }
    }
    
    /**
     * URL对：包含jsDelivr CDN URL和原始GitHub URL
     */
    data class UrlPair(
        val jsDelivrUrl: String,  // jsDelivr CDN URL（优先使用）
        val githubUrl: String      // 原始GitHub URL（回退使用）
    )
    
    /**
     * 转换清单中所有音频文件的URL
     * 注意：这里只转换URL，实际下载时会智能选择使用哪个URL
     */
    private fun convertManifestUrls(manifest: SoundsManifest): SoundsManifest {
        val convertedSounds = manifest.sounds.map { sound ->
            if (sound.remoteUrl != null) {
                val convertedUrl = convertToJsDelivrUrl(sound.remoteUrl)
                // 保存转换后的URL（优先使用jsDelivr）
                sound.copy(remoteUrl = convertedUrl)
            } else {
                sound
            }
        }
        return manifest.copy(sounds = convertedSounds)
    }
    
    /**
     * 获取URL对（用于智能回退）
     */
    fun getUrlPair(originalUrl: String): UrlPair {
        val jsDelivrUrl = convertToJsDelivrUrl(originalUrl)
        return UrlPair(jsDelivrUrl, originalUrl)
    }
    
    /**
     * 加载网络音频清单（带重试机制）
     */
    suspend fun loadManifest(forceRefresh: Boolean = false): SoundsManifest {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            
            for (attempt in 1..MAX_RETRY_COUNT) {
                try {
                    val request = Request.Builder()
                        .url(REMOTE_MANIFEST_URL)
                        .apply {
                            if (forceRefresh) {
                                addHeader("Cache-Control", "no-cache")
                            }
                        }
                        .build()
                    
                    val response = okHttpClient.newCall(request).execute()
                    
                    if (!response.isSuccessful) {
                        throw IOException("加载清单失败: HTTP ${response.code}")
                    }
                    
                    val json = response.body?.string() 
                        ?: throw IOException("响应体为空")
                    
                    val manifest = gson.fromJson(json, SoundsManifest::class.java)
                    
                    // 转换所有音频文件的URL为jsDelivr CDN
                    val convertedManifest = convertManifestUrls(manifest)
                    
                    Log.d(TAG, "成功加载网络音频清单，共 ${convertedManifest.sounds.size} 个音频 (尝试 $attempt/$MAX_RETRY_COUNT)")
                    return@withContext convertedManifest
                } catch (e: Exception) {
                    lastException = e
                    Log.w(TAG, "加载网络音频清单失败 (尝试 $attempt/$MAX_RETRY_COUNT): ${e.message}")
                    
                    // 如果不是最后一次尝试，等待后重试
                    if (attempt < MAX_RETRY_COUNT) {
                        val retryDelay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                        delay(retryDelay)
                        Log.d(TAG, "等待 ${retryDelay}ms 后重试...")
                    }
                }
            }
            
            // 所有重试都失败
            Log.e(TAG, "加载网络音频清单失败，已重试 $MAX_RETRY_COUNT 次: ${lastException?.message}")
            throw lastException ?: IOException("加载清单失败")
        }
    }
}

