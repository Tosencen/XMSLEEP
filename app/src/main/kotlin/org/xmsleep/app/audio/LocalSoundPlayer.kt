package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.common.MediaItem
import org.xmsleep.app.R
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * 本地声音播放器
 * 负责管理本地内置声音（OGG音频）的播放
 */
class LocalSoundPlayer private constructor() {

    companion object {
        private const val TAG = "LocalSoundPlayer"
        private const val DEFAULT_VOLUME = 0.5f

        @Volatile
        private var instance: LocalSoundPlayer? = null

        fun getInstance(): LocalSoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: LocalSoundPlayer().also { instance = it }
            }
        }
    }

    // 内置声音类型 -> raw 资源 ID 的映射
    private val SOUND_RAW_RES_MAP: Map<AudioManager.Sound, Int> = mapOf(
        AudioManager.Sound.UMBRELLA_RAIN to R.raw.umbrella_rain,
        AudioManager.Sound.ROWING to R.raw.rowing,
        AudioManager.Sound.OFFICE to R.raw.office,
        AudioManager.Sound.LIBRARY to R.raw.library,
        AudioManager.Sound.HEAVY_RAIN to R.raw.heavy_rain,
        AudioManager.Sound.TYPEWRITER to R.raw.typewriter,
        AudioManager.Sound.THUNDER to R.raw.thunder,
        AudioManager.Sound.CLOCK to R.raw.clock,
        AudioManager.Sound.FOREST_BIRDS to R.raw.forest_birds,
        AudioManager.Sound.DRIFTING to R.raw.drifting,
        AudioManager.Sound.CAMPFIRE to R.raw.campfire,
        AudioManager.Sound.WIND to R.raw.wind,
        AudioManager.Sound.KEYBOARD to R.raw.keyboard,
        AudioManager.Sound.SNOW_WALKING to R.raw.snow_walking,
        AudioManager.Sound.MORNING_COFFEE to R.raw.morning_coffee,
        AudioManager.Sound.WINDMILL to R.raw.windmill,
    )

    // 为每种声音类型创建单独的ExoPlayer实例
    private val players = ConcurrentHashMap<AudioManager.Sound, ExoPlayer?>()

    // 各声音的播放状态（ConcurrentHashMap用于内部管理）
    private val playingStatesInternal = ConcurrentHashMap<AudioManager.Sound, Boolean>()

    // 响应式播放状态（用于UI观察）
    private val _playingStates = MutableStateFlow<Map<AudioManager.Sound, Boolean>>(emptyMap())
    val playingStates: StateFlow<Map<AudioManager.Sound, Boolean>> = _playingStates.asStateFlow()

    // 是否有任何声音正在播放
    private val _hasAnyPlaying = MutableStateFlow(false)
    val hasAnyPlaying: StateFlow<Boolean> = _hasAnyPlaying.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // 各声音的音量设置
    private val volumeSettings = mutableMapOf<AudioManager.Sound, Float>()

    // 记录哪些音量已经从 SharedPreferences 加载过
    private val volumeLoaded = mutableSetOf<AudioManager.Sound>()

    // 播放队列，用于限制最多同时播放的声音数量
    private val playingQueue = java.util.concurrent.ConcurrentLinkedQueue<AudioManager.Sound>()

    interface Callback {
        fun onSoundPlaybackStateChanged(sound: AudioManager.Sound, isPlaying: Boolean)
    }

    private var callback: Callback? = null
    private var applicationContext: Context? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setApplicationContext(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
            loadAllVolumes(context)
        }
    }

    /**
     * 初始化播放器
     */
    private fun initializePlayer(context: Context, sound: AudioManager.Sound) {
        if (players[sound] != null) {
            return
        }

        try {
            val player = ExoPlayer.Builder(context).build().apply {
                addListener(createPlayerListener(sound))
            }
            players[sound] = player
            updatePlayingState(sound, false)
            Logger.d(TAG, "${sound.name} 播放器初始化成功")
        } catch (e: Exception) {
            Logger.e(TAG, "初始化 ${sound.name} 播放器失败: ${e.message}")
        }
    }

    /**
     * 创建播放器监听器
     */
    private fun createPlayerListener(sound: AudioManager.Sound): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        val player = players[sound]
                        if (player != null && playingQueue.contains(sound)) {
                            if (!player.playWhenReady) {
                                player.playWhenReady = true
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        val player = players[sound]
                        if (player != null && player.playWhenReady && playingQueue.contains(sound)) {
                            updatePlayingState(sound, true)
                        } else if (player != null && !player.playWhenReady) {
                            updatePlayingState(sound, false)
                        }
                    }
                    Player.STATE_IDLE -> {
                        updatePlayingState(sound, false)
                    }
                    Player.STATE_BUFFERING -> {
                        val player = players[sound]
                        if (player != null && player.playWhenReady && playingQueue.contains(sound)) {
                            // 保持播放状态
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val player = players[sound]
                if (player != null && playingQueue.contains(sound)) {
                    if (isPlaying) {
                        updatePlayingState(sound, true)
                    } else if (player.playWhenReady) {
                        // 循环衔接时的短暂缓冲，保持播放状态
                    } else {
                        updatePlayingState(sound, false)
                    }
                } else if (!isPlaying) {
                    updatePlayingState(sound, false)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Logger.e(TAG, "${sound.name} 播放错误: ${error.message}")
                updatePlayingState(sound, false)
                playingQueue.remove(sound)
                callback?.onSoundPlaybackStateChanged(sound, false)
            }
        }
    }

    /**
     * 准备声音音频源
     */
    @UnstableApi
    private fun prepareSoundAudio(
        context: Context,
        sound: AudioManager.Sound,
        resourceId: Int,
        startPositionMs: Long = 0L,
        endPositionMs: Long = 0L,
        soundName: String
    ) {
        try {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")

            val baseSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            if (players[sound] == null) {
                initializePlayer(context, sound)
            }

            val player = players[sound]
            if (player == null) {
                Logger.e(TAG, "播放器 $soundName 未初始化，无法设置媒体源")
                return
            }

            try {
                if (player.playbackState != Player.STATE_IDLE) {
                    player.stop()
                }
                player.playWhenReady = false
            } catch (e: Exception) {
                Logger.w(TAG, "重置播放器状态失败: ${e.message}")
            }

            val endPositionUs = if (endPositionMs > 0) {
                endPositionMs * 1000
            } else {
                C.TIME_END_OF_SOURCE
            }

            val clipped = ClippingMediaSource.Builder(baseSource)
                .setStartPositionUs(startPositionMs * 1000)
                .setEndPositionUs(endPositionUs)
                .build()

            player.setMediaSource(clipped)
            player.repeatMode = Player.REPEAT_MODE_ONE

            Logger.d(TAG, "$soundName 音频媒体源已设置，循环范围: ${startPositionMs}ms - ${if (endPositionMs > 0) "${endPositionMs}ms" else "音源末尾"}")
        } catch (e: Exception) {
            Logger.e(TAG, "准备$soundName 音频失败", e)
        }
    }

    /**
     * 播放指定类型的声音
     */
    @UnstableApi
    fun playSound(context: Context, sound: AudioManager.Sound, maxConcurrentSounds: Int): Boolean {
        Logger.d(TAG, "playSound 被调用: ${sound.name}")

        if (sound == AudioManager.Sound.NONE) {
            Logger.w(TAG, "声音类型为 NONE，取消播放")
            return false
        }

        if (isPlayingSound(sound)) {
            Logger.d(TAG, "${sound.name} 已经在播放中")
            return true
        }

        ensureVolumeLoaded(context, sound)

        // 检查是否已达到最大播放数量
        if (playingQueue.size >= maxConcurrentSounds) {
            val oldestSound = playingQueue.poll()
            if (oldestSound != null) {
                pauseSound(oldestSound)
                Logger.d(TAG, "已达到最大播放数量，停止最早播放的声音: ${oldestSound.name}")
            }
        }

        // 如果播放器已存在，先停止并重置状态
        val existingPlayer = players[sound]
        if (existingPlayer != null) {
            try {
                existingPlayer.stop()
                existingPlayer.playWhenReady = false
                try {
                    existingPlayer.clearMediaItems()
                } catch (e: NoSuchMethodError) {
                    Logger.d(TAG, "clearMediaItems 方法不可用，使用 stop() 重置播放器")
                } catch (e: Exception) {
                    Logger.w(TAG, "清除媒体项失败: ${e.message}")
                }
            } catch (e: Exception) {
                Logger.w(TAG, "重置播放器 ${sound.name} 状态时出错: ${e.message}")
                try {
                    existingPlayer.release()
                    players.remove(sound)
                    updatePlayingState(sound, false)
                } catch (releaseException: Exception) {
                    Logger.e(TAG, "释放播放器 ${sound.name} 失败: ${releaseException.message}")
                    players.remove(sound)
                    updatePlayingState(sound, false)
                }
            }
        }

        if (players[sound] == null) {
            initializePlayer(context, sound)
        }

        val player = players[sound]
        if (player == null) {
            Logger.e(TAG, "播放器 ${sound.name} 初始化失败，无法播放")
            return false
        }

        try {
            val rawResId = SOUND_RAW_RES_MAP[sound]
            if (rawResId == null) {
                Logger.e(TAG, "未知的声音类型: ${sound.name}")
                return false
            }
            prepareSoundAudio(context, sound, rawResId, 0L, 0L, sound.displayName)
        } catch (e: Exception) {
            Logger.e(TAG, "准备 ${sound.name} 音频源失败", e)
            return false
        }

        if (player.mediaItemCount == 0) {
            Logger.e(TAG, "播放器 ${sound.name} 媒体源设置失败，mediaItemCount = 0")
            return false
        }

        try {
            player.volume = volumeSettings[sound] ?: DEFAULT_VOLUME
            player.repeatMode = Player.REPEAT_MODE_ONE

            if (player.playbackState != Player.STATE_IDLE) {
                Logger.w(TAG, "播放器 ${sound.name} 状态不是 IDLE: ${player.playbackState}，尝试重置")
                try {
                    player.stop()
                    player.playWhenReady = false
                } catch (e: Exception) {
                    Logger.w(TAG, "重置播放器状态失败: ${e.message}")
                }
            }

            player.prepare()
            player.playWhenReady = true

            updatePlayingState(sound, true)
            playingQueue.offer(sound)

            callback?.onSoundPlaybackStateChanged(sound, true)

            Logger.d(TAG, "${sound.name} 开始播放，媒体源数量: ${player.mediaItemCount}，播放器状态: ${player.playbackState}，playWhenReady: ${player.playWhenReady}")
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "播放 ${sound.name} 时出错", e)
            updatePlayingState(sound, false)
            return false
        }
    }

    /**
     * 暂停指定声音的播放
     */
    fun pauseSound(sound: AudioManager.Sound) {
        try {
            Logger.d(TAG, "准备暂停声音: ${sound.name}")

            players[sound]?.pause()
            updatePlayingState(sound, false)
            playingQueue.remove(sound)

            callback?.onSoundPlaybackStateChanged(sound, false)

            Logger.d(TAG, "${sound.name} 已暂停")
        } catch (e: Exception) {
            Logger.e(TAG, "暂停 ${sound.name} 失败: ${e.message}")
        }
    }

    /**
     * 暂停所有本地声音
     */
    fun pauseAllSounds() {
        try {
            players.forEach { (sound, player) ->
                try {
                    player?.let {
                        it.playWhenReady = false
                        it.pause()
                    }
                    updatePlayingState(sound, false)
                    callback?.onSoundPlaybackStateChanged(sound, false)
                } catch (e: Exception) {
                    Logger.e(TAG, "暂停 ${sound.name} 失败: ${e.message}")
                    updatePlayingState(sound, false)
                }
            }
            playingQueue.clear()
            Logger.d(TAG, "所有本地声音已暂停")
        } catch (e: Exception) {
            Logger.e(TAG, "暂停所有本地声音时发生错误: ${e.message}")
        }
    }

    /**
     * 停止所有本地声音
     */
    fun stopAllSounds() {
        try {
            players.forEach { (sound, player) ->
                try {
                    player?.let {
                        it.playWhenReady = false
                        it.stop()
                        if (it.isPlaying) {
                            Logger.w(TAG, "${sound.name} 停止后仍在播放，强制暂停")
                            it.pause()
                            it.playWhenReady = false
                        }
                    }
                    updatePlayingState(sound, false)
                    callback?.onSoundPlaybackStateChanged(sound, false)
                    Logger.d(TAG, "${sound.name} 已停止")
                } catch (e: Exception) {
                    Logger.e(TAG, "停止 ${sound.name} 失败: ${e.message}")
                    updatePlayingState(sound, false)
                }
            }
            playingQueue.clear()
            Logger.d(TAG, "停止所有本地声音完成")
        } catch (e: Exception) {
            Logger.e(TAG, "停止所有本地声音时发生错误: ${e.message}", e)
        }
    }

    /**
     * 检查指定声音是否正在播放
     */
    fun isPlayingSound(sound: AudioManager.Sound): Boolean {
        return playingStatesInternal[sound] == true
    }

    /**
     * 更新播放状态并通知响应式流
     */
    private fun updatePlayingState(sound: AudioManager.Sound, isPlaying: Boolean) {
        playingStatesInternal[sound] = isPlaying
        _playingStates.value = playingStatesInternal.toMap()
        _hasAnyPlaying.value = playingStatesInternal.values.any { it }
    }

    /**
     * 设置音量
     */
    fun setVolume(sound: AudioManager.Sound, volume: Float) {
        val coercedVolume = volume.coerceIn(0f, 1f)
        volumeSettings[sound] = coercedVolume
        players[sound]?.volume = coercedVolume

        applicationContext?.let { context ->
            PreferencesManager.saveLocalSoundVolume(context, sound.name, coercedVolume)
        }
    }

    /**
     * 获取音量
     */
    fun getVolume(sound: AudioManager.Sound): Float {
        if (!volumeLoaded.contains(sound)) {
            applicationContext?.let { context ->
                ensureVolumeLoaded(context, sound)
            }
        }
        return volumeSettings[sound] ?: DEFAULT_VOLUME
    }

    /**
     * 确保指定声音的音量已从 SharedPreferences 加载
     */
    private fun ensureVolumeLoaded(context: Context, sound: AudioManager.Sound) {
        if (!volumeLoaded.contains(sound)) {
            val savedVolume = PreferencesManager.getLocalSoundVolume(
                context,
                sound.name,
                DEFAULT_VOLUME
            )
            volumeSettings[sound] = savedVolume
            volumeLoaded.add(sound)
            Logger.d(TAG, "加载 ${sound.name} 的保存音量: $savedVolume")
        }
    }

    /**
     * 从 SharedPreferences 加载所有本地声音的音量设置
     */
    private fun loadAllVolumes(context: Context) {
        AudioManager.Sound.values().forEach { sound ->
            if (sound != AudioManager.Sound.NONE) {
                ensureVolumeLoaded(context, sound)
            }
        }
        Logger.d(TAG, "已加载所有本地声音音量设置")
    }

    /**
     * 释放指定声音的播放器资源
     */
    fun releasePlayer(sound: AudioManager.Sound) {
        if (sound == AudioManager.Sound.NONE) return

        try {
            players[sound]?.stop()
            players[sound]?.release()
            players.remove(sound)
            updatePlayingState(sound, false)
            Logger.d(TAG, "成功释放 ${sound.name} 播放器资源")
        } catch (e: Exception) {
            Logger.e(TAG, "释放 ${sound.name} 播放器资源失败: ${e.message}")
            players.remove(sound)
        }
    }

    /**
     * 释放所有播放器资源
     */
    fun releaseAllPlayers() {
        try {
            players.keys.forEach { sound ->
                releasePlayer(sound)
            }
            Logger.d(TAG, "已释放所有本地播放器资源")
        } catch (e: Exception) {
            Logger.e(TAG, "释放所有本地播放器资源失败: ${e.message}")
        }
    }

    /**
     * 获取正在播放的声音列表
     */
    fun getPlayingSounds(): List<AudioManager.Sound> {
        return playingStatesInternal.filter { it.value }.keys.toList()
    }

    /**
     * 检查是否有任何本地声音正在播放
     */
    fun hasAnyPlayingSounds(): Boolean {
        return playingStatesInternal.values.any { it }
    }
}
