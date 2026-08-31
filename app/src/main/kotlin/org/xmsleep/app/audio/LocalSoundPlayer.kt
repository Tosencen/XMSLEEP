package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                        .build(),
                    false
                )
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
            // 先取快照：下面会在遍历过程中修改 players（移除已释放的条目），
            // 直接遍历原 map 会触发 ConcurrentModificationException 导致后续播放器全部漏释放
            val snapshot = players.entries.toList()
            var releasedCount = 0

            snapshot.forEach { (sound, player) ->
                try {
                    player?.let {
                        it.playWhenReady = false
                        it.stop()
                    }
                    updatePlayingState(sound, false)
                    callback?.onSoundPlaybackStateChanged(sound, false)
                } catch (e: Exception) {
                    Logger.e(TAG, "停止 ${sound.name} 失败: ${e.message}")
                    updatePlayingState(sound, false)
                } finally {
                    // 无论 stop 是否成功都必须 release：ExoPlayer 持有解码器与音频轨资源，
                    // 只 stop 不 release 会让实例长期驻留（本项目最多 10 个并发），整夜播放时内存持续增长。
                    // 下次播放会按需重建，代价仅数十毫秒。
                    try {
                        player?.release()
                        releasedCount++
                    } catch (e: Exception) {
                        Logger.w(TAG, "释放 ${sound.name} 播放器失败: ${e.message}")
                    }
                    players.remove(sound)
                }
            }
            playingQueue.clear()
            Logger.d(TAG, "停止所有本地声音完成，已释放 $releasedCount 个播放器")
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
     * 淡出并停止所有本地声音（用于定时器到点平滑停止）
     * 只渐变播放器音量，不写入偏好设置，避免覆盖用户保存的音量。
     * @param durationMs 淡出时长（默认 5 秒）
     */
    fun fadeOutAndStopAll(durationMs: Long = 5_000L) {
        // ExoPlayer 必须在主线程访问，淡出协程切到 Main dispatcher
        scope.launch(Dispatchers.Main) {
            try {
                val playing = players.entries.filter { playingStatesInternal[it.key] == true }
                if (playing.isEmpty()) {
                    stopAllSounds()
                    return@launch
                }
                val originals = playing.associate { (sound, _) ->
                    sound to (volumeSettings[sound] ?: DEFAULT_VOLUME)
                }
                val steps = 60L
                val stepMs = (durationMs / steps).coerceAtLeast(50L)
                for (i in 1..steps) {
                    val factor = 1f - i.toFloat() / steps.toFloat()
                    playing.forEach { (sound, player) ->
                        val orig = originals[sound] ?: DEFAULT_VOLUME
                        try {
                            player?.volume = (orig * factor).coerceIn(0f, 1f)
                        } catch (e: Exception) {
                            Logger.e(TAG, "淡出 ${sound.name} 音量失败: ${e.message}")
                        }
                    }
                    delay(stepMs)
                }
                stopAllSounds()
                Logger.d(TAG, "淡出完成，已停止所有本地声音")
            } catch (e: Exception) {
                Logger.e(TAG, "淡出所有本地声音时发生错误: ${e.message}", e)
                stopAllSounds()
            }
        }
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
            // 必须取快照：releasePlayer() 内部会 players.remove(sound)，
            // 直接遍历 players.keys 会抛 ConcurrentModificationException，
            // 异常被 catch 吞掉后只有第一个播放器被释放，其余全部泄漏且无任何日志。
            val snapshot = players.keys.toList()
            snapshot.forEach { sound ->
                releasePlayer(sound)
            }
            // 兜底：确保没有任何残留条目
            if (players.isNotEmpty()) {
                Logger.w(TAG, "释放后仍有 ${players.size} 个残留播放器，强制清理")
                players.clear()
            }
            Logger.d(TAG, "已释放所有本地播放器资源（共 ${snapshot.size} 个）")
        } catch (e: Exception) {
            Logger.e(TAG, "释放所有本地播放器资源失败: ${e.message}", e)
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
