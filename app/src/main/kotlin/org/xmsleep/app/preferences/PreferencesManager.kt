package org.xmsleep.app.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.xmsleep.app.theme.DarkModeOption

/**
 * 应用偏好设置管理器
 */
object PreferencesManager {
    private const val PREFS_NAME = "app_prefs"
    private const val KEY_DARK_MODE = "dark_mode"
    private const val KEY_SELECTED_COLOR = "selected_color"
    private const val KEY_USE_DYNAMIC_COLOR = "use_dynamic_color"
    private const val KEY_USE_BLACK_BACKGROUND = "use_black_background"
    private const val KEY_HIDE_ANIMATION = "hide_animation"
    private const val KEY_SOUND_CARDS_COLUMNS_COUNT = "sound_cards_columns_count"
    private const val KEY_STAR_SKY_COLUMNS_COUNT = "star_sky_columns_count"
    private const val KEY_QUICK_PLAY_EXPANDED = "quick_play_expanded"
    private const val KEY_NOW_PLAYING_EXPANDED = "now_playing_expanded"
    private const val KEY_REMOTE_FAVORITES = "remote_favorites"
    private const val KEY_REMOTE_PINNED = "remote_pinned"
    private const val KEY_MIGRATION_DONE = "migration_done"
    private const val KEY_FLOATING_BUTTON_X = "floating_button_x"
    private const val KEY_FLOATING_BUTTON_Y = "floating_button_y"
    private const val KEY_FLOATING_BUTTON_IS_LEFT = "floating_button_is_left"
    private const val KEY_FLOATING_BUTTON_EXPANDED = "floating_button_expanded"
    
    /**
     * 从旧版本迁移数据（如果存在）
     */
    fun migrateFromOldVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 检查是否已经迁移过
        if (prefs.getBoolean(KEY_MIGRATION_DONE, false)) {
            return
        }
        
