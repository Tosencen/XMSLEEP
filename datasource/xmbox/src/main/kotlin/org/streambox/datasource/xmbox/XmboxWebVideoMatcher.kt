package org.streambox.datasource.xmbox

import org.streambox.datasource.xmbox.config.XmboxConfig
import android.content.Context

/**
 * XMBOX WebView 视频匹配器
 * 基于 Animeko 的 WebVideoMatcher 设计理念，用于从 WebView 中提取视频 URL
 * 
 * 这个匹配器会：
 * 1. 拦截 WebView 的网络请求，识别视频 URL
 * 2. 执行 CatVodTV JavaScript 代码来提取视频 URL
 * 3. 支持多种视频格式（MP4, M3U8, FLV 等）
 */
class XmboxWebVideoMatcher(
    private val context: Context,
    private val config: XmboxConfig
) {
    private val extractor = WebViewVideoUrlExtractor(context)
    
    /**
     * 匹配结果
     */
    sealed class MatchResult {
        /**
         * 匹配成功，找到视频 URL
         */
        data class Matched(
            val videoUrl: String,
            val headers: Map<String, String> = emptyMap()
        ) : MatchResult()
        
        /**
         * 继续等待/加载
         */
        object Continue : MatchResult()
        
        /**
         * 需要加载页面
         */
        object LoadPage : MatchResult()
        
        /**
         * 匹配失败
         */
        object Failed : MatchResult()
    }
    
    /**
     * 从页面 URL 中提取视频播放地址
     * 
     * @param pageUrl 页面 URL
     * @return 匹配结果
     */
    suspend fun match(pageUrl: String): MatchResult {
        return when (config) {
            is XmboxConfig.VodConfig -> {
                // 对于 JSON 配置的点播源
                matchVodConfig(pageUrl, config)
            }
            
            is XmboxConfig.LiveConfig -> {
                // 对于 JSON 配置的直播源
                matchLiveConfig(config)
            }
            
            is XmboxConfig.JavaScriptConfig -> {
                // 对于 JavaScript 配置
                matchJavaScriptConfig(pageUrl, config)
            }
        }
    }
    
    /**
     * 匹配点播配置
     */
    private suspend fun matchVodConfig(
        pageUrl: String,
        config: XmboxConfig.VodConfig
    ): MatchResult {
        // 如果是直接的视频链接，直接返回
        if (isDirectVideoUrl(pageUrl)) {
            return MatchResult.Matched(pageUrl)
        }
        
        // 否则尝试从页面中提取
        val videoUrl = extractor.extractVideoUrl(pageUrl, config)
        return if (videoUrl != null) {
            MatchResult.Matched(videoUrl)
        } else {
            MatchResult.Continue
        }
    }
    
    /**
     * 匹配直播配置
     */
    private suspend fun matchLiveConfig(
        config: XmboxConfig.LiveConfig
    ): MatchResult {
        // 直播源直接使用 URL
        val headers = buildMap<String, String> {
            config.ua?.let { put("User-Agent", it) }
            config.referer?.let { put("Referer", it) }
            config.origin?.let { put("Origin", it) }
        }
        
        return MatchResult.Matched(config.url, headers)
    }
    
    /**
     * 匹配 JavaScript 配置
     */
    private suspend fun matchJavaScriptConfig(
        pageUrl: String,
        config: XmboxConfig.JavaScriptConfig
    ): MatchResult {
        val videoUrl = extractor.extractVideoUrl(pageUrl, config)
        return if (videoUrl != null) {
            MatchResult.Matched(videoUrl)
        } else {
            MatchResult.Continue
        }
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

