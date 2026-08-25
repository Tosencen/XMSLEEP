package org.xmsleep.app.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.xmsleep.app.R
import org.xmsleep.app.utils.NetworkClient
import java.util.concurrent.TimeUnit

data class WeatherData(
    val temperature: Double,
    val weatherCode: Int,
    val windSpeed: Double,
    val description: String,
    val icon: String,
    val cityName: String = "",
    val humidity: Int = 0,
    val feelsLike: Double = 0.0,
    val precipitation: Double = 0.0,
    val isDay: Boolean = true,
    val cloudCover: Int = 0,
    val source: String = ""
)

object WeatherCodeMapper {
    fun toDescription(code: Int, context: android.content.Context? = null): String {
        return when (code) {
            0 -> context?.getString(R.string.weather_clear) ?: "晴"
            1 -> context?.getString(R.string.weather_partly_cloudy) ?: "晴间多云"
            2 -> context?.getString(R.string.weather_cloudy) ?: "多云"
            3 -> context?.getString(R.string.weather_overcast) ?: "阴"
            45, 48 -> context?.getString(R.string.weather_fog) ?: "雾"
            51, 53, 55 -> context?.getString(R.string.weather_drizzle) ?: "毛毛雨"
            56, 57 -> context?.getString(R.string.weather_freezing_fog) ?: "冻毛毛雨"
            61, 63, 65 -> context?.getString(R.string.weather_rain) ?: "雨"
            66, 67 -> context?.getString(R.string.weather_freezing_rain) ?: "冻雨"
            71, 73, 75 -> context?.getString(R.string.weather_snow) ?: "雪"
            77 -> context?.getString(R.string.weather_snow_grains) ?: "雪粒"
            80, 81, 82 -> context?.getString(R.string.weather_shower) ?: "阵雨"
            85, 86 -> context?.getString(R.string.weather_snow_shower) ?: "阵雪"
            95 -> context?.getString(R.string.weather_thunderstorm) ?: "雷暴"
            96, 99 -> context?.getString(R.string.weather_thunderstorm_hail) ?: "雷暴伴冰雹"
            else -> context?.getString(R.string.weather_unknown) ?: "未知"
        }
    }

    fun toIcon(code: Int, isDay: Boolean = true): String {
        return when (code) {
            0 -> if (isDay) "☀️" else "🌙"
            1, 2 -> if (isDay) "⛅" else "☁️"
            3 -> "☁️"
            45, 48 -> "🌫️"
            51, 53, 55, 56, 57 -> "🌧️"
            61, 63, 65, 66, 67 -> "🌧️"
            71, 73, 75, 77 -> "❄️"
            80, 81, 82 -> "🌧️"
            85, 86 -> "🌨️"
            95, 96, 99 -> "⛈️"
            else -> "🌤️"
        }
    }

    /**
     * 返回对应的 Meteocons Lottie 动画资源 ID（fill 风格，区分昼夜）。
     * 资源文件位于 res/raw/，由 Meteocons (MIT) 提供，离线打包随 APK 分发。
     */
    fun toLottieResId(code: Int, isDay: Boolean = true): Int {
        return when (code) {
            0 -> if (isDay) R.raw.wx_clear_day else R.raw.wx_clear_night
            1, 2 -> if (isDay) R.raw.wx_partly_cloudy_day else R.raw.wx_partly_cloudy_night
            3 -> R.raw.wx_overcast
            45, 48 -> if (isDay) R.raw.wx_fog_day else R.raw.wx_fog_night
            51, 53, 55 -> R.raw.wx_drizzle
            56, 57, 66, 67 -> R.raw.wx_sleet
            61, 63, 65 -> R.raw.wx_rain
            80, 81, 82 -> if (isDay) R.raw.wx_partly_cloudy_day_rain else R.raw.wx_partly_cloudy_night_rain
            71, 73, 75, 77 -> R.raw.wx_snow
            85, 86 -> if (isDay) R.raw.wx_partly_cloudy_day_snow else R.raw.wx_partly_cloudy_night_snow
            95 -> R.raw.wx_thunderstorms_night
            96, 99 -> R.raw.wx_thunderstorms_extreme_night
            else -> if (isDay) R.raw.wx_clear_day else R.raw.wx_clear_night
        }
    }

    fun toWeatherType(code: Int, isDay: Boolean = true): WeatherType {
        return when (code) {
            0 -> if (isDay) WeatherType.SUNNY_CLEAR else WeatherType.SUNNY_NIGHT
            1, 2 -> WeatherType.CLOUDY_PARTLY
            3 -> WeatherType.CLOUDY_OVERCAST
            45, 48 -> WeatherType.FOGGY
            51, 53, 55 -> WeatherType.FOGGY_DRIZZLE
            56, 57 -> WeatherType.FOGGY_DRIZZLE
            61 -> WeatherType.RAIN_LIGHT
            63 -> WeatherType.RAIN_MODERATE
            65 -> WeatherType.RAIN_HEAVY
            66, 67 -> WeatherType.RAIN_HEAVY
            80, 81, 82 -> WeatherType.RAIN_SHOWER
            71, 77 -> WeatherType.SNOW_LIGHT
            73 -> WeatherType.SNOW_MODERATE
            75 -> WeatherType.SNOW_HEAVY
            85, 86 -> WeatherType.SNOW_SLEET
            95 -> WeatherType.THUNDERSTORM
            96, 99 -> WeatherType.THUNDERSTORM_HAIL
            else -> WeatherType.UNKNOWN
        }
    }
}

