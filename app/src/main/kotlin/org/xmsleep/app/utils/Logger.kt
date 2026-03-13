package org.xmsleep.app.utils

import android.util.Log
import org.xmsleep.app.BuildConfig
import timber.log.Timber

/**
 * 统一日志管理工具
 * 使用 Timber 框架，在 Release 版本中自动禁用日志输出
 */
object Logger {

    /**
     * 初始化 Timber
     * 必须在 Application 类中调用
     */
    fun init() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    /**
     * 调试日志 (Debug)
     * 用于开发调试，Release 版本不输出
     */
    fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    /**
     * 信息日志 (Info)
     * 重要信息，Release 版本不输出
     */
    fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }

    /**
     * 警告日志 (Warning)
     * 警告信息，Release 版本输出
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).w(throwable, message)
        } else {
            Timber.tag(tag).w(message)
        }
    }

    /**
     * 错误日志 (Error)
     * 错误信息，Release 版本输出
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Timber.tag(tag).e(throwable, message)
        } else {
            Timber.tag(tag).e(message)
        }
    }

    /**
     * Verbose 日志
     * 详细信息，Release 版本不输出
     */
    fun v(tag: String, message: String) {
        Timber.tag(tag).v(message)
    }
}
