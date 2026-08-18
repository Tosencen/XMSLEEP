package org.xmsleep.app.preferences

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.xmsleep.app.audio.BilibiliApi
import org.xmsleep.app.theme.DarkModeOption
import org.xmsleep.app.ui.BackgroundSelection
import kotlinx.coroutines.flow.StateFlow

/**
 * 应用偏好设置管理器（委托层）
 *
 * 已按域拆分到以下对象，新代码请直接使用对应对象：
 *  - [ThemePrefs]      主题与外观（深色模式、主题色、列数、动画等）
 *  - [PresetPrefs]     预设（声音组合）
 *  - [AudioPrefs]      音频播放（本地/远程音频、收藏、最近播放、音量）
 *  - [TimerPrefs]      定时器与番茄时钟
 *  - [BackgroundPrefs] 背景动画
 *  - [RadioPrefs]      电台与 Bilibili
 *  - [UiPrefs]         悬浮按钮、页面开关、小组件、设备标识、迁移
 *
 * 本类仅保留委托方法以兼容历史调用点，方法签名与之前完全一致。
 */
object PreferencesManager {

    // ==================== 预设 ====================

    /** 预设条目（兼容旧引用，实际定义在 PresetPrefs） */
    val allPresetRemotePinned: StateFlow<Set<String>>
        get() = PresetPrefs.allPresetRemotePinned

    fun initialize(context: Context) = PresetPrefs.initialize(context)

    fun getPresetList(context: Context): List<PresetPrefs.PresetEntry> = PresetPrefs.getPresetList(context)

    fun savePresetList(context: Context, entries: List<PresetPrefs.PresetEntry>) = PresetPrefs.savePresetList(context, entries)

    fun addPreset(context: Context, name: String): Int = PresetPrefs.addPreset(context, name)

    fun removePreset(context: Context, id: Int) = PresetPrefs.removePreset(context, id)

    fun renamePreset(context: Context, id: Int, name: String) = PresetPrefs.renamePreset(context, id, name)

    fun savePresetLocalPinned(context: Context, presetId: Int, soundNames: Set<String>) =
        PresetPrefs.savePresetLocalPinned(context, presetId, soundNames)

    fun getPresetLocalPinned(context: Context, presetId: Int): Set<String> =
        PresetPrefs.getPresetLocalPinned(context, presetId)

    fun savePresetRemotePinned(context: Context, presetId: Int, soundIds: Set<String>) =
        PresetPrefs.savePresetRemotePinned(context, presetId, soundIds)

    fun getPresetRemotePinned(context: Context, presetId: Int): Set<String> =
        PresetPrefs.getPresetRemotePinned(context, presetId)

    fun getAllPresetRemotePinned(context: Context): Set<String> =
        PresetPrefs.getAllPresetRemotePinned(context)

    fun saveActivePreset(context: Context, presetId: Int) = PresetPrefs.saveActivePreset(context, presetId)

    fun getActivePreset(context: Context): Int = PresetPrefs.getActivePreset(context)

    // ==================== 主题与外观 ====================

    fun saveDarkMode(context: Context, darkMode: DarkModeOption) = ThemePrefs.saveDarkMode(context, darkMode)

    fun getDarkMode(context: Context, default: DarkModeOption = DarkModeOption.DARK): DarkModeOption =
        ThemePrefs.getDarkMode(context, default)

    fun saveSelectedColor(context: Context, color: Color) = ThemePrefs.saveSelectedColor(context, color)

    fun getSelectedColor(context: Context, default: Color): Color = ThemePrefs.getSelectedColor(context, default)

    fun saveUseDynamicColor(context: Context, useDynamicColor: Boolean) =
        ThemePrefs.saveUseDynamicColor(context, useDynamicColor)

    fun getUseDynamicColor(context: Context, default: Boolean = false): Boolean =
        ThemePrefs.getUseDynamicColor(context, default)

    fun saveUseBlackBackground(context: Context, useBlackBackground: Boolean) =
        ThemePrefs.saveUseBlackBackground(context, useBlackBackground)

    fun getUseBlackBackground(context: Context, default: Boolean = false): Boolean =
        ThemePrefs.getUseBlackBackground(context, default)

    fun saveUseMonochrome(context: Context, useMonochrome: Boolean) =
        ThemePrefs.saveUseMonochrome(context, useMonochrome)

    fun getUseMonochrome(context: Context, default: Boolean = false): Boolean =
        ThemePrefs.getUseMonochrome(context, default)

    fun saveHideAnimation(context: Context, hideAnimation: Boolean) =
        ThemePrefs.saveHideAnimation(context, hideAnimation)

    fun getHideAnimation(context: Context, default: Boolean = false): Boolean =
        ThemePrefs.getHideAnimation(context, default)

