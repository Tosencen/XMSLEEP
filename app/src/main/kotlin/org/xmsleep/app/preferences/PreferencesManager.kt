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
    
    // 3个预设的本地声音固定列表
    private const val KEY_PRESET1_LOCAL_PINNED = "preset1_local_pinned"
    private const val KEY_PRESET2_LOCAL_PINNED = "preset2_local_pinned"
    private const val KEY_PRESET3_LOCAL_PINNED = "preset3_local_pinned"
    // 3个预设的远程声音固定列表
    private const val KEY_PRESET1_REMOTE_PINNED = "preset1_remote_pinned"
    private const val KEY_PRESET2_REMOTE_PINNED = "preset2_remote_pinned"
    private const val KEY_PRESET3_REMOTE_PINNED = "preset3_remote_pinned"
    // 当前激活的预设
    private const val KEY_ACTIVE_PRESET = "active_preset"
    // 本地音频收藏列表
    private const val KEY_LOCAL_AUDIO_FAVORITES = "local_audio_favorites"
    // 最近播放的声音列表（本地声音）
    private const val KEY_RECENT_LOCAL_SOUNDS = "recent_local_sounds"
    // 最近播放的声音列表（远程声音）
    private const val KEY_RECENT_REMOTE_SOUNDS = "recent_remote_sounds"
    // 最近播放的本地音频文件列表
    private const val KEY_RECENT_LOCAL_AUDIO_FILES = "recent_local_audio_files"
    // 音量设置前缀
    private const val KEY_VOLUME_PREFIX = "volume_"
    
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
    
    /**
     * 保存预设的本地声音固定列表
     * @param presetIndex 预设索引 (1, 2, 3)
     * @param soundNames 声音名称集合
     */
    fun savePresetLocalPinned(context: Context, presetIndex: Int, soundNames: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = when (presetIndex) {
            1 -> KEY_PRESET1_LOCAL_PINNED
            2 -> KEY_PRESET2_LOCAL_PINNED
            3 -> KEY_PRESET3_LOCAL_PINNED
            else -> return
        }
        prefs.edit().putStringSet(key, soundNames).apply()
    }
    
    /**
     * 获取预设的本地声音固定列表
     * @param presetIndex 预设索引 (1, 2, 3)
     */
    fun getPresetLocalPinned(context: Context, presetIndex: Int): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = when (presetIndex) {
            1 -> KEY_PRESET1_LOCAL_PINNED
            2 -> KEY_PRESET2_LOCAL_PINNED
            3 -> KEY_PRESET3_LOCAL_PINNED
            else -> return emptySet()
        }
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }
    
    /**
     * 保存预设的远程声音固定列表
     * @param presetIndex 预设索引 (1, 2, 3)
     * @param soundIds 声音ID集合
     */
    fun savePresetRemotePinned(context: Context, presetIndex: Int, soundIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = when (presetIndex) {
            1 -> KEY_PRESET1_REMOTE_PINNED
            2 -> KEY_PRESET2_REMOTE_PINNED
            3 -> KEY_PRESET3_REMOTE_PINNED
            else -> return
        }
        prefs.edit().putStringSet(key, soundIds).apply()
    }
    
    /**
     * 获取预设的远程声音固定列表
     * @param presetIndex 预设索引 (1, 2, 3)
     */
    fun getPresetRemotePinned(context: Context, presetIndex: Int): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = when (presetIndex) {
            1 -> KEY_PRESET1_REMOTE_PINNED
            2 -> KEY_PRESET2_REMOTE_PINNED
            3 -> KEY_PRESET3_REMOTE_PINNED
            else -> return emptySet()
        }
        return prefs.getStringSet(key, emptySet()) ?: emptySet()
    }
    
    /**
     * 保存当前激活的预设
     * @param presetIndex 预设索引 (1, 2, 3)
     */
    fun saveActivePreset(context: Context, presetIndex: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_ACTIVE_PRESET, presetIndex).apply()
    }
    
    /**
     * 获取当前激活的预设
     */
    fun getActivePreset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_ACTIVE_PRESET, 1) // 默认为预设1
    }
    
    /**
     * 保存本地音频收藏列表
     */
    fun saveLocalAudioFavorites(context: Context, uris: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_LOCAL_AUDIO_FAVORITES, uris).apply()
    }
    
    /**
     * 获取本地音频收藏列表
     */
    fun getLocalAudioFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_LOCAL_AUDIO_FAVORITES, emptySet()) ?: emptySet()
    }
    
    /**
     * 保存最近播放的本地声音列表
     */
    fun saveRecentLocalSounds(context: Context, sounds: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_RECENT_LOCAL_SOUNDS, sounds.toSet()).apply()
    }
    
    /**
     * 获取最近播放的本地声音列表
     */
    fun getRecentLocalSounds(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getStringSet(KEY_RECENT_LOCAL_SOUNDS, emptySet()) ?: emptySet()).toList()
    }
    
    /**
     * 保存最近播放的远程声音列表
     */
    fun saveRecentRemoteSounds(context: Context, soundIds: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_RECENT_REMOTE_SOUNDS, soundIds.toSet()).apply()
    }
    
    /**
     * 获取最近播放的远程声音列表
     */
    fun getRecentRemoteSounds(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getStringSet(KEY_RECENT_REMOTE_SOUNDS, emptySet()) ?: emptySet()).toList()
    }
    
    /**
     * 保存最近播放的本地音频文件列表（包含 URI 映射）
     */
    fun saveRecentLocalAudioFiles(context: Context, audioUriMap: Map<Long, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 将 Map 转换为 JSON 字符串保存
        val jsonString = audioUriMap.entries.joinToString(";") { "${it.key}:${it.value}" }
        prefs.edit().putString(KEY_RECENT_LOCAL_AUDIO_FILES, jsonString).apply()
    }
    
    /**
     * 获取最近播放的本地音频文件列表（包含 URI 映射）
     */
    fun getRecentLocalAudioFiles(context: Context): Map<Long, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_RECENT_LOCAL_AUDIO_FILES, "") ?: ""
        if (jsonString.isEmpty()) return emptyMap()
        
        return try {
            jsonString.split(";")
                .mapNotNull { entry ->
                    val parts = entry.split(":", limit = 2)
                    if (parts.size == 2) {
                        val audioId = parts[0].toLongOrNull()
                        val uri = parts[1]
                        if (audioId != null) audioId to uri else null
                    } else null
                }
                .toMap()
        } catch (e: Exception) {
            android.util.Log.e("PreferencesManager", "解析最近播放的本地音频文件失败: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * 保存本地声音的音量
     * @param soundName 声音名称（如 "UMBRELLA_RAIN"）
     * @param volume 音量值（0.0 - 1.0）
     */
    fun saveLocalSoundVolume(context: Context, soundName: String, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("${KEY_VOLUME_PREFIX}local_$soundName", volume).apply()
    }
    
    /**
     * 获取本地声音的音量
     * @param soundName 声音名称（如 "UMBRELLA_RAIN"）
     * @param default 默认音量值
     */
    fun getLocalSoundVolume(context: Context, soundName: String, default: Float = 0.5f): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("${KEY_VOLUME_PREFIX}local_$soundName", default)
    }
    
    /**
     * 保存远程声音的音量
     * @param soundId 声音ID
     * @param volume 音量值（0.0 - 1.0）
     */
    fun saveRemoteSoundVolume(context: Context, soundId: String, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("${KEY_VOLUME_PREFIX}remote_$soundId", volume).apply()
    }
    
    /**
     * 获取远程声音的音量
     * @param soundId 声音ID
     * @param default 默认音量值
     */
    fun getRemoteSoundVolume(context: Context, soundId: String, default: Float = 0.5f): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("${KEY_VOLUME_PREFIX}remote_$soundId", default)
    }
    
    /**
     * 保存本地音频文件的音量
     * @param audioId 音频ID
     * @param volume 音量值（0.0 - 1.0）
     */
    fun saveLocalAudioVolume(context: Context, audioId: Long, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("${KEY_VOLUME_PREFIX}audio_$audioId", volume).apply()
    }
    
    /**
     * 获取本地音频文件的音量
     * @param audioId 音频ID
     * @param default 默认音量值
     */
    fun getLocalAudioVolume(context: Context, audioId: Long, default: Float = 0.5f): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("${KEY_VOLUME_PREFIX}audio_$audioId", default)
    }
}

