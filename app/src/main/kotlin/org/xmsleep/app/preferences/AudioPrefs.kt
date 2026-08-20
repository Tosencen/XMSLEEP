package org.xmsleep.app.preferences

import android.content.Context
import android.util.Base64
import org.xmsleep.app.Constants
import org.xmsleep.app.utils.Logger

/**
 * 音频播放偏好设置（本地音频、远程音频、收藏、最近播放、音量）
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object AudioPrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    // === 远程音频收藏/置顶 ===

    fun saveRemoteFavorites(context: Context, soundIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.REMOTE_FAVORITES, soundIds).apply()
    }

    fun getRemoteFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.REMOTE_FAVORITES, emptySet()) ?: emptySet()
    }

    fun saveRemotePinned(context: Context, soundIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.REMOTE_PINNED, soundIds).apply()
    }

    fun getRemotePinned(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.REMOTE_PINNED, emptySet()) ?: emptySet()
    }

    // === 本地音频 ===

    fun saveLocalAudioFavorites(context: Context, uris: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.LOCAL_AUDIO_FAVORITES, uris).apply()
    }

    fun getLocalAudioFavorites(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.LOCAL_AUDIO_FAVORITES, emptySet()) ?: emptySet()
    }

    fun saveLocalAudioPlayMode(context: Context, mode: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.LOCAL_AUDIO_PLAY_MODE, mode).apply()
    }

    fun getLocalAudioPlayMode(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.LOCAL_AUDIO_PLAY_MODE, "SEQUENTIAL") ?: "SEQUENTIAL"
    }

    fun saveLocalAudioFilterFolder(context: Context, folderPath: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.LOCAL_AUDIO_FILTER_FOLDER, folderPath).apply()
    }

    fun getLocalAudioFilterFolder(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.LOCAL_AUDIO_FILTER_FOLDER, "") ?: ""
    }

    fun saveLocalAudioFilterDuration(context: Context, filter: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.LOCAL_AUDIO_FILTER_DURATION, filter).apply()
    }

    fun getLocalAudioFilterDuration(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.LOCAL_AUDIO_FILTER_DURATION, "ALL") ?: "ALL"
    }

    fun saveLocalAudioSort(context: Context, sort: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.LOCAL_AUDIO_SORT, sort).apply()
    }

    fun getLocalAudioSort(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(Constants.PrefsKeys.LOCAL_AUDIO_SORT, "DATE_DESC") ?: "DATE_DESC"
    }

    fun saveLocalAudioEnabledFolders(context: Context, folders: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.LOCAL_AUDIO_ENABLED_FOLDERS, folders).apply()
    }

    fun getLocalAudioEnabledFolders(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.LOCAL_AUDIO_ENABLED_FOLDERS, emptySet()) ?: emptySet()
    }

    fun saveLocalAudioHiddenFolders(context: Context, folders: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.LOCAL_AUDIO_HIDDEN_FOLDERS, folders).apply()
    }

    fun getLocalAudioHiddenFolders(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.LOCAL_AUDIO_HIDDEN_FOLDERS, emptySet()) ?: emptySet()
    }

    fun saveLocalAudioPosition(context: Context, audioId: Long, positionMs: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("${Constants.PrefsKeys.LOCAL_AUDIO_POSITION}_$audioId", positionMs).apply()
    }

    fun getLocalAudioPosition(context: Context, audioId: Long): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("${Constants.PrefsKeys.LOCAL_AUDIO_POSITION}_$audioId", 0)
    }

    // === 最近播放 ===

    fun saveRecentLocalSounds(context: Context, sounds: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.RECENT_LOCAL_SOUNDS, sounds.toSet()).apply()
    }

    fun getRecentLocalSounds(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getStringSet(Constants.PrefsKeys.RECENT_LOCAL_SOUNDS, emptySet()) ?: emptySet()).toList()
    }

    fun saveRecentRemoteSounds(context: Context, soundIds: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.RECENT_REMOTE_SOUNDS, soundIds.toSet()).apply()
    }

    fun getRecentRemoteSounds(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return (prefs.getStringSet(Constants.PrefsKeys.RECENT_REMOTE_SOUNDS, emptySet()) ?: emptySet()).toList()
    }

    /** 保存最近播放的本地音频文件列表（包含 URI 映射），Base64 编码 URI 以避免特殊字符 */
    fun saveRecentLocalAudioFiles(context: Context, audioUriMap: Map<Long, String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = audioUriMap.entries.joinToString(";") { entry ->
            val encodedUri = Base64.encodeToString(entry.value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            "${entry.key}:$encodedUri"
        }
        prefs.edit().putString(Constants.PrefsKeys.RECENT_LOCAL_AUDIO_FILES, jsonString).apply()
    }

    fun getRecentLocalAudioFiles(context: Context): Map<Long, String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(Constants.PrefsKeys.RECENT_LOCAL_AUDIO_FILES, "") ?: ""
        if (jsonString.isEmpty()) return emptyMap()

        return try {
            jsonString.split(";")
                .mapNotNull { entry ->
                    val parts = entry.split(":", limit = 2)
                    if (parts.size == 2) {
                        val audioId = parts[0].toLongOrNull()
                        val encodedUri = parts[1]
                        val uri = String(Base64.decode(encodedUri, Base64.NO_WRAP), Charsets.UTF_8)
                        if (audioId != null) audioId to uri else null
                    } else null
                }
                .toMap()
        } catch (e: Exception) {
            Logger.e("AudioPrefs", "解析最近播放的本地音频文件失败: ${e.message}")
            emptyMap()
        }
    }

    // === 音量 ===

    fun saveLocalSoundVolume(context: Context, soundName: String, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("${Constants.PrefsKeys.VOLUME_PREFIX}local_$soundName", volume).apply()
    }

    fun getLocalSoundVolume(context: Context, soundName: String, default: Float = 0.5f): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("${Constants.PrefsKeys.VOLUME_PREFIX}local_$soundName", default)
    }

    fun saveRemoteSoundVolume(context: Context, soundId: String, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("${Constants.PrefsKeys.VOLUME_PREFIX}remote_$soundId", volume).apply()
    }

    fun getRemoteSoundVolume(context: Context, soundId: String, default: Float = 0.5f): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("${Constants.PrefsKeys.VOLUME_PREFIX}remote_$soundId", default)
    }

    fun saveLocalAudioVolume(context: Context, audioId: Long, volume: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putFloat("${Constants.PrefsKeys.VOLUME_PREFIX}audio_$audioId", volume).apply()
    }

    fun getLocalAudioVolume(context: Context, audioId: Long, default: Float = 0.5f): Float {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getFloat("${Constants.PrefsKeys.VOLUME_PREFIX}audio_$audioId", default)
    }
}