enum class WeatherType {
    SUNNY_CLEAR,
    SUNNY_NIGHT,
    CLOUDY_PARTLY,
    CLOUDY_OVERCAST,
    FOGGY,
    FOGGY_DRIZZLE,
    RAIN_LIGHT,
    RAIN_MODERATE,
    RAIN_HEAVY,
    RAIN_SHOWER,
    SNOW_LIGHT,
    SNOW_MODERATE,
    SNOW_HEAVY,
    SNOW_SLEET,
    THUNDERSTORM,
    THUNDERSTORM_HAIL,
    UNKNOWN;

    fun toDisplayString(): String {
        return when (this) {
            SUNNY_CLEAR -> "晴"
            SUNNY_NIGHT -> "晴晚"
            CLOUDY_PARTLY -> "多云"
            CLOUDY_OVERCAST -> "阴"
            FOGGY -> "雾"
            FOGGY_DRIZZLE -> "小雾"
            RAIN_LIGHT -> "小雨"
            RAIN_MODERATE -> "中雨"
            RAIN_HEAVY -> "大雨"
            RAIN_SHOWER -> "阵雨"
            SNOW_LIGHT -> "小雪"
            SNOW_MODERATE -> "中雪"
            SNOW_HEAVY -> "大雪"
            SNOW_SLEET -> "雨夹雪"
            THUNDERSTORM -> "雷暴"
            THUNDERSTORM_HAIL -> "雷暴冰雹"
            UNKNOWN -> "未知"
        }
    }
}

class WeatherService {
    private val client = NetworkClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 获取天气：
     * 1. 若用户已配置和风天气（Host+Key），优先使用和风（国内精准、中文城市名）；
     * 2. 和风失败（Key 无效/网络错误）则回退 Open-Meteo（免费、无需 Key）。
     */
    suspend fun getWeather(latitude: Double, longitude: Double, context: android.content.Context? = null): Result<WeatherData> {
        val lang = resolveLang(context)
        if (context != null && WeatherSourceConfig.isConfigured(context)) {
            val qw = getWeatherQWeather(
                latitude, longitude,
                WeatherSourceConfig.normalizedHost(context),
                WeatherSourceConfig.getKey(context),
                lang
            )
            if (qw.isSuccess) return qw
            // 和风失败，继续回退
        }
        return getWeatherOpenMeteo(latitude, longitude, lang)
    }

    /** 把当前语言映射成和风/各 API 支持的语言码（多语言城市名） */
    private fun resolveLang(context: android.content.Context?): String {
        val locale = if (context != null) {
            org.xmsleep.app.i18n.LanguageManager.getCurrentLocale(context)
        } else {
            java.util.Locale.getDefault()
        }
        return when (locale.language) {
            "zh" -> if (locale.country.equals("TW", true) || locale.country.equals("HK", true)) "zh-hk" else "zh"
            "ja" -> "ja"
            "ko" -> "ko"
            "ru" -> "ru"
            "en" -> "en"
            else -> "en"
        }
    }

