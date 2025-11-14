package org.xmsleep.app

/**
 * 应用常量配置
 * 统一管理所有硬编码的常量值
 */
object Constants {
    
    // ==================== 应用信息 ====================
    const val APP_NAME = "XMSLEEP"
    
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
        const val DARK_MODE = "dark_mode"
        const val SELECTED_COLOR = "selected_color"
        const val USE_DYNAMIC_COLOR = "use_dynamic_color"
        const val USE_BLACK_BACKGROUND = "use_black_background"
        const val HIDE_ANIMATION = "hide_animation"
        const val SOUND_CARDS_COLUMNS = "sound_cards_columns_count"
        const val LANGUAGE = "language"
        const val LAST_UPDATE_CHECK = "last_update_check"
    }
    
    // ==================== Intent Actions ====================
    object Actions {
        const val VIEW_URL = "android.content.Intent.ACTION_VIEW"
    }
}
