package org.xmsleep.app.audio

import android.content.Context
import androidx.media3.common.util.UnstableApi
import org.xmsleep.app.utils.Logger
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 全局音频管理器
 * 负责管理应用中的音频播放，支持多个音频同时播放
 *
 * 重构后：委托给各个子模块处理具体逻辑
 *  - AudioFocusManager  —— 音频焦点申请/放弃
 *  - LocalSoundPlayer   —— 本地内置声音播放逻辑
 *  - RemoteSoundPlayer  —— 远程在线声音播放逻辑
 *  - MusicServiceManager —— 服务管理、蓝牙/电话监听、最近播放
 */
class AudioManager private constructor() {

    companion object {
        private const val TAG = "AudioManager"
        private const val DEFAULT_VOLUME = 0.5f
        const val MAX_CONCURRENT_SOUNDS = 10 // 最多同时播放10个音频

        @Volatile
        private var instance: AudioManager? = null

        fun getInstance(): AudioManager {
            return instance ?: synchronized(this) {
                instance ?: AudioManager().also { instance = it }
            }
        }
    }

    // 声音类型枚举
    enum class Sound(val displayName: String) {
        NONE(""),
        UMBRELLA_RAIN("伞上雨声"),
        ROWING("划船"),
        OFFICE("办公室"),
        LIBRARY("图书馆"),
        HEAVY_RAIN("大雨"),
        TYPEWRITER("打字机"),
        THUNDER("打雷"),
        CLOCK("时钟"),
        FOREST_BIRDS("森林鸟鸣"),
        DRIFTING("漂流"),
        CAMPFIRE("篝火"),
        WIND("起风了"),
        KEYBOARD("键盘"),
        SNOW_WALKING("雪地徒步"),
        MORNING_COFFEE("早晨咖啡"),
        WINDMILL("风车")
    }

    // 子模块
    private val audioFocusManager = AudioFocusManager()
    private val localSoundPlayer = LocalSoundPlayer.getInstance()
    private val remoteSoundPlayer = RemoteSoundPlayer.getInstance()
    private val musicServiceManager = MusicServiceManager.getInstance()

    // 播放顺序队列，用于限制最多同时播放的声音数量
    private val playingQueue = ConcurrentLinkedQueue<PlayingItem>()

    // 响应式状态：电台播放
    private val _radioPlaying = MutableStateFlow(false)
    val radioPlaying: StateFlow<Boolean> = _radioPlaying.asStateFlow()

    // 电台停止请求回调（由 RadioViewModel 注册）
    private var onStopRadioRequested: (() -> Unit)? = null

    fun setOnStopRadioRequested(callback: (() -> Unit)?) {
        onStopRadioRequested = callback
    }

    fun stopRadio() {
        onStopRadioRequested?.invoke()
    }

    // 电台恢复请求回调（由 RadioViewModel 注册）
    private var onRadioResumeRequested: (() -> Unit)? = null
    // 电台是否在暂停前正在播放（跨 MusicService 销毁持久化）
    private var _wasRadioPlaying = false

    fun setOnRadioResumeRequested(callback: (() -> Unit)?) {
        onRadioResumeRequested = callback
    }

    fun setRadioWasPlaying(wasPlaying: Boolean) {
        _wasRadioPlaying = wasPlaying
    }

    fun isRadioWasPlaying(): Boolean = _wasRadioPlaying

    fun resumeRadio() {
        onRadioResumeRequested?.invoke()
    }

    fun setRadioPlaying(isPlaying: Boolean) {
        _radioPlaying.value = isPlaying
        notifyServicePlayingStateChanged()
    }

    // 响应式状态：是否有任何声音正在播放（本地+远程+电台）
    val hasAnyPlayingSounds: StateFlow<Boolean> = combine(
        localSoundPlayer.hasAnyPlaying,
        remoteSoundPlayer.hasAnyPlaying,
        _radioPlaying
    ) { localPlaying, remotePlaying, radioPlaying ->
        localPlaying || remotePlaying || radioPlaying
    }.stateIn(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
        initialValue = false
    )

    // 响应式状态：本地播放状态
    val localPlayingStates: StateFlow<Map<Sound, Boolean>>
        get() = localSoundPlayer.playingStates

