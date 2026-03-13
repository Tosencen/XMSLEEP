package org.xmsleep.app.weather

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object WeatherSoundMapper {
    private const val KEY_WEATHER_SOUND_MAPPING = "weather_sound_mapping"
    private const val KEY_WEATHER_ENABLED = "weather_enabled"
    private const val KEY_LAST_WEATHER_CODE = "last_weather_code"
    private const val KEY_LAST_LATITUDE = "last_latitude"
    private const val KEY_LAST_LONGITUDE = "last_longitude"
    private const val KEY_LAST_TEMPERATURE = "last_temperature"
    private const val KEY_LAST_CITY_NAME = "last_city_name"
    private const val KEY_LAST_HUMIDITY = "last_humidity"
    private const val KEY_LAST_FEELS_LIKE = "last_feels_like"

    private val gson = Gson()

    data class SoundMapping(
        val weatherTypes: List<WeatherType>,
        val soundIds: List<String>
    )

    fun getDefaultMappings(): List<SoundMapping> {
        return listOf(
            // 晴天：厨房烹饪、风铃、轻钢琴
            SoundMapping(
                weatherTypes = listOf(WeatherType.SUNNY_CLEAR),
                soundIds = listOf("kitchen", "wind-chimes", "light-piano")
            ),
            // 晴晚：田野、风声
            SoundMapping(
                weatherTypes = listOf(WeatherType.SUNNY_NIGHT),
                soundIds = listOf("field", "wind")
            ),
            // 多云：轻钢琴、鸟叫
            SoundMapping(
                weatherTypes = listOf(WeatherType.CLOUDY_PARTLY),
                soundIds = listOf("light-piano", "birds")
            ),
            // 阴：湖泊、风声
            SoundMapping(
                weatherTypes = listOf(WeatherType.CLOUDY_OVERCAST),
                soundIds = listOf("lake", "wind")
            ),
            // 雾：风铃、厨房烹饪
            SoundMapping(
                weatherTypes = listOf(WeatherType.FOGGY),
                soundIds = listOf("wind-chimes", "kitchen")
            ),
            // 小雾：湖泊、风声
            SoundMapping(
                weatherTypes = listOf(WeatherType.FOGGY_DRIZZLE),
                soundIds = listOf("lake", "wind")
            ),
            // 雨夹雪：风声、踩雪、毛毛雨
            SoundMapping(
                weatherTypes = listOf(WeatherType.SNOW_SLEET),
                soundIds = listOf("wind", "walk-in-snow", "drizzle")
            ),
            // 小雨
            SoundMapping(
                weatherTypes = listOf(WeatherType.RAIN_LIGHT),
                soundIds = listOf("rain", "light-rain", "drizzle")
            ),
            // 中雨
            SoundMapping(
                weatherTypes = listOf(WeatherType.RAIN_MODERATE),
                soundIds = listOf("rain", "light-rain")
            ),
            // 大雨/阵雨
            SoundMapping(
                weatherTypes = listOf(WeatherType.RAIN_HEAVY, WeatherType.RAIN_SHOWER),
                soundIds = listOf("heavy-rain", "rain", "drizzle")
            ),
            // 雷暴
            SoundMapping(
                weatherTypes = listOf(WeatherType.THUNDERSTORM, WeatherType.THUNDERSTORM_HAIL),
                soundIds = listOf("thunderstorm", "rain", "heavy-rain")
            ),
            // 小雪
            SoundMapping(
                weatherTypes = listOf(WeatherType.SNOW_LIGHT),
                soundIds = listOf("walk-in-snow", "wind")
            ),
            // 中雪/大雪
            SoundMapping(
                weatherTypes = listOf(WeatherType.SNOW_MODERATE, WeatherType.SNOW_HEAVY),
                soundIds = listOf("walk-in-snow", "wind", "field")
            )
        )
    }

    fun saveMappings(context: Context, mappings: List<SoundMapping>) {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val json = gson.toJson(mappings)
        prefs.edit().putString(KEY_WEATHER_SOUND_MAPPING, json).apply()
    }

    fun getMappings(context: Context): List<SoundMapping> {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_WEATHER_SOUND_MAPPING, null)
        return if (json != null) {
            try {
                val type = object : TypeToken<List<SoundMapping>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                getDefaultMappings()
            }
        } else {
            getDefaultMappings()
        }
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_WEATHER_ENABLED, enabled).apply()
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_WEATHER_ENABLED, false)
    }

    fun saveLastWeather(context: Context, weatherCode: Int, latitude: Double, longitude: Double, temperature: Double = 0.0, cityName: String = "", humidity: Int = 0, feelsLike: Double = 0.0) {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(KEY_LAST_WEATHER_CODE, weatherCode)
            .putFloat(KEY_LAST_LATITUDE, latitude.toFloat())
            .putFloat(KEY_LAST_LONGITUDE, longitude.toFloat())
            .putFloat(KEY_LAST_TEMPERATURE, temperature.toFloat())
            .putString(KEY_LAST_CITY_NAME, cityName)
            .putInt(KEY_LAST_HUMIDITY, humidity)
            .putFloat(KEY_LAST_FEELS_LIKE, feelsLike.toFloat())
            .apply()
    }

    fun getLastWeather(context: Context): WeatherData? {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        val weatherCode = prefs.getInt(KEY_LAST_WEATHER_CODE, -1)
        if (weatherCode == -1) return null
        
        val humidity = prefs.getInt(KEY_LAST_HUMIDITY, 0)
        val feelsLike = prefs.getFloat(KEY_LAST_FEELS_LIKE, 0f).toDouble()
        
        return WeatherData(
            temperature = prefs.getFloat(KEY_LAST_TEMPERATURE, 0f).toDouble(),
            weatherCode = weatherCode,
            windSpeed = 0.0,
            description = "",  // 空字符串，由 UI 层动态生成
            icon = WeatherCodeMapper.toIcon(weatherCode),
            cityName = prefs.getString(KEY_LAST_CITY_NAME, "") ?: "",
            humidity = humidity,
            feelsLike = feelsLike
        )
    }

    fun getLastWeatherCode(context: Context): Int? {
        val prefs = context.getSharedPreferences("weather_prefs", Context.MODE_PRIVATE)
        return if (prefs.contains(KEY_LAST_WEATHER_CODE)) {
            prefs.getInt(KEY_LAST_WEATHER_CODE, -1)
        } else {
            null
        }
    }

    fun getRecommendedSoundIds(context: Context, weatherCode: Int): List<String> {
        val mappings = getMappings(context)
        val weatherType = WeatherCodeMapper.toWeatherType(weatherCode)

        for (mapping in mappings) {
            if (mapping.weatherTypes.contains(weatherType)) {
                return mapping.soundIds
            }
        }

        return emptyList()
    }
}
