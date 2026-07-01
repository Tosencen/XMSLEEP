package org.xmsleep.app.utils

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