    // 响应式状态：远程播放状态
    val remotePlayingStates: StateFlow<Map<String, Boolean>>
        get() = remoteSoundPlayer.playingStates

    var applicationContext: Context? = null
        private set

    // 音频焦点变化回调
    private val audioFocusCallback = object : AudioFocusManager.Callback {
        override fun onAudioFocusLost() {
            pauseAllSounds()
        }

        override fun onAudioFocusLostTransient() {
            pauseAllSounds()
        }

        override fun onAudioFocusLostCanDuck() {
            // 降低音量
            localSoundPlayer.getPlayingSounds().forEach { sound ->
                localSoundPlayer.setVolume(sound, 0.1f)
            }
        }

        override fun onAudioFocusGained() {
            // 恢复音量
            localSoundPlayer.getPlayingSounds().forEach { sound ->
                val originalVolume = localSoundPlayer.getVolume(sound)
                localSoundPlayer.setVolume(sound, originalVolume)
            }
            musicServiceManager.onAudioFocusGained()
        }
    }

    // 本地播放器回调
    private val localPlayerCallback = object : LocalSoundPlayer.Callback {
        override fun onSoundPlaybackStateChanged(sound: Sound, isPlaying: Boolean) {
            if (isPlaying) {
                playingQueue.offer(PlayingItem.LocalSound(sound))
            } else {
                playingQueue.remove(PlayingItem.LocalSound(sound))
            }
            notifyServicePlayingStateChanged()
        }
    }

    // 远程播放器回调
    private val remotePlayerCallback = object : RemoteSoundPlayer.Callback {
        override fun onRemoteSoundPlaybackStateChanged(soundId: String, isPlaying: Boolean) {
            if (isPlaying) {
                playingQueue.offer(PlayingItem.RemoteSound(soundId))
            } else {
                playingQueue.remove(PlayingItem.RemoteSound(soundId))
            }
            notifyServicePlayingStateChanged()
        }
    }

    // 音乐服务管理器回调
    private val musicServiceCallback = object : MusicServiceManager.Callback {
        override fun onPlayRecentSoundsRequested() {
            // 处理最近播放请求
        }
    }

    init {
        audioFocusManager.setCallback(audioFocusCallback)
        localSoundPlayer.setCallback(localPlayerCallback)
        remoteSoundPlayer.setCallback(remotePlayerCallback)
        musicServiceManager.setCallback(musicServiceCallback)
    }

    // =========================================================================
    // region 公共播放控制 API
    // =========================================================================

    /**
     * 播放指定类型的声音
     */
    @UnstableApi
    fun playSound(context: Context, sound: Sound) {
        stopRadio()
        musicServiceManager.setPausedState(false)
        Logger.d(TAG, "playSound 被调用: ${sound.name}")

        initializeIfNeeded(context)

        if (sound == Sound.NONE) {
            Logger.w(TAG, "声音类型为 NONE，取消播放")
            return
        }

        if (!audioFocusManager.hasAudioFocus() && !audioFocusManager.requestAudioFocus(context)) {
            Logger.w(TAG, "无法获取音频焦点，取消播放")
            return
        }

        if (localSoundPlayer.isPlayingSound(sound)) {
            Logger.d(TAG, "${sound.name} 已经在播放中")
            return
        }

        // 检查是否已达到最大播放数量
        if (playingQueue.size >= MAX_CONCURRENT_SOUNDS) {
            val oldestItem = playingQueue.poll()
            when (oldestItem) {
                is PlayingItem.LocalSound -> {
                    localSoundPlayer.pauseSound(oldestItem.sound)
                    Logger.d(TAG, "已达到最大播放数量，停止最早播放的本地声音: ${oldestItem.sound.name}")
                }
                is PlayingItem.RemoteSound -> {
                    remoteSoundPlayer.pauseRemoteSound(oldestItem.soundId)
                    Logger.d(TAG, "已达到最大播放数量，停止最早播放的远程声音: ${oldestItem.soundId}")
                }
            }
        }

        localSoundPlayer.playSound(context, sound, MAX_CONCURRENT_SOUNDS)
    }

    /**
     * 暂停指定声音的播放
     */
    fun pauseSound(sound: Sound = Sound.NONE) {
        try {
            if (sound == Sound.NONE) {
                pauseAllSounds()
                return
            }

            localSoundPlayer.pauseSound(sound)
            saveRecentPlayingSounds()
        } catch (e: Exception) {
            Logger.e(TAG, "暂停 ${sound.name} 失败: ${e.message}")
        }
    }

