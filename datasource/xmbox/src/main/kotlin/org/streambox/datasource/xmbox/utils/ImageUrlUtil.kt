package org.streambox.datasource.xmbox.utils

import android.net.Uri
import android.text.TextUtils
import android.util.Log
import kotlinx.serialization.json.*

/**
 * 图片URL处理工具
 * 从XMBOX的ImgUtil.getUrl()移植
 * 支持URL中的特殊参数：@Headers=, @Cookie=, @Referer=, @User-Agent=
 */
object ImageUrlUtil {
    
    private const val TAG = "ImageUrlUtil"
    
    /**
     * 解析URL中的Headers参数
     * 格式：url@Headers={"key":"value"}@Cookie=value@Referer=url
     * 
     * @return Pair<实际URL, Headers映射>
     */
    fun parseUrlWithHeaders(url: String): Pair<String, Map<String, String>> {
        if (url.isBlank()) return "" to emptyMap()
        
        var actualUrl = url
        val headers = mutableMapOf<String, String>()
        
        // 处理 @Headers= 参数（JSON格式）
        if (url.contains("@Headers=")) {
            try {
                val headersStr = url.split("@Headers=")[1].split("@")[0]
                val json = Json { ignoreUnknownKeys = true }
                val jsonObj = json.parseToJsonElement(headersStr) as? JsonObject
                jsonObj?.forEach { (key, value) ->
                    val headerKey = fixHeaderName(key)
                    headers[headerKey] = value.jsonPrimitive.content
                }
            } catch (e: Exception) {
                Log.w(TAG, "解析@Headers失败: ${e.message}")
            }
            actualUrl = url.substringBefore("@Headers=")
        }
        
        // 处理 @Cookie= 参数
        if (url.contains("@Cookie=")) {
            val cookie = url.split("@Cookie=")[1].split("@")[0]
            headers["Cookie"] = cookie
            if (!actualUrl.contains("@Cookie=")) {
                actualUrl = actualUrl.substringBefore("@Cookie=")
            }
        }
        
        // 处理 @Referer= 参数
        if (url.contains("@Referer=")) {
            val referer = url.split("@Referer=")[1].split("@")[0]
            headers["Referer"] = referer
            if (!actualUrl.contains("@Referer=")) {
                actualUrl = actualUrl.substringBefore("@Referer=")
            }
        }
        
        // 处理 @User-Agent= 参数
        if (url.contains("@User-Agent=")) {
            val userAgent = url.split("@User-Agent=")[1].split("@")[0]
            headers["User-Agent"] = userAgent
            if (!actualUrl.contains("@User-Agent=")) {
                actualUrl = actualUrl.substringBefore("@User-Agent=")
            }
        }
        
        // 清理URL（移除所有@参数）
        actualUrl = actualUrl.split("@").firstOrNull() ?: actualUrl
        
        return actualUrl.trim() to headers
    }
    
    /**
     * 修复Header名称（标准化）
     */
    private fun fixHeaderName(key: String): String {
        val lowerKey = key.lowercase()
        return when {
            lowerKey == "user-agent" || lowerKey == "useragent" -> "User-Agent"
            lowerKey == "referer" || lowerKey == "referrer" -> "Referer"
            lowerKey == "cookie" -> "Cookie"
            else -> key
        }
    }
    
    /**
     * URL转换（支持assets://, file://, proxy://等）
     * 从XMBOX的UrlUtil.convert()移植
     * 注意：proxy://需要本地服务器支持，这里只做基本转换
     */
    fun convertUrl(url: String): String {
        if (url.isBlank()) return url
        
        val scheme = Uri.parse(url).scheme?.lowercase() ?: ""
        
        return when (scheme) {
            "assets" -> {
                // assets:// 需要本地服务器支持，这里返回原URL
                // 如果需要，可以替换为本地服务器地址
                url.replace("assets://", "/")
            }
            "file" -> {
                // file:// 需要本地服务器支持
                url.replace("file://", "/file/")
            }
            "proxy" -> {
                // proxy:// 需要本地服务器支持
                url.replace("proxy://", "/proxy?")
            }
            else -> url
        }
    }
    
    /**
     * 获取完整的图片URL（处理后）
     */
    fun getImageUrl(originalUrl: String): String {
        if (originalUrl.isBlank()) return ""
        
        // 如果是data URI，直接返回
        if (originalUrl.startsWith("data:")) {
            return originalUrl
        }
        
        // 转换URL
        val (url, _) = parseUrlWithHeaders(originalUrl)
        return convertUrl(url)
    }
    
    /**
     * 获取URL和Headers（用于需要Headers的图片加载器）
     */
    fun getImageUrlWithHeaders(originalUrl: String): Pair<String, Map<String, String>> {
        if (originalUrl.isBlank()) return "" to emptyMap()
        
        // 如果是data URI，直接返回
        if (originalUrl.startsWith("data:")) {
            return originalUrl to emptyMap()
        }
        
        val (url, headers) = parseUrlWithHeaders(originalUrl)
        val convertedUrl = convertUrl(url)
        return convertedUrl to headers
    }
}

