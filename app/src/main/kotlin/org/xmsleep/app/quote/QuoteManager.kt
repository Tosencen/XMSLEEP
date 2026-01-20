package org.xmsleep.app.quote

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate

/**
 * 名句管理器
 * 负责获取每日一言，支持在线API和本地备用
 */
class QuoteManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "QuoteManager"
        // 丰富类型：动画(a)、漫画(b)、游戏(c)、文学(d)、影视(e)、诗词(i)、网易云(j)、哲学(k)、抖机灵(l)
        private const val HITOKOTO_API = "https://v1.hitokoto.cn/?c=a&c=b&c=c&c=d&c=e&c=i&c=j&c=k"
        
        @Volatile
        private var instance: QuoteManager? = null
        
        fun getInstance(context: Context): QuoteManager {
            return instance ?: synchronized(this) {
                instance ?: QuoteManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
    
    private val gson = Gson()
    private val okHttpClient = OkHttpClient()
    private val prefs = context.getSharedPreferences("daily_quote", Context.MODE_PRIVATE)
    
    // 本地名句列表（懒加载）
    private val localQuotes: List<Quote> by lazy {
        loadLocalQuotes()
    }
    
    /**
     * 获取今日名句
     * 优先从API获取，失败则使用本地备用
     */
    suspend fun getTodayQuote(): Quote {
        return withContext(Dispatchers.IO) {
            try {
                // 1. 尝试从API获取
                val apiQuote = fetchQuoteFromAPI()
                if (apiQuote != null) {
                    Log.d(TAG, "从API获取名句成功: ${apiQuote.text}")
                    // 保存到历史记录
                    saveToHistory(apiQuote)
                    return@withContext apiQuote
                }
            } catch (e: Exception) {
                Log.e(TAG, "从API获取名句失败: ${e.message}")
            }
            
            // 2. API失败，使用本地备用
            Log.d(TAG, "使用本地备用名句")
            val localQuote = getLocalQuoteByDate()
            // 保存到历史记录
            saveToHistory(localQuote)
            localQuote
        }
    }
    
    /**
     * 从一言API获取名句
     */
    private suspend fun fetchQuoteFromAPI(): Quote? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(HITOKOTO_API)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        gson.fromJson(json, Quote::class.java)
                    } else null
                } else {
                    Log.w(TAG, "API请求失败: ${response.code}")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "API请求异常: ${e.message}")
                null
            }
        }
    }
    
    /**
     * 从本地加载名句列表
     */
    private fun loadLocalQuotes(): List<Quote> {
        return try {
            val json = context.assets.open("quotes.json").use { inputStream ->
                inputStream.bufferedReader().use { it.readText() }
            }
            val manifest = gson.fromJson(json, QuotesManifest::class.java)
            manifest.quotes.map { it.toQuote() }
        } catch (e: Exception) {
            Log.e(TAG, "加载本地名句失败: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 根据日期获取本地名句（每天不同）
     */
    private fun getLocalQuoteByDate(): Quote {
        if (localQuotes.isEmpty()) {
            // 如果本地名句加载失败，返回默认名句
            return Quote(
                hitokoto = "生活不是等待暴风雨过去，而是学会在雨中跳舞。",
                from_who = "Vivian Greene"
            )
        }
        
        // 根据日期计算索引，确保每天显示不同的名句
        val today = LocalDate.now()
        val daysSinceEpoch = today.toEpochDay()
        val index = (daysSinceEpoch % localQuotes.size).toInt()
        
        return localQuotes[index]
    }
    
    /**
     * 检查是否应该自动显示每日一言
     */
    fun shouldAutoShow(): Boolean {
        // 1. 检查用户设置
        val autoShowEnabled = prefs.getBoolean("auto_show_enabled", true)
        if (!autoShowEnabled) return false
        
        // 2. 检查是否今天已显示
        val lastShownDate = prefs.getString("last_shown_date", "")
        val today = LocalDate.now().toString()
        if (lastShownDate == today) return false
        
        return true
    }
    
    /**
     * 记录已显示
     */
    fun markAsShown() {
        prefs.edit()
            .putString("last_shown_date", LocalDate.now().toString())
            .apply()
    }
    
    /**
     * 设置自动显示开关
     */
    fun setAutoShowEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean("auto_show_enabled", enabled)
            .apply()
    }
    
    /**
     * 获取自动显示设置
     */
    fun isAutoShowEnabled(): Boolean {
        return prefs.getBoolean("auto_show_enabled", true)
    }
    
    /**
     * 保存名句到历史记录
     */
    private fun saveToHistory(quote: Quote) {
        try {
            val history = getHistory().toMutableList()
            
            // 添加时间戳
            val historyItem = QuoteHistoryItem(
                quote = quote,
                timestamp = System.currentTimeMillis(),
                date = LocalDate.now().toString()
            )
            
            // 添加到列表开头
            history.add(0, historyItem)
            
            // 只保留最近100条
            if (history.size > 100) {
                history.subList(100, history.size).clear()
            }
            
            // 保存到 SharedPreferences
            val json = gson.toJson(QuoteHistory(history))
            prefs.edit()
                .putString("quote_history", json)
                .apply()
            
            Log.d(TAG, "保存历史记录成功，当前共 ${history.size} 条")
        } catch (e: Exception) {
            Log.e(TAG, "保存历史记录失败: ${e.message}")
        }
    }
    
    /**
     * 获取历史记录
     */
    fun getHistory(): List<QuoteHistoryItem> {
        return try {
            val json = prefs.getString("quote_history", null)
            if (json != null) {
                val history = gson.fromJson(json, QuoteHistory::class.java)
                history.items
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "读取历史记录失败: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * 清空历史记录
     */
    fun clearHistory() {
        prefs.edit()
            .remove("quote_history")
            .apply()
        Log.d(TAG, "历史记录已清空")
    }
}

/**
 * 历史记录项
 */
data class QuoteHistoryItem(
    val quote: Quote,
    val timestamp: Long,
    val date: String
)

/**
 * 历史记录容器
 */
data class QuoteHistory(
    val items: List<QuoteHistoryItem>
)
