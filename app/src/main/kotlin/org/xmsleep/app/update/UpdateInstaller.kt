package org.xmsleep.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Android 更新安装器
 */
class UpdateInstaller(private val context: Context) {
    
    /**
     * 检查是否有安装权限
     * @return 是否有安装权限
     */
    fun hasInstallPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }
    
    /**
     * 请求安装权限（打开设置页面）
     */
    fun requestInstallPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * 安装 APK 文件
     * @param apkFile APK 文件
     * @return 是否成功启动安装流程
     */
    fun install(apkFile: File): Boolean {
        return try {
            // Android 8.0+ 需要请求安装权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!hasInstallPermission()) {
                    // 没有权限，返回 false，由调用方处理
                    return false
                }
            }
            
            // 检查文件是否存在
            if (!apkFile.exists() || !apkFile.isFile) {
                return false
            }
            
            // 使用 FileProvider 获取 URI
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
