package org.xmsleep.app.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.xmsleep.app.MainActivity
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全局异常处理器
 * 捕获未处理的异常并显示崩溃页面
 */
class CrashHandler private constructor(private val context: Context) : Thread.UncaughtExceptionHandler {
    
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
    
    companion object {
        private const val TAG = "CrashHandler"
        private const val CRASH_ERROR_KEY = "crash_error_message"
        private const val CRASH_STACK_KEY = "crash_stack_trace"
        
        @Volatile
        private var instance: CrashHandler? = null
        
        fun init(context: Context) {
            if (instance == null) {
                synchronized(CrashHandler::class.java) {
                    if (instance == null) {
                        instance = CrashHandler(context.applicationContext)
                    }
                }
            }
            Thread.setDefaultUncaughtExceptionHandler(instance)
        }
    }
    
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            // 记录异常信息
            Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)
            
            // 获取异常信息
            val errorMessage = throwable.message ?: throwable::class.java.simpleName
            val stackTrace = getStackTraceString(throwable)
            
            // 保存崩溃信息到文件或日志（可选）
            saveCrashLog(errorMessage, stackTrace)
            
            // 重启应用并显示崩溃页面
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(CRASH_ERROR_KEY, errorMessage)
                putExtra(CRASH_STACK_KEY, stackTrace)
            }
            context.startActivity(intent)
            
            // 终止进程
            android.os.Process.killProcess(android.os.Process.myPid())
            kotlin.system.exitProcess(1)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in crash handler", e)
            // 如果崩溃处理失败，使用默认处理器
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
    
    /**
     * 获取堆栈跟踪字符串
     */
    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
    
    /**
     * 保存崩溃日志（可选实现）
     */
    private fun saveCrashLog(errorMessage: String, stackTrace: String) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val deviceInfo = buildDeviceInfo()
            
            val crashLog = """
                ========== CRASH LOG ==========
                Time: $timestamp
                Error: $errorMessage
                
                Device Info:
                $deviceInfo
                
                Stack Trace:
                $stackTrace
                ===============================
            """.trimIndent()
            
            Log.e(TAG, crashLog)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }
    
    /**
     * 构建设备信息
     */
    private fun buildDeviceInfo(): String {
        return """
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            Device: ${Build.MANUFACTURER} ${Build.MODEL}
            Brand: ${Build.BRAND}
            Device: ${Build.DEVICE}
            Board: ${Build.BOARD}
        """.trimIndent()
    }
}

/**
 * 从 Intent 中提取崩溃信息
 */
fun Intent.getCrashInfo(): Pair<String?, String?> {
    val errorMessage = getStringExtra("crash_error_message")
    val stackTrace = getStringExtra("crash_stack_trace")
    return errorMessage to stackTrace
}
