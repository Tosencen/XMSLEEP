package org.xmsleep.app.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import org.xmsleep.app.R

/**
 * 全局音频管理器
 * 负责管理应用中的音频播放，支持多个音频同时播放
 */
class AudioManager private constructor() {

    companion object {
        private const val TAG = "AudioManager"
        private const val DEFAULT_VOLUME = 0.5f
        
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
        RAIN,
        CAMPFIRE,
        THUNDER,
        CAT_PURRING,
        BIRD_CHIRPING,
        NIGHT_INSECTS
    }
    
    // 网络音频播放器（使用soundId作为key）
    private val remotePlayers = java.util.concurrent.ConcurrentHashMap<String, ExoPlayer?>()
    
    // 网络音频的播放状态
    private val remotePlayingStates = java.util.concurrent.ConcurrentHashMap<String, Boolean>()
    
    // 网络音频的音量设置
    private val remoteVolumeSettings = mutableMapOf<String, Float>()

    private var applicationContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // 为每种声音类型创建单独的ExoPlayer实例
    private val players = java.util.concurrent.ConcurrentHashMap<Sound, ExoPlayer?>()
    
    // 各声音的播放状态
    private val playingStates = java.util.concurrent.ConcurrentHashMap<Sound, Boolean>()
    
    // 各声音的音量设置
    private val volumeSettings = mutableMapOf(
        Sound.RAIN to DEFAULT_VOLUME,
        Sound.CAMPFIRE to DEFAULT_VOLUME,
        Sound.THUNDER to DEFAULT_VOLUME,
        Sound.CAT_PURRING to DEFAULT_VOLUME,
        Sound.BIRD_CHIRPING to DEFAULT_VOLUME,
        Sound.NIGHT_INSECTS to DEFAULT_VOLUME
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
                        playingStates[sound] = false
                        players[sound]?.prepare()
                        players[sound]?.play()
                        playingStates[sound] = true
                    }
                    Player.STATE_READY -> {
                        if (players[sound]?.playWhenReady == true) {
                            playingStates[sound] = true
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playingStates[sound] = isPlaying
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "${sound.name} 播放错误: ${error.message}")
                playingStates[sound] = false
            }
        }
    }

    /**
     * 准备声音音频源
     */
    @UnstableApi
    private fun prepareSoundAudio(
        context: Context,
        sound: Sound,
        resourceId: Int,
        startPositionMs: Long = 500L,
        endPositionMs: Long,
        soundName: String
    ) {
        try {
            val dataSourceFactory = DefaultDataSource.Factory(context)
            val uri = Uri.parse("android.resource://${context.packageName}/$resourceId")
            
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            val clippingMediaSource = ClippingMediaSource(
                mediaSource,
                startPositionMs * 1000,
                endPositionMs * 1000
            )

            if (players[sound] == null) {
                initializePlayer(context, sound)
            }

            players[sound]?.apply {
                setMediaSource(clippingMediaSource)
                repeatMode = Player.REPEAT_MODE_ONE
            }
            Log.d(TAG, "$soundName 音频媒体源已设置")
        } catch (e: Exception) {
            Log.e(TAG, "准备$soundName 音频失败: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 准备各种声音
     */
    private fun prepareRainSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.rain_sound_188158,
            500L, 3400000L,
            "雨声"
        )
    }

    private fun prepareThunderSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.dalei,
            500L, 60000L,
            "雷声"
        )
    }

    private fun prepareCampfireSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.gouhuo,
            500L, 90000L,
            "篝火声"
        )
    }

    private fun prepareCatPurringSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.cat_purring,
            500L, 60000L,
            "呼噜声"
        )
    }

    private fun prepareBirdChirpingSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.bird_chirping,
            500L, 60000L,
            "鸟鸣声"
        )
    }

    private fun prepareNightInsectsSound(context: Context, sound: Sound) {
        prepareSoundAudio(
            context, sound,
            R.raw.xishuai_animation,
            500L, 60000L,
            "夜虫声"
        )
    }

    /**
     * 播放指定类型的声音
     */
    fun playSound(context: Context, sound: Sound) {
        try {
            if (applicationContext == null) {
                applicationContext = context.applicationContext
            }

            if (sound == Sound.NONE) {
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

            initializePlayer(context, sound)
            
            when (sound) {
                Sound.RAIN -> prepareRainSound(context, sound)
                Sound.CAMPFIRE -> prepareCampfireSound(context, sound)
                Sound.THUNDER -> prepareThunderSound(context, sound)
                Sound.CAT_PURRING -> prepareCatPurringSound(context, sound)
                Sound.BIRD_CHIRPING -> prepareBirdChirpingSound(context, sound)
                Sound.NIGHT_INSECTS -> prepareNightInsectsSound(context, sound)
                else -> return
            }

            players[sound]?.apply {
                volume = volumeSettings[sound] ?: DEFAULT_VOLUME
                prepare()
                play()
            }
            playingStates[sound] = true
            Log.d(TAG, "${sound.name} 开始播放")
        } catch (e: Exception) {
            Log.e(TAG, "播放 ${sound.name} 声音失败: ${e.message}")
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

            players[sound]?.pause()
            playingStates[sound] = false
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
                    player?.pause()
                    playingStates[sound] = false
                } catch (e: Exception) {
                    Log.e(TAG, "暂停 ${sound.name} 失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "暂停所有声音时发生错误: ${e.message}")
        }
    }
    
    /**
     * 立即停止所有声音播放
     */
    fun stopAllSounds() {
        try {
            players.forEach { (sound, player) ->
                try {
                    player?.stop()
                    playingStates[sound] = false
                } catch (e: Exception) {
                    Log.e(TAG, "停止 ${sound.name} 失败: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止所有声音时发生错误: ${e.message}")
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
            players[sound]?.stop()
            players[sound]?.release()
            players.remove(sound)
            playingStates[sound] = false
            Log.d(TAG, "成功释放 ${sound.name} 播放器资源")
        } catch (e: Exception) {
            Log.e(TAG, "释放 ${sound.name} 播放器资源失败: ${e.message}")
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
                }
            }
            
            // 准备音频源
            try {
                val dataSourceFactory = DefaultDataSource.Factory(context)
                val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
                
                val clippingMediaSource = ClippingMediaSource(
                    mediaSource,
                    metadata.loopStart * 1000,
                    metadata.loopEnd * 1000
                )
                
                remotePlayers[soundId]?.apply {
                    setMediaSource(clippingMediaSource)
                    repeatMode = Player.REPEAT_MODE_ONE
                    volume = remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
                    prepare()
                    play()
                }
                remotePlayingStates[soundId] = true
                Log.d(TAG, "$soundId 开始播放")
            } catch (e: Exception) {
                Log.e(TAG, "播放 $soundId 声音失败: ${e.message}")
                e.printStackTrace()
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放网络音频失败: ${e.message}")
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
                        remotePlayingStates[soundId] = false
                        remotePlayers[soundId]?.prepare()
                        remotePlayers[soundId]?.play()
                        remotePlayingStates[soundId] = true
                    }
                    Player.STATE_READY -> {
                        if (remotePlayers[soundId]?.playWhenReady == true) {
                            remotePlayingStates[soundId] = true
                        }
                    }
                }
            }
            
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                remotePlayingStates[soundId] = isPlaying
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "$soundId 播放错误: ${error.message}")
                remotePlayingStates[soundId] = false
            }
        }
    }
    
    /**
     * 暂停网络音频
     */
    fun pauseRemoteSound(soundId: String) {
        try {
            remotePlayers[soundId]?.pause()
            remotePlayingStates[soundId] = false
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
            remotePlayers[soundId]?.stop()
            remotePlayers[soundId]?.release()
            remotePlayers.remove(soundId)
            remotePlayingStates.remove(soundId)
            remoteVolumeSettings.remove(soundId)
            Log.d(TAG, "成功释放 $soundId 播放器资源")
        } catch (e: Exception) {
            Log.e(TAG, "释放 $soundId 播放器资源失败: ${e.message}")
            remotePlayers.remove(soundId)
            remotePlayingStates.remove(soundId)
            remoteVolumeSettings.remove(soundId)
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