    fun saveFlipClockSensorRotation(context: Context, enabled: Boolean) =
        ThemePrefs.saveFlipClockSensorRotation(context, enabled)

    fun getFlipClockSensorRotation(context: Context, default: Boolean = true): Boolean =
        ThemePrefs.getFlipClockSensorRotation(context, default)

    fun saveSoundCardsColumnsCount(context: Context, columnsCount: Int) =
        ThemePrefs.saveSoundCardsColumnsCount(context, columnsCount)

    fun getSoundCardsColumnsCount(context: Context, default: Int = 2): Int =
        ThemePrefs.getSoundCardsColumnsCount(context, default)

    fun saveStarSkyColumnsCount(context: Context, columnsCount: Int) =
        ThemePrefs.saveStarSkyColumnsCount(context, columnsCount)

    fun getStarSkyColumnsCount(context: Context, default: Int = 3): Int =
        ThemePrefs.getStarSkyColumnsCount(context, default)

    fun saveQuickPlayExpanded(context: Context, isExpanded: Boolean) =
        ThemePrefs.saveQuickPlayExpanded(context, isExpanded)

    fun getQuickPlayExpanded(context: Context, default: Boolean = true): Boolean =
        ThemePrefs.getQuickPlayExpanded(context, default)

    fun saveNowPlayingExpanded(context: Context, isExpanded: Boolean) =
        ThemePrefs.saveNowPlayingExpanded(context, isExpanded)

    fun getNowPlayingExpanded(context: Context, default: Boolean = true): Boolean =
        ThemePrefs.getNowPlayingExpanded(context, default)

    // ==================== 音频播放 ====================

    fun saveRemoteFavorites(context: Context, soundIds: Set<String>) = AudioPrefs.saveRemoteFavorites(context, soundIds)

    fun getRemoteFavorites(context: Context): Set<String> = AudioPrefs.getRemoteFavorites(context)

    fun saveRemotePinned(context: Context, soundIds: Set<String>) = AudioPrefs.saveRemotePinned(context, soundIds)

    fun getRemotePinned(context: Context): Set<String> = AudioPrefs.getRemotePinned(context)

    fun saveLocalAudioFavorites(context: Context, uris: Set<String>) =
        AudioPrefs.saveLocalAudioFavorites(context, uris)

    fun getLocalAudioFavorites(context: Context): Set<String> = AudioPrefs.getLocalAudioFavorites(context)

    fun saveLocalAudioPlayMode(context: Context, mode: String) = AudioPrefs.saveLocalAudioPlayMode(context, mode)

    fun getLocalAudioPlayMode(context: Context): String = AudioPrefs.getLocalAudioPlayMode(context)

    fun saveLocalAudioFilterFolder(context: Context, folderPath: String) =
        AudioPrefs.saveLocalAudioFilterFolder(context, folderPath)

    fun getLocalAudioFilterFolder(context: Context): String = AudioPrefs.getLocalAudioFilterFolder(context)

    fun saveLocalAudioFilterDuration(context: Context, filter: String) =
        AudioPrefs.saveLocalAudioFilterDuration(context, filter)

    fun getLocalAudioFilterDuration(context: Context): String = AudioPrefs.getLocalAudioFilterDuration(context)

    fun saveLocalAudioSort(context: Context, sort: String) = AudioPrefs.saveLocalAudioSort(context, sort)

    fun getLocalAudioSort(context: Context): String = AudioPrefs.getLocalAudioSort(context)

    fun saveLocalAudioEnabledFolders(context: Context, folders: Set<String>) =
        AudioPrefs.saveLocalAudioEnabledFolders(context, folders)

    fun getLocalAudioEnabledFolders(context: Context): Set<String> = AudioPrefs.getLocalAudioEnabledFolders(context)

    fun saveLocalAudioPosition(context: Context, audioId: Long, positionMs: Int) =
        AudioPrefs.saveLocalAudioPosition(context, audioId, positionMs)

    fun getLocalAudioPosition(context: Context, audioId: Long): Int =
        AudioPrefs.getLocalAudioPosition(context, audioId)

    fun saveRecentLocalSounds(context: Context, sounds: List<String>) =
        AudioPrefs.saveRecentLocalSounds(context, sounds)

    fun getRecentLocalSounds(context: Context): List<String> = AudioPrefs.getRecentLocalSounds(context)

    fun saveRecentRemoteSounds(context: Context, soundIds: List<String>) =
        AudioPrefs.saveRecentRemoteSounds(context, soundIds)

    fun getRecentRemoteSounds(context: Context): List<String> = AudioPrefs.getRecentRemoteSounds(context)

