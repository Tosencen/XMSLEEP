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
     * 注意：不调用 LocaleManager.setApplicationLocales（会导致 Activity 重建黑屏），
     * 而是在 Compose 层通过 CompositionLocalProvider 实时切换，无需重建 Activity。
     */
    fun setLanguage(context: Context, language: Language) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, language.code).apply()
    }
    
    /**
     * 获取当前语言的Locale
     */
    fun getCurrentLocale(context: Context): Locale {
        return getCurrentLanguage(context).locale
    }
    
    /**
     * 创建语言化的Context（用于实时更新语言）
     * 基于 Application Context 创建 ConfigurationContext，避免嵌套 Activity 的 ConfigurationContext 导致 locale 丢失
     */
    fun createLocalizedContext(context: Context, language: Language): Context {
        val appConfig = Configuration(context.applicationContext.resources.configuration)
        appConfig.setLocale(language.locale)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.applicationContext.createConfigurationContext(appConfig)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(appConfig, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * 更新应用语言（用于 attachBaseContext）
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

