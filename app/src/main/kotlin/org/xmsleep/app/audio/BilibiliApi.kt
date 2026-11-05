package org.xmsleep.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.xmsleep.app.utils.Logger
import org.xmsleep.app.utils.NetworkClient
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object BilibiliApi {
    private const val TAG = "BilibiliApi"
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
    private const val REFERER = "https://live.bilibili.com/"

    private val client = NetworkClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var buvidLock = Mutex()
    @Volatile private var cachedCookie: String? = null

    private suspend fun getCookie(): String {
        if (cachedCookie != null) return cachedCookie!!
        buvidLock.withLock {
            if (cachedCookie != null) return cachedCookie!!
            try {
                val request = Request.Builder()
                    .url("https://api.bilibili.com/x/frontend/finger/spi")
                    .header("User-Agent", USER_AGENT)
                    .build()
                val response = client.newCall(request).execute()
                val json = JSONObject(response.use { it.body?.string() ?: "{}" })
                val data = json.getJSONObject("data")
                cachedCookie = "buvid3=${data.getString("b_3")};buvid4=${data.getString("b_4")};"
            } catch (e: Exception) {
                Logger.w(TAG, "getCookie failed: ${e.message}, using random buvid")
                cachedCookie = "buvid3=${UUID.randomUUID().toString().replace("-", "")};buvid4=${UUID.randomUUID().toString().replace("-", "")};"
            }
        }
        return cachedCookie!!
    }

    private suspend fun req(url: String): Request.Builder = Request.Builder().url(url)
        .header("User-Agent", USER_AGENT)
        .header("Referer", REFERER)
        .header("Cookie", getCookie())

    private suspend fun reqUrl(httpUrl: HttpUrl): Request.Builder = Request.Builder().url(httpUrl)
        .header("User-Agent", USER_AGENT)
        .header("Referer", REFERER)
        .header("Cookie", getCookie())

    data class RoomInfo(
        val isLive: Boolean,
        val title: String,
        val online: Int,
        val realRoomId: String
    )

    suspend fun prewarm() { getCookie() }

    suspend fun getRoomInfo(roomId: String): RoomInfo? = withContext(Dispatchers.IO) {
        try {
            val request = req("https://api.live.bilibili.com/room/v1/Room/get_info?room_id=$roomId").build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.use { it.body?.string() ?: "{}" })
            if (json.getInt("code") != 0) {
                Logger.w(TAG, "getRoomInfo: code=${json.getInt("code")}, msg=${json.optString("msg")}")
                return@withContext null
            }
            val data = json.getJSONObject("data")
            RoomInfo(
                isLive = data.getInt("live_status") == 1,
                title = data.optString("title", ""),
                online = data.getInt("online"),
                realRoomId = data.optString("room_id", roomId)
            )
        } catch (e: Exception) {
            Logger.e(TAG, "getRoomInfo failed for roomId=$roomId: ${e.message}")
            null
        }
    }

    data class LiveRoom(
        val roomId: String,
        val title: String,
        val userName: String,
        val online: Int,
        val cateName: String = "",
        val area: Int = 0
    )

    // ========== WBI 签名 ==========

    private val mixinKeyEncTab = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )

    @Volatile private var cachedImgKey = ""
    @Volatile private var cachedSubKey = ""
    @Volatile private var cachedAccessId = ""

    private suspend fun getWbiKeys(): Pair<String, String> = withContext(Dispatchers.IO) {
        if (cachedImgKey.isNotEmpty() && cachedSubKey.isNotEmpty()) {
            return@withContext Pair(cachedImgKey, cachedSubKey)
        }
        try {
            val request = req("https://api.bilibili.com/x/web-interface/nav").build()
            val response = client.newCall(request).execute()
            val json = JSONObject(response.use { it.body?.string() ?: "{}" })
            val wbi = json.getJSONObject("data").getJSONObject("wbi_img")
            val imgUrl = wbi.getString("img_url")
            val subUrl = wbi.getString("sub_url")
            cachedImgKey = imgUrl.substringAfterLast("/").substringBefore(".")
            cachedSubKey = subUrl.substringAfterLast("/").substringBefore(".")
            Pair(cachedImgKey, cachedSubKey)
        } catch (e: Exception) {
            Logger.e(TAG, "getWbiKeys failed: ${e.message}")
            Pair("", "")
        }
    }

    private fun getMixinKey(origin: String): String {
        val maxIndex = mixinKeyEncTab.maxOrNull() ?: 0
        if (origin.length <= maxIndex) return origin
        val sb = StringBuilder(32)
        for (i in 0 until 32) {
            sb.append(origin[mixinKeyEncTab[i]])
        }
        return sb.toString()
    }

    private fun encodeQueryValue(s: String): String {
        val sb = StringBuilder(s.length * 3)
        for (c in s) {
            when {
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '.' || c == '_' || c == '~' -> sb.append(c)
                c == ' ' -> sb.append("%20")
                else -> sb.append("%${c.code.toString(16).uppercase().padStart(2, '0')}")
            }
        }
        return sb.toString()
    }

    private suspend fun signParams(queryParams: MutableMap<String, String>): Map<String, String> {
        val (imgKey, subKey) = getWbiKeys()
        if (imgKey.isEmpty()) return queryParams

        val mixinKey = getMixinKey(imgKey + subKey)
        val wts = (System.currentTimeMillis() / 1000).toString()

        queryParams["wts"] = wts

        val sortedKeys = queryParams.keys.sorted()
        val filtered = sortedKeys.associate { key ->
            val value = queryParams[key] ?: ""
            key to value.filter { it !in "!'()*" }
        }

        val query = filtered.entries.joinToString("&") { (k, v) ->
            "$k=${encodeQueryValue(v)}"
        }

        val digest = MessageDigest.getInstance("MD5")
        val sign = digest.digest((query + mixinKey).toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

        queryParams["w_rid"] = sign
        return queryParams
    }

    private suspend fun getAccessId(): String = withContext(Dispatchers.IO) {
        if (cachedAccessId.isNotEmpty()) return@withContext cachedAccessId
        try {
            val request = req("https://live.bilibili.com/lol").build()
            val response = client.newCall(request).execute()
            val html = response.use { it.body?.string() ?: "" }
            val match = Regex("\"access_id\":\"(.*?)\"").find(html)
            cachedAccessId = match?.groupValues?.get(1)?.replace("\\", "") ?: ""
            cachedAccessId
        } catch (e: Exception) {
            Logger.e(TAG, "getAccessId failed: ${e.message}")
            ""
        }
    }

    suspend fun getCategoryRooms(parentAreaId: String, areaId: String, page: Int = 1): List<LiveRoom> = withContext(Dispatchers.IO) {
        try {
            val params = mutableMapOf(
                "platform" to "web",
                "parent_area_id" to parentAreaId,
                "area_id" to areaId,
                "page" to page.toString(),
                "w_webid" to getAccessId()
            )
            val signed = signParams(params)

            val httpUrlBuilder = HttpUrl.Builder()
                .scheme("https")
                .host("api.live.bilibili.com")
                .encodedPath("/xlive/web-interface/v1/second/getList")
            for ((k, v) in signed) {
                httpUrlBuilder.addQueryParameter(k, v)
            }

            val request = reqUrl(httpUrlBuilder.build()).build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.use { it.body?.string() ?: "{}" })
            if (json.getInt("code") != 0) {
                Logger.w(TAG, "getCategoryRooms: code=${json.getInt("code")}")
                return@withContext emptyList()
            }

            val list = json.getJSONObject("data").getJSONArray("list")
            (0 until list.length()).map { i ->
                val r = list.getJSONObject(i)
                LiveRoom(
                    roomId = r.getString("roomid"),
                    title = r.optString("title", ""),
                    userName = r.getString("uname"),
                    online = r.getInt("online"),
                    cateName = ""
                )
            }
        } catch (e: Exception) {
            Logger.e(TAG, "getCategoryRooms failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchRooms(keyword: String, page: Int = 1): List<LiveRoom> = withContext(Dispatchers.IO) {
        try {
            val httpUrl = HttpUrl.Builder()
                .scheme("https")
                .host("api.bilibili.com")
                .encodedPath("/x/web-interface/search/type")
                .addQueryParameter("context", "")
                .addQueryParameter("search_type", "live")
                .addQueryParameter("cover_type", "user_cover")
                .addQueryParameter("order", "")
                .addQueryParameter("keyword", keyword)
                .addQueryParameter("category_id", "")
                .addQueryParameter("__refresh__", "")
                .addQueryParameter("_extra", "")
                .addQueryParameter("highlight", "0")
                .addQueryParameter("single_column", "0")
                .addQueryParameter("page", page.toString())
                .build()
            val request = reqUrl(httpUrl).build()
            val response = client.newCall(request).execute()
            val responseStr = response.use { it.body?.string() ?: "" }
            if (responseStr.trimStart().startsWith("<!DOCTYPE") || responseStr.trimStart().startsWith("<html")) {
                Logger.w(TAG, "searchRooms: got HTML instead of JSON, trying fallback")
                return@withContext searchRoomsFallback(keyword, page)
            }
            val json = JSONObject(responseStr)
            if (json.getInt("code") != 0) {
                Logger.w(TAG, "searchRooms: code=${json.getInt("code")}, trying fallback")
                return@withContext searchRoomsFallback(keyword, page)
            }
            val rooms = json.getJSONObject("data").getJSONObject("result").optJSONArray("live_room")
                ?: return@withContext searchRoomsFallback(keyword, page)
            (0 until rooms.length()).map { i ->
                val r = rooms.getJSONObject(i)
                LiveRoom(
                    roomId = r.getString("roomid"),
                    title = r.getString("title").replace(Regex("<.*?>"), ""),
                    userName = r.getString("uname"),
                    online = r.getInt("online"),
                    cateName = r.optString("cate_name", ""),
                    area = r.optInt("area", 0)
                )
            }.filter { it.online > 0 }
        } catch (e: Exception) {
            Logger.e(TAG, "searchRooms failed for keyword=$keyword: ${e.message}, trying fallback")
            searchRoomsFallback(keyword, page)
        }
    }

    private suspend fun searchRoomsFallback(keyword: String, page: Int = 1): List<LiveRoom> {
        Logger.d(TAG, "searchRoomsFallback: keyword=$keyword, page=$page")
        // Try api.live.bilibili.com category endpoints (not affected by api.bilibili.com blocks)
        val areaConfigs = listOf(
            "0" to "0",   // 推荐 (recommended)
            "4" to "0",   // 电台-全部 (radio-all)
        )
        val allResults = mutableListOf<LiveRoom>()
        for ((parentAreaId, areaId) in areaConfigs) {
            try {
                val rooms = getCategoryRooms(parentAreaId, areaId, page)
                allResults.addAll(rooms)
            } catch (e: Exception) {
                Logger.w(TAG, "searchRoomsFallback: getCategoryRooms($parentAreaId,$areaId) failed: ${e.message}")
            }
        }
        val filtered = allResults
            .filter { it.title.contains(keyword) && it.online > 0 }
            .distinctBy { it.roomId }
            .sortedByDescending { it.online }
        Logger.d(TAG, "searchRoomsFallback: got ${filtered.size} results for keyword=$keyword")
        return filtered
    }

    suspend fun getLiveUrl(roomId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.live.bilibili.com/xlive/web-room/v2/index/getRoomPlayInfo" +
                    "?room_id=$roomId&protocol=0,1&format=0,2&codec=0&platform=web&qn=10000"
            val request = req(url).build()
            val response = client.newCall(request).execute()
            val responseStr = response.use { it.body?.string() ?: "" }
            val json = JSONObject(responseStr)

            Logger.d(TAG, "getLiveUrl full response: ${responseStr.take(1000)}")

            if (json.optInt("code") != 0) {
                Logger.w(TAG, "getLiveUrl: code=${json.optInt("code")}")
                return@withContext null
            }

            val data = json.optJSONObject("data")
            if (data == null) {
                Logger.w(TAG, "getLiveUrl: no data in response")
                return@withContext null
            }

            val pi = data.optJSONObject("playurl_info")
            if (pi == null) {
                Logger.w(TAG, "getLiveUrl: no playurl_info: $data")
                return@withContext null
            }

            val pu = pi.optJSONObject("playurl")
            if (pu == null) {
                Logger.w(TAG, "getLiveUrl: no playurl in playurl_info")
                return@withContext null
            }

            val stream = pu.optJSONArray("stream")
            if (stream == null || stream.length() == 0) {
                Logger.w(TAG, "getLiveUrl: no stream, raw=${pu.toString().take(500)}")
                return@withContext null
            }

            val urls = mutableListOf<String>()
            for (si in 0 until stream.length()) {
                val fmtList = stream.getJSONObject(si).optJSONArray("format") ?: continue
                for (fi in 0 until fmtList.length()) {
                    val cdcList = fmtList.getJSONObject(fi).optJSONArray("codec") ?: continue
                    for (ci in 0 until cdcList.length()) {
                        val cdc = cdcList.getJSONObject(ci)
                        val base = cdc.optString("base_url", "")
                        val infoArr = cdc.optJSONArray("url_info") ?: continue
                        for (ui in 0 until infoArr.length()) {
                            val info = infoArr.getJSONObject(ui)
                            val host = info.optString("host", "")
                            val extra = info.optString("extra", "")
                            if (host.isNotEmpty() && base.isNotEmpty()) {
                                urls.add("$host$base$extra")
                            }
                        }
                    }
                }
            }

            if (urls.isEmpty()) {
                Logger.w(TAG, "getLiveUrl: stream parsed but no valid URLs found")
                return@withContext null
            }

            // HTTP-FMP4 (no .m3u8/.flv) > FLV > HLS; mcdn URLs last
            urls.sortWith(compareBy<String> { a ->
                var score = 0
                if (a.contains("mcdn")) score += 100
                if (a.contains(".m3u8") || a.contains(".m3u")) score += 10
                if (a.contains(".flv") || a.contains("flv?")) score += 5
                score
            })

            val result = urls.first()
            Logger.d(TAG, "getLiveUrl OK: ${result.takeLast(40)}")
            result
        } catch (e: Exception) {
            Logger.e(TAG, "getLiveUrl failed for roomId=$roomId: ${e.message}")
            null
        }
    }
}
