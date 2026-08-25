package org.xmsleep.app.quote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import org.xmsleep.app.utils.NetworkClient

/**
 * 必应每日壁纸（HPImageArchive），免 Key、国内直连。
 * random=true 时在最近 8 天里随机取一张，用作"每日一言"背景图。
 */
object DailyImageProvider {
    private const val BING_API = "https://www.bing.com/HPImageArchive.aspx?format=js&idx=%d&n=1&mkt=zh-CN"

    suspend fun getTodayImage(random: Boolean = true): DailyImage? = withContext(Dispatchers.IO) {
        try {
            val idx = if (random) kotlin.random.Random.Default.nextInt(8) else 0
            val request = Request.Builder().url(BING_API.format(idx)).get().build()
            val response = NetworkClient.default.newCall(request).execute()
            response.use {
                if (!it.isSuccessful) return@withContext null
                val body = it.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val images = json.optJSONArray("images") ?: return@withContext null
                if (images.length() == 0) return@withContext null
                val img = images.getJSONObject(0)
                val rawUrl = img.optString("url", "")
                if (rawUrl.isBlank()) return@withContext null
                val fullUrl = if (rawUrl.startsWith("http", ignoreCase = true)) rawUrl else "https://www.bing.com$rawUrl"
                val copyright = img.optString("copyright", "").ifBlank { null }
                DailyImage(fullUrl, copyright)
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class DailyImage(
    val url: String,
    val copyright: String?
)
