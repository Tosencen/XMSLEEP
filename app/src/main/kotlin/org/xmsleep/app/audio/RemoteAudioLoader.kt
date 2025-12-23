package org.xmsleep.app.audio

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmsleep.app.audio.model.AudioSource
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
                Log.d(TAG, "开始会载网络音频清单，URL: $url")
                
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
                        
                        Log.d(TAG, "尝试 $attempt/$MAX_RETRY_COUNT: 正在请求 $url...")
                        val response = okHttpClient.newCall(request).execute()
                        
                        Log.d(TAG, "尝试 $attempt/$MAX_RETRY_COUNT: 收到响应，状态码 ${response.code}")
                        
                        if (!response.isSuccessful) {
                            throw IOException("加载清单失败: HTTP ${response.code}")
                        }
                        
                        val json = response.body?.string() 
                            ?: throw IOException("响应体为空")
                        
                        Log.d(TAG, "尝试 $attempt/$MAX_RETRY_COUNT: 收到 JSON，长度: ${json.length} 字符")
                        
                        try {
                            val manifest = gson.fromJson(json, SoundsManifest::class.java)
                            
                            Log.d(TAG, "尝试 $attempt/$MAX_RETRY_COUNT: JSON 解析成功，包含 ${manifest.sounds.size} 个音频，${manifest.categories.size} 个分类")
                            
                            if (manifest.sounds.isEmpty()) {
                                Log.w(TAG, "警告: 解析的 JSON 中没有音频！JSON 子串: ${json.take(200)}...")
                            }
                            
                            // 一主二为：需业修复不完整的音频数据，然后转换 URL
                            val fixedManifest = fixManifestData(manifest)
                            val convertedManifest = convertManifestUrls(fixedManifest)
                            
                            Log.d(TAG, "成功加载网络音频清单，共 ${convertedManifest.sounds.size} 个音频 (尝试 $attempt/$MAX_RETRY_COUNT, URL: $url)")
                            return@withContext convertedManifest
                        } catch (jsonError: Exception) {
                            Log.e(TAG, "JSON 解析失败: ${jsonError.javaClass.simpleName} - ${jsonError.message}")
                            Log.e(TAG, "JSON 内容预览 (前500个字符): ${json.take(500)}")
                            throw jsonError
                        }
                    } catch (e: Exception) {
                        lastException = e
                        Log.w(TAG, "加载失败 (尝试 $attempt/$MAX_RETRY_COUNT, URL: $url): ${e.javaClass.simpleName} - ${e.message}")
                        e.printStackTrace()
                        
                        // 如果不是最后一次尝试，等待后重试
                        if (attempt < MAX_RETRY_COUNT) {
                            val retryDelay = INITIAL_RETRY_DELAY * attempt // 递增延迟
                            delay(retryDelay)
                            Log.d(TAG, "等待 ${retryDelay}ms 后重试...")
                        }
                    }
                }
                
                // 当前URL所有重试都失败，尝试下一个URL
                Log.w(TAG, "URL 加载失败: $url，尝试备用 URL...")
            }
            
            // 所有URL都失败
            Log.e(TAG, "加载网络音频清单失败，所有重试都广頙: ${lastException?.message}")
            throw lastException ?: IOException("加载清单失败")
        }
    }
}

