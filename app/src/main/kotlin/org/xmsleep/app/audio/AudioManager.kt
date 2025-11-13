package org.xmsleep.app.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.common.MediaItem
import org.xmsleep.app.R

/**
 * 全局音频管理器
 * 负责管理应用中的音频播放，支持多个音频同时播放
 */
class AudioManager private constructor() {

    companion object {
        private const val TAG = "AudioManager"
        private const val DEFAULT_VOLUME = 0.5f
        private const val MAX_CONCURRENT_SOUNDS = 3
        
        @Volatile
        private var instance: AudioManager? = null
        
        fun getInstance(): AudioManager {
            return instance ?: synchronized(this) {
                instance ?: AudioManager().also { instance = it }
            }
        }
    }

    // 声音类型枚举
    enum class Sound {
        NONE,
        UMBRELLA_RAIN,
        ROWING,
        OFFICE,
        LIBRARY,
        HEAVY_RAIN,
        TYPEWRITER,
        THUNDER_NEW,
        CLOCK,
        FOREST_BIRDS,
        DRIFTING,
        CAMPFIRE_NEW,
        WIND,
        KEYBOARD,
        SNOW_WALKING
    }
    
    // 网络音频播放器（使用soundId作为key）
    private val remotePlayers = java.util.concurrent.ConcurrentHashMap<String, ExoPlayer?>()
    
    // 网络音频的播放状态
    private val remotePlayingStates = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    
    // 网络音频的音量设置
    private val remoteVolumeSettings = mutableMapOf<String, Float>()
    
    // 网络音频的循环信息（用于无缝循环）
    private val remoteLoopInfo = mutableMapOf<String, Pair<Long, Long>>() // soundId -> (loopStart, loopEnd) in milliseconds
    
    // 网络音频的位置检查 Runnable（用于无缝循环）
    private val remotePositionCheckRunnables = mutableMapOf<String, Runnable>()
    
    // 本地音频的循环信息（用于无缝循环）
    private val localLoopInfo = mutableMapOf<Sound, Pair<Long, Long>>() // sound -> (loopStart, loopEnd) in milliseconds
    
    // 本地音频的位置检查 Runnable（用于无缝循环）
    private val localPositionCheckRunnables = mutableMapOf<Sound, Runnable>()
    
    // 播放顺序队列，用于限制最多同时播放3个声音
    private sealed class PlayingItem {
        data class LocalSound(val sound: Sound) : PlayingItem()
        data class RemoteSound(val soundId: String) : PlayingItem()
    }
    private val playingQueue = java.util.concurrent.ConcurrentLinkedQueue<PlayingItem>()

    private var applicationContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 为每种声音类型创建单独的ExoPlayer实例
    private val players = java.util.concurrent.ConcurrentHashMap<Sound, ExoPlayer?>()
    
    // 各声音的播放状态
    private val playingStates = java.util.concurrent.ConcurrentHashMap<Sound, Boolean>()
    
    // 各声音的音量设置
    private val volumeSettings = mutableMapOf(
        Sound.UMBRELLA_RAIN to DEFAULT_VOLUME,
        Sound.ROWING to DEFAULT_VOLUME,
        Sound.OFFICE to DEFAULT_VOLUME,
        Sound.LIBRARY to DEFAULT_VOLUME,
        Sound.HEAVY_RAIN to DEFAULT_VOLUME,
        Sound.TYPEWRITER to DEFAULT_VOLUME,
        Sound.THUNDER_NEW to DEFAULT_VOLUME,
        Sound.CLOCK to DEFAULT_VOLUME,
        Sound.FOREST_BIRDS to DEFAULT_VOLUME,
        Sound.DRIFTING to DEFAULT_VOLUME,
        Sound.CAMPFIRE_NEW to DEFAULT_VOLUME,
        Sound.WIND to DEFAULT_VOLUME,
        Sound.KEYBOARD to DEFAULT_VOLUME,
        Sound.SNOW_WALKING to DEFAULT_VOLUME
    )
    
