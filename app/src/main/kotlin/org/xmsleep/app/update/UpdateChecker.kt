package org.xmsleep.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.Request
import org.xmsleep.app.Constants
import org.xmsleep.app.utils.Logger
import org.xmsleep.app.utils.NetworkClient
import java.io.IOException

/**
 * GitHub Releases API 更新检查器
 * @param repositoryOwner 仓库所有者
 * @param repositoryName 仓库名称
 * @param githubToken 可选的GitHub Personal Access Token，如果提供则使用认证请求（5000次/小时），否则使用未认证请求（60次/小时）
 */
class UpdateChecker(
    private val repositoryOwner: String = "Tosencen",
    private val repositoryName: String = "XMSLEEP",
    private val githubToken: String? = null
) {
    private val client = NetworkClient.default
    
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    /**
     * 检查是否有新版本
     * @param currentVersion 当前版本号（如 "1.0.0"）
     * @return 如果有新版本返回 NewVersion，否则返回 null
     * @throws IOException 当网络错误或rate limit时抛出
     */
    suspend fun checkLatestVersion(currentVersion: String): NewVersion? = withContext(Dispatchers.IO) {
        try {
            val url = "${Constants.GITHUB_API_BASE_URL}/repos/$repositoryOwner/$repositoryName/releases/latest"
            Logger.d("UpdateChecker", "请求URL: $url")
            Logger.d("UpdateChecker", "当前版本: $currentVersion")
            
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
            
            // 如果提供了Token，添加Authorization header
            if (!githubToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $githubToken")
                Logger.d("UpdateChecker", "使用GitHub Token进行认证请求")
            } else {
                Logger.d("UpdateChecker", "使用未认证请求（限制60次/小时）")
            }
            
            val request = requestBuilder.get().build()
            
            val response = client.newCall(request).execute()
            
            // 检查 rate limit
            val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull() ?: -1
            val rateLimitReset = response.header("X-RateLimit-Reset")?.toLongOrNull()
            
            Logger.d("UpdateChecker", "HTTP响应码: ${response.code}, RateLimit剩余: $remaining")
            
            if (!response.isSuccessful) {
                // 处理 rate limit 错误
                if (response.code == 403 && remaining == 0) {
                    // Rate limit 已耗尽
                    val resetTime = rateLimitReset?.let { 
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(it * 1000))
                    } ?: "稍后"
                    Logger.e("UpdateChecker", "Rate limit已耗尽，重置时间: $resetTime")
                    throw IOException("GitHub API 请求次数已达上限，请于 $resetTime 后重试")
                }
                val errorBody = response.body?.string()
                Logger.e("UpdateChecker", "HTTP请求失败: ${response.code}, 响应体: $errorBody")
                return@withContext null
            }
            
            val body = response.body?.string() ?: run {
                Logger.e("UpdateChecker", "响应体为空")
                return@withContext null
            }
            
            Logger.d("UpdateChecker", "响应体长度: ${body.length}")
            
            val release = json.decodeFromString<GitHubRelease>(body)
            Logger.d("UpdateChecker", "最新Release: tagName=${release.tagName}, name=${release.name}, assets数量=${release.assets.size}")
            
            val latestVersion = release.tagName.removePrefix("v")
            val compareResult = compareVersions(latestVersion, currentVersion)
            Logger.d("UpdateChecker", "版本比较: $latestVersion vs $currentVersion = $compareResult")
            
            // 比较版本号
            if (compareResult > 0) {
                // 查找 APK 下载链接
                val apkAsset = release.assets.firstOrNull { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                Logger.d("UpdateChecker", "找到APK资源: ${apkAsset?.name ?: "未找到"}")
                
                if (apkAsset != null) {
                    // 构建多个下载源：GitHub Release 代理（国内可达）优先，GitHub 直链最后兜底
                    val githubUrl = apkAsset.browserDownloadUrl
                    val downloadUrls = buildDownloadUrls(githubUrl)

                    val newVersion = NewVersion(
                        version = latestVersion,
                        name = release.name.ifEmpty { release.tagName },
                        changelog = release.body,
                        downloadUrls = downloadUrls,
                        publishedAt = release.publishedAt
                    )
                    Logger.d("UpdateChecker", "返回NewVersion: version=${newVersion.version}, 下载源数量=${newVersion.downloadUrls.size}")
                    return@withContext newVersion
                } else {
                    Logger.w("UpdateChecker", "未找到APK资源文件")
                }
            } else {
                Logger.d("UpdateChecker", "当前版本已是最新版本或更新")
            }
            
            null
        } catch (e: IOException) {
            // 重新抛出IOException，让UpdateViewModel正确处理
            Logger.e("UpdateChecker", "IOException: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Logger.e("UpdateChecker", "检查更新失败", e)
            // 其他异常也转换为 IOException
            throw IOException("检查更新失败：${e.message}", e)
        }
    }
    
    /**
     * 构建下载源列表（按国内可达性排序）
     * 依次尝试多个 GitHub Release 代理（如 ghproxy.net），最后以 GitHub 原始直链兜底。
     * 拼接规则：代理地址（带末尾斜杠） + GitHub Release 下载 URL。
     * 例如: https://github.com/Tosencen/XMSLEEP/releases/download/v2.2.8/XMSLEEP-v2.2.8.apk
     * 转换为: https://ghproxy.net/https://github.com/Tosencen/XMSLEEP/releases/download/v2.2.8/XMSLEEP-v2.2.8.apk
     */
    private fun buildDownloadUrls(githubUrl: String): List<String> {
        return try {
            val proxyUrls = Constants.GITHUB_PROXIES.map { proxy ->
                val url = "$proxy$githubUrl"
                Logger.d("UpdateChecker", "生成下载源: $url")
                url
            }
            // 代理在前，GitHub 直链最后兜底
            proxyUrls + githubUrl
        } catch (e: Exception) {
            Logger.e("UpdateChecker", "构建下载源失败，回退到GitHub直链: ${e.message}", e)
            listOf(githubUrl)
        }
    }
    
    /**
     * 比较版本号
     * @return >0 表示 version1 > version2, <0 表示 version1 < version2, 0 表示相等
     */
    private fun compareVersions(version1: String, version2: String): Int {
        val v1Parts = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val v2Parts = version2.split(".").map { it.toIntOrNull() ?: 0 }
        
        val maxLength = maxOf(v1Parts.size, v2Parts.size)
        for (i in 0 until maxLength) {
            val v1Part = v1Parts.getOrElse(i) { 0 }
            val v2Part = v2Parts.getOrElse(i) { 0 }
            val compare = v1Part.compareTo(v2Part)
            if (compare != 0) {
                return compare
            }
        }
        return 0
    }
}

@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val body: String,
    @SerialName("published_at")
    val publishedAt: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val browserDownloadUrl: String,
    val size: Long
)

data class NewVersion(
    val version: String,
    val name: String,
    val changelog: String,
    val downloadUrls: List<String>,  // 下载源列表（按优先级排序，依次尝试）
    val publishedAt: String
)