    fun saveRecentLocalAudioFiles(context: Context, audioUriMap: Map<Long, String>) =
        AudioPrefs.saveRecentLocalAudioFiles(context, audioUriMap)

    fun getRecentLocalAudioFiles(context: Context): Map<Long, String> =
        AudioPrefs.getRecentLocalAudioFiles(context)

    fun saveLocalSoundVolume(context: Context, soundName: String, volume: Float) =
        AudioPrefs.saveLocalSoundVolume(context, soundName, volume)

    fun getLocalSoundVolume(context: Context, soundName: String, default: Float = 0.5f): Float =
        AudioPrefs.getLocalSoundVolume(context, soundName, default)

    fun saveRemoteSoundVolume(context: Context, soundId: String, volume: Float) =
        AudioPrefs.saveRemoteSoundVolume(context, soundId, volume)

    fun getRemoteSoundVolume(context: Context, soundId: String, default: Float = 0.5f): Float =
        AudioPrefs.getRemoteSoundVolume(context, soundId, default)

    fun saveLocalAudioVolume(context: Context, audioId: Long, volume: Float) =
        AudioPrefs.saveLocalAudioVolume(context, audioId, volume)

    fun getLocalAudioVolume(context: Context, audioId: Long, default: Float = 0.5f): Float =
        AudioPrefs.getLocalAudioVolume(context, audioId, default)

    // ==================== 定时器与番茄时钟 ====================

    fun saveAutoCountdownMinutes(context: Context, minutes: Int) =
        TimerPrefs.saveAutoCountdownMinutes(context, minutes)

    fun getAutoCountdownMinutes(context: Context): Int = TimerPrefs.getAutoCountdownMinutes(context)

    fun saveLastTimerMinutes(context: Context, minutes: Int) = TimerPrefs.saveLastTimerMinutes(context, minutes)

    fun getLastTimerMinutes(context: Context): Int = TimerPrefs.getLastTimerMinutes(context)

    fun saveKeepScreenOn(context: Context, keepScreenOn: Boolean) = TimerPrefs.saveKeepScreenOn(context, keepScreenOn)

    fun getKeepScreenOn(context: Context): Boolean = TimerPrefs.getKeepScreenOn(context)

    fun saveTomatoRingtone(context: Context, ringtoneId: String) = TimerPrefs.saveTomatoRingtone(context, ringtoneId)

    fun getTomatoRingtone(context: Context): String = TimerPrefs.getTomatoRingtone(context)

    fun saveTomatoPulseAnimation(context: Context, enabled: Boolean) =
        TimerPrefs.saveTomatoPulseAnimation(context, enabled)

    fun getTomatoPulseAnimation(context: Context): Boolean = TimerPrefs.getTomatoPulseAnimation(context)

    fun saveTomatoVibrate(context: Context, enabled: Boolean) = TimerPrefs.saveTomatoVibrate(context, enabled)

    fun getTomatoVibrate(context: Context): Boolean = TimerPrefs.getTomatoVibrate(context)

    fun saveShowRecentPlayDialog(context: Context, show: Boolean) =
        TimerPrefs.saveShowRecentPlayDialog(context, show)

    fun getShowRecentPlayDialog(context: Context): Boolean = TimerPrefs.getShowRecentPlayDialog(context)

    fun setAutoPlayOnStart(context: Context, enabled: Boolean) = TimerPrefs.setAutoPlayOnStart(context, enabled)

    fun getAutoPlayOnStart(context: Context): Boolean = TimerPrefs.getAutoPlayOnStart(context)

    // ==================== 背景动画 ====================

    fun saveBackgroundSelection(context: Context, selection: BackgroundSelection) =
        BackgroundPrefs.saveBackgroundSelection(context, selection)

    fun getBackgroundSelection(context: Context): BackgroundSelection = BackgroundPrefs.getBackgroundSelection(context)

    fun saveCustomBackgroundUri(context: Context, uri: String) = BackgroundPrefs.saveCustomBackgroundUri(context, uri)

    fun getCustomBackgroundUri(context: Context): String? = BackgroundPrefs.getCustomBackgroundUri(context)

    fun saveCustomBackgroundThumbnail(context: Context, uri: String) =
        BackgroundPrefs.saveCustomBackgroundThumbnail(context, uri)

    fun getCustomBackgroundThumbnail(context: Context): String? = BackgroundPrefs.getCustomBackgroundThumbnail(context)

    fun saveCustomBackgroundColor(context: Context, color: Color) =
        BackgroundPrefs.saveCustomBackgroundColor(context, color)

    fun getCustomBackgroundColor(context: Context, default: Color): Color =
        BackgroundPrefs.getCustomBackgroundColor(context, default)

