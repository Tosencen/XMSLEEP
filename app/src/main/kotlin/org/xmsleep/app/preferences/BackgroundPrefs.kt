package org.xmsleep.app.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.xmsleep.app.Constants
import org.xmsleep.app.ui.BackgroundSelection

/**
 * 背景动画偏好设置
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object BackgroundPrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    fun saveBackgroundSelection(context: Context, selection: BackgroundSelection) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.BACKGROUND_SELECTION, selection.value).apply()
    }

    fun getBackgroundSelection(context: Context): BackgroundSelection {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(Constants.PrefsKeys.BACKGROUND_SELECTION, "none") ?: "none"
        return BackgroundSelection.fromValue(value)
    }

    fun saveCustomBackgroundUri(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.CUSTOM_BACKGROUND_URI, uri).apply()
    }

    fun getCustomBackgroundUri(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.CUSTOM_BACKGROUND_URI, null)
    }

    fun saveCustomBackgroundThumbnail(context: Context, uri: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.CUSTOM_BACKGROUND_THUMBNAIL, uri).apply()
    }

    fun getCustomBackgroundThumbnail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.CUSTOM_BACKGROUND_THUMBNAIL, null)
    }

    fun saveCustomBackgroundColor(context: Context, color: Color) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(Constants.PrefsKeys.CUSTOM_BACKGROUND_COLOR, color.value.toLong()).apply()
    }

    fun getCustomBackgroundColor(context: Context, default: Color): Color {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorValue = prefs.getLong(Constants.PrefsKeys.CUSTOM_BACKGROUND_COLOR, -1L)
        return if (colorValue != -1L) Color(colorValue.toULong()) else default
    }

    fun saveBackgroundOpacity(context: Context, opacity: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(Constants.PrefsKeys.BACKGROUND_OPACITY, opacity).apply()
    }

    fun getBackgroundOpacity(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(Constants.PrefsKeys.BACKGROUND_OPACITY, 0.2f)
    }

    fun saveBackgroundBlurRadius(context: Context, radius: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(Constants.PrefsKeys.BACKGROUND_BLUR_RADIUS, radius).apply()
    }

    fun getBackgroundBlurRadius(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(Constants.PrefsKeys.BACKGROUND_BLUR_RADIUS, 0f)
    }
}