    /**
     * 暂停所有声音（不触发电台停止）
     */
    fun pauseSoundsOnly() {
        musicServiceManager.setPausedState(true)
        try {
            saveRecentPlayingSounds()

            localSoundPlayer.pauseAllSounds()
            remoteSoundPlayer.pauseAllRemoteSounds()

            playingQueue.clear()
            notifyServicePlayingStateChanged()

            Logger.d(TAG, "所有声音已暂停（电台除外）")
        } catch (e: Exception) {
            Logger.e(TAG, "暂停所有声音时发生错误: ${e.message}")
        }
    }

    /**
     * 暂停所有声音（含电台）
     */
    fun pauseAllSounds() {
        musicServiceManager.setPausedState(true)
        try {
            saveRecentPlayingSounds()

            localSoundPlayer.pauseAllSounds()
            remoteSoundPlayer.pauseAllRemoteSounds()
            onStopRadioRequested?.invoke()

            playingQueue.clear()
            notifyServicePlayingStateChanged()

            Logger.d(TAG, "所有声音已暂停")
        } catch (e: Exception) {
            Logger.e(TAG, "暂停所有声音时发生错误: ${e.message}")
        }
    }

    /**
     * 立即停止所有声音播放
     */
    fun stopAllSounds() {
        musicServiceManager.setPausedState(false)
        try {
            Logger.d(TAG, "开始停止所有声音...")

            localSoundPlayer.stopAllSounds()
            remoteSoundPlayer.pauseAllRemoteSounds()
            onStopRadioRequested?.invoke()

            playingQueue.clear()
            notifyServicePlayingStateChanged()

            Logger.d(TAG, "停止所有声音完成")
        } catch (e: Exception) {
            Logger.e(TAG, "停止所有声音时发生错误: ${e.message}", e)
        }
    }

    /**
     * 检查指定声音是否正在播放
     */
    fun isPlayingSound(sound: Sound): Boolean {
        return localSoundPlayer.isPlayingSound(sound)
    }

    /**
     * 设置音量
     */
    fun setVolume(sound: Sound, volume: Float) {
        localSoundPlayer.setVolume(sound, volume)
    }

    /**
     * 获取音量
     */
    fun getVolume(sound: Sound): Float {
        return localSoundPlayer.getVolume(sound)
    }

    /**
     * 释放指定声音的播放器资源
     */
    fun releasePlayer(sound: Sound) {
        localSoundPlayer.releasePlayer(sound)
    }

    /**
     * 释放所有播放器资源
     */
    fun releaseAllPlayers() {
        try {
            saveRecentPlayingSounds()

            localSoundPlayer.releaseAllPlayers()
            remoteSoundPlayer.releaseAllRemotePlayers()

            audioFocusManager.abandonAudioFocus(applicationContext!!)
            musicServiceManager.release()

            applicationContext = null
            Logger.d(TAG, "已释放所有播放器资源")
        } catch (e: Exception) {
            Logger.e(TAG, "释放所有播放器资源失败: ${e.message}")
        }
    }

    // =========================================================================
    // endregion
    // =========================================================================

    // =========================================================================
    // region 蓝牙耳机 & MusicService & 最近播放
    // =========================================================================

    /**
     * 保存当前正在播放的声音列表
     */
    fun saveRecentPlayingSounds() {
        musicServiceManager.saveRecentPlayingSounds(localSoundPlayer, remoteSoundPlayer)
    }

    /**
     * 播放最近播放的声音
     */
    fun playRecentSounds(context: Context) {
        musicServiceManager.playRecentSounds(context)
    }

    /**
     * 检查是否有最近播放的声音
     */
    fun hasRecentSounds(context: Context): Boolean {
        return musicServiceManager.hasRecentSounds(context)
    }

    /**
     * 获取正在播放的声音列表
     */
    fun getPlayingSounds(): List<Sound> {
        return localSoundPlayer.getPlayingSounds()
    }

    /**
     * 检查是否有任何声音正在播放
     */
    fun hasAnyPlayingSounds(): Boolean {
        return localSoundPlayer.hasAnyPlayingSounds() || remoteSoundPlayer.hasAnyPlayingSounds() || _radioPlaying.value
    }

