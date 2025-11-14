package org.xmsleep.app.utils

import android.util.Log
import org.xmsleep.app.BuildConfig

/**
 * 统一日志管理工具
 * 在 Release 版本中自动禁用日志输出
 */
object Logger {
    
    /**
     * 是否启用日志
     * Debug 版本启用，Release 版本禁用
     */
    private val isEnabled: Boolean = BuildConfig.DEBUG
    
    /**
     * 调试日志 (Debug)
     * 用于开发调试，Release 版本不输出
     */
    fun d(tag: String, message: String) {
        if (isEnabled) {
            Log.d(tag, message)
        }
    }
    
    /**
     * 信息日志 (Info)
     * 重要信息，Release 版本不输出
     */
    fun i(tag: String, message: String) {
        if (isEnabled) {
            Log.i(tag, message)
        }
    }
    
    /**
     * 警告日志 (Warning)
     * 警告信息，Release 版本输出
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.w(tag, message, throwable)
        } else {
            Log.w(tag, message)
        }
    }
    
    /**
     * 错误日志 (Error)
     * 错误信息，Release 版本输出
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }
    
    /**
     * Verbose 日志
     * 详细信息，Release 版本不输出
     */
    fun v(tag: String, message: String) {
        if (isEnabled) {
            Log.v(tag, message)
        }
    }
}
