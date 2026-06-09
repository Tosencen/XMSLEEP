package org.xmsleep.app.preferences

import android.content.Context
import android.util.Base64
import androidx.compose.ui.graphics.Color
import org.xmsleep.app.Constants
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用偏好设置管理器
 */
object PreferencesManager {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME
    
    private val KEY_DARK_MODE = Constants.PrefsKeys.DARK_MODE
    private val KEY_SELECTED_COLOR = Constants.PrefsKeys.SELECTED_COLOR
    private val KEY_USE_DYNAMIC_COLOR = Constants.PrefsKeys.USE_DYNAMIC_COLOR
    private val KEY_USE_BLACK_BACKGROUND = Constants.PrefsKeys.USE_BLACK_BACKGROUND
    private val KEY_HIDE_ANIMATION = Constants.PrefsKeys.HIDE_ANIMATION
    private val KEY_SOUND_CARDS_COLUMNS_COUNT = Constants.PrefsKeys.SOUND_CARDS_COLUMNS
    private val KEY_STAR_SKY_COLUMNS_COUNT = Constants.PrefsKeys.STAR_SKY_COLUMNS_COUNT
    private val KEY_QUICK_PLAY_EXPANDED = Constants.PrefsKeys.QUICK_PLAY_EXPANDED
    private val KEY_NOW_PLAYING_EXPANDED = Constants.PrefsKeys.NOW_PLAYING_EXPANDED
    private val KEY_REMOTE_FAVORITES = Constants.PrefsKeys.REMOTE_FAVORITES
    private val KEY_REMOTE_PINNED = Constants.PrefsKeys.REMOTE_PINNED
    private val KEY_MIGRATION_DONE = Constants.PrefsKeys.MIGRATION_DONE
    private val KEY_FLOATING_BUTTON_X = Constants.PrefsKeys.FLOATING_BUTTON_X
    private val KEY_FLOATING_BUTTON_Y = Constants.PrefsKeys.FLOATING_BUTTON_Y
    private val KEY_FLOATING_BUTTON_IS_LEFT = Constants.PrefsKeys.FLOATING_BUTTON_IS_LEFT
    private val KEY_FLOATING_BUTTON_EXPANDED = Constants.PrefsKeys.FLOATING_BUTTON_EXPANDED
    
    private val KEY_PRESET1_LOCAL_PINNED = Constants.PrefsKeys.PRESET1_LOCAL_PINNED
    private val KEY_PRESET2_LOCAL_PINNED = Constants.PrefsKeys.PRESET2_LOCAL_PINNED
    private val KEY_PRESET3_LOCAL_PINNED = Constants.PrefsKeys.PRESET3_LOCAL_PINNED
    private val KEY_PRESET1_REMOTE_PINNED = Constants.PrefsKeys.PRESET1_REMOTE_PINNED
    private val KEY_PRESET2_REMOTE_PINNED = Constants.PrefsKeys.PRESET2_REMOTE_PINNED
    private val KEY_PRESET3_REMOTE_PINNED = Constants.PrefsKeys.PRESET3_REMOTE_PINNED
    private val KEY_ACTIVE_PRESET = Constants.PrefsKeys.ACTIVE_PRESET
    private val KEY_LOCAL_AUDIO_FAVORITES = Constants.PrefsKeys.LOCAL_AUDIO_FAVORITES
    private val KEY_RECENT_LOCAL_SOUNDS = Constants.PrefsKeys.RECENT_LOCAL_SOUNDS
    private val KEY_RECENT_REMOTE_SOUNDS = Constants.PrefsKeys.RECENT_REMOTE_SOUNDS
    private val KEY_RECENT_LOCAL_AUDIO_FILES = Constants.PrefsKeys.RECENT_LOCAL_AUDIO_FILES
    private val KEY_VOLUME_PREFIX = Constants.PrefsKeys.VOLUME_PREFIX
    private val KEY_BACKGROUND_SELECTION = Constants.PrefsKeys.BACKGROUND_SELECTION
    private val KEY_AUTO_COUNTDOWN_MINUTES = Constants.PrefsKeys.AUTO_COUNTDOWN_MINUTES
    private val KEY_KEEP_SCREEN_ON = Constants.PrefsKeys.KEEP_SCREEN_ON
    private val KEY_SHOW_RECENT_PLAY_DIALOG = Constants.PrefsKeys.SHOW_RECENT_PLAY_DIALOG
    private val KEY_AUTO_PLAY_ON_START = Constants.PrefsKeys.AUTO_PLAY_ON_START
    private val KEY_QUOTE_WIDGET_ADDED = Constants.PrefsKeys.QUOTE_WIDGET_ADDED

    // 响应式状态：预设远程固定列表
    private val _preset1RemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val preset1RemotePinned: StateFlow<Set<String>> = _preset1RemotePinned.asStateFlow()

    private val _preset2RemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val preset2RemotePinned: StateFlow<Set<String>> = _preset2RemotePinned.asStateFlow()

    private val _preset3RemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val preset3RemotePinned: StateFlow<Set<String>> = _preset3RemotePinned.asStateFlow()

    /**
     * 初始化响应式状态（应在应用启动时调用）
     */
    fun initialize(context: Context) {
        _preset1RemotePinned.value = getPresetRemotePinned(context, 1)
        _preset2RemotePinned.value = getPresetRemotePinned(context, 2)
        _preset3RemotePinned.value = getPresetRemotePinned(context, 3)
    }

    /**
     * 获取指定预设的响应式状态
     */
    fun getPresetRemotePinnedState(presetIndex: Int): StateFlow<Set<String>> {
        return when (presetIndex) {
            1 -> _preset1RemotePinned
            2 -> _preset2RemotePinned
            3 -> _preset3RemotePinned
            else -> _preset1RemotePinned
        }
    }
    
