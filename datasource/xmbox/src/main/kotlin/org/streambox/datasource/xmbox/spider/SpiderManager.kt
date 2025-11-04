package org.streambox.datasource.xmbox.spider

import android.content.Context
import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.Android
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Spider JAR 下载和管理器
 * 负责下载、验证、缓存 CatVod Spider JAR 文件
 */
class SpiderManager(private val context: Context) {
    
    // 使用codeCacheDir而不是filesDir（Android 10+对代码缓存目录的限制更宽松）
    private val spiderDir = File(context.codeCacheDir, "spiders")
    private val httpClient = HttpClient(Android) {
        followRedirects = true
        engine {
            connectTimeout = 30_000
            socketTimeout = 30_000
        }
        expectSuccess = false
    }
    
    init {
        // 确保spider目录存在
        if (!spiderDir.exists()) {
            spiderDir.mkdirs()
        }
    }
    
    /**
     * 下载Spider JAR文件
     * 
     * @param spiderUrl Spider URL，格式：https://example.com/spider.jar;md5;checksum
     * @return 本地JAR文件路径，如果失败返回null
     */
    suspend fun downloadSpider(spiderUrl: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "开始下载Spider: $spiderUrl")
                
                // 解析URL和MD5
                val parts = spiderUrl.split(";")
                val actualUrl = parts[0]
                val expectedMd5 = if (parts.size >= 3) parts[2] else null
                
                // 生成本地文件名（基于URL的hash）
                val fileName = "spider_${actualUrl.hashCode().toString(16)}.jar"
                val localFile = File(spiderDir, fileName)
                
                // 如果文件已存在且MD5匹配，直接返回
                if (localFile.exists() && expectedMd5 != null) {
                    val currentMd5 = calculateMd5(localFile)
                    if (currentMd5 == expectedMd5) {
                        // 确保文件是只读的（Android 10+要求）
                        if (localFile.canWrite()) {
                            localFile.setReadOnly()
                            Log.d(TAG, "将缓存的Spider设置为只读: ${localFile.absolutePath}")
                        }
                        Log.d(TAG, "Spider已缓存: ${localFile.absolutePath}")
                        return@withContext localFile
                    } else {
                        Log.w(TAG, "缓存的Spider MD5不匹配，重新下载")
                        localFile.delete()
                    }
                }
                
                // 下载文件
                Log.d(TAG, "从 $actualUrl 下载Spider...")
                val response = httpClient.get(actualUrl) {
                    header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                }
                
                val bytes = response.body<ByteArray>()
                Log.d(TAG, "下载完成，大小: ${bytes.size} 字节")
                
                // 验证MD5（如果提供）
                if (expectedMd5 != null) {
                    val actualMd5 = calculateMd5(bytes)
                    if (actualMd5 != expectedMd5) {
                        Log.e(TAG, "Spider MD5校验失败！期望: $expectedMd5, 实际: $actualMd5")
                        return@withContext null
                    }
                    Log.d(TAG, "MD5校验通过: $actualMd5")
                }
                
                // 保存到本地
                localFile.writeBytes(bytes)
                
                // 设置文件为只读（Android 10+要求）
                localFile.setReadOnly()
                Log.d(TAG, "Spider已保存到: ${localFile.absolutePath}, 只读=${!localFile.canWrite()}")
                
                localFile
            } catch (e: Exception) {
                Log.e(TAG, "下载Spider失败: ${e.message}", e)
                null
            }
        }
    }
    
    /**
     * 计算文件的MD5
     */
    private fun calculateMd5(file: File): String {
        return calculateMd5(file.readBytes())
    }
    
    /**
     * 计算字节数组的MD5
     */
    private fun calculateMd5(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * 清理所有缓存的Spider
     */
    fun clearCache() {
        spiderDir.listFiles()?.forEach { it.delete() }
        Log.d(TAG, "已清理Spider缓存")
    }
    
    companion object {
        private const val TAG = "SpiderManager"
        
        @Volatile
        private var instance: SpiderManager? = null
        
        fun getInstance(context: Context): SpiderManager {
            return instance ?: synchronized(this) {
                instance ?: SpiderManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

