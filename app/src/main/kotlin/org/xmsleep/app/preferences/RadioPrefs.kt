package org.xmsleep.app.preferences

import android.content.Context
import org.xmsleep.app.Constants
import org.xmsleep.app.audio.BilibiliApi

/**
 * 电台与 Bilibili 偏好设置
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object RadioPrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    fun saveRadioStationId(context: Context, stationId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.RADIO_STATION_ID, stationId).apply()
    }

    fun getRadioStationId(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.RADIO_STATION_ID, null)
    }

    fun saveRadioVolume(context: Context, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat(Constants.PrefsKeys.RADIO_VOLUME, volume).apply()
    }

    fun getRadioVolume(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(Constants.PrefsKeys.RADIO_VOLUME, 0.5f)
    }

    fun saveLottieAnimation(context: Context, fileName: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.RADIO_LOTTIE_FILE, fileName).apply()
    }

    fun getLottieAnimation(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.RADIO_LOTTIE_FILE, "dq.lottie") ?: "dq.lottie"
    }

    fun saveBilibiliPinnedRooms(context: Context, roomIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.BILIBILI_PINNED_ROOMS, roomIds).apply()
    }

    fun getBilibiliPinnedRooms(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.BILIBILI_PINNED_ROOMS, setOf("25248835", "31868497"))
            ?: setOf("25248835", "31868497")
    }

    fun saveBilibiliPinnedRoomsInfo(context: Context, rooms: List<BilibiliApi.LiveRoom>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonArray = org.json.JSONArray()
        for (room in rooms) {
            val obj = org.json.JSONObject()
            obj.put("roomId", room.roomId)
            obj.put("title", room.title)
            obj.put("userName", room.userName)
            obj.put("online", room.online)
            obj.put("cateName", room.cateName)
            jsonArray.put(obj)
        }
        prefs.edit().putString(Constants.PrefsKeys.BILIBILI_PINNED_ROOMS_INFO, jsonArray.toString()).apply()
    }

    fun getBilibiliPinnedRoomsInfo(context: Context): List<BilibiliApi.LiveRoom> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(Constants.PrefsKeys.BILIBILI_PINNED_ROOMS_INFO, null) ?: return emptyList()
        val jsonArray = org.json.JSONArray(json)
        val rooms = mutableListOf<BilibiliApi.LiveRoom>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            rooms.add(BilibiliApi.LiveRoom(
                roomId = obj.getString("roomId"),
                title = obj.getString("title"),
                userName = obj.getString("userName"),
                online = obj.getInt("online"),
                cateName = obj.optString("cateName", "")
            ))
        }
        return rooms
    }

    fun saveRadioFloatingButtonPosition(context: Context, y: Float, isLeft: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(Constants.PrefsKeys.RADIO_FLOATING_BUTTON_Y, y)
            .putBoolean(Constants.PrefsKeys.RADIO_FLOATING_BUTTON_IS_LEFT, isLeft)
            .apply()
    }

    fun getRadioFloatingButtonY(context: Context): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat(Constants.PrefsKeys.RADIO_FLOATING_BUTTON_Y, -1f)
    }

    fun getRadioFloatingButtonIsLeft(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(Constants.PrefsKeys.RADIO_FLOATING_BUTTON_IS_LEFT, true)
    }
}
