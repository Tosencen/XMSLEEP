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
import javax.inject.Inject

/**
 * MainScreen 的 ViewModel
 * 负责管理主页面的状态，消除轮询循环
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val audioManager = AudioManager.getInstance()

    // 天气开关状态
    private val _weatherEnabled = MutableStateFlow(false)
    val weatherEnabled: StateFlow<Boolean> = _weatherEnabled.asStateFlow()

    // 是否有任何声音正在播放
    private val _hasPlayingSounds = MutableStateFlow(false)
    val hasPlayingSounds: StateFlow<Boolean> = _hasPlayingSounds.asStateFlow()

    // 预设远程固定列表
    private val _preset1RemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val preset1RemotePinned: StateFlow<Set<String>> = _preset1RemotePinned.asStateFlow()

    private val _preset2RemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val preset2RemotePinned: StateFlow<Set<String>> = _preset2RemotePinned.asStateFlow()

    private val _preset3RemotePinned = MutableStateFlow<Set<String>>(emptySet())
    val preset3RemotePinned: StateFlow<Set<String>> = _preset3RemotePinned.asStateFlow()

    // 当前预设的声音列表（由 MainScreen 更新）
    private val _presetSounds = MutableStateFlow<Set<AudioManager.Sound>>(emptySet())

    // 默认区域播放状态（响应式：自动从预设声音列表和逐个播放状态推导）
    private val _defaultAreaSoundsPlaying = combine(
        _presetSounds,
        audioManager.localPlayingStates
    ) { preset, localStates ->
        preset.any { localStates[it] == true }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val defaultAreaSoundsPlaying: StateFlow<Boolean> = _defaultAreaSoundsPlaying

    init {
        initializeStates()
        observeAudioStates()
    }

    /**
     * 初始化状态
     */
    private fun initializeStates() {
        // 初始化天气状态
        _weatherEnabled.value = WeatherSoundMapper.isEnabled(context)

        // 监听天气开关变化
        viewModelScope.launch {
            WeatherSoundMapper.weatherEnabled.collect { enabled ->
                _weatherEnabled.value = enabled
            }
        }

        // 初始化预设远程固定列表
        updateAllPresetRemotePinned()

        // 监听预设远程固定列表变化
        viewModelScope.launch {
            org.xmsleep.app.preferences.PreferencesManager.preset1RemotePinned.collect { pinned ->
                _preset1RemotePinned.value = pinned
                updateAllPresetRemotePinnedState()
            }
        }
        viewModelScope.launch {
            org.xmsleep.app.preferences.PreferencesManager.preset2RemotePinned.collect { pinned ->
                _preset2RemotePinned.value = pinned
                updateAllPresetRemotePinnedState()
            }
        }
        viewModelScope.launch {
            org.xmsleep.app.preferences.PreferencesManager.preset3RemotePinned.collect { pinned ->
                _preset3RemotePinned.value = pinned
                updateAllPresetRemotePinnedState()
            }
        }
    }

    /**
     * 观察音频播放状态
     */
    private fun observeAudioStates() {
        viewModelScope.launch {
            audioManager.hasAnyPlayingSounds.collect { hasPlaying ->
                _hasPlayingSounds.value = hasPlaying
                
                if (hasPlaying) {
                    // 有音频播放，启动服务
                    audioManager.startMusicService(context)
                } else {
                    // 没有音频播放，停止服务
                    audioManager.stopMusicService(context)
                }
            }
        }
    }

    /**
     * 更新所有预设远程固定列表
     */
    private fun updateAllPresetRemotePinned() {
        _preset1RemotePinned.value = org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, 1)
        _preset2RemotePinned.value = org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, 2)
        _preset3RemotePinned.value = org.xmsleep.app.preferences.PreferencesManager.getPresetRemotePinned(context, 3)
    }

    /**
     * 更新所有预设远程固定列表状态（合并后的）
     */
    private fun updateAllPresetRemotePinnedState() {
        // 触发一个合并后的状态更新
        val allPinned = _preset1RemotePinned.value + _preset2RemotePinned.value + _preset3RemotePinned.value
        // 这里可以添加一个合并后的 StateFlow 如果需要
    }

    /**
     * 更新当前预设的声音列表（触发默认区域播放状态重算）
     */
    fun updatePresetSounds(presetSounds: Set<AudioManager.Sound>) {
        _presetSounds.value = presetSounds
    }

    /**
     * 获取所有预设远程固定列表的合并
     */
    fun getAllRemotePinned(): Set<String> {
        return _preset1RemotePinned.value + _preset2RemotePinned.value + _preset3RemotePinned.value
    }
}
