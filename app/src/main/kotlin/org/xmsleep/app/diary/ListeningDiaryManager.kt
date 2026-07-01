package org.xmsleep.app.diary

import android.content.Context
import com.google.gson.Gson
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.utils.Logger
import java.time.LocalDate
import java.util.Calendar

data class DiaryEntry(
    val timestamp: Long,
    val date: String,
    val sounds: List<String>,
    val durationMinutes: Int,
    val period: String
)

data class DiaryHistory(
    val items: List<DiaryEntry>
)

object ListeningDiaryManager {

    private const val TAG = "ListeningDiaryManager"
    private const val PREFS_NAME = "listening_diary"
    private const val KEY_HISTORY = "diary_history"
    private const val MAX_ENTRIES = 500

    private val gson = Gson()
    private var context: Context? = null
    private var initialized = false
    private val displayNameToEnum = AudioManager.Sound.entries
        .filter { it.displayName.isNotEmpty() }
        .associateBy { it.displayName }

    fun init(ctx: Context) {
        if (initialized) return
        context = ctx.applicationContext
        TimerManager.getInstance().setBeforeFinishCallback { durationMinutes ->
            recordSession(durationMinutes)
        }
        initialized = true
        Logger.d(TAG, "ListeningDiaryManager initialized")
    }

    fun getDisplaySoundName(
        context: Context,
        sound: String,
        remoteSoundsCache: Map<String, SoundMetadata> = emptyMap()
    ): String {
        try {
            val soundEnum = AudioManager.Sound.valueOf(sound)
            val resId = context.resources.getIdentifier(
                "sound_${soundEnum.name.lowercase()}", "string", context.packageName
            )
            if (resId != 0) return context.getString(resId)
        } catch (_: IllegalArgumentException) {}

        displayNameToEnum[sound]?.let {
            val resId = context.resources.getIdentifier(
                "sound_${it.name.lowercase()}", "string", context.packageName
            )
            if (resId != 0) return context.getString(resId)
        }

        remoteSoundsCache[sound]?.let {
            val lang = LanguageManager.getCurrentLanguage(context)
            return it.getLocalizedName(lang)
        }

        return sound
    }

    private fun recordSession(durationMinutes: Int) {
        val ctx = context ?: return
        if (durationMinutes <= 0) return

        val audioManager = AudioManager.getInstance()
        val localSounds = audioManager.getPlayingSounds().map { it.name }
        val remoteIds = audioManager.getPlayingRemoteSoundIds()
        val allSounds = localSounds + remoteIds

        val entry = DiaryEntry(
            timestamp = System.currentTimeMillis(),
            date = LocalDate.now().toString(),
            sounds = allSounds,
            durationMinutes = durationMinutes,
            period = getCurrentPeriod()
        )
        saveEntry(ctx, entry)
        Logger.d(TAG, "Recorded diary: ${allSounds.size} sounds, ${durationMinutes}min")
    }

    private fun saveEntry(ctx: Context, entry: DiaryEntry) {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val history = getHistory(ctx).toMutableList()
        history.add(0, entry)
        if (history.size > MAX_ENTRIES) {
            history.subList(MAX_ENTRIES, history.size).clear()
        }
        prefs.edit().putString(KEY_HISTORY, gson.toJson(DiaryHistory(history))).apply()
    }

    fun getHistory(ctx: Context): List<DiaryEntry> {
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
        return try {
            gson.fromJson(json, DiaryHistory::class.java).items
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to load diary history: ${e.message}")
            emptyList()
        }
    }

    fun clearHistory(ctx: Context) {
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().remove(KEY_HISTORY).apply()
        Logger.d(TAG, "Diary history cleared")
    }

    fun getThisWeekEntries(ctx: Context): List<DiaryEntry> {
        val weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        return getHistory(ctx).filter { it.timestamp >= weekAgo }
    }

    fun generateWeeklySummaryRaw(
        ctx: Context,
        remoteSoundsCache: Map<String, SoundMetadata> = emptyMap()
    ): WeeklySummaryData? {
        val weekEntries = getThisWeekEntries(ctx)
        if (weekEntries.isEmpty()) return null

        val totalSessions = weekEntries.size
        val totalMinutes = weekEntries.sumOf { it.durationMinutes }
        val allSounds = weekEntries.flatMap { it.sounds }.filter { it.isNotBlank() }
        val topSound = allSounds.groupBy { it }.maxByOrNull { it.value.size }?.key
        val topSoundCount = allSounds.groupBy { it }.maxByOrNull { it.value.size }?.value?.size ?: 0
        val uniqueSoundCount = allSounds.distinct().size

        val localizedTopSound = topSound?.let {
            getDisplaySoundName(ctx, it, remoteSoundsCache)
        }

        val message = when {
            totalSessions >= 10 -> ctx.getString(
                org.xmsleep.app.R.string.diary_summary_high_freq,
                totalSessions, formatDuration(ctx, totalMinutes)
            )
            totalSessions >= 5 -> ctx.getString(
                org.xmsleep.app.R.string.diary_summary_medium_freq,
                totalSessions, formatDuration(ctx, totalMinutes)
            )
            else -> ctx.getString(
                org.xmsleep.app.R.string.diary_summary_low_freq,
                totalSessions, formatDuration(ctx, totalMinutes)
            )
        } + if (localizedTopSound != null) {
            ctx.getString(org.xmsleep.app.R.string.diary_summary_top_sound, localizedTopSound)
        } else ""

        return WeeklySummaryData(
            totalSessions = totalSessions,
            totalMinutes = totalMinutes,
            topSound = localizedTopSound,
            topSoundCount = topSoundCount,
            uniqueSoundCount = uniqueSoundCount,
            message = message
        )
    }

    private fun getCurrentPeriod(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 6..11 -> "morning"
            in 12..17 -> "afternoon"
            else -> "evening"
        }
    }

    private fun formatDuration(ctx: Context, minutes: Int): String {
        return if (minutes >= 60) {
            ctx.getString(org.xmsleep.app.R.string.diary_duration_hours, minutes / 60, minutes % 60)
        } else {
            ctx.getString(org.xmsleep.app.R.string.diary_duration_minutes, minutes)
        }
    }
}

data class WeeklySummaryData(
    val totalSessions: Int,
    val totalMinutes: Int,
    val topSound: String?,
    val topSoundCount: Int,
    val uniqueSoundCount: Int,
    val message: String
)
