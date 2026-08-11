package org.xmsleep.app.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import org.xmsleep.app.utils.Logger

/**
 * 匿名使用统计埋点（Firebase Analytics）
 *
 * - 只上报匿名事件，不携带任何个人身份信息
 * - 无 google-services.json 的构建（F-Droid/CI 等）下 FirebaseApp 不会初始化，
 *   此时自动降级为空实现：不崩溃、不采集任何数据
 */
object AnalyticsLogger {

    private const val TAG = "AnalyticsLogger"

    private var analytics: FirebaseAnalytics? = null

    fun initialize(context: Context) {
        try {
            analytics = if (FirebaseApp.getApps(context).isNotEmpty()) {
                FirebaseAnalytics.getInstance(context)
            } else {
                Logger.d(TAG, "FirebaseApp 未初始化，统计功能停用")
                null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Analytics 初始化失败，统计功能停用: ${e.message}", e)
            analytics = null
        }
    }

    fun isEnabled(): Boolean = analytics != null

    private fun log(eventName: String, params: Bundle? = null) {
        val instance = analytics ?: return
        try {
            instance.logEvent(eventName, params)
            // 仅 debug 构建可见（release 构建会被 R8 移除），便于排查埋点是否生效
            Logger.d(TAG, "埋点上报: $eventName ${params?.keySet()?.joinToString()}")
        } catch (e: Exception) {
            Logger.e(TAG, "上报事件失败 $eventName: ${e.message}", e)
        }
    }

    /** 播放声音（内置/网络声音） */
    fun logSoundPlay(soundName: String) {
        log("sound_play", Bundle().apply { putString("sound_name", soundName) })
    }

    /** 番茄钟完成（phase: focus=专注结束，break=休息结束） */
    fun logTomatoComplete(phase: String, focusMinutes: Int) {
        log("tomato_complete", Bundle().apply {
            putString("phase", phase)
            putInt("focus_minutes", focusMinutes)
        })
    }

    /** 设置倒计时 */
    fun logCountdownSet(minutes: Int) {
        log("countdown_set", Bundle().apply { putInt("minutes", minutes) })
    }

    /** 开始收听（含息屏/后台播放） */
    fun logListeningStart() {
        log("listening_start")
    }

    /** 结束收听，上报真实收听时长（秒） */
    fun logListeningEnd(durationSeconds: Long) {
        log("listening_end", Bundle().apply { putLong("duration_seconds", durationSeconds) })
    }
}