        try {
            // 尝试创建旧版本的Context来访问其SharedPreferences
            val oldContext = context.createPackageContext(
                "org.streambox.app",
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )
            
            val oldPrefs = oldContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            
            // 迁移数据
            val editor = prefs.edit()
            
            // 迁移深色模式
            val darkMode = oldPrefs.getString(KEY_DARK_MODE, null)
            if (darkMode != null) {
                editor.putString(KEY_DARK_MODE, darkMode)
            }
            
            // 迁移主题色
            val selectedColor = oldPrefs.getLong(KEY_SELECTED_COLOR, -1L)
            if (selectedColor != -1L) {
                editor.putLong(KEY_SELECTED_COLOR, selectedColor)
            }
            
            // 迁移动态颜色
            if (oldPrefs.contains(KEY_USE_DYNAMIC_COLOR)) {
                editor.putBoolean(KEY_USE_DYNAMIC_COLOR, oldPrefs.getBoolean(KEY_USE_DYNAMIC_COLOR, false))
            }
            
            // 迁移纯黑背景
            if (oldPrefs.contains(KEY_USE_BLACK_BACKGROUND)) {
                editor.putBoolean(KEY_USE_BLACK_BACKGROUND, oldPrefs.getBoolean(KEY_USE_BLACK_BACKGROUND, false))
            }
            
            // 迁移隐藏动画
            if (oldPrefs.contains(KEY_HIDE_ANIMATION)) {
                editor.putBoolean(KEY_HIDE_ANIMATION, oldPrefs.getBoolean(KEY_HIDE_ANIMATION, true))
            }
            
            // 迁移声音卡片列数
            if (oldPrefs.contains(KEY_SOUND_CARDS_COLUMNS_COUNT)) {
                editor.putInt(KEY_SOUND_CARDS_COLUMNS_COUNT, oldPrefs.getInt(KEY_SOUND_CARDS_COLUMNS_COUNT, 2))
            }
            
            // 标记迁移完成
            editor.putBoolean(KEY_MIGRATION_DONE, true)
            editor.apply()
            
            android.util.Log.d("PreferencesManager", "成功从旧版本迁移数据")
        } catch (e: Exception) {
            // 如果无法访问旧版本（比如旧版本已卸载），标记迁移完成，避免重复尝试
            prefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
            android.util.Log.d("PreferencesManager", "无法访问旧版本数据（可能已卸载）: ${e.message}")
        }
    }
    
    /**
     * 保存深色模式设置
     */
    fun saveDarkMode(context: Context, darkMode: DarkModeOption) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_DARK_MODE, darkMode.name).apply()
    }
    
    /**
     * 获取深色模式设置
     */
    fun getDarkMode(context: Context, default: DarkModeOption = DarkModeOption.AUTO): DarkModeOption {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeName = prefs.getString(KEY_DARK_MODE, null)
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
    
    /**
     * 保存主题色
     */
    fun saveSelectedColor(context: Context, color: Color) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_SELECTED_COLOR, color.value.toLong()).apply()
    }
    
    /**
     * 获取主题色
     */
    fun getSelectedColor(context: Context, default: Color): Color {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val colorValue = prefs.getLong(KEY_SELECTED_COLOR, -1L)
        return if (colorValue != -1L) {
            Color(colorValue.toULong())
        } else {
            default
        }
    }
    
    /**
     * 保存动态颜色设置
     */
    fun saveUseDynamicColor(context: Context, useDynamicColor: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_DYNAMIC_COLOR, useDynamicColor).apply()
    }
    
    /**
     * 获取动态颜色设置
     */
    fun getUseDynamicColor(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_DYNAMIC_COLOR, default)
    }
    
    /**
     * 保存纯黑背景设置
     */
    fun saveUseBlackBackground(context: Context, useBlackBackground: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_USE_BLACK_BACKGROUND, useBlackBackground).apply()
    }
    
    /**
     * 获取纯黑背景设置
     */
    fun getUseBlackBackground(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_USE_BLACK_BACKGROUND, default)
    }
    
    /**
     * 保存隐藏动画设置
     */
    fun saveHideAnimation(context: Context, hideAnimation: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_HIDE_ANIMATION, hideAnimation).apply()
    }
    
    /**
     * 获取隐藏动画设置
     */
    fun getHideAnimation(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_HIDE_ANIMATION, default)
    }
    
    /**
     * 保存声音卡片列数设置
     */
    fun saveSoundCardsColumnsCount(context: Context, columnsCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SOUND_CARDS_COLUMNS_COUNT, columnsCount).apply()
    }
    
    /**
     * 获取声音卡片列数设置
     */
    fun getSoundCardsColumnsCount(context: Context, default: Int = 2): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SOUND_CARDS_COLUMNS_COUNT, default)
    }
    
    /**
     * 保存星空页面列数设置
     */
    fun saveStarSkyColumnsCount(context: Context, columnsCount: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_STAR_SKY_COLUMNS_COUNT, columnsCount).apply()
    }
    
    /**
     * 获取星空页面列数设置
     */
    fun getStarSkyColumnsCount(context: Context, default: Int = 2): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_STAR_SKY_COLUMNS_COUNT, default)
    }
    
    /**
     * 保存快捷播放模块展开状态
     */
    fun saveQuickPlayExpanded(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_QUICK_PLAY_EXPANDED, isExpanded).apply()
    }
    
    /**
     * 获取快捷播放模块展开状态
     */
    fun getQuickPlayExpanded(context: Context, default: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_QUICK_PLAY_EXPANDED, default)
    }
    
    /**
     * 保存正在播放模块展开状态
     */
    fun saveNowPlayingExpanded(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOW_PLAYING_EXPANDED, isExpanded).apply()
    }
    
    /**
     * 获取正在播放模块展开状态
     */
    fun getNowPlayingExpanded(context: Context, default: Boolean = true): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOW_PLAYING_EXPANDED, default)
    }
    
    /**
     * 保存远程音频收藏列表
     */
    fun saveRemoteFavorites(context: Context, soundIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_REMOTE_FAVORITES, soundIds).apply()
    }
    
    /**
     * 获取远程音频收藏列表
     */
    fun getRemoteFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_REMOTE_FAVORITES, emptySet()) ?: emptySet()
    }
    
    /**
     * 保存远程音频置顶列表
     */
    fun saveRemotePinned(context: Context, soundIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_REMOTE_PINNED, soundIds).apply()
    }
    
    /**
     * 获取远程音频置顶列表
     */
    fun getRemotePinned(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_REMOTE_PINNED, emptySet()) ?: emptySet()
    }
    
    /**
     * 保存浮动按钮位置
     */
    fun saveFloatingButtonPosition(context: Context, x: Float, y: Float, isLeft: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(KEY_FLOATING_BUTTON_X, x)
            .putFloat(KEY_FLOATING_BUTTON_Y, y)
            .putBoolean(KEY_FLOATING_BUTTON_IS_LEFT, isLeft)
            .apply()
    }
    
    /**
     * 获取浮动按钮位置
     */
    fun getFloatingButtonPosition(context: Context, defaultX: Float, defaultY: Float, defaultIsLeft: Boolean): Triple<Float, Float, Boolean> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val x = prefs.getFloat(KEY_FLOATING_BUTTON_X, defaultX)
        val y = prefs.getFloat(KEY_FLOATING_BUTTON_Y, defaultY)
        val isLeft = prefs.getBoolean(KEY_FLOATING_BUTTON_IS_LEFT, defaultIsLeft)
        return Triple(x, y, isLeft)
    }
    
    /**
     * 保存浮动按钮展开状态
     */
    fun saveFloatingButtonExpanded(context: Context, isExpanded: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FLOATING_BUTTON_EXPANDED, isExpanded).apply()
    }
    
    /**
     * 获取浮动按钮展开状态
     */
    fun getFloatingButtonExpanded(context: Context, default: Boolean = false): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FLOATING_BUTTON_EXPANDED, default)
    }
}

