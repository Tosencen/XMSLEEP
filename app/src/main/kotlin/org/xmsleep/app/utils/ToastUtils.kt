package org.xmsleep.app.utils

import android.content.Context

/**
 * 统一的 Toast 工具类（保留旧 API 兼容性）
 */
object ToastUtils {
    fun showToast(context: Context, message: String, duration: Int = android.widget.Toast.LENGTH_SHORT) {
        // 使用原生 Toast，设置为居中显示
        val toast = android.widget.Toast.makeText(context, message, duration)
        toast.setGravity(android.view.Gravity.CENTER, 0, 0) // 居中显示
        toast.show()
    }
}

