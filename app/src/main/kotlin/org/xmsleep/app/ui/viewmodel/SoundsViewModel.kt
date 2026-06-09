package org.xmsleep.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.weather.WeatherSoundMapper
import org.xmsleep.app.weather.WeatherData
import javax.inject.Inject

/**
 * SoundsScreen 的 ViewModel
 * 负责管理声音页面的状态，消除轮询循环
 */
@HiltViewModel
class SoundsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val audioManager = AudioManager.getInstance()

    // 天气相关状态
    private val _weatherEnabled = MutableStateFlow(false)
    val weatherEnabled: StateFlow<Boolean> = _weatherEnabled.asStateFlow()

    private val _currentWeather = MutableStateFlow<WeatherData?>(null)
    val currentWeather: StateFlow<WeatherData?> = _currentWeather.asStateFlow()

    // 预设远程固定列表
    private val _presetRemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val presetRemotePinned: StateFlow<Set<String>> = _presetRemotePinned.asStateFlow()

    // 远程播放状态（自动从远程播放状态和预设固定列表推导，响应式更新）
    private val _playingRemoteSounds = combine(
        audioManager.remotePlayingStates,
        _presetRemotePinned
    ) { states, pinned ->
        states.filterKeys { pinned.contains(it) }.filterValues { it }.keys
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val playingRemoteSounds: StateFlow<Set<String>> = _playingRemoteSounds

    // 本地播放状态
    private val _playingStates = MutableStateFlow<Map<AudioManager.Sound, Boolean>>(emptyMap())
    val playingStates: StateFlow<Map<AudioManager.Sound, Boolean>> = _playingStates.asStateFlow()

    // 是否有任何声音正在播放
    private val _hasAnyPlayingSounds = MutableStateFlow(false)
    val hasAnyPlayingSounds: StateFlow<Boolean> = _hasAnyPlayingSounds.asStateFlow()

    // 当前预设的固定声音列表（由 SoundsScreen 更新）
    private val _pinnedSounds = MutableStateFlow<Set<AudioManager.Sound>>(emptySet())

    // 当前预设的远程声音列表（由 SoundsScreen 更新）
    private val _defaultRemoteSounds = MutableStateFlow<List<org.xmsleep.app.audio.model.SoundMetadata>>(emptyList())

    // 默认区域播放状态（响应式：自动从播放状态和预设列表中推导，随播放状态变化实时更新）
    private val _defaultAreaSoundsPlaying = combine(
        _pinnedSounds,
        _defaultRemoteSounds,
        _playingStates,
        _playingRemoteSounds
    ) { pinned, remote, localStates, remotePlayingIds ->
        val localPlaying = pinned.any { localStates[it] == true }
        val remotePlaying = remote.any { remotePlayingIds.contains(it.id) }
        localPlaying || remotePlaying
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val defaultAreaSoundsPlaying: StateFlow<Boolean> = _defaultAreaSoundsPlaying

    init {
        initializeWeatherState()
        observeAudioStates()
    }

    /**
     * 初始化天气状态
     */
    private fun initializeWeatherState() {
        _weatherEnabled.value = WeatherSoundMapper.isEnabled(context)
        
        // 加载缓存的天气数据
        val lastWeather = WeatherSoundMapper.getLastWeather(context)
        if (lastWeather != null) {
            _currentWeather.value = lastWeather
        }

        // 监听天气开关变化
        viewModelScope.launch {
            WeatherSoundMapper.weatherEnabled.collect { enabled ->
                _weatherEnabled.value = enabled
                if (!enabled) {
                    _currentWeather.value = null
                }
            }
        }
    }

    /**
     * 观察音频播放状态
     */
    private fun observeAudioStates() {
        // 观察本地播放状态
        viewModelScope.launch {
            audioManager.localPlayingStates.collect { states ->
                _playingStates.value = states
            }
        }

        // 观察是否有任何声音正在播放
        viewModelScope.launch {
            audioManager.hasAnyPlayingSounds.collect { hasPlaying ->
                _hasAnyPlayingSounds.value = hasPlaying
            }
        }
    }

    /**
     * 更新预设远程固定列表
     */
    fun updatePresetRemotePinned(presetIndex: Int) {
        val pinned = org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, presetIndex)
        _presetRemotePinned.value = pinned
    }

    /**
     * 更新当前预设的固定声音列表
     */
    fun updatePinnedSounds(sounds: Set<AudioManager.Sound>) {
        _pinnedSounds.value = sounds
    }

    /**
     * 更新当前预设的远程声音列表
     */
    fun updateDefaultRemoteSounds(sounds: List<org.xmsleep.app.audio.model.SoundMetadata>) {
        _defaultRemoteSounds.value = sounds
    }

    /**
     * 刷新天气数据
     */
    fun refreshWeather() {
        viewModelScope.launch {
            val result = fetchAndRefreshWeather()
            if (result != null) {
                _currentWeather.value = result
            }
        }
    }

    /**
     * 获取并刷新天气数据
     */
    private suspend fun fetchAndRefreshWeather(): WeatherData? {
        return try {
            // 实现天气数据获取逻辑
            null // 临时返回 null
        } catch (e: Exception) {
            null
        }
    }
}
