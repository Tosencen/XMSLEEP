package org.xmsleep.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * 语言管理器
 */
object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "language"
    
    // 支持的语言
    enum class Language(val code: String, val displayName: String, val locale: Locale) {
        SIMPLIFIED_CHINESE("zh_CN", "简体中文", Locale.SIMPLIFIED_CHINESE),
        TRADITIONAL_CHINESE("zh_TW", "繁體中文", Locale.TRADITIONAL_CHINESE),
        ENGLISH("en", "English", Locale.ENGLISH);
        
        companion object {
            fun fromCode(code: String): Language {
                return values().find { it.code == code } ?: SIMPLIFIED_CHINESE
            }
        }
    }
    
    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(context: Context): Language {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANGUAGE, null)
        return if (code != null) {
            Language.fromCode(code)
        } else {
            // 如果没有保存的语言设置，默认使用简体中文
            Language.SIMPLIFIED_CHINESE
        }
    }
    
    /**
     * 保存语言设置
     */
    fun setLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
    
    /**
     * 获取系统语言
     */
    private fun getSystemLanguage(context: Context): Language {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        
        return when {
            locale.language == "zh" && locale.country == "CN" -> Language.SIMPLIFIED_CHINESE
            locale.language == "zh" && (locale.country == "TW" || locale.country == "HK" || locale.country == "MO") -> Language.TRADITIONAL_CHINESE
            locale.language == "en" -> Language.ENGLISH
            else -> Language.SIMPLIFIED_CHINESE // 默认使用简体中文
        }
    }
    
    /**
     * 获取当前语言的Locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return getCurrentLanguage(context).locale
    }
    
    /**
     * 创建语言化的Context（用于实时更新语言）
     */
    fun createLocalizedContext(context: Context, language: Language): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(language.locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 更新应用语言
     */
    fun updateAppLanguage(context: Context): Context {
        val language = getCurrentLanguage(context)
        val locale = language.locale
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }
}