    /** 校验用户填写的和风 Host+Key 是否可用 */
    suspend fun validateQWeather(host: String, key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val normalized = host.trim().removePrefix("https://").removePrefix("http://").trim()
            if (normalized.isEmpty() || key.isBlank()) {
                return@withContext Result.failure(Exception("请填写完整的 Host 和 Key"))
            }
            val url = "https://$normalized/v7/weather/now?location=116.41,39.92&key=${key.trim()}"
            val response = client.newCall(Request.Builder().url(url).get().build()).execute()
            response.use {
                if (!it.isSuccessful) return@withContext Result.failure(Exception("HTTP ${it.code}"))
                val body = it.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
                val json = JSONObject(body)
                val code = json.optString("code", "")
                if (code == "200") Result.success(Unit)
                else Result.failure(Exception("和风返回 code=$code（Key 无效或权限不足）"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getWeatherQWeather(latitude: Double, longitude: Double, host: String, key: String, lang: String): Result<WeatherData> =
        withContext(Dispatchers.IO) {
            try {
                // 和风国内要求 GCJ-02 坐标
                val (gcjLat, gcjLon) = CoordinateUtils.wgs84ToGcj02(latitude, longitude)
                val loc = "$gcjLon,$gcjLat"

                val nowUrl = "https://$host/v7/weather/now?location=$loc&key=$key"
                val nowResp = client.newCall(Request.Builder().url(nowUrl).get().build()).execute()
                val weatherData = nowResp.use { resp ->
                    if (!resp.isSuccessful) return@withContext Result.failure(Exception("QWeather HTTP ${resp.code}"))
                    val body = resp.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
                    val json = JSONObject(body)
                    if (json.optString("code", "") != "200") {
                        return@withContext Result.failure(Exception("QWeather code ${json.optString("code")}"))
                    }
                    val now = json.getJSONObject("now")
                    val temperature = now.getDouble("temp")
                    val icon = now.optString("icon", "100")
                    val weatherCode = QWeatherMapper.iconToWmo(icon)
                    val isDay = QWeatherMapper.isDayFromIcon(icon)
                    val humidity = now.optInt("humidity", 0)
                    val feelsLike = now.optDouble("feelsLike", temperature)
                    val windSpeed = now.optDouble("windSpeed", 0.0)
                    val precip = now.optDouble("precip", 0.0)
                    val cloud = now.optInt("cloud", 0)

                    val cityNameDeferred = async { getQWeatherCityName(host, key, loc, lang) }
                    val cityName = cityNameDeferred.await().getOrElse { "" }

                    WeatherData(
                        temperature = temperature,
                        weatherCode = weatherCode,
                        windSpeed = windSpeed,
                        description = "",
                        icon = WeatherCodeMapper.toIcon(weatherCode, isDay),
                        cityName = cityName,
                        humidity = humidity,
                        feelsLike = feelsLike,
                        precipitation = precip,
                        isDay = isDay,
                        cloudCover = cloud,
                        source = "和风天气"
                    )
                }
                Result.success(weatherData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun getQWeatherCityName(host: String, key: String, loc: String, lang: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url = "https://$host/geo/v2/city/lookup?location=$loc&key=$key&lang=$lang"
                val response = client.newCall(Request.Builder().url(url).get().build()).execute()
                response.use {
                    if (!it.isSuccessful) return@withContext Result.failure(Exception("HTTP ${it.code}"))
                    val body = it.body?.string() ?: return@withContext Result.failure(Exception("空响应"))
                    val json = JSONObject(body)
                    if (json.optString("code", "") != "200") return@withContext Result.success("")
                    val arr = json.optJSONArray("location") ?: return@withContext Result.success("")
                    val name = if (arr.length() > 0) {
                        val loc0 = arr.getJSONObject(0)
                        // 只显示城市级（adm2），避免细化到区/镇
                        loc0.optString("adm2", "").ifBlank { loc0.optString("name", "") }
                    } else ""
                    Result.success(name)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getWeatherOpenMeteo(latitude: Double, longitude: Double, lang: String = "en"): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            try {
                val cityNameDeferred = async { getCityName(latitude, longitude, lang) }

                val weatherUrl = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$latitude" +
                        "&longitude=$longitude" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m,is_day,cloud_cover" +
                        "&timezone=auto"

                val request = Request.Builder()
                    .url(weatherUrl)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${it.code}"))
                    }

                    val body = it.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

                    val json = JSONObject(body)
                    val current = json.getJSONObject("current")

                    val temperature = current.getDouble("temperature_2m")
                    val weatherCode = current.getInt("weather_code")
                    val windSpeed = current.getDouble("wind_speed_10m")
                    val humidity = current.optInt("relative_humidity_2m", 0)
                    val feelsLike = current.optDouble("apparent_temperature", temperature)
                    val precipitation = current.optDouble("precipitation", 0.0)
                    val isDay = current.optInt("is_day", 1) == 1
                    val cloudCover = current.optInt("cloud_cover", 0)

                    val cityName = cityNameDeferred.await().getOrElse { "" }

                    val weatherData = WeatherData(
                        temperature = temperature,
                        weatherCode = weatherCode,
                        windSpeed = windSpeed,
                        description = "",
                        icon = WeatherCodeMapper.toIcon(weatherCode, isDay),
                        cityName = cityName,
                        humidity = humidity,
                        feelsLike = feelsLike,
                        precipitation = precipitation,
                        isDay = isDay,
                        cloudCover = cloudCover,
                        source = "Open-Meteo"
                    )

                    Result.success(weatherData)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun getCityName(latitude: Double, longitude: Double, lang: String = "en"): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse" +
                        "?lat=$latitude" +
                        "&lon=$longitude" +
                        "&format=json&addressdetails=1"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "XMSleep/1.0")
                    .header("Accept-Language", lang.substringBefore("-").ifBlank { "en" })
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                response.use {
                    if (!it.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP ${it.code}"))
                    }

                    val body = it.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                    val json = JSONObject(body)
                    val address = json.optJSONObject("address") ?: JSONObject()

                    // 只取城市级：city > county(区/县) > town > village，避免细化到村镇
                    val city = address.optString("city", "")
                    val county = address.optString("county", "")
                    val town = address.optString("town", "")
                    val village = address.optString("village", "")
                    val cityName = city.ifEmpty { county.ifEmpty { town.ifEmpty { village } } }

                    Result.success(cityName)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
