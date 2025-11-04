package org.streambox.datasource.xmbox

import android.content.Context
import org.streambox.datasource.xmbox.config.XmboxConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 视频 URL 提取器
 * 从网页中提取视频播放地址
 */
interface VideoUrlExtractor {
    /**
     * 从 URL 提取视频地址
     * 
     * @param pageUrl 网页 URL 或视频 ID
     * @param config 配置
     * @return 视频播放 URL，如果未找到则返回 null
     */
    suspend fun extractVideoUrl(pageUrl: String, config: XmboxConfig): String?
}

/**
 * 使用 WebView 提取视频 URL
 */
class WebViewVideoUrlExtractor(
    private val context: Context
) : VideoUrlExtractor {
    
    private val jsExecutor = CatVodTVJavaScriptExecutor(context)
    
    override suspend fun extractVideoUrl(pageUrl: String, config: XmboxConfig): String? {
        return withContext(Dispatchers.Main) {
            when (config) {
                is XmboxConfig.VodConfig -> {
                    // 对于简单的 JSON 配置，检查 URL 是否是直接的视频链接
                    if (isDirectVideoUrl(pageUrl)) {
                        pageUrl
                    } else if (isDirectVideoUrl(config.url)) {
                        config.url
                    } else {
                        // 如果不是直接视频链接，尝试通过 WebView 加载页面并提取
                        extractFromPage(pageUrl, config)
                    }
                }
                
                is XmboxConfig.LiveConfig -> {
                    // 直播源直接使用 URL
                    if (pageUrl.isNotBlank() && pageUrl != config.url) {
                        // 如果 pageUrl 是具体的播放地址，使用它
                        pageUrl
                    } else {
                        config.url
                    }
                }
                
                is XmboxConfig.JavaScriptConfig -> {
                    // 对于 JavaScript 配置，在 WebView 中执行 JS 代码
                    extractFromJavaScript(pageUrl, config)
                }
            }
        }
    }
    
    /**
     * 从页面中提取视频 URL（用于简单的 VOD 配置）
     */
    private suspend fun extractFromPage(
        pageUrl: String,
        config: XmboxConfig.VodConfig
    ): String? {
        val urls = jsExecutor.execute(pageUrl, "", config.timeout.toLong())
        return urls.firstOrNull()
    }
    
    /**
     * 从 JavaScript 配置中提取视频 URL
     */
    private suspend fun extractFromJavaScript(
        pageUrl: String,
        config: XmboxConfig.JavaScriptConfig
    ): String? {
        val urls = jsExecutor.execute(pageUrl, config.jsCode, config.timeout.toLong())
        return urls.firstOrNull()
    }
    
    /**
     * 判断是否是直接的视频 URL
     */
    private fun isDirectVideoUrl(url: String): Boolean {
        if (url.isBlank()) return false
        
        val videoExtensions = listOf(
            ".mp4", ".mkv", ".m3u8", ".flv", ".avi", 
            ".webm", ".mov", ".wmv", ".ts", ".m3u"
        )
        val lowerUrl = url.lowercase()
        
        return videoExtensions.any { lowerUrl.endsWith(it) } ||
               Regex("""(m3u8|mp4|flv|avi|mkv|webm|mov|wmv|ts)""", RegexOption.IGNORE_CASE)
                   .containsMatchIn(url)
    }
}