    /**
     * 启动音乐服务
     */
    fun startMusicService(context: Context) {
        musicServiceManager.startMusicService(context)
    }

    /**
     * 停止音乐服务
     */
    fun stopMusicService(context: Context) {
        musicServiceManager.stopMusicService(context)
    }

    /**
     * 获取当前活跃声音的描述列表（用于 MediaSession metadata）
     */
    fun getActiveSoundDescriptions(): List<String> {
        return musicServiceManager.getActiveSoundDescriptions(playingQueue)
    }

    /**
     * 通知服务播放状态已改变
     */
    private fun notifyServicePlayingStateChanged() {
        val isPlaying = hasAnyPlayingSounds()
        val localCount = localSoundPlayer.getPlayingSounds().size
        val remoteCount = remoteSoundPlayer.getPlayingRemoteSoundIds().size
        val radioCount = if (_radioPlaying.value) 1 else 0
        val totalCount = localCount + remoteCount + radioCount
        val descriptions = getActiveSoundDescriptions()
        val allDescriptions = if (_radioPlaying.value) {
            descriptions + "电台噪音"
        } else {
            descriptions
        }

        musicServiceManager.notifyServicePlayingStateChanged(isPlaying, totalCount, allDescriptions)
    }

    // =========================================================================
    // endregion
    // =========================================================================

    // =========================================================================
    // region 远程在线声音播放
    // =========================================================================

    /**
     * 播放网络音频
     */
    @UnstableApi
    fun playRemoteSound(
        context: Context,
        metadata: org.xmsleep.app.audio.model.SoundMetadata,
        uri: android.net.Uri
    ) {
        stopRadio()
        musicServiceManager.setPausedState(false)
        initializeIfNeeded(context)

        if (!audioFocusManager.hasAudioFocus() && !audioFocusManager.requestAudioFocus(context)) {
            Logger.w(TAG, "无法获取音频焦点，取消播放")
            return
        }

        remoteSoundPlayer.playRemoteSound(context, metadata, uri, MAX_CONCURRENT_SOUNDS)
    }

    /**
     * 暂停网络音频
     */
    fun pauseRemoteSound(soundId: String) {
        remoteSoundPlayer.pauseRemoteSound(soundId)
        saveRecentPlayingSounds()
    }

    /**
     * 检查网络音频是否正在播放
     */
    fun isPlayingRemoteSound(soundId: String): Boolean {
        return remoteSoundPlayer.isPlayingRemoteSound(soundId)
    }

    /**
     * 设置网络音频音量
     */
    fun setRemoteVolume(soundId: String, volume: Float) {
        remoteSoundPlayer.setRemoteVolume(soundId, volume)
    }

    /**
     * 获取网络音频音量
     */
    fun getRemoteVolume(soundId: String): Float {
        return remoteSoundPlayer.getRemoteVolume(soundId)
    }

    /**
     * 释放网络音频播放器
     */
    fun releaseRemotePlayer(soundId: String) {
        remoteSoundPlayer.releaseRemotePlayer(soundId)
    }

    /**
     * 释放所有网络音频播放器
     */
    fun releaseAllRemotePlayers() {
        remoteSoundPlayer.releaseAllRemotePlayers()
    }

    /**
     * 获取正在播放的远程声音ID列表
     */
    fun getPlayingRemoteSoundIds(): List<String> {
        return remoteSoundPlayer.getPlayingRemoteSoundIds()
    }

    /**
     * 获取远程音频的元数据和URI（用于恢复播放）
     */
    fun getRemoteMetadata(soundId: String): Pair<org.xmsleep.app.audio.model.SoundMetadata, android.net.Uri>? {
        return remoteSoundPlayer.getRemoteMetadata(soundId)
    }

    // =========================================================================
    // endregion
    // =========================================================================

    private fun initializeIfNeeded(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            localSoundPlayer.setApplicationContext(context)
            remoteSoundPlayer.setApplicationContext(context)
            musicServiceManager.setApplicationContext(context)

            musicServiceManager.initializeBluetoothHeadsetListener(context) {
                pauseAllSounds()
            }
            musicServiceManager.registerPhoneStateListener(context)
        }
    }
}
