package org.xmsleep.app.audio

import android.content.Context
import org.xmsleep.app.utils.Logger
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.xmsleep.app.audio.model.AudioSource
import org.xmsleep.app.audio.model.SoundsManifest
import org.xmsleep.app.utils.NetworkClient
import java.io.IOException

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
        
        // 备用 URL - GitHub raw（如果 CDN 失败）
        private const val BACKUP_MANIFEST_URL =
            "https://raw.githubusercontent.com/Tosencen/XMSLEEP/main/sounds_remote.json"
        
        // GitHub raw URL模式（用于URL转换）
        private const val GITHUB_RAW_PATTERN = 
            "https://raw.githubusercontent.com/([^/]+)/([^/]+)/([^/]+)/(.+)"
        
        private const val MAX_RETRY_COUNT = 3  // 最大重试次数
        private const val INITIAL_RETRY_DELAY = 500L  // 初始重试延迟（毫秒）
    }
    
    private val gson = Gson()
    private val okHttpClient = NetworkClient.default
    
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
                "https://cdn.jsdelivr.net/gh/$username/$repo@$branch/$path"
            } else {
                // 如果无法匹配，返回原URL
                Logger.w(TAG, "无法转换URL，使用原URL: $githubUrl")
                githubUrl
            }
        } catch (e: Exception) {
            Logger.e(TAG, "URL转换失败: ${e.message}", e)
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
     * 修复清单中不完整的音频数据
     * 为缺少的填幅默认值，不影响存在的数据
     */
    private fun fixManifestData(manifest: SoundsManifest): SoundsManifest {
        val fixedSounds = manifest.sounds.map { sound ->
            // 为缺少的字段提供默认值
            sound.copy(
                source = sound.source ?: (if (sound.remoteUrl != null) AudioSource.REMOTE else AudioSource.LOCAL),
                loopStart = sound.loopStart ?: 0L,
                loopEnd = sound.loopEnd ?: 0L
            )
        }
        return manifest.copy(sounds = fixedSounds)
    }
    
    /**
     * 转换清单中所有音频文件的URL
     */
    private fun convertManifestUrls(manifest: SoundsManifest): SoundsManifest {
        val convertedSounds = manifest.sounds.map { sound ->
            if (sound.remoteUrl != null) {
                val convertedUrl = convertToJsDelivrUrl(sound.remoteUrl)
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
     * 加载网络音频清单（带重试机制和备用URL）
     */
    suspend fun loadManifest(forceRefresh: Boolean = false): SoundsManifest {
        return withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            
            // 添加时间戳参数来绕过 CDN 缓存
            val timestamp = System.currentTimeMillis()
            val urlWithTimestamp = "$REMOTE_MANIFEST_URL?t=$timestamp"
            val backupUrlWithTimestamp = "$BACKUP_MANIFEST_URL?t=$timestamp"
            
            val urls = listOf(urlWithTimestamp, backupUrlWithTimestamp)
            
            for (url in urls) {
                for (attempt in 1..MAX_RETRY_COUNT) {
                    try {
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            .addHeader("Accept", "application/json, text/plain, */*")
                            .addHeader("Accept-Language", "en-US,en;q=0.9")
                            .addHeader("Referer", "https://github.com/")
                            .apply {
                                if (forceRefresh) {
                                    addHeader("Cache-Control", "no-cache")
                                    addHeader("Pragma", "no-cache")
                                }
                            }
                            .build()
                        
                        val response = okHttpClient.newCall(request).execute()
                        
                        if (!response.isSuccessful) {
                            throw IOException("加载清单失败: HTTP ${response.code}")
                        }
                        
                        val json = response.body?.string() 
                            ?: throw IOException("响应体为空")
                        
                        try {
                            val manifest = gson.fromJson(json, SoundsManifest::class.java)
                            
                            if (manifest.sounds.isEmpty()) {
                                Logger.w(TAG, "警告: 解析的 JSON 中没有音频！JSON 子串: ${json.take(200)}...")
                            }
                            
                            // 一主二为：需业修复不完整的音频数据，然后转换 URL
                            val fixedManifest = fixManifestData(manifest)
                            val convertedManifest = convertManifestUrls(fixedManifest)
                            
                            return@withContext convertedManifest
                        } catch (jsonError: Exception) {
                            Logger.e(TAG, "JSON 解析失败: ${jsonError.javaClass.simpleName} - ${jsonError.message}")
                            Logger.e(TAG, "JSON 内容预览 (前500个字符): ${json.take(500)}")
                            throw jsonError
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Logger.w(TAG, "加载失败 (尝试 $attempt/$MAX_RETRY_COUNT, URL: $url): ${e.javaClass.simpleName} - ${e.message}")
                        
                        // 如果不是最后一次尝试，等待后重试
                        if (attempt < MAX_RETRY_COUNT) {
                            val retryDelay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                            delay(retryDelay)
                        }
                    }
                }
                
                // 当前URL所有重试都失败，尝试下一个URL
                Logger.w(TAG, "URL 加载失败: $url，尝试备用 URL...")
            }
            
            // 所有URL都失败
            Logger.e(TAG, "加载网络音频清单失败，所有重试都广頙: ${lastException?.message}")
            throw lastException ?: IOException("加载清单失败")
        }
    }
}