    fun saveBackgroundOpacity(context: Context, opacity: Float) = BackgroundPrefs.saveBackgroundOpacity(context, opacity)

    fun getBackgroundOpacity(context: Context): Float = BackgroundPrefs.getBackgroundOpacity(context)

    fun saveBackgroundBlurRadius(context: Context, radius: Float) =
        BackgroundPrefs.saveBackgroundBlurRadius(context, radius)

    fun getBackgroundBlurRadius(context: Context): Float = BackgroundPrefs.getBackgroundBlurRadius(context)

    // ==================== 电台与 Bilibili ====================

    fun saveRadioStationId(context: Context, stationId: String) = RadioPrefs.saveRadioStationId(context, stationId)

    fun getRadioStationId(context: Context): String? = RadioPrefs.getRadioStationId(context)

    fun saveRadioVolume(context: Context, volume: Float) = RadioPrefs.saveRadioVolume(context, volume)

    fun getRadioVolume(context: Context): Float = RadioPrefs.getRadioVolume(context)

    fun saveLottieAnimation(context: Context, fileName: String) = RadioPrefs.saveLottieAnimation(context, fileName)

    fun getLottieAnimation(context: Context): String = RadioPrefs.getLottieAnimation(context)

    fun saveBilibiliPinnedRooms(context: Context, roomIds: Set<String>) =
        RadioPrefs.saveBilibiliPinnedRooms(context, roomIds)

    fun getBilibiliPinnedRooms(context: Context): Set<String> = RadioPrefs.getBilibiliPinnedRooms(context)

    fun saveBilibiliPinnedRoomsInfo(context: Context, rooms: List<BilibiliApi.LiveRoom>) =
        RadioPrefs.saveBilibiliPinnedRoomsInfo(context, rooms)

    fun getBilibiliPinnedRoomsInfo(context: Context): List<BilibiliApi.LiveRoom> =
        RadioPrefs.getBilibiliPinnedRoomsInfo(context)

    fun saveRadioFloatingButtonPosition(context: Context, y: Float, isLeft: Boolean) =
        RadioPrefs.saveRadioFloatingButtonPosition(context, y, isLeft)

    fun getRadioFloatingButtonY(context: Context): Float = RadioPrefs.getRadioFloatingButtonY(context)

    fun getRadioFloatingButtonIsLeft(context: Context): Boolean = RadioPrefs.getRadioFloatingButtonIsLeft(context)

    // ==================== UI 与通用 ====================

    fun saveFloatingButtonPosition(context: Context, x: Float, y: Float, isLeft: Boolean) =
        UiPrefs.saveFloatingButtonPosition(context, x, y, isLeft)

    fun saveFloatingButtonPosition(context: Context, y: Float, isLeft: Boolean) =
        UiPrefs.saveFloatingButtonPosition(context, y, isLeft)

    fun getFloatingButtonPosition(context: Context, defaultX: Float, defaultY: Float, defaultIsLeft: Boolean): Triple<Float, Float, Boolean> =
        UiPrefs.getFloatingButtonPosition(context, defaultX, defaultY, defaultIsLeft)

    fun getFloatingButtonY(context: Context): Float = UiPrefs.getFloatingButtonY(context)

    fun getFloatingButtonIsLeft(context: Context): Boolean = UiPrefs.getFloatingButtonIsLeft(context)

    fun saveFloatingButtonExpanded(context: Context, isExpanded: Boolean) =
        UiPrefs.saveFloatingButtonExpanded(context, isExpanded)

    fun getFloatingButtonExpanded(context: Context, default: Boolean = false): Boolean =
        UiPrefs.getFloatingButtonExpanded(context, default)

    fun setShowRadioTab(context: Context, show: Boolean) = UiPrefs.setShowRadioTab(context, show)

    fun getShowRadioTab(context: Context): Boolean = UiPrefs.getShowRadioTab(context)

    fun setShowBreathingTab(context: Context, show: Boolean) = UiPrefs.setShowBreathingTab(context, show)

    fun getShowBreathingTab(context: Context): Boolean = UiPrefs.getShowBreathingTab(context)

    fun saveQuoteWidgetAdded(context: Context, added: Boolean) = UiPrefs.saveQuoteWidgetAdded(context, added)

    fun isQuoteWidgetAdded(context: Context): Boolean = UiPrefs.isQuoteWidgetAdded(context)

    fun getAnonymousDeviceId(context: Context): String = UiPrefs.getAnonymousDeviceId(context)

    fun resetAnonymousDeviceId(context: Context) = UiPrefs.resetAnonymousDeviceId(context)

    fun migrateFromOldVersion(context: Context) = UiPrefs.migrateFromOldVersion(context)
}
