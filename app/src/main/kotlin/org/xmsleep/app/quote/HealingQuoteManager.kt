package org.xmsleep.app.quote

import android.content.Context
import java.util.Calendar

/**
 * 治愈句子管理器
 * 根据时段（早中晚）随机显示治愈文案
 * 使用日期作为种子，确保同一天内显示同一句
 */
object HealingQuoteManager {
    
    /**
     * 获取当前时段的随机治愈句子
     * 同一天内返回同一句（基于日期种子）
     */
    fun getRandomQuote(context: Context): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val year = calendar.get(Calendar.YEAR)
        
        val quotesArrayId = when (hour) {
            in 6..11 -> org.xmsleep.app.R.array.healing_quotes_morning
            in 12..17 -> org.xmsleep.app.R.array.healing_quotes_afternoon
            else -> org.xmsleep.app.R.array.healing_quotes_evening
        }
        
        val quotes = context.resources.getStringArray(quotesArrayId)
        
        // 使用日期作为种子，确保同一天同一时段返回同一句
        val seed = (year.toLong() * 1000 + dayOfYear) * 100 + hour
        val index = (seed % quotes.size).toInt()
        
        return quotes[index]
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
