package org.xmsleep.app.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import org.xmsleep.app.Constants
import java.util.Locale

/**
 * 语言管理器
 * 语言设置已合并到 PreferencesManager 中，此对象提供便捷访问方法
 */
object LanguageManager {
    private val PREFS_NAME = Constants.PrefsKeys.LANGUAGE_PREFS_NAME
    private val KEY_LANGUAGE = Constants.PrefsKeys.KEY_LANGUAGE
    
    // 支持的语言
    enum class Language(val code: String, val displayName: String, val locale: Locale) {
        SIMPLIFIED_CHINESE("zh_CN", "简体中文", Locale.SIMPLIFIED_CHINESE),
        TRADITIONAL_CHINESE("zh_TW", "繁體中文", Locale.TRADITIONAL_CHINESE),
        ENGLISH("en", "English", Locale.ENGLISH),
        KOREAN("ko", "한국어", Locale.KOREAN),
        JAPANESE("ja", "日本語", Locale.JAPANESE),
        RUSSIAN("ru", "Русский", Locale("ru"));
        
        companion object {
            fun fromCode(code: String): Language {
                return entries.find { it.code == code } ?: SIMPLIFIED_CHINESE
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
            locale.language == "ko" -> Language.KOREAN
            locale.language == "ja" -> Language.JAPANESE
            locale.language == "ru" -> Language.RUSSIAN
            else -> Language.SIMPLIFIED_CHINESE
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

