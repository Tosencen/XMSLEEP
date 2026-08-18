package org.xmsleep.app.preferences

import android.content.Context
import org.xmsleep.app.Constants

/**
 * 定时器与番茄时钟偏好设置
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object TimerPrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    fun saveAutoCountdownMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(Constants.PrefsKeys.AUTO_COUNTDOWN_MINUTES, minutes).apply()
    }

    fun getAutoCountdownMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.PrefsKeys.AUTO_COUNTDOWN_MINUTES, 0)
    }

    fun saveLastTimerMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(Constants.PrefsKeys.LAST_TIMER_MINUTES, minutes).apply()
    }

    fun getLastTimerMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.PrefsKeys.LAST_TIMER_MINUTES, 0)
    }

    fun saveKeepScreenOn(context: Context, keepScreenOn: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.KEEP_SCREEN_ON, keepScreenOn).apply()
    }

    fun getKeepScreenOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.KEEP_SCREEN_ON, true)
    }

    fun saveTomatoRingtone(context: Context, ringtoneId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.TOMATO_RINGTONE, ringtoneId).apply()
    }

    fun getTomatoRingtone(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.TOMATO_RINGTONE, "") ?: ""
    }

    fun saveTomatoPulseAnimation(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.TOMATO_PULSE_ANIMATION, enabled).apply()
    }

    fun getTomatoPulseAnimation(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.TOMATO_PULSE_ANIMATION, true)
    }

    fun saveTomatoVibrate(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.TOMATO_VIBRATE, enabled).apply()
    }

    fun getTomatoVibrate(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.TOMATO_VIBRATE, true)
    }

    fun saveShowRecentPlayDialog(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.SHOW_RECENT_PLAY_DIALOG, show).apply()
    }

    fun getShowRecentPlayDialog(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.SHOW_RECENT_PLAY_DIALOG, false)
    }

    fun setAutoPlayOnStart(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.AUTO_PLAY_ON_START, enabled).apply()
    }

    fun getAutoPlayOnStart(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.AUTO_PLAY_ON_START, false)
    }
}
