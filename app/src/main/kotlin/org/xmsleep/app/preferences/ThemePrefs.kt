package org.xmsleep.app.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.xmsleep.app.Constants
import org.xmsleep.app.theme.DarkModeOption

/**
 * 主题与外观偏好设置
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object ThemePrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    fun saveDarkMode(context: Context, darkMode: DarkModeOption) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.DARK_MODE, darkMode.name).apply()
    }

    fun getDarkMode(context: Context, default: DarkModeOption = DarkModeOption.DARK): DarkModeOption {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(Constants.PrefsKeys.DARK_MODE, null)
        return if (modeName != null) {
            try {
                DarkModeOption.valueOf(modeName)
            } catch (e: IllegalArgumentException) {
                default
            }
        } else {
            default
        }
    }

    fun saveSelectedColor(context: Context, color: Color) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(Constants.PrefsKeys.SELECTED_COLOR, color.value.toLong()).apply()
    }

    fun getSelectedColor(context: Context, default: Color): Color {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorValue = prefs.getLong(Constants.PrefsKeys.SELECTED_COLOR, -1L)
        return if (colorValue != -1L) {
            Color(colorValue.toULong())
        } else {
            default
        }
    }

    fun saveUseDynamicColor(context: Context, useDynamicColor: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.USE_DYNAMIC_COLOR, useDynamicColor).apply()
    }

    fun getUseDynamicColor(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.USE_DYNAMIC_COLOR, default)
    }

    fun saveUseBlackBackground(context: Context, useBlackBackground: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.USE_BLACK_BACKGROUND, useBlackBackground).apply()
    }

    fun getUseBlackBackground(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.USE_BLACK_BACKGROUND, default)
    }

    fun saveUseMonochrome(context: Context, useMonochrome: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.USE_MONOCHROME, useMonochrome).apply()
    }

    fun getUseMonochrome(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.USE_MONOCHROME, default)
    }

    fun saveHideAnimation(context: Context, hideAnimation: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.HIDE_ANIMATION, hideAnimation).apply()
    }

    fun getHideAnimation(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.HIDE_ANIMATION, default)
    }

    fun saveFlipClockSensorRotation(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.FLIP_CLOCK_SENSOR_ROTATION, enabled).apply()
    }

    fun getFlipClockSensorRotation(context: Context, default: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.FLIP_CLOCK_SENSOR_ROTATION, default)
    }

    fun saveSoundCardsColumnsCount(context: Context, columnsCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(Constants.PrefsKeys.SOUND_CARDS_COLUMNS, columnsCount).apply()
    }

    fun getSoundCardsColumnsCount(context: Context, default: Int = 2): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.PrefsKeys.SOUND_CARDS_COLUMNS, default)
    }

    fun saveStarSkyColumnsCount(context: Context, columnsCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(Constants.PrefsKeys.STAR_SKY_COLUMNS_COUNT, columnsCount).apply()
    }

    fun getStarSkyColumnsCount(context: Context, default: Int = 3): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.PrefsKeys.STAR_SKY_COLUMNS_COUNT, default)
    }

    fun saveQuickPlayExpanded(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.QUICK_PLAY_EXPANDED, isExpanded).apply()
    }

    fun getQuickPlayExpanded(context: Context, default: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.QUICK_PLAY_EXPANDED, default)
    }

    fun saveNowPlayingExpanded(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.NOW_PLAYING_EXPANDED, isExpanded).apply()
    }

    fun getNowPlayingExpanded(context: Context, default: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.NOW_PLAYING_EXPANDED, default)
    }
}
