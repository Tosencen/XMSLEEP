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
    val cloudCover: Int = 0
)

object WeatherCodeMapper {
    fun toDescription(code: Int, context: android.content.Context? = null): String {
        return when (code) {
            0 -> context?.getString(R.string.weather_clear) ?: "晴"
            1 -> context?.getString(R.string.weather_partly_cloudy) ?: "晴间多云"
            2 -> context?.getString(R.string.weather_cloudy) ?: "多云"
            3 -> context?.getString(R.string.weather_overcast) ?: "阴"
            45, 48 -> context?.getString(R.string.weather_fog) ?: "雾"
            51, 53, 55 -> context?.getString(R.string.weather_drizzle) ?: "小雾"
            56, 57 -> context?.getString(R.string.weather_freezing_fog) ?: "冻雾"
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
            45, 48 -> R.raw.wx_fog
            51, 53, 55, 56, 57 -> R.raw.wx_drizzle
            61, 63, 65, 66, 67, 80, 81, 82 -> R.raw.wx_rain
            71, 73, 75, 77, 85, 86 -> R.raw.wx_snow
            95, 96, 99 -> if (isDay) R.raw.wx_thunderstorms_day else R.raw.wx_thunderstorms_night
            else -> if (isDay) R.raw.wx_clear_day else R.raw.wx_clear_night
        }
    }

    fun toWeatherType(code: Int): WeatherType {
        return when (code) {
            0 -> WeatherType.SUNNY_CLEAR
            1, 2 -> WeatherType.CLOUDY_PARTLY
            3 -> WeatherType.CLOUDY_OVERCAST
            45, 48 -> WeatherType.FOGGY
            51, 53, 55 -> WeatherType.FOGGY_DRIZZLE
            56, 57 -> WeatherType.FOGGY_DRIZZLE
            61 -> WeatherType.RAIN_LIGHT
            63 -> WeatherType.RAIN_MODERATE
            65, 66, 67 -> WeatherType.RAIN_HEAVY
            80, 81, 82 -> WeatherType.RAIN_SHOWER
            71 -> WeatherType.SNOW_LIGHT
            73 -> WeatherType.SNOW_MODERATE
            75, 77 -> WeatherType.SNOW_HEAVY
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

    suspend fun getWeather(latitude: Double, longitude: Double): Result<WeatherData> {
        return withContext(Dispatchers.IO) {
            try {
                // 并行请求城市名和天气数据
                val cityNameDeferred = async { getCityName(latitude, longitude) }

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

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))

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

                // 等待城市名请求完成（与天气请求并行，不额外增加延迟）
                val cityName = cityNameDeferred.await().getOrElse { "" }

                val weatherData = WeatherData(
                    temperature = temperature,
                    weatherCode = weatherCode,
                    windSpeed = windSpeed,
                    description = "",  // 不再使用 description 字段，改为在 UI 层动态生成
                    icon = WeatherCodeMapper.toIcon(weatherCode, isDay),
                    cityName = cityName,
                    humidity = humidity,
                    feelsLike = feelsLike,
                    precipitation = precipitation,
                    isDay = isDay,
                    cloudCover = cloudCover
                )

                Result.success(weatherData)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    private suspend fun getCityName(latitude: Double, longitude: Double): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "https://nominatim.openstreetmap.org/reverse" +
                        "?lat=$latitude" +
                        "&lon=$longitude" +
                        "&format=json"

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "XMSleep/1.0")
                    .get()
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}"))
                }

                val body = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(body)
                
                val city = json.optString("city", "")
                val town = json.optString("town", "")
                val village = json.optString("village", "")
                val county = json.optString("county", "")
                
                val cityName = city.ifEmpty { town.ifEmpty { village.ifEmpty { county } } }
                
                Result.success(cityName)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