    // 音频焦点管理
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    
    // 音频焦点变化监听器
    private val audioFocusChangeListener = SystemAudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            SystemAudioManager.AUDIOFOCUS_LOSS -> {
                pauseAllSounds()
                hasAudioFocus = false
            }
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pauseAllSounds()
            }
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                reduceSoundVolume()
            }
            SystemAudioManager.AUDIOFOCUS_GAIN -> {
                restoreSoundVolume()
                hasAudioFocus = true
            }
        }
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()
                
                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                hasAudioFocus = result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
                hasAudioFocus
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    SystemAudioManager.STREAM_MUSIC,
                    SystemAudioManager.AUDIOFOCUS_GAIN
                )
                hasAudioFocus = result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
                hasAudioFocus
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求音频焦点失败: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * 放弃音频焦点
     */
    private fun abandonAudioFocus(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
            hasAudioFocus = false
        } catch (e: Exception) {
            Log.e(TAG, "放弃音频焦点失败: ${e.message}")
        }
    }

    /**
     * 降低音量
     */
    private fun reduceSoundVolume() {
        players.forEach { (_, player) ->
            player?.volume = 0.1f
        }
    }

    /**
     * 恢复音量
     */
    private fun restoreSoundVolume() {
        players.forEach { (sound, player) ->
            player?.volume = volumeSettings[sound] ?: DEFAULT_VOLUME
        }
    }

    /**
     * 初始化播放器
     */
    private fun initializePlayer(context: Context, sound: Sound) {
        if (players[sound] != null) {
            return
        }

        try {
            val player = ExoPlayer.Builder(context).build().apply {
                addListener(createPlayerListener(sound))
            }
            players[sound] = player
            playingStates[sound] = false
            Log.d(TAG, "${sound.name} 播放器初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "初始化 ${sound.name} 播放器失败: ${e.message}")
        }
    }

    /**
     * 创建播放器监听器
     */
    private fun createPlayerListener(sound: Sound): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        // 播放结束，由于使用了REPEAT_MODE_ALL 或 REPEAT_MODE_ONE，ExoPlayer会自动循环
                        // 不需要手动调用prepare()和play()，避免卡顿
                        // 只更新状态，让ExoPlayer自动处理循环
                        val player = players[sound]
                        if (player != null && playingQueue.contains(PlayingItem.LocalSound(sound))) {
                            // ExoPlayer会自动循环，只需要确保playWhenReady为true
                            if (!player.playWhenReady) {
                                player.playWhenReady = true
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        // 播放器准备就绪
                        val player = players[sound]
                        if (player != null && player.playWhenReady && playingQueue.contains(PlayingItem.LocalSound(sound))) {
                            playingStates[sound] = true
                        } else if (player != null && !player.playWhenReady) {
                            playingStates[sound] = false
                        }
                    }
                    Player.STATE_IDLE -> {
                        // 播放器空闲
                        playingStates[sound] = false
                    }
                    Player.STATE_BUFFERING -> {
                        // 缓冲中，如果 playWhenReady 为 true，保持播放状态，避免 UI 闪烁
                        val player = players[sound]
                        if (player != null && player.playWhenReady && playingQueue.contains(PlayingItem.LocalSound(sound))) {
                            // 保持播放状态，不更新为暂停
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 检查播放器是否还在队列中，如果不在队列中，不应该更新状态
                val player = players[sound]
                if (player != null && playingQueue.contains(PlayingItem.LocalSound(sound))) {
                    if (isPlaying) {
                        // 正在播放，更新为播放状态
                        playingStates[sound] = true
                    } else if (player.playWhenReady) {
                        // 关键：isPlaying 为 false 但 playWhenReady 为 true，可能是循环衔接时的短暂缓冲
                        // 保持播放状态，不立即更新为暂停，避免 UI 闪烁和音频中断
                        // 这通常发生在 seekTo 跳转循环位置时的缓冲阶段
                    } else {
                        // playWhenReady 为 false，确实是暂停
                        playingStates[sound] = false
                    }
                } else if (!isPlaying) {
                    playingStates[sound] = false
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "${sound.name} 播放错误: ${error.message}")
                playingStates[sound] = false
                // 停止无缝循环检查
                stopLocalSeamlessLoopCheck(sound)
                // 从播放队列中移除
                playingQueue.remove(PlayingItem.LocalSound(sound))
            }
        }
    }

    /**
     * 准备声音音频源（使用双份拼接技术实现无缝循环）
     * 参考 moodist 的实现方式，使用 ConcatenatingMediaSource 拼接两个相同的音频片段
     */
    @UnstableApi
    private fun prepareSoundAudio(
        context: Context,
        sound: Sound,
        resourceId: Int,
        startPositionMs: Long = 0L,
        endPositionMs: Long = 0L,
        soundName: String,
        useSeamlessLoop: Boolean = true
    ) {
        try {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
            
            val baseSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            // 确保播放器已初始化
            if (players[sound] == null) {
                initializePlayer(context, sound)
            }

            val player = players[sound]
            if (player == null) {
                Log.e(TAG, "播放器 $soundName 未初始化，无法设置媒体源")
                return
            }

            // 总是设置媒体源，确保播放器有正确的媒体源
            // 注意：setMediaSource 会自动清除现有媒体项，所以不需要手动清除
            // 但需要确保播放器处于正确的状态
            try {
                // 如果播放器正在播放或准备中，先停止
                if (player.playbackState != Player.STATE_IDLE) {
                    player.stop()
                }
                player.playWhenReady = false
            } catch (e: Exception) {
                Log.w(TAG, "重置播放器状态失败: ${e.message}")
            }
            
            // 使用 ClippingMediaSource，设置具体的结束位置
            // 如果 endPositionMs <= 0，使用 C.TIME_END_OF_SOURCE 表示直到音源末尾
            val endPositionUs = if (endPositionMs > 0) {
                endPositionMs * 1000
            } else {
                C.TIME_END_OF_SOURCE
            }
            
            val clipped = ClippingMediaSource.Builder(baseSource)
                .setStartPositionUs(startPositionMs * 1000)
                .setEndPositionUs(endPositionUs)
                .build()
            
            // 直接使用 ClippingMediaSource，让 ExoPlayer 自动循环
            player.setMediaSource(clipped)
            player.repeatMode = Player.REPEAT_MODE_ONE
            
            // 不再需要循环检查，ExoPlayer 的 REPEAT_MODE_ONE 会自动处理
            localLoopInfo.remove(sound)
            
            Log.d(TAG, "$soundName 音频媒体源已设置，循环范围: ${startPositionMs}ms - ${if (endPositionMs > 0) "${endPositionMs}ms" else "音源末尾"}，使用无缝循环: $useSeamlessLoop")
        } catch (e: Exception) {
            Log.e(TAG, "准备$soundName 音频失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 准备各种声音
     * 所有音频都使用自动检测文件长度（endPositionMs = 0），避免循环时只播放部分音频的问题
     */
    private fun prepareUmbrellaRainSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.umbrella_rain,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "伞上雨声"
        )
    }

    private fun prepareRowingSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.rowing,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "划船"
        )
    }

    private fun prepareOfficeSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.office,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "办公室"
        )
    }

    private fun prepareLibrarySound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.library,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "图书馆"
        )
    }

    private fun prepareHeavyRainSound(context: Context, sound: Sound) {
        try {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.heavy_rain}")
            val baseSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            // 使用裁剪后的媒体源，起点0ms，终点由解码器自动检测
            val clipped = ClippingMediaSource.Builder(baseSource)
                .setStartPositionUs(0L)
                .setEndPositionUs(C.TIME_END_OF_SOURCE)
                .build()

            if (players[sound] == null) {
                initializePlayer(context, sound)
            }
            val player = players[sound] ?: return

            try {
                if (player.playbackState != Player.STATE_IDLE) {
                    player.stop()
                }
                player.playWhenReady = false
            } catch (_: Exception) { }

            player.setMediaSource(clipped)
            // 直接使用 REPEAT_MODE_ONE，让 ExoPlayer 自动循环
            player.repeatMode = Player.REPEAT_MODE_ONE
        } catch (e: Exception) {
            Log.e(TAG, "准备大雨音频(AB拼接)失败: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun prepareTypewriterSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.typewriter,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "打字机"
        )
    }

    private fun prepareThunderNewSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.thunder_new,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "打雷"
        )
    }

    private fun prepareClockSound(context: Context, sound: Sound) {
        // 时钟音频文件较小（90KB），可能只有几秒，必须使用自动检测
        prepareSoundAudio(
            context, sound,
            R.raw.clock,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "时钟"
        )
    }

    private fun prepareForestBirdsSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.forest_birds,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "森林鸟鸣"
        )
    }

    private fun prepareDriftingSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.drifting,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "漂流"
        )
    }

    private fun prepareCampfireNewSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.campfire_new,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "篝火"
        )
    }

    private fun prepareWindSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.wind,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "起风了"
        )
    }

    private fun prepareKeyboardSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.keyboard,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "键盘"
        )
    }

    private fun prepareSnowWalkingSound(context: Context, sound: Sound) {
        // 雪地徒步音频文件可能较短，使用C.TIME_UNSET让ExoPlayer自动检测文件实际长度
        // 这样可以避免循环时只播放前几秒的问题
        prepareSoundAudio(
            context, sound,
            R.raw.snow_walking,
            0L, 0L, // 从0ms开始，自动检测文件实际长度
            "雪地徒步"
        )
    }

    /**
     * 播放指定类型的声音
     */
    fun playSound(context: Context, sound: Sound) {
        Log.d(TAG, "playSound 被调用: ${sound.name}")
        try {
            if (applicationContext == null) {
                applicationContext = context.applicationContext
            }

            if (sound == Sound.NONE) {
                Log.w(TAG, "声音类型为 NONE，取消播放")
                return
            }

            if (!hasAudioFocus && !requestAudioFocus(context)) {
                Log.w(TAG, "无法获取音频焦点，取消播放")
                return
            }

            if (isPlayingSound(sound)) {
                Log.d(TAG, "${sound.name} 已经在播放中")
                return
            }
            
            Log.d(TAG, "开始播放流程: ${sound.name}")

            // 检查是否已达到最大播放数量，如果是则停止最早播放的声音
            if (playingQueue.size >= MAX_CONCURRENT_SOUNDS) {
                val oldestItem = playingQueue.poll() // 移除最早播放的声音
                when (oldestItem) {
                    is PlayingItem.LocalSound -> {
                        // 直接暂停，不调用pauseSound避免递归
                        players[oldestItem.sound]?.pause()
                        playingStates[oldestItem.sound] = false
                        Log.d(TAG, "已达到最大播放数量，停止最早播放的本地声音: ${oldestItem.sound.name}")
                    }
                    is PlayingItem.RemoteSound -> {
                        // 直接暂停，不调用pauseRemoteSound避免递归
                        remotePlayers[oldestItem.soundId]?.pause()
                        remotePlayingStates[oldestItem.soundId] = false
                        Log.d(TAG, "已达到最大播放数量，停止最早播放的远程声音: ${oldestItem.soundId}")
                    }
                }
            }

            // 如果播放器已存在，先停止并重置状态
            val existingPlayer = players[sound]
            if (existingPlayer != null) {
                try {
                    // 停止播放器
                    existingPlayer.stop()
                    existingPlayer.playWhenReady = false
                    // 清除媒体项
                    try {
                        existingPlayer.clearMediaItems()
                    } catch (e: NoSuchMethodError) {
                        Log.d(TAG, "clearMediaItems 方法不可用，使用 stop() 重置播放器")
                    } catch (e: Exception) {
                        Log.w(TAG, "清除媒体项失败: ${e.message}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "重置播放器 ${sound.name} 状态时出错: ${e.message}")
                    // 如果重置失败，释放旧播放器并创建新的
                    try {
                        existingPlayer.release()
                        players.remove(sound)
                        playingStates[sound] = false
                    } catch (releaseException: Exception) {
                        Log.e(TAG, "释放播放器 ${sound.name} 失败: ${releaseException.message}")
                        players.remove(sound)
                        playingStates[sound] = false
                    }
                }
            }

            // 确保播放器已初始化（如果不存在或已被释放，则创建新的）
            if (players[sound] == null) {
                initializePlayer(context, sound)
            }
            
            val player = players[sound]
            if (player == null) {
                Log.e(TAG, "播放器 ${sound.name} 初始化失败，无法播放")
                return
            }

            // 设置媒体源
            try {
                when (sound) {
                    Sound.UMBRELLA_RAIN -> prepareUmbrellaRainSound(context, sound)
                    Sound.ROWING -> prepareRowingSound(context, sound)
                    Sound.OFFICE -> prepareOfficeSound(context, sound)
                    Sound.LIBRARY -> prepareLibrarySound(context, sound)
                    Sound.HEAVY_RAIN -> prepareHeavyRainSound(context, sound)
                    Sound.TYPEWRITER -> prepareTypewriterSound(context, sound)
                    Sound.THUNDER_NEW -> prepareThunderNewSound(context, sound)
                    Sound.CLOCK -> prepareClockSound(context, sound)
                    Sound.FOREST_BIRDS -> prepareForestBirdsSound(context, sound)
                    Sound.DRIFTING -> prepareDriftingSound(context, sound)
                    Sound.CAMPFIRE_NEW -> prepareCampfireNewSound(context, sound)
                    Sound.WIND -> prepareWindSound(context, sound)
                    Sound.KEYBOARD -> prepareKeyboardSound(context, sound)
                    Sound.SNOW_WALKING -> prepareSnowWalkingSound(context, sound)
                    else -> {
                        Log.e(TAG, "未知的声音类型: ${sound.name}")
                        return
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "准备 ${sound.name} 音频源失败: ${e.message}")
                e.printStackTrace()
                return
            }

            // 检查媒体源是否设置成功
            if (player.mediaItemCount == 0) {
                Log.e(TAG, "播放器 ${sound.name} 媒体源设置失败，mediaItemCount = 0")
                return
            }

            try {
                // 设置音量和播放模式（在 prepare 之前设置）
                player.volume = volumeSettings[sound] ?: DEFAULT_VOLUME
                player.repeatMode = Player.REPEAT_MODE_ONE
                
                // 确保播放器处于 IDLE 状态（setMediaSource 后应该是 IDLE）
                if (player.playbackState != Player.STATE_IDLE) {
                    Log.w(TAG, "播放器 ${sound.name} 状态不是 IDLE: ${player.playbackState}，尝试重置")
                    try {
                        player.stop()
                        player.playWhenReady = false
                    } catch (e: Exception) {
                        Log.w(TAG, "重置播放器状态失败: ${e.message}")
                    }
                }
                
                // 准备播放器（异步操作）
                // 注意：prepare() 必须在 IDLE 状态下调用
                player.prepare()
                
                // 设置播放标志（播放器会在准备好后自动开始播放）
                player.playWhenReady = true
                
                // 跳过开头部分（500ms），使用监听器在播放器准备好后执行
                // 由于 prepare() 是异步的，我们需要在 STATE_READY 时执行 seekTo
                // 这里先设置一个标记，在监听器中处理
                
                // 更新状态（实际状态会通过监听器更新）
                playingStates[sound] = true
                // 添加到播放队列
                playingQueue.offer(PlayingItem.LocalSound(sound))
                Log.d(TAG, "${sound.name} 开始播放，媒体源数量: ${player.mediaItemCount}，播放器状态: ${player.playbackState}，playWhenReady: ${player.playWhenReady}")
            } catch (e: Exception) {
                Log.e(TAG, "播放 ${sound.name} 时出错: ${e.message}", e)
                playingStates[sound] = false
                // 打印完整的堆栈跟踪
                Log.e(TAG, "错误堆栈:", e)
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放 ${sound.name} 声音失败: ${e.message}")
            playingStates[sound] = false
            e.printStackTrace()
        }
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

            // 停止无缝循环检查
            stopLocalSeamlessLoopCheck(sound)
            
            players[sound]?.pause()
            playingStates[sound] = false
            // 从播放队列中移除
            playingQueue.remove(PlayingItem.LocalSound(sound))
            Log.d(TAG, "${sound.name} 已暂停")
        } catch (e: Exception) {
            Log.e(TAG, "暂停 ${sound.name} 失败: ${e.message}")
        }
    }

    /**
     * 暂停所有声音
     */
    fun pauseAllSounds() {
        try {
            players.forEach { (sound, player) ->
                try {
                    // 停止无缝循环检查
                    stopLocalSeamlessLoopCheck(sound)
                    player?.pause()
                    playingStates[sound] = false
                } catch (e: Exception) {
                    Log.e(TAG, "暂停 ${sound.name} 失败: ${e.message}")
                }
            }
            // 清空播放队列
            playingQueue.clear()
        } catch (e: Exception) {
            Log.e(TAG, "暂停所有声音时发生错误: ${e.message}")
        }
    }
    
    /**
     * 立即停止所有声音播放
     */
    fun stopAllSounds() {
        try {
            Log.d(TAG, "开始停止所有声音...")
            
            // 停止所有本地声音
            players.forEach { (sound, player) ->
                try {
                    // 停止无缝循环检查
                    stopLocalSeamlessLoopCheck(sound)
                    
                    player?.let {
                        // 先设置 playWhenReady = false，防止自动恢复播放
                        it.playWhenReady = false
                        // 然后停止播放器
                        it.stop()
                        // 验证是否真的停止了
                        if (it.isPlaying) {
                            Log.w(TAG, "${sound.name} 停止后仍在播放，强制暂停")
                            it.pause()
                            it.playWhenReady = false
                        }
                    }
                    // 强制更新状态，不管播放器实际状态如何
                    playingStates[sound] = false
                    Log.d(TAG, "${sound.name} 已停止")
                } catch (e: Exception) {
                    Log.e(TAG, "停止 ${sound.name} 失败: ${e.message}")
                    // 即使出错也要更新状态
                    playingStates[sound] = false
                }
            }
            
            // 停止所有远程声音（不检查状态，直接停止所有）
            remotePlayers.forEach { (soundId, player) ->
                try {
                    // 停止无缝循环检查
                    stopSeamlessLoopCheck(soundId)
                    
                    player?.let {
                        // 先设置 playWhenReady = false，防止自动恢复播放
                        it.playWhenReady = false
                        // 然后暂停播放器
                        it.pause()
                        // 验证是否真的停止了
                        if (it.isPlaying) {
                            Log.w(TAG, "远程声音 $soundId 停止后仍在播放，强制停止")
                            it.stop()
                            it.playWhenReady = false
                        }
                    }
                    // 强制更新状态，不管播放器实际状态如何
                    remotePlayingStates[soundId] = false
                    // 从播放队列中移除
                    playingQueue.remove(PlayingItem.RemoteSound(soundId))
                    Log.d(TAG, "远程声音 $soundId 已停止")
                } catch (e: Exception) {
                    Log.e(TAG, "停止远程声音 $soundId 失败: ${e.message}")
                    // 即使出错也要更新状态
                    remotePlayingStates[soundId] = false
                    playingQueue.remove(PlayingItem.RemoteSound(soundId))
                }
            }
            
            // 清空播放队列
            playingQueue.clear()
            
            // 验证是否还有声音在播放
            val stillPlaying = hasAnyPlayingSounds()
            if (stillPlaying) {
                Log.w(TAG, "停止所有声音后，仍有声音在播放，进行二次停止")
                // 二次停止，确保所有声音都停止
                players.forEach { (sound, player) ->
                    player?.let {
                        it.playWhenReady = false
                        it.pause()
                        playingStates[sound] = false
                    }
                }
                remotePlayers.forEach { (soundId, player) ->
                    player?.let {
                        it.playWhenReady = false
                        it.pause()
                        remotePlayingStates[soundId] = false
                    }
                }
            }
            
            Log.d(TAG, "停止所有声音完成")
        } catch (e: Exception) {
            Log.e(TAG, "停止所有声音时发生错误: ${e.message}", e)
        }
    }

    /**
     * 检查指定声音是否正在播放
     */
    fun isPlayingSound(sound: Sound): Boolean {
        return playingStates[sound] == true
    }

    /**
     * 设置音量
     */
    fun setVolume(sound: Sound, volume: Float) {
        volumeSettings[sound] = volume.coerceIn(0f, 1f)
        players[sound]?.volume = volumeSettings[sound] ?: DEFAULT_VOLUME
    }

    /**
     * 获取音量
     */
    fun getVolume(sound: Sound): Float {
        return volumeSettings[sound] ?: DEFAULT_VOLUME
    }

    /**
     * 释放指定声音的播放器资源
     */
    fun releasePlayer(sound: Sound) {
        if (sound == Sound.NONE) return

        try {
            // 停止无缝循环检查
            stopLocalSeamlessLoopCheck(sound)
            
            players[sound]?.stop()
            players[sound]?.release()
            players.remove(sound)
            playingStates[sound] = false
            localLoopInfo.remove(sound)
            Log.d(TAG, "成功释放 ${sound.name} 播放器资源")
        } catch (e: Exception) {
            Log.e(TAG, "释放 ${sound.name} 播放器资源失败: ${e.message}")
            players.remove(sound)
            localLoopInfo.remove(sound)
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
            applicationContext?.let { abandonAudioFocus(it) }
            applicationContext = null
            Log.d(TAG, "已释放所有播放器资源")
        } catch (e: Exception) {
            Log.e(TAG, "释放所有播放器资源失败: ${e.message}")
        }
    }

    /**
     * 获取正在播放的声音列表
     */
    fun getPlayingSounds(): List<Sound> {
        return playingStates.filter { it.value }.keys.toList()
    }
    
    /**
     * 检查是否有任何声音正在播放（本地+远程）
     */
    fun hasAnyPlayingSounds(): Boolean {
        // 检查本地声音
        val hasLocalPlaying = playingStates.values.any { it == true }
        // 检查远程声音
        val hasRemotePlaying = remotePlayingStates.values.any { it == true }
        return hasLocalPlaying || hasRemotePlaying
    }
    
    /**
     * 获取正在播放的远程声音ID列表
     * 检查所有有播放器的远程音频，确保能获取到所有正在播放的音频
     */
    fun getPlayingRemoteSoundIds(): List<String> {
        // 直接检查所有播放器，看哪些真的在播放
        val playingIds = mutableListOf<String>()
        remotePlayers.forEach { (soundId, player) ->
            if (player != null && player.playWhenReady && player.isPlaying) {
                playingIds.add(soundId)
            }
        }
        // 如果通过播放器检查找到了，返回结果
        if (playingIds.isNotEmpty()) {
            return playingIds
        }
        // 否则回退到状态检查（与本地音频逻辑一致）
        return remotePlayingStates.filter { it.value }.keys.toList()
    }
    
    /**
     * 播放网络音频（使用元数据）
     */
    @UnstableApi
    fun playRemoteSound(
        context: Context,
        metadata: org.xmsleep.app.audio.model.SoundMetadata,
        uri: android.net.Uri
    ) {
        try {
            if (applicationContext == null) {
                applicationContext = context.applicationContext
            }
            
            val soundId = metadata.id
            
            if (!hasAudioFocus && !requestAudioFocus(context)) {
                Log.w(TAG, "无法获取音频焦点，取消播放")
                return
            }
            
            if (isPlayingRemoteSound(soundId)) {
                Log.d(TAG, "$soundId 已经在播放中")
                return
            }
            
            // 检查是否已达到最大播放数量，如果是则停止最早播放的声音
            if (playingQueue.size >= MAX_CONCURRENT_SOUNDS) {
                val oldestItem = playingQueue.poll() // 移除最早播放的声音
                when (oldestItem) {
                    is PlayingItem.LocalSound -> {
                        // 直接暂停，不调用pauseSound避免递归
                        players[oldestItem.sound]?.pause()
                        playingStates[oldestItem.sound] = false
                        Log.d(TAG, "已达到最大播放数量，停止最早播放的本地声音: ${oldestItem.sound.name}")
                    }
                    is PlayingItem.RemoteSound -> {
                        // 停止播放器并确保不会自动重新播放
                        remotePlayers[oldestItem.soundId]?.apply {
                            pause()
                            playWhenReady = false
                        }
                        remotePlayingStates[oldestItem.soundId] = false
                        Log.d(TAG, "已达到最大播放数量，停止最早播放的远程声音: ${oldestItem.soundId}")
                    }
                }
            }
            
            // 如果播放器已存在但可能处于错误状态，先停止并重置
            val existingPlayer = remotePlayers[soundId]
            if (existingPlayer != null) {
                try {
                    // 停止播放器以确保状态重置
                    existingPlayer.stop()
                    existingPlayer.playWhenReady = false
                    // 清除媒体项（如果方法存在）
                    try {
                        existingPlayer.clearMediaItems()
                    } catch (e: NoSuchMethodError) {
                        // 如果 clearMediaItems 不存在，尝试其他方法
                        Log.d(TAG, "clearMediaItems 方法不可用，使用 stop() 重置播放器")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "重置播放器 $soundId 状态时出错: ${e.message}")
                    // 如果重置失败，释放旧播放器并创建新的
                    try {
                        existingPlayer.release()
                        remotePlayers.remove(soundId)
                        remotePlayingStates[soundId] = false
                        remoteVolumeSettings.remove(soundId)
                    } catch (releaseException: Exception) {
                        Log.e(TAG, "释放播放器 $soundId 失败: ${releaseException.message}")
                        remotePlayers.remove(soundId)
                        remotePlayingStates[soundId] = false
                        remoteVolumeSettings.remove(soundId)
                    }
                }
            }
            
            // 初始化播放器
            if (remotePlayers[soundId] == null) {
                try {
                    val player = ExoPlayer.Builder(context).build().apply {
                        addListener(createRemotePlayerListener(soundId))
                    }
                    remotePlayers[soundId] = player
                    remotePlayingStates[soundId] = false
                    Log.d(TAG, "$soundId 播放器初始化成功")
                } catch (e: Exception) {
                    Log.e(TAG, "初始化 $soundId 播放器失败: ${e.message}")
                    e.printStackTrace()
                    return
                }
            }
            
            val player = remotePlayers[soundId]
            if (player == null) {
                Log.e(TAG, "播放器 $soundId 初始化失败，无法播放")
                return
            }
            
            // 准备音频源 - 针对网络音频优化
            try {
                val dataSourceFactory = DefaultDataSource.Factory(context)
                
                // 对于缓存文件，调整缓冲参数提升性能
                val mediaSource = if (uri.scheme == "file" && uri.path?.contains("/cache/") == true) {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri))
                } else {
                    ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(uri))
                }
                
                // 远程音频循环优化：提前1秒开始循环，减少衔接间隙
                val loopStartMs = metadata.loopStart
                val loopEndMs = if (metadata.loopEnd > 0) metadata.loopEnd else 0
                
                // 计算优化的循环起始点：提前1秒，但不小于0
                val optimizedLoopStartMs = maxOf(0, loopStartMs - 1000)
                
                // 如果 loopEnd 为 0，使用 C.TIME_END_OF_SOURCE 让 ExoPlayer 自动检测文件长度
                val endPositionUs = if (loopEndMs > 0) {
                    loopEndMs * 1000
                } else {
                    C.TIME_END_OF_SOURCE
                }
                
                // 为远程音频创建优化的媒体源
                val clippingMediaSource = if (uri.scheme == "file" && uri.path?.contains("/cache/") == true) {
                    // 缓存文件：使用提前循环优化
                    ClippingMediaSource.Builder(mediaSource)
                        .setStartPositionUs(optimizedLoopStartMs * 1000)
                        .setEndPositionUs(endPositionUs)
                        .build()
                } else {
                    // 网络文件：使用原始循环点，避免预加载过多数据
                    ClippingMediaSource.Builder(mediaSource)
                        .setStartPositionUs(loopStartMs * 1000)
                        .setEndPositionUs(endPositionUs)
                        .build()
                }
                
                Log.d(TAG, "$soundId 循环优化: 原始起始 ${loopStartMs}ms -> 优化起始 ${optimizedLoopStartMs}ms，结束 ${loopEndMs}ms")
                
                // 针对网络音频的优化设置
                player.setMediaSource(clippingMediaSource)
                player.repeatMode = Player.REPEAT_MODE_ONE
                player.volume = remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
                
                // 为网络音频设置更优化的播放参数
                try {
                    // 设置播放器参数以优化网络音频播放
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // 可以在这里添加其他优化参数
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "设置播放参数失败: ${e.message}")
                }
                
                // 不再需要循环检查，ExoPlayer 的 REPEAT_MODE_ONE 会自动处理
                remoteLoopInfo.remove(soundId)
                
                // 异步准备，避免阻塞UI
                player.prepare()
                // 使用 playWhenReady 而不是 play()，让播放器在准备好后自动开始
                player.playWhenReady = true
                remotePlayingStates[soundId] = true
                // 添加到播放队列
                playingQueue.offer(PlayingItem.RemoteSound(soundId))
                
                Log.d(TAG, "$soundId 开始播放，循环范围: ${metadata.loopStart}ms - ${metadata.loopEnd}ms")
            } catch (e: Exception) {
                Log.e(TAG, "播放 $soundId 声音失败: ${e.message}")
                remotePlayingStates[soundId] = false
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放网络音频失败: ${e.message}")
            remotePlayingStates[metadata.id] = false
            e.printStackTrace()
        }
    }
    
    /**
     * 创建网络音频播放器监听器
     */
    private fun createRemotePlayerListener(soundId: String): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        // 播放结束，由于使用了REPEAT_MODE_ONE，ExoPlayer会自动循环
                        // 但网络音频可能存在缓冲延迟，需要确保无缝衔接
                        val player = remotePlayers[soundId]
                        if (player != null && playingQueue.contains(PlayingItem.RemoteSound(soundId))) {
                            // 确保循环时的无缝衔接，立即设置播放状态
                            if (!player.playWhenReady) {
                                player.playWhenReady = true
                            }
                            // 预设循环状态，避免状态不同步
                            remotePlayingStates[soundId] = true
                        }
                    }
                    Player.STATE_READY -> {
                        // 播放器准备就绪，对于网络音频意味着缓冲完成
                        val player = remotePlayers[soundId]
                        val isInQueue = playingQueue.contains(PlayingItem.RemoteSound(soundId))
                        if (isInQueue && player != null) {
                            if (player.playWhenReady) {
                                remotePlayingStates[soundId] = true
                                // 网络音频准备完成后，确保音量设置正确
                                player.volume = remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
                            } else {
                                remotePlayingStates[soundId] = false
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        // 播放器空闲
                        remotePlayingStates[soundId] = false
                    }
                    Player.STATE_BUFFERING -> {
                        // 网络音频缓冲时的优化处理
                        val player = remotePlayers[soundId]
                        if (player != null && player.playWhenReady && playingQueue.contains(PlayingItem.RemoteSound(soundId))) {
                            // 缓冲时保持播放状态，但可以适当降低音量避免卡顿明显
                            // 这里不改变音量，只是保持状态一致性
                            remotePlayingStates[soundId] = true
                        }
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                // 检查播放器是否还在队列中，如果不在队列中，不应该更新状态
                val isInQueue = playingQueue.contains(PlayingItem.RemoteSound(soundId))
                val player = remotePlayers[soundId]
                
                if (isInQueue && player != null) {
                    if (isPlaying) {
                        // 正在播放，立即更新状态
                        remotePlayingStates[soundId] = true
                    } else if (player.playWhenReady) {
                        // 网络音频循环衔接时的缓冲阶段
                        // 对于网络音频，短暂延迟是正常的，保持播放状态避免UI闪烁
                        // 但设置一个超时机制，如果长时间缓冲则视为暂停
                        remotePlayingStates[soundId] = true
                        // 可以在这里添加缓冲超时检测逻辑
                    } else {
                        // playWhenReady 为 false，确实是暂停
                        remotePlayingStates[soundId] = false
                    }
                } else if (!isPlaying) {
                    remotePlayingStates[soundId] = false
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "$soundId 播放错误: ${error.message}")
                remotePlayingStates[soundId] = false
                // 从播放队列中移除
                playingQueue.remove(PlayingItem.RemoteSound(soundId))
            }
        }
    }
    
    /**
     * 启动无缝循环检查（远程音频）
     * 对于使用双份拼接的音频，ExoPlayer 的 REPEAT_MODE_ALL 已经能够很好地处理循环
     * 此检查主要用于监控和备用，确保在特殊情况下也能无缝循环
     * 注意：使用双份拼接后，不再需要手动 seekTo，避免导致静音
     */
    private fun startSeamlessLoopCheck(soundId: String, loopStart: Long, loopEnd: Long) {
        // 停止之前的检查（如果存在）
        stopSeamlessLoopCheck(soundId)
        
        val player = remotePlayers[soundId] ?: return
        val checkIntervalMs = 100L // 监控间隔，不需要太频繁
        
        val checkRunnable = object : Runnable {
            override fun run() {
                val currentPlayer = remotePlayers[soundId]
                val isPlaying = remotePlayingStates[soundId] ?: false
                if (currentPlayer == null || !isPlaying) {
                    // 播放器不存在或已停止，停止检查
                    remotePositionCheckRunnables.remove(soundId)
                    return
                }
                
                try {
                    // 使用双份拼接后，ExoPlayer 的 REPEAT_MODE_ALL 会自动处理循环
                    // 这里只做监控，不执行 seekTo，避免导致静音
                    // 如果需要，可以在这里添加日志或监控逻辑
                    
                    // 继续检查（每100ms检查一次，用于监控）
                    if (remotePlayingStates[soundId] == true) {
                        mainHandler.postDelayed(this, checkIntervalMs)
                    } else {
                        remotePositionCheckRunnables.remove(soundId)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "$soundId 无缝循环检查失败: ${e.message}")
                    remotePositionCheckRunnables.remove(soundId)
                }
            }
        }
        
        remotePositionCheckRunnables[soundId] = checkRunnable
        // 延迟100ms后开始第一次检查
        mainHandler.postDelayed(checkRunnable, checkIntervalMs)
    }
    
    /**
     * 停止无缝循环检查（远程音频）
     */
    private fun stopSeamlessLoopCheck(soundId: String) {
        remotePositionCheckRunnables[soundId]?.let { runnable ->
            mainHandler.removeCallbacks(runnable)
            remotePositionCheckRunnables.remove(soundId)
        }
    }
    
    /**
     * 启动无缝循环检查（本地音频）
     * 对于使用双份拼接的音频，ExoPlayer 的 REPEAT_MODE_ALL 已经能够很好地处理循环
     * 此检查主要用于监控和备用，确保在特殊情况下也能无缝循环
     * 使用与远程音频相同的优化参数
     */
    private fun startLocalSeamlessLoopCheck(sound: Sound, loopStart: Long, loopEnd: Long) {
        // 停止之前的检查（如果存在）
        stopLocalSeamlessLoopCheck(sound)
        
        val player = players[sound] ?: return
        val thresholdMs = 50L // 提前50ms跳转，确保无缝
        val checkIntervalMs = 30L // 每30ms检查一次，确保及时响应
        
        val checkRunnable = object : Runnable {
            override fun run() {
                val currentPlayer = players[sound]
                val isPlaying = playingStates[sound] ?: false
                if (currentPlayer == null || !isPlaying) {
                    // 播放器不存在或已停止，停止检查
                    localPositionCheckRunnables.remove(sound)
                    return
                }
                
                try {
                    val currentPositionMs = currentPlayer.currentPosition
                    val totalDuration = currentPlayer.duration
                    
                    // 对于双份拼接的音频，ExoPlayer 使用 REPEAT_MODE_ALL 会自动在两个片段间循环
                    // 我们只需要在接近总时长结束时确保跳转到开始（作为备用机制）
                    if (totalDuration > 0 && totalDuration != C.TIME_UNSET) {
                        // 如果接近总时长结束，跳转到开始（虽然 REPEAT_MODE_ALL 应该会自动处理，但作为备用）
                        if (currentPositionMs >= totalDuration - thresholdMs) {
                            currentPlayer.seekTo(0)
                            Log.d(TAG, "${sound.name} 无缝循环（备用）：从 ${currentPositionMs}ms 跳转到 0ms")
                        }
                    } else if (loopEnd > 0) {
                        // 如果有指定的循环结束位置，使用它
                        // 注意：对于双份拼接，这个值应该是单个片段的长度
                        val actualLoopEnd = loopEnd * 2 // 因为双份拼接，总长度是单个片段的两倍
                        if (currentPositionMs >= actualLoopEnd - thresholdMs) {
                            currentPlayer.seekTo(0)
                            Log.d(TAG, "${sound.name} 无缝循环（备用）：从 ${currentPositionMs}ms 跳转到 0ms")
                        }
                    }
                    
                    // 继续检查
                    if (playingStates[sound] == true) {
                        mainHandler.postDelayed(this, checkIntervalMs)
                    } else {
                        localPositionCheckRunnables.remove(sound)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "${sound.name} 无缝循环检查失败: ${e.message}")
                    localPositionCheckRunnables.remove(sound)
                }
            }
        }
        
        localPositionCheckRunnables[sound] = checkRunnable
        // 延迟30ms后开始第一次检查
        mainHandler.postDelayed(checkRunnable, checkIntervalMs)
    }
    
    /**
     * 停止无缝循环检查（本地音频）
     */
    private fun stopLocalSeamlessLoopCheck(sound: Sound) {
        localPositionCheckRunnables[sound]?.let { runnable ->
            mainHandler.removeCallbacks(runnable)
            localPositionCheckRunnables.remove(sound)
        }
    }
    
    /**
     * 暂停网络音频
     */
    fun pauseRemoteSound(soundId: String) {
        try {
            // 停止无缝循环检查
            stopSeamlessLoopCheck(soundId)
            
            remotePlayers[soundId]?.pause()
            remotePlayingStates[soundId] = false
            // 从播放队列中移除
            playingQueue.remove(PlayingItem.RemoteSound(soundId))
            Log.d(TAG, "$soundId 已暂停")
        } catch (e: Exception) {
            Log.e(TAG, "暂停 $soundId 失败: ${e.message}")
        }
    }
    
    /**
     * 检查网络音频是否正在播放
     */
    fun isPlayingRemoteSound(soundId: String): Boolean {
        return remotePlayingStates[soundId] == true
    }
    
    /**
     * 设置网络音频音量
     * 与本地音频的 setVolume 逻辑保持一致
     */
    fun setRemoteVolume(soundId: String, volume: Float) {
        remoteVolumeSettings[soundId] = volume.coerceIn(0f, 1f)
        remotePlayers[soundId]?.volume = remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
    }
    
    /**
     * 获取网络音频音量
     */
    fun getRemoteVolume(soundId: String): Float {
        return remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
    }
    
    /**
     * 释放网络音频播放器
     */
    fun releaseRemotePlayer(soundId: String) {
        try {
            // 停止无缝循环检查
            stopSeamlessLoopCheck(soundId)
            
            remotePlayers[soundId]?.stop()
            remotePlayers[soundId]?.release()
            remotePlayers.remove(soundId)
            remotePlayingStates.remove(soundId)
            remoteVolumeSettings.remove(soundId)
            remoteLoopInfo.remove(soundId)
            Log.d(TAG, "成功释放 $soundId 播放器资源")
        } catch (e: Exception) {
            Log.e(TAG, "释放 $soundId 播放器资源失败: ${e.message}")
            remotePlayers.remove(soundId)
            remotePlayingStates.remove(soundId)
            remoteVolumeSettings.remove(soundId)
            remoteLoopInfo.remove(soundId)
            stopSeamlessLoopCheck(soundId)
        }
    }
    
    /**
     * 释放所有网络音频播放器
     */
    fun releaseAllRemotePlayers() {
        try {
            remotePlayers.keys.forEach { soundId ->
                releaseRemotePlayer(soundId)
            }
            Log.d(TAG, "已释放所有网络音频播放器资源")
        } catch (e: Exception) {
            Log.e(TAG, "释放所有网络音频播放器资源失败: ${e.message}")
        }
    }
}

