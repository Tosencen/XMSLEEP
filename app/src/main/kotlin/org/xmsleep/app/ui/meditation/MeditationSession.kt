package org.xmsleep.app.ui.meditation

data class MeditationSession(
    val id: String,
    val name: String,
    val nameEn: String = "",
    val nameJa: String = "",
    val nameKo: String = "",
    val nameRu: String = "",
    val nameZhTW: String = "",
    val category: String,
    val duration: Int,
    val audioUrl: String,
    val coverUrl: String = "",
    val sourceId: String = "",
    val sourceTitle: String = ""
) {
    fun getLocalizedName(languageCode: String): String {
        return when (languageCode) {
            "zh" -> name
            "zh-TW" -> nameZhTW.ifEmpty { name }
            "en" -> nameEn.ifEmpty { name }
            "ja" -> nameJa.ifEmpty { name }
            "ko" -> nameKo.ifEmpty { name }
            "ru" -> nameRu.ifEmpty { name }
            else -> name
        }
    }

    val durationText: String
        get() {
            val mins = duration / 60
            val secs = duration % 60
            return if (secs > 0) "${mins}:${String.format("%02d", secs)}" else "${mins}:00"
        }
}

data class MeditationCategory(
    val id: String,
    val name: String,
    val nameEn: String = "",
    val nameJa: String = "",
    val nameKo: String = "",
    val nameRu: String = "",
    val nameZhTW: String = "",
    val order: Int = 0
) {
    fun getLocalizedName(languageCode: String): String {
        return when (languageCode) {
            "zh" -> name
            "zh-TW" -> nameZhTW.ifEmpty { name }
            "en" -> nameEn.ifEmpty { name }
            "ja" -> nameJa.ifEmpty { name }
            "ko" -> nameKo.ifEmpty { name }
            "ru" -> nameRu.ifEmpty { name }
            else -> name
        }
    }
}

data class MeditationManifest(
    val version: String,
    val categories: List<MeditationCategory>,
    val sessions: List<MeditationSession>
)
