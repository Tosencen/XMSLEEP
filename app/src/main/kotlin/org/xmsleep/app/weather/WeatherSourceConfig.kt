package org.xmsleep.app.weather

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 和风天气（用户自备）数据源配置。
 * Host + Key 存于 EncryptedSharedPreferences，避免明文落盘。
 */
object WeatherSourceConfig {
    private const val PREF_NAME = "weather_source_prefs"
    private const val KEY_HOST = "qweather_host"
    private const val KEY_KEY = "qweather_key"

    private fun masterKey(context: Context): MasterKey =
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    private fun prefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREF_NAME,
        masterKey(context),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getHost(context: Context): String = prefs(context).getString(KEY_HOST, "") ?: ""

    fun getKey(context: Context): String = prefs(context).getString(KEY_KEY, "") ?: ""

    fun isConfigured(context: Context): Boolean =
        getHost(context).isNotBlank() && getKey(context).isNotBlank()

    /** 去掉可能带上的协议前缀，统一在请求时补 https:// */
    fun normalizedHost(context: Context): String {
        return getHost(context)
            .trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .trim()
    }

    fun save(context: Context, host: String, key: String) {
        prefs(context).edit()
            .putString(KEY_HOST, host.trim())
            .putString(KEY_KEY, key.trim())
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_HOST).remove(KEY_KEY).apply()
    }

    fun maskedHost(context: Context): String {
        val h = getHost(context)
        if (h.isEmpty()) return ""
        if (h.length <= 8) return h
        return h.substring(0, 4) + "****" + h.substring(h.length - 4)
    }
}
