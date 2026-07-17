package org.xmsleep.app.meditation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmsleep.app.utils.Logger
import java.util.concurrent.TimeUnit

object BilibiliAudioHelper {

    private const val TAG = "BilibiliAudioHelper"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val headers = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "Referer" to "https://www.bilibili.com"
    )

    data class VideoInfo(
        val bvid: String,
        val cid: Long,
        val title: String,
        val duration: Int,
        val coverUrl: String
    )

    data class AudioStream(
        val url: String,
        val quality: Int,
        val codec: String,
        val bandwidth: Int
    )

    /**
     * 获取视频信息（cid等）
     */
    suspend fun getVideoInfo(bvid: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bilibili.com/x/web-interface/view?bvid=$bvid"
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            if (json.optInt("code") != 0) {
                Logger.w(TAG, "getVideoInfo failed: code=${json.optInt("code")}")
                return@withContext null
            }

            val data = json.getJSONObject("data")
            VideoInfo(
                bvid = bvid,
                cid = data.getLong("cid"),
                title = data.getString("title"),
                duration = data.getInt("duration"),
                coverUrl = data.getString("pic")
            )
        } catch (e: Exception) {
            Logger.e(TAG, "getVideoInfo error: ${e.message}")
            null
        }
    }

    /**
     * 获取音频流URL（fnval=16 返回 DASH 格式，有独立音频流）
     * 优先返回高质量音频
     */
    suspend fun getAudioStreamUrl(bvid: String, cid: Long): AudioStream? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.bilibili.com/x/player/playurl?bvid=$bvid&cid=$cid&fnval=16&fnver=0&fourk=1"
            val requestBuilder = Request.Builder().url(url)
            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            val response = client.newCall(requestBuilder.build()).execute()
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)

            if (json.optInt("code") != 0) {
                Logger.w(TAG, "getAudioStreamUrl failed: code=${json.optInt("code")}")
                return@withContext null
            }

            val dash = json.getJSONObject("data").optJSONObject("dash")
            if (dash == null) {
                Logger.w(TAG, "getAudioStreamUrl: no dash data")
                return@withContext null
            }

            val audioArray = dash.optJSONArray("audio")
            if (audioArray == null || audioArray.length() == 0) {
                Logger.w(TAG, "getAudioStreamUrl: no audio streams")
                return@withContext null
            }

            // 选择高质量音频（按bandwidth降序排列，取第一个）
            var bestStream: AudioStream? = null
            for (i in 0 until audioArray.length()) {
                val audio = audioArray.getJSONObject(i)
                val stream = AudioStream(
                    url = audio.getString("baseUrl"),
                    quality = audio.getInt("id"),
                    codec = audio.getString("codecs"),
                    bandwidth = audio.getInt("bandwidth")
                )
                if (bestStream == null || stream.bandwidth > bestStream.bandwidth) {
                    bestStream = stream
                }
            }

            Logger.d(TAG, "getAudioStreamUrl: quality=${bestStream?.quality}, bandwidth=${bestStream?.bandwidth}")
            bestStream
        } catch (e: Exception) {
            Logger.e(TAG, "getAudioStreamUrl error: ${e.message}")
            null
        }
    }

    /**
     * 便捷方法：直接获取音频流URL
     */
    suspend fun getAudioUrl(bvid: String): String? {
        val videoInfo = getVideoInfo(bvid) ?: return null
        val audioStream = getAudioStreamUrl(bvid, videoInfo.cid) ?: return null
        return audioStream.url
    }
}
