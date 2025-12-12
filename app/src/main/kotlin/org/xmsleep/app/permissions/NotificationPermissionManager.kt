package org.xmsleep.app.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 通知权限管理器
 * 用于在需要发送通知时请求权限
 */
class NotificationPermissionManager(private val context: Context) {
    
    companion object {
        const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1002
        private const val PREFS_NAME = "notification_permissions"
        private const val KEY_HAS_REQUESTED = "has_requested_notification_permission"
    }
    
    /**
     * 检查通知权限状态
     */
    fun checkNotificationPermission(): PermissionStatus {
        // Android 13 以下不需要通知权限
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return PermissionStatus.GRANTED
        }
        
        val permission = Manifest.permission.POST_NOTIFICATIONS
        
        return when {
            // 已授予权限
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            // 可以显示权限说明
            context is Activity && ActivityCompat.shouldShowRequestPermissionRationale(context, permission) -> {
                PermissionStatus.DENIED
            }
            // 其他情况：首次请求或永久拒绝
            else -> {
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val hasRequested = sharedPrefs.getBoolean(KEY_HAS_REQUESTED, false)
                
                if (hasRequested) {
                    PermissionStatus.PERMANENTLY_DENIED
                } else {
                    PermissionStatus.NOT_REQUESTED
                }
            }
        }
    }
    
    /**
     * 请求通知权限
     */
    fun requestNotificationPermission(activity: Activity, callback: PermissionCallback) {
        // Android 13 以下不需要请求
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            callback.onPermissionGranted()
            return
        }
        
        val permission = Manifest.permission.POST_NOTIFICATIONS
        
        // 记录已经请求过权限
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_HAS_REQUESTED, true).apply()
        
        // 存储回调
        permissionCallback = callback
        
        // 发起权限请求
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            NOTIFICATION_PERMISSION_REQUEST_CODE
        )
    }
    
    /**
     * 处理权限请求结果
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val callback = permissionCallback
            if (callback != null && permissions.isNotEmpty() && grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callback.onPermissionGranted()
                } else {
                    val isPermanent = context is Activity && 
                        !ActivityCompat.shouldShowRequestPermissionRationale(context, permissions[0])
                    callback.onPermissionDenied(isPermanent)
                }
            }
            permissionCallback = null
        }
    }
    
    // 实例变量存储回调
    private var permissionCallback: PermissionCallback? = null
}
