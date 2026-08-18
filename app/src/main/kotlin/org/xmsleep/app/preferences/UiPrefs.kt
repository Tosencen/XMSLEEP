package org.xmsleep.app.preferences

import android.content.Context
import org.xmsleep.app.Constants
import org.xmsleep.app.utils.Logger

/**
 * UI 与通用偏好设置（悬浮按钮、页面开关、小组件、设备标识、迁移）
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object UiPrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    // === 悬浮按钮 ===

    fun saveFloatingButtonPosition(context: Context, x: Float, y: Float, isLeft: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(Constants.PrefsKeys.FLOATING_BUTTON_X, x)
            .putFloat(Constants.PrefsKeys.FLOATING_BUTTON_Y, y)
            .putBoolean(Constants.PrefsKeys.FLOATING_BUTTON_IS_LEFT, isLeft)
            .apply()
    }

    /** 保存浮动按钮位置（简化版，只保存Y和isLeft） */
    fun saveFloatingButtonPosition(context: Context, y: Float, isLeft: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(Constants.PrefsKeys.FLOATING_BUTTON_Y, y)
            .putBoolean(Constants.PrefsKeys.FLOATING_BUTTON_IS_LEFT, isLeft)
            .apply()
    }

    fun getFloatingButtonPosition(context: Context, defaultX: Float, defaultY: Float, defaultIsLeft: Boolean): Triple<Float, Float, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val x = prefs.getFloat(Constants.PrefsKeys.FLOATING_BUTTON_X, defaultX)
        val y = prefs.getFloat(Constants.PrefsKeys.FLOATING_BUTTON_Y, defaultY)
        val isLeft = prefs.getBoolean(Constants.PrefsKeys.FLOATING_BUTTON_IS_LEFT, defaultIsLeft)
        return Triple(x, y, isLeft)
    }

    fun getFloatingButtonY(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 默认值为屏幕中央（使用负数表示需要计算）
        return prefs.getFloat(Constants.PrefsKeys.FLOATING_BUTTON_Y, -1f)
    }

    fun getFloatingButtonIsLeft(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.FLOATING_BUTTON_IS_LEFT, true)
    }

    fun saveFloatingButtonExpanded(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.FLOATING_BUTTON_EXPANDED, isExpanded).apply()
    }

    fun getFloatingButtonExpanded(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.FLOATING_BUTTON_EXPANDED, default)
    }

    // === 页面开关 ===

    fun setShowRadioTab(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.SHOW_RADIO_TAB, show).apply()
    }

    fun getShowRadioTab(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.SHOW_RADIO_TAB, true)
    }

    fun setShowBreathingTab(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.SHOW_BREATHING_TAB, show).apply()
    }

    fun getShowBreathingTab(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.SHOW_BREATHING_TAB, true)
    }

    // === 小组件 ===

    fun saveQuoteWidgetAdded(context: Context, added: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(Constants.PrefsKeys.QUOTE_WIDGET_ADDED, added).apply()
    }

    fun isQuoteWidgetAdded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.QUOTE_WIDGET_ADDED, false)
    }

    // === 匿名设备标识 ===

    /**
     * 获取匿名设备唯一标识。首次调用生成并持久化随机 UUID。
     * 用途：共建壁纸/晚安信/赞助的去重与署名，不关联任何真实身份。
     */
    fun getAnonymousDeviceId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(Constants.PrefsKeys.ANONYMOUS_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val newId = java.util.UUID.randomUUID().toString()
        prefs.edit().putString(Constants.PrefsKeys.ANONYMOUS_DEVICE_ID, newId).apply()
        Logger.d("UiPrefs", "生成匿名设备标识: $newId")
        return newId
    }

    fun resetAnonymousDeviceId(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(Constants.PrefsKeys.ANONYMOUS_DEVICE_ID).apply()
    }

    // === 数据迁移 ===

    /**
     * 从旧版本迁移数据（如果存在）。
     * 由于包名从未变更（OLD_APP_PACKAGE == APP_PACKAGE），此逻辑实际是死代码，
     * 保留方法签名兼容调用点，直接标记迁移完成。
     */
    fun migrateFromOldVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(Constants.PrefsKeys.MIGRATION_DONE, false)) {
            return
        }
        prefs.edit().putBoolean(Constants.PrefsKeys.MIGRATION_DONE, true).apply()
        Logger.d("UiPrefs", "包名未变更，跳过旧版本数据迁移")
    }
}
