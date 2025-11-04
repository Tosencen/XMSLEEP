package org.streambox.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

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
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
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
            val url = "https://api.github.com/repos/$repositoryOwner/$repositoryName/releases/latest"
            android.util.Log.d("UpdateChecker", "请求URL: $url")
            android.util.Log.d("UpdateChecker", "当前版本: $currentVersion")
            
            val requestBuilder = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
            
            // 如果提供了Token，添加Authorization header
            if (!githubToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $githubToken")
                android.util.Log.d("UpdateChecker", "使用GitHub Token进行认证请求")
            } else {
                android.util.Log.d("UpdateChecker", "使用未认证请求（限制60次/小时）")
            }
            
            val request = requestBuilder.get().build()
            
            val response = client.newCall(request).execute()
            
            // 检查 rate limit
            val remaining = response.header("X-RateLimit-Remaining")?.toIntOrNull() ?: -1
            val rateLimitReset = response.header("X-RateLimit-Reset")?.toLongOrNull()
            
            android.util.Log.d("UpdateChecker", "HTTP响应码: ${response.code}, RateLimit剩余: $remaining")
            
            if (!response.isSuccessful) {
                // 处理 rate limit 错误
                if (response.code == 403 && remaining == 0) {
                    // Rate limit 已耗尽
                    val resetTime = rateLimitReset?.let { 
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(it * 1000))
                    } ?: "稍后"
                    android.util.Log.e("UpdateChecker", "Rate limit已耗尽，重置时间: $resetTime")
                    throw IOException("GitHub API 请求次数已达上限，请于 $resetTime 后重试")
                }
                val errorBody = response.body?.string()
                android.util.Log.e("UpdateChecker", "HTTP请求失败: ${response.code}, 响应体: $errorBody")
                return@withContext null
            }
            
            val body = response.body?.string() ?: run {
                android.util.Log.e("UpdateChecker", "响应体为空")
                return@withContext null
            }
            
            android.util.Log.d("UpdateChecker", "响应体长度: ${body.length}")
            
            val release = json.decodeFromString<GitHubRelease>(body)
            android.util.Log.d("UpdateChecker", "最新Release: tagName=${release.tagName}, name=${release.name}, assets数量=${release.assets.size}")
            
            val latestVersion = release.tagName.removePrefix("v")
            val compareResult = compareVersions(latestVersion, currentVersion)
            android.util.Log.d("UpdateChecker", "版本比较: $latestVersion vs $currentVersion = $compareResult")
            
            // 比较版本号
            if (compareResult > 0) {
                // 查找 APK 下载链接
                val apkAsset = release.assets.firstOrNull { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                android.util.Log.d("UpdateChecker", "找到APK资源: ${apkAsset?.name ?: "未找到"}")
                
                if (apkAsset != null) {
                    val newVersion = NewVersion(
                        version = latestVersion,
                        name = release.name.ifEmpty { release.tagName },
                        changelog = release.body,
                        downloadUrl = apkAsset.browserDownloadUrl,
                        publishedAt = release.publishedAt
                    )
                    android.util.Log.d("UpdateChecker", "返回NewVersion: version=${newVersion.version}, downloadUrl=${newVersion.downloadUrl}")
                    return@withContext newVersion
                } else {
                    android.util.Log.w("UpdateChecker", "未找到APK资源文件")
                }
            } else {
                android.util.Log.d("UpdateChecker", "当前版本已是最新版本或更新")
            }
            
            null
        } catch (e: IOException) {
            // 重新抛出IOException，让UpdateViewModel正确处理
            android.util.Log.e("UpdateChecker", "IOException: ${e.message}", e)
            throw e
        } catch (e: Exception) {
            android.util.Log.e("UpdateChecker", "Exception: ${e.message}", e)
            e.printStackTrace()
            // 其他异常也转换为IOException
            throw IOException("检查更新失败: ${e.message}", e)
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
    val downloadUrl: String,
    val publishedAt: String
)
