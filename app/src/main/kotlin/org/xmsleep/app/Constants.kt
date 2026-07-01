package org.xmsleep.app

/**
 * 应用常量配置
 * 
 * 统一管理所有硬编码的常量值，便于维护和修改。
 * 避免在代码中直接使用字符串字面量，提高代码可读性和可维护性。
 * 
 * ## 使用示例
 * ```kotlin
 * // 使用 GitHub URL
 * val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.GITHUB_URL))
 * 
 * // 使用 API 基础 URL
 * val url = "${Constants.GITHUB_API_BASE_URL}/repos/..."
 * 
 * // 使用 SharedPreferences Key
 * prefs.getString(Constants.PrefsKeys.DARK_MODE, "")
 * ```
 * 
 * @since 2.0.5
 * @author XMSLEEP Team
 */
object Constants {
    
    // ==================== 应用信息 ====================
    const val APP_NAME = "XMSLEEP"
    const val APP_PACKAGE = "org.xmsleep.app"
    
    // ==================== 旧版本信息（用于数据迁移）====================
    // 注意：OLD_APP_PACKAGE 与 APP_PACKAGE 相同，说明包名从未变更过。
    // migrateFromOldVersion 中的迁移逻辑实际上是死代码，首次运行时
    // createPackageContext 会抛出 NameNotFoundException 并被 catch 捕获，
    // 迁移标记会被置为 true，后续不再执行。保留此常量仅作历史记录。
    @Deprecated("包名从未变更，迁移逻辑为死代码，不需要使用此常量")
    const val OLD_APP_PACKAGE = "org.xmsleep.app"
    
    // ==================== 外部链接 ====================
    const val GITHUB_URL = "https://github.com/Tosencen/XMSLEEP"
    const val TELEGRAM_URL = "https://t.me/xmsleep"
    
    // ==================== API 配置 ====================
    const val GITHUB_API_BASE_URL = "https://api.github.com"
    const val CDN_BASE_URL = "https://cdn.jsdelivr.net"
    
    // ==================== 仓库信息 ====================
    const val GITHUB_OWNER = "Tosencen"
    const val GITHUB_REPO = "XMSLEEP"
    
    // ==================== 缓存配置 ====================
    const val CACHE_MAX_SIZE_MB = 200L
    const val CACHE_DIR_NAME = "audio_cache"
    
    // ==================== 更新检查 ====================
    const val UPDATE_CHECK_INTERVAL_HOURS = 24
    
    // ==================== 默认配置 ====================
    const val DEFAULT_SOUND_COLUMNS = 2
    const val DEFAULT_VOLUME = 1.0f
    
    // ==================== SharedPreferences Keys ====================
    object PrefsKeys {
        const val PREFS_NAME = "app_prefs"
        
        const val DARK_MODE = "dark_mode"
        const val SELECTED_COLOR = "selected_color"
        const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val USE_BLACK_BACKGROUND = "use_black_background"
        const val HIDE_ANIMATION = "hide_animation"
        const val SOUND_CARDS_COLUMNS = "sound_cards_columns_count"
        const val LANGUAGE = "language"
        const val LAST_UPDATE_CHECK = "last_update_check"
        
        const val STAR_SKY_COLUMNS_COUNT = "star_sky_columns_count"
        const val QUICK_PLAY_EXPANDED = "quick_play_expanded"
        const val NOW_PLAYING_EXPANDED = "now_playing_expanded"
        const val REMOTE_FAVORITES = "remote_favorites"
        const val REMOTE_PINNED = "remote_pinned"
        const val MIGRATION_DONE = "migration_done"
        const val FLOATING_BUTTON_X = "floating_button_x"
        const val FLOATING_BUTTON_Y = "floating_button_y"
        const val FLOATING_BUTTON_IS_LEFT = "floating_button_is_left"
        const val FLOATING_BUTTON_EXPANDED = "floating_button_expanded"
        
        const val PRESET1_LOCAL_PINNED = "preset1_local_pinned"
        const val PRESET2_LOCAL_PINNED = "preset2_local_pinned"
        const val PRESET3_LOCAL_PINNED = "preset3_local_pinned"
        const val PRESET1_REMOTE_PINNED = "preset1_remote_pinned"
        const val PRESET2_REMOTE_PINNED = "preset2_remote_pinned"
        const val PRESET3_REMOTE_PINNED = "preset3_remote_pinned"
        const val ACTIVE_PRESET = "active_preset"
        const val PRESET_LIST = "preset_list"
        const val PRESET_NAME_PREFIX = "preset_name_"
        const val PRESET_LOCAL_PREFIX = "preset_local_"
        const val PRESET_REMOTE_PREFIX = "preset_remote_"
        const val MAX_PRESET_COUNT = 10
        
        const val LOCAL_AUDIO_FAVORITES = "local_audio_favorites"
        const val RECENT_LOCAL_SOUNDS = "recent_local_sounds"
        const val RECENT_REMOTE_SOUNDS = "recent_remote_sounds"
        const val RECENT_LOCAL_AUDIO_FILES = "recent_local_audio_files"
        const val VOLUME_PREFIX = "volume_"
        
        const val BACKGROUND_SELECTION = "background_animation_selection"
        const val CUSTOM_BACKGROUND_URI = "custom_background_uri"
        const val CUSTOM_BACKGROUND_COLOR = "custom_background_color"
        const val AUTO_COUNTDOWN_MINUTES = "auto_countdown_minutes"
        const val LAST_TIMER_MINUTES = "last_timer_minutes"
        const val KEEP_SCREEN_ON = "keep_screen_on"
        
        // 最近播放弹窗开关
        const val SHOW_RECENT_PLAY_DIALOG = "show_recent_play_dialog"
        // 应用启动自动播放
        const val AUTO_PLAY_ON_START = "auto_play_on_start"
        // 一言一句小组件相关
        const val QUOTE_WIDGET_ADDED = "quote_widget_added"

        // 电台相关
        const val RADIO_STATION_ID = "radio_station_id"
        const val RADIO_VOLUME = "radio_volume"
        const val RADIO_LOTTIE_FILE = "radio_lottie_file"
        const val BILIBILI_PINNED_ROOMS = "bilibili_pinned_rooms"
        const val BILIBILI_PINNED_ROOMS_INFO = "bilibili_pinned_rooms_info"
        const val RADIO_FLOATING_BUTTON_Y = "radio_floating_button_y"
        const val RADIO_FLOATING_BUTTON_IS_LEFT = "radio_floating_button_is_left"
        const val SHOW_RADIO_TAB = "show_radio_tab"
        const val SHOW_BREATHING_TAB = "show_breathing_tab"
        
        const val LANGUAGE_PREFS_NAME = "language_prefs"
        const val KEY_LANGUAGE = "language"
    }
    
    // ==================== Intent Actions ====================
    object Actions {
        const val VIEW_URL = "android.content.Intent.ACTION_VIEW"
    }
}
