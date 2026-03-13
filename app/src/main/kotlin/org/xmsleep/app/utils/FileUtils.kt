package org.xmsleep.app.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 获取目录大小（排除指定的子目录）
 */
fun getDirectorySizeExcluding(directory: File, excludeDirName: String): Long {
    var size = 0L
    try {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                // 排除指定目录
                if (file.name == excludeDirName && file.isDirectory) {
                    return@forEach
                }
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
    } catch (e: Exception) {
        // 忽略错误，继续计算其他文件
    }
    return size
}

/**
 * 获取目录大小
 */
fun getDirectorySize(directory: File): Long {
    var size = 0L
    try {
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
    } catch (e: Exception) {
        // 忽略错误，继续计算其他文件
    }
    return size
}

/**
 * 格式化字节数为可读格式
 */
fun formatBytes(bytes: Long): String {
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.getDefault(), "%.2f KB", kb)
        else -> "$bytes B"
    }
}

/**
 * 计算缓存大小（包括音频缓存和应用缓存）
 */
suspend fun calculateCacheSize(context: Context): Long {
    return withContext(Dispatchers.IO) {
        var totalSize = 0L
        
        // 首先获取专用音频缓存大小
        try {
            val cacheManager = org.xmsleep.app.audio.AudioCacheManager.getInstance(context)
            totalSize += cacheManager.getCacheSize()
        } catch (e: Exception) {
            // 如果获取音频缓存失败，继续计算其他缓存
            Logger.w("FileUtils", "获取音频缓存大小失败: ${e.message}")
        }
        
        // 内部缓存目录（排除音频缓存子目录以避免重复计算）
        context.cacheDir?.let { cacheDir ->
            if (cacheDir.exists()) {
                // 计算缓存目录大小（排除 audio_cache 子目录）
                totalSize += getDirectorySizeExcluding(cacheDir, "audio_cache")
            }
        }
        
        // 外部缓存目录
        context.externalCacheDir?.let { externalCacheDir ->
            if (externalCacheDir.exists()) {
                totalSize += getDirectorySize(externalCacheDir)
            }
        }
        
        totalSize
    }
}

/**
 * 清理应用缓存
 */
suspend fun clearApplicationCache(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            // 清理音频缓存
            try {
                val cacheManager = org.xmsleep.app.audio.AudioCacheManager.getInstance(context)
                cacheManager.clearCache()
                Logger.d("FileUtils", "音频缓存清理完成")
            } catch (e: Exception) {
                Logger.w("FileUtils", "音频缓存清理失败: ${e.message}")
            }
            
            // 清理应用缓存目录
            val cacheDir = context.cacheDir
            if (cacheDir != null && cacheDir.exists()) {
                deleteRecursive(cacheDir)
                cacheDir.mkdirs()
            }
            
            // 清理外部缓存目录
            val externalCacheDir = context.externalCacheDir
            if (externalCacheDir != null && externalCacheDir.exists()) {
                deleteRecursive(externalCacheDir)
                externalCacheDir.mkdirs()
            }
        } catch (e: Exception) {
            throw e
        }
    }
}

/**
 * 递归删除目录
 */
fun deleteRecursive(fileOrDirectory: File) {
    if (fileOrDirectory.isDirectory) {
        fileOrDirectory.listFiles()?.forEach { child ->
            deleteRecursive(child)
        }
    }
    fileOrDirectory.delete()
}