    /**
     * 从旧版本迁移数据（如果存在）
     * 注意：由于包名从未变更（OLD_APP_PACKAGE == APP_PACKAGE），此迁移逻辑实际上是死代码。
     * 保留方法签名以兼容现有调用点，但直接标记迁移完成跳过无效的 createPackageContext 调用。
     */
    fun migrateFromOldVersion(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // 检查是否已经迁移过
        if (prefs.getBoolean(KEY_MIGRATION_DONE, false)) {
            return
        }
        
        // OLD_APP_PACKAGE 与 APP_PACKAGE 相同，createPackageContext 无法读取自身历史数据，
        // 直接标记迁移完成，避免每次冷启动都触发一次必然失败的跨包 Context 创建。
        prefs.edit().putBoolean(KEY_MIGRATION_DONE, true).apply()
        Logger.d("PreferencesManager", "包名未变更，跳过旧版本数据迁移")
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
    fun getDarkMode(context: Context, default: DarkModeOption = DarkModeOption.DARK): DarkModeOption {
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
     * 保存浮动按钮位置（简化版，只保存Y和isLeft）
     */
    fun saveFloatingButtonPosition(context: Context, y: Float, isLeft: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
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
     * 获取浮动按钮Y位置
     */
    fun getFloatingButtonY(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // 默认值为屏幕中央（使用负数表示需要计算）
        return prefs.getFloat(KEY_FLOATING_BUTTON_Y, -1f)
    }
    
    /**
     * 获取浮动按钮是否在左侧
     */
    fun getFloatingButtonIsLeft(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_FLOATING_BUTTON_IS_LEFT, true) // 默认在左侧
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
        // 更新响应式状态
        when (presetIndex) {
            1 -> _preset1RemotePinned.value = soundIds
            2 -> _preset2RemotePinned.value = soundIds
            3 -> _preset3RemotePinned.value = soundIds
        }
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
     * 使用 Base64 编码 URI 以避免特殊字符导致的解析问题
     */
    fun saveRecentLocalAudioFiles(context: Context, audioUriMap: Map<Long, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = audioUriMap.entries.joinToString(";") { entry ->
            val encodedUri = Base64.encodeToString(entry.value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "${entry.key}:$encodedUri"
        }
        prefs.edit().putString(KEY_RECENT_LOCAL_AUDIO_FILES, jsonString).apply()
    }
    
    /**
     * 获取最近播放的本地音频文件列表（包含 URI 映射）
     * 使用 Base64 解码 URI
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
                        val encodedUri = parts[1]
                        val uri = String(Base64.decode(encodedUri, Base64.NO_WRAP), Charsets.UTF_8)
                        if (audioId != null) audioId to uri else null
                    } else null
                }
                .toMap()
        } catch (e: Exception) {
            Logger.e("PreferencesManager", "解析最近播放的本地音频文件失败: ${e.message}")
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
    
    /**
     * 保存背景动画选择
     * @param selection 背景选择枚举
     */
    fun saveBackgroundSelection(context: Context, selection: org.xmsleep.app.ui.BackgroundSelection) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BACKGROUND_SELECTION, selection.value).apply()
    }
    
    /**
     * 获取背景动画选择
     * @return 背景选择枚举，默认为 None
     */
    fun getBackgroundSelection(context: Context): org.xmsleep.app.ui.BackgroundSelection {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val value = prefs.getString(KEY_BACKGROUND_SELECTION, "none") ?: "none"
        return org.xmsleep.app.ui.BackgroundSelection.fromValue(value)
    }
    
    /**
     * 保存自动倒计时时间（分钟）
     * @param minutes 倒计时分钟数，0 表示不设置倒计时
     */
    fun saveAutoCountdownMinutes(context: Context, minutes: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_AUTO_COUNTDOWN_MINUTES, minutes).apply()
    }
    
    /**
     * 获取自动倒计时时间（分钟）
     * @return 倒计时分钟数，0 表示不设置倒计时
     */
    fun getAutoCountdownMinutes(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_AUTO_COUNTDOWN_MINUTES, 0)
    }
    
    /**
     * 保存屏幕常亮设置
     * @param keepScreenOn 是否保持屏幕常亮
     */
    fun saveKeepScreenOn(context: Context, keepScreenOn: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepScreenOn).apply()
    }
    
    /**
     * 获取屏幕常亮设置
     * @return 是否保持屏幕常亮，默认为 true
     */
    fun getKeepScreenOn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
    }
    
    /**
     * 保存是否显示最近播放弹窗设置
     * @param show 是否显示，默认为 true
     */
    fun saveShowRecentPlayDialog(context: Context, show: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_RECENT_PLAY_DIALOG, show).apply()
    }
    
    /**
     * 获取是否显示最近播放弹窗设置
     * @return 是否显示，默认为 true
     */
    fun getShowRecentPlayDialog(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_SHOW_RECENT_PLAY_DIALOG, true)
    }

    /**
     * 保存应用启动自动播放设置
     * @param enabled 是否启用
     */
    fun setAutoPlayOnStart(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_AUTO_PLAY_ON_START, enabled).apply()
    }

    /**
     * 获取应用启动自动播放设置
     * @return 是否启用，默认为 false
     */
    fun getAutoPlayOnStart(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_PLAY_ON_START, false)
    }

    /**
     * 保存一言一句小组件是否已添加
     * @param added 是否已添加
     */
    fun saveQuoteWidgetAdded(context: Context, added: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_QUOTE_WIDGET_ADDED, added).apply()
    }
    
    /**
     * 获取一言一句小组件是否已添加
     * @return 是否已添加，默认为 false
     */
    fun isQuoteWidgetAdded(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_QUOTE_WIDGET_ADDED, false)
    }
}

