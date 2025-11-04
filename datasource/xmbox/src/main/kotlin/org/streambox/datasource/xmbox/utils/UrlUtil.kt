package org.streambox.datasource.xmbox.utils

import android.net.Uri
import android.util.Log

/**
 * URL工具类
 * 从XMBOX的UrlUtil移植
 * 支持URL转换和解析
 */
object UrlUtil {
    
    private const val TAG = "UrlUtil"
    
    /**
     * 获取URL的scheme
     */
    fun scheme(url: String?): String {
        if (url == null || url.isBlank()) return ""
        return try {
            val uri = Uri.parse(url.trim())
            uri.scheme?.lowercase()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 获取URL的host
     */
    fun host(url: String?): String {
        if (url == null || url.isBlank()) return ""
        return try {
            val uri = Uri.parse(url.trim())
            uri.host?.lowercase()?.trim() ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 解析URI
     */
    fun uri(url: String): Uri {
        return Uri.parse(url.trim().replace("\\", ""))
    }
    
    /**
     * 转换URL（处理特殊scheme）
     * 从XMBOX的UrlUtil.convert()移植
     * 
     * 对于assets://, file://, proxy://，如果没有本地服务器，直接读取文件内容
     */
    fun convert(url: String): String {
        if (url.isBlank()) return url
        
        val scheme = scheme(url)
        
        return when (scheme) {
            "assets" -> {
                // assets:// - 从assets目录读取
                // 如果没有本地服务器，无法转换，返回原URL
                // 实际使用中，assets://通常用于内置配置，可以在添加源时直接读取
                url
            }
            "file" -> {
                // file:// - 本地文件
                // 尝试提取文件路径，直接读取文件内容
                // 格式：file:///path/to/file 或 file://path/to/file
                val filePath = url.removePrefix("file://")
                // 如果路径是绝对路径，直接使用；否则需要进一步处理
                if (filePath.startsWith("/")) {
                    filePath
                } else {
                    "/$filePath"
                }
            }
            "proxy" -> {
                // proxy:// - 需要通过代理访问
                // 如果没有本地服务器，无法转换，返回原URL
                // 实际使用中，proxy://需要本地服务器，暂时返回原URL
                url
            }
            else -> url
        }
    }
    
    /**
     * 解析相对URL
     * 从XMBOX的UrlUtil.resolve()移植
     */
    fun resolve(baseUri: String, referenceUri: String): String {
        return try {
            val base = Uri.parse(baseUri.trim())
            val resolved = base.buildUpon()
            when {
                referenceUri == "../" -> {
                    // 向上级目录
                    val path = base.path ?: ""
                    val lastSlash = path.lastIndexOf("/")
                    if (lastSlash > 0) {
                        resolved.path(path.substring(0, lastSlash + 1))
                    }
                    resolved.build().toString()
                }
                referenceUri == "./" -> {
                    // 当前目录
                    baseUri
                }
                referenceUri.startsWith("../") -> {
                    // 相对路径向上
                    val basePath = base.path ?: "/"
                    var refPath = referenceUri.removePrefix("../")
                    
                    var pathSegments = basePath.split("/").filter { it.isNotBlank() }
                    while (refPath.startsWith("../")) {
                        if (pathSegments.isNotEmpty()) {
                            pathSegments = pathSegments.dropLast(1)
                        }
                        refPath = refPath.removePrefix("../")
                    }
                    
                    val newPath = "/" + pathSegments.joinToString("/") + "/" + refPath
                    resolved.path(newPath)
                    resolved.build().toString()
                }
                referenceUri.startsWith("./") -> {
                    // 相对路径当前目录
                    val basePath = base.path ?: "/"
                    val refPath = referenceUri.removePrefix("./")
                    val newPath = if (basePath.endsWith("/")) {
                        basePath + refPath
                    } else {
                        basePath.substringBeforeLast("/") + "/" + refPath
                    }
                    resolved.path(newPath)
                    resolved.build().toString()
                }
                else -> {
                    // 其他情况，直接拼接
                    Uri.withAppendedPath(base, referenceUri).toString()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "解析相对URL失败: base=$baseUri, ref=$referenceUri", e)
            // Fallback: 简单拼接
            if (baseUri.endsWith("/")) {
                baseUri + referenceUri.removePrefix("./").removePrefix("../")
            } else {
                val lastSlash = baseUri.lastIndexOf("/")
                if (lastSlash >= 0) {
                    baseUri.substring(0, lastSlash + 1) + referenceUri.removePrefix("./").removePrefix("../")
                } else {
                    baseUri + "/" + referenceUri.removePrefix("./").removePrefix("../")
                }
            }
        }
    }
    
    /**
     * 修复Header名称（标准化）
     */
    fun fixHeader(key: String): String {
        val lowerKey = key.lowercase()
        return when {
            lowerKey == "user-agent" || lowerKey == "useragent" -> "User-Agent"
            lowerKey == "referer" || lowerKey == "referrer" -> "Referer"
            lowerKey == "cookie" -> "Cookie"
            else -> key
        }
    }
}

