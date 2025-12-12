package org.xmsleep.app.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 权限状态枚举
 */
enum class PermissionStatus {
    GRANTED,           // 已授予
    DENIED,            // 被拒绝（可以再次请求）
    PERMANENTLY_DENIED, // 永久拒绝（需要去设置页面）
    NOT_REQUESTED      // 未请求过
}

/**
 * 权限请求回调接口
 */
interface PermissionCallback {
    fun onPermissionGranted()
    fun onPermissionDenied(isPermanent: Boolean)
}

/**
 * 音频权限管理器
 * 提供完善的权限请求和状态管理
 */
class AudioPermissionManager(private val context: Context) {
    
    companion object {
        const val AUDIO_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "audio_permissions"
        private const val KEY_HAS_REQUESTED = "has_requested_audio_permission"
        
        /**
         * 获取当前系统版本所需的权限
         */
        private fun getRequiredPermission(): String {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        }
    }
    
    /**
     * 检查存储权限状态
     * 返回详细的权限状态，用于 UI 显示不同的提示
     */
    fun checkStoragePermission(): PermissionStatus {
        val permission = getRequiredPermission()
        
        return when {
            // 已授予权限
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED -> {
                PermissionStatus.GRANTED
            }
            // 可以显示权限说明（用户之前拒绝过，但没有选择"不再询问"）
            context is Activity && ActivityCompat.shouldShowRequestPermissionRationale(context, permission) -> {
                PermissionStatus.DENIED
            }
            // 其他情况：首次请求或永久拒绝
            else -> {
                // 通过 SharedPreferences 判断是否请求过
                val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val hasRequested = sharedPrefs.getBoolean(KEY_HAS_REQUESTED, false)
                
                if (hasRequested) {
                    // 请求过但没有授予，且不显示说明 = 永久拒绝
                    PermissionStatus.PERMANENTLY_DENIED
                } else {
                    // 从未请求过
                    PermissionStatus.NOT_REQUESTED
                }
            }
        }
    }
    
    /**
     * 请求存储权限
     * @param activity 当前 Activity
     * @param callback 权限请求结果回调
     */
    fun requestStoragePermission(activity: Activity, callback: PermissionCallback) {
        val permission = getRequiredPermission()
        android.util.Log.d("AudioPermissionManager", "开始请求权限: $permission")
        android.util.Log.d("AudioPermissionManager", "Activity: $activity")
        
        // 记录已经请求过权限（用于判断永久拒绝状态）
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().putBoolean(KEY_HAS_REQUESTED, true).apply()
        android.util.Log.d("AudioPermissionManager", "已记录权限请求历史")
        
        // 存储回调以便在结果中使用
        permissionCallback = callback
        android.util.Log.d("AudioPermissionManager", "已存储回调")
        
        // 发起权限请求
        try {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                AUDIO_PERMISSION_REQUEST_CODE
            )
            android.util.Log.d("AudioPermissionManager", "权限请求已发起")
        } catch (e: Exception) {
            android.util.Log.e("AudioPermissionManager", "权限请求失败", e)
            throw e
        }
    }
    
    /**
     * 处理权限请求结果
     * 在 Activity 的 onRequestPermissionsResult 中调用
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE) {
            val callback = permissionCallback
            if (callback != null && permissions.isNotEmpty() && grantResults.isNotEmpty()) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 权限已授予
                    callback.onPermissionGranted()
                } else {
                    // 权限被拒绝，判断是否永久拒绝
                    val isPermanent = context is Activity && 
                        !ActivityCompat.shouldShowRequestPermissionRationale(context, permissions[0])
                    callback.onPermissionDenied(isPermanent)
                }
            }
            // 清除回调引用，避免内存泄漏
            permissionCallback = null
        }
    }
    
    /**
     * 检查是否应该显示权限说明
     * 用于在请求权限前显示说明对话框
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        val permission = getRequiredPermission()
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
    
    /**
     * 打开应用设置页面
     * 用于永久拒绝的情况，引导用户手动授予权限
     */
    fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
    
    /**
     * 重置权限请求记录
     * 用于测试或特殊场景
     */
    fun resetPermissionRequest() {
        val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().remove(KEY_HAS_REQUESTED).apply()
    }
    
    // 实例变量存储回调
    private var permissionCallback: PermissionCallback? = null
}