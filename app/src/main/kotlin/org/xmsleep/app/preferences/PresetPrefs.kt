package org.xmsleep.app.preferences

import android.content.Context
import org.xmsleep.app.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 预设（声音组合）偏好设置
 * 从 PreferencesManager 按域拆分，新代码请直接使用本对象。
 */
object PresetPrefs {
    private val PREFS_NAME = Constants.PrefsKeys.PREFS_NAME

    data class PresetEntry(val id: Int, val name: String)

    private val _allPresetRemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val allPresetRemotePinned: StateFlow<Set<String>> = _allPresetRemotePinned.asStateFlow()

    fun refreshAllPresetRemotePinned(context: Context) {
        _allPresetRemotePinned.value = getAllPresetRemotePinned(context)
    }

    private fun migratePresets(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(Constants.PrefsKeys.PRESET_LIST)) {
            ensureMinimumPresets(prefs, 3)
            return
        }
        // 读取旧数据
        val local1 = prefs.getStringSet(Constants.PrefsKeys.PRESET1_LOCAL_PINNED, emptySet()) ?: emptySet()
        val local2 = prefs.getStringSet(Constants.PrefsKeys.PRESET2_LOCAL_PINNED, emptySet()) ?: emptySet()
        val local3 = prefs.getStringSet(Constants.PrefsKeys.PRESET3_LOCAL_PINNED, emptySet()) ?: emptySet()
        val remote1 = prefs.getStringSet(Constants.PrefsKeys.PRESET1_REMOTE_PINNED, emptySet()) ?: emptySet()
        val remote2 = prefs.getStringSet(Constants.PrefsKeys.PRESET2_REMOTE_PINNED, emptySet()) ?: emptySet()
        val remote3 = prefs.getStringSet(Constants.PrefsKeys.PRESET3_REMOTE_PINNED, emptySet()) ?: emptySet()
        val hasOldData = local1.isNotEmpty() || local2.isNotEmpty() || local3.isNotEmpty() ||
                remote1.isNotEmpty() || remote2.isNotEmpty() || remote3.isNotEmpty()
        if (!hasOldData) {
            createDefaultPresets(prefs)
            return
        }
        // 迁移旧数据
        val names = listOf("预设1", "预设2", "预设3")
        val locals = listOf(local1, local2, local3)
        val remotes = listOf(remote1, remote2, remote3)
        val ids = mutableListOf<Int>()
        for (i in 0..2) {
            if (locals[i].isNotEmpty() || remotes[i].isNotEmpty()) {
                val id = i + 1
                ids.add(id)
                prefs.edit()
                    .putString(Constants.PrefsKeys.PRESET_NAME_PREFIX + id, names[i])
                    .putStringSet(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + id, locals[i])
                    .putStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + id, remotes[i])
                    .apply()
            }
        }
        prefs.edit().putString(Constants.PrefsKeys.PRESET_LIST, ids.joinToString(",")).apply()
        ensureMinimumPresets(prefs, 3)
    }

    private fun ensureMinimumPresets(prefs: android.content.SharedPreferences, minCount: Int) {
        val listStr = prefs.getString(Constants.PrefsKeys.PRESET_LIST, null)
        if (listStr.isNullOrBlank()) {
            createDefaultPresets(prefs)
            return
        }
        val ids = listStr.split(",").mapNotNull { it.toIntOrNull() }.toMutableSet()
        var changed = false
        for (i in 1..minCount) {
            if (!ids.contains(i)) {
                ids.add(i)
                prefs.edit()
                    .putString(Constants.PrefsKeys.PRESET_NAME_PREFIX + i, "预设$i")
                    .putStringSet(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + i, emptySet())
                    .putStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + i, emptySet())
                    .apply()
                changed = true
            }
        }
        if (changed) {
            prefs.edit().putString(Constants.PrefsKeys.PRESET_LIST, ids.sorted().joinToString(",")).apply()
        }
    }

    private fun createDefaultPresets(prefs: android.content.SharedPreferences) {
        val names = listOf("预设1", "预设2", "预设3")
        for (i in 0..2) {
            val id = i + 1
            prefs.edit()
                .putString(Constants.PrefsKeys.PRESET_NAME_PREFIX + id, names[i])
                .putStringSet(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + id, emptySet())
                .putStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + id, emptySet())
                .apply()
        }
        prefs.edit().putString(Constants.PrefsKeys.PRESET_LIST, "1,2,3").apply()
    }

    /** 应用启动时调用：迁移旧数据并初始化置顶状态 */
    fun initialize(context: Context) {
        migratePresets(context)
        refreshAllPresetRemotePinned(context)
    }

    fun getPresetList(context: Context): List<PresetEntry> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val listStr = prefs.getString(Constants.PrefsKeys.PRESET_LIST, null)
        if (listStr.isNullOrBlank()) return emptyList()
        return listStr.split(",").mapNotNull { idStr ->
            val id = idStr.toIntOrNull() ?: return@mapNotNull null
            val name = prefs.getString(Constants.PrefsKeys.PRESET_NAME_PREFIX + id, "预设$id") ?: "预设$id"
            PresetEntry(id, name)
        }
    }

    fun savePresetList(context: Context, entries: List<PresetEntry>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        edit.putString(Constants.PrefsKeys.PRESET_LIST, entries.joinToString(",") { it.id.toString() })
        entries.forEach { entry ->
            edit.putString(Constants.PrefsKeys.PRESET_NAME_PREFIX + entry.id, entry.name)
        }
        edit.apply()
    }

    fun addPreset(context: Context, name: String): Int {
        val entries = getPresetList(context).toMutableList()
        val maxId = entries.maxOfOrNull { it.id } ?: 0
        val newId = maxId + 1
        entries.add(PresetEntry(newId, name))
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(Constants.PrefsKeys.PRESET_LIST, entries.joinToString(",") { it.id.toString() })
            .putString(Constants.PrefsKeys.PRESET_NAME_PREFIX + newId, name)
            .putStringSet(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + newId, emptySet())
            .putStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + newId, emptySet())
            .apply()
        return newId
    }

    fun removePreset(context: Context, id: Int) {
        val entries = getPresetList(context).filter { it.id != id }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()
        edit.putString(Constants.PrefsKeys.PRESET_LIST, entries.joinToString(",") { it.id.toString() })
        edit.remove(Constants.PrefsKeys.PRESET_NAME_PREFIX + id)
        edit.remove(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + id)
        edit.remove(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + id)
        edit.apply()
        refreshAllPresetRemotePinned(context)
    }

    fun renamePreset(context: Context, id: Int, name: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(Constants.PrefsKeys.PRESET_NAME_PREFIX + id, name).apply()
    }

    fun savePresetLocalPinned(context: Context, presetId: Int, soundNames: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + presetId, soundNames).apply()
    }

    fun getPresetLocalPinned(context: Context, presetId: Int): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.PRESET_LOCAL_PREFIX + presetId, emptySet()) ?: emptySet()
    }

    fun savePresetRemotePinned(context: Context, presetId: Int, soundIds: Set<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + presetId, soundIds).apply()
        refreshAllPresetRemotePinned(context)
    }

    fun getPresetRemotePinned(context: Context, presetId: Int): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + presetId, emptySet()) ?: emptySet()
    }

    fun getAllPresetRemotePinned(context: Context): Set<String> {
        val entries = getPresetList(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val result = mutableSetOf<String>()
        entries.forEach { entry ->
            result.addAll(prefs.getStringSet(Constants.PrefsKeys.PRESET_REMOTE_PREFIX + entry.id, emptySet()) ?: emptySet())
        }
        return result
    }

    fun saveActivePreset(context: Context, presetId: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(Constants.PrefsKeys.ACTIVE_PRESET, presetId).apply()
    }

    fun getActivePreset(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(Constants.PrefsKeys.ACTIVE_PRESET, 1)
    }
}
