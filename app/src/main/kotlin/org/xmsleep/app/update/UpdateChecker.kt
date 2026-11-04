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
 * 使用未认证请求（60次/小时），避免在应用内嵌入任何访问令牌
 * @param repositoryOwner 仓库所有者
 * @param repositoryName 仓库名称
 */
class UpdateChecker(
    private val repositoryOwner: String = "Tosencen",
    private val repositoryName: String = "XMSLEEP"
) {
    private val client = NetworkClient.default
    
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    /**
     * 检查是否有新版本
     * 检查阶段双通道：gh-proxy.com 镜像 API 优先（国内可达），直连 GitHub API 兜底。
     * @param currentVersion 当前版本号（如 "1.0.0"）
     * @return 如果有新版本返回 NewVersion，否则返回 null
     * @throws IOException 当网络错误或rate limit时抛出
     */
    suspend fun checkLatestVersion(currentVersion: String): NewVersion? = withContext(Dispatchers.IO) {
        try {
            val apiUrl = "${Constants.GITHUB_API_BASE_URL}/repos/$repositoryOwner/$repositoryName/releases/latest"
            // 镜像优先、直链兜底（镜像失败不抛错，继续走直连）
            val body = fetchReleaseBody("$API_MIRROR$apiUrl") ?: fetchReleaseBody(apiUrl)
                ?: throw IOException("检查更新失败：网络不可达")

            Logger.d("UpdateChecker", "当前版本: $currentVersion, 响应体长度: ${body.length}")

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
                        publishedAt = release.publishedAt,
                        sizeBytes = apkAsset.size
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
    
    companion object {
        /** GitHub API 镜像（gh-proxy.com 可代理 api.github.com，国内可达） */
        private const val API_MIRROR = "https://gh-proxy.com/"
    }

    /**
     * 拉取 releases/latest JSON。成功返回响应体；普通失败返回 null（由调用方换下一通道）。
     * - 直连通道遇 rate limit（403 且剩余 0）抛 IOException，携带重置时间提示用户
     * - 镜像通道遇 rate limit 仅返回 null 换直连（镜像额度与本机无关，直连可能仍可用）
     */
    private fun fetchReleaseBody(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                val remaining = resp.header("X-RateLimit-Remaining")?.toIntOrNull() ?: -1
                Logger.d("UpdateChecker", "HTTP ${resp.code} ($url), RateLimit剩余: $remaining")
                if (!resp.isSuccessful) {
                    if (resp.code == 403 && remaining == 0) {
                        val resetTime = resp.header("X-RateLimit-Reset")?.toLongOrNull()?.let {
                            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(it * 1000))
                        } ?: "稍后"
                        if (!url.startsWith(API_MIRROR)) {
                            Logger.e("UpdateChecker", "直连 Rate limit已耗尽，重置时间: $resetTime")
                            throw IOException("GitHub API 请求次数已达上限，请于 $resetTime 后重试")
                        }
                        Logger.w("UpdateChecker", "镜像 Rate limit已耗尽，改走直连")
                    } else {
                        Logger.w("UpdateChecker", "请求失败 HTTP ${resp.code}: $url")
                    }
                    return@use null
                }
                resp.body?.string()
            }
        } catch (e: IOException) {
            if (e.message?.contains("请求次数已达上限") == true) throw e
            Logger.w("UpdateChecker", "通道失败: $url (${e.message})")
            null
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
    val publishedAt: String,
    val sizeBytes: Long = 0          // Release 资产字节数（下载完整性校验用，0=未知）
)
