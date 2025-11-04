package org.streambox.app.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * GitHub Releases API 更新检查器
 */
class UpdateChecker(
    private val repositoryOwner: String = "Tosencen",
    private val repositoryName: String = "XMSLEEP"
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
     */
    suspend fun checkLatestVersion(currentVersion: String): NewVersion? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/$repositoryOwner/$repositoryName/releases/latest"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext null
            }
            
            val body = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GitHubRelease>(body)
            
            // 比较版本号
            if (compareVersions(release.tagName.removePrefix("v"), currentVersion) > 0) {
                // 查找 APK 下载链接
                val apkAsset = release.assets.firstOrNull { 
                    it.name.endsWith(".apk", ignoreCase = true) 
                }
                
                if (apkAsset != null) {
                    return@withContext NewVersion(
                        version = release.tagName.removePrefix("v"),
                        name = release.name.ifEmpty { release.tagName },
                        changelog = release.body,
                        downloadUrl = apkAsset.browserDownloadUrl,
                        publishedAt = release.publishedAt
                    )
                }
            }
            
            null
        } catch (e: IOException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
    val tagName: String,
    val name: String,
    val body: String,
    val publishedAt: String,
    val assets: List<GitHubAsset>
)

@Serializable
data class GitHubAsset(
    val name: String,
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
