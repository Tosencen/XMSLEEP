package org.xmsleep.app.quote

import android.content.Context
import org.xmsleep.app.utils.Logger
import java.util.Calendar
import kotlin.random.Random

/**
 * 治愈句子管理器
 * 根据时段（早中晚）随机显示治愈文案
 * 句子在应用启动时生成，页面切换时不会刷新
 * 语言切换时会自动刷新为新语言的句子
 */
object HealingQuoteManager {
    
    // 缓存当前显示的句子，只在应用重启或语言切换时刷新
    private var cachedQuote: String? = null
    private var lastUpdateTime: Long = 0
    private var lastLanguageCode: String? = null
    
    /**
     * 获取当前时段的随机治愈句子
     * 只在应用启动或语言切换时生成新句子，页面切换时保持不变
     */
    fun getRandomQuote(context: Context): String {
        val currentTime = System.currentTimeMillis()
        val currentLanguageCode = org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context).code
        
        // 检查语言是否发生变化
        val languageChanged = lastLanguageCode != null && lastLanguageCode != currentLanguageCode
        
        // 如果语言切换了，强制刷新句子
        if (languageChanged) {
            Logger.d("HealingQuoteManager", "语言切换: $lastLanguageCode -> $currentLanguageCode")
            cachedQuote = null
            lastUpdateTime = 0
            lastLanguageCode = currentLanguageCode
        }
        
        // 如果缓存的句子存在且距离上次更新不超过5分钟，直接返回缓存
        // 这样可以确保页面切换时不会刷新句子
        if (cachedQuote != null && (currentTime - lastUpdateTime) < 5 * 60 * 1000) {
            return cachedQuote!!
        }
        
        // 生成新句子
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        val quotesArrayId = when (hour) {
            in 6..11 -> org.xmsleep.app.R.array.healing_quotes_morning      // 早上 6:00-11:59
            in 12..17 -> org.xmsleep.app.R.array.healing_quotes_afternoon   // 中午 12:00-17:59
            else -> org.xmsleep.app.R.array.healing_quotes_evening          // 晚上 18:00-5:59
        }
        
        // 使用当前语言的Context获取字符串资源
        val localizedContext = org.xmsleep.app.i18n.LanguageManager.createLocalizedContext(
            context, 
            org.xmsleep.app.i18n.LanguageManager.getCurrentLanguage(context)
        )
        val quotes = localizedContext.resources.getStringArray(quotesArrayId)
        val newQuote = quotes[Random.nextInt(quotes.size)]
        
        Logger.d("HealingQuoteManager", "生成新句子: $newQuote (语言: $currentLanguageCode)")
        
        // 更新缓存
        cachedQuote = newQuote
        lastUpdateTime = currentTime
        lastLanguageCode = currentLanguageCode
        
        return newQuote
    }
    
    /**
     * 强制刷新句子（用于语言切换或特殊场景）
     */
    fun forceRefresh() {
        cachedQuote = null
        lastUpdateTime = 0
        lastLanguageCode = null
    }
    
    /**
     * 获取当前时段名称（用于调试）
     */
    fun getCurrentPeriod(): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 6..11 -> "早上"
            in 12..17 -> "中午"
            else -> "晚上"
        }
    }
}
