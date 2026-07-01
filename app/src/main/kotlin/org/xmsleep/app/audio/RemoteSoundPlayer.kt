package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.common.MediaItem
import org.xmsleep.app.audio.model.SoundMetadata
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.utils.Logger
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 远程声音播放器
 * 负责管理远程在线声音的播放
 */
class RemoteSoundPlayer private constructor() {

    companion object {
        private const val TAG = "RemoteSoundPlayer"
        private const val DEFAULT_VOLUME = 0.5f

        @Volatile
        private var instance: RemoteSoundPlayer? = null

        fun getInstance(): RemoteSoundPlayer {
            return instance ?: synchronized(this) {
                instance ?: RemoteSoundPlayer().also { instance = it }
            }
        }
    }

    // 网络音频播放器（使用soundId作为key）
    private val remotePlayers = ConcurrentHashMap<String, ExoPlayer?>()

    // 网络音频的播放状态（ConcurrentHashMap用于内部管理）
    private val remotePlayingStatesInternal = ConcurrentHashMap<String, Boolean>()

    // 响应式播放状态（用于UI观察）
    private val _playingStates = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val playingStates: StateFlow<Map<String, Boolean>> = _playingStates.asStateFlow()

    // 是否有任何远程声音正在播放
    private val _hasAnyPlaying = MutableStateFlow(false)
    val hasAnyPlaying: StateFlow<Boolean> = _hasAnyPlaying.asStateFlow()

    // 网络音频的音量设置
    private val remoteVolumeSettings = ConcurrentHashMap<String, Float>()

    // 记录哪些远程音频的音量已经从 SharedPreferences 加载过
    private val remoteVolumeLoaded = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    // 网络音频的元数据（用于恢复播放）
    private val remoteMetadataCache = ConcurrentHashMap<String, Pair<SoundMetadata, Uri>>()

    // 播放队列
    private val playingQueue = java.util.concurrent.ConcurrentLinkedQueue<String>()

    interface Callback {
        fun onRemoteSoundPlaybackStateChanged(soundId: String, isPlaying: Boolean)
    }

    private var callback: Callback? = null
    private var applicationContext: Context? = null

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setApplicationContext(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    /**
     * 更新播放状态并通知响应式流
     */
    private fun updatePlayingState(soundId: String, isPlaying: Boolean) {
        remotePlayingStatesInternal[soundId] = isPlaying
        _playingStates.value = remotePlayingStatesInternal.toMap()
        _hasAnyPlaying.value = remotePlayingStatesInternal.values.any { it }
    }

    /**
     * 创建网络音频播放器监听器
     */
    private fun createRemotePlayerListener(soundId: String): Player.Listener {
        return object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> {
                        val player = remotePlayers[soundId]
                        if (player != null && playingQueue.contains(soundId)) {
                            if (!player.playWhenReady) {
                                player.playWhenReady = true
                            }
                            updatePlayingState(soundId, true)
                        }
                    }
                    Player.STATE_READY -> {
                        val player = remotePlayers[soundId]
                        val isInQueue = playingQueue.contains(soundId)
                        if (isInQueue && player != null) {
                            if (player.playWhenReady) {
                                updatePlayingState(soundId, true)
                                player.volume = remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
                            } else {
                                updatePlayingState(soundId, false)
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        updatePlayingState(soundId, false)
                    }
                    Player.STATE_BUFFERING -> {
                        val player = remotePlayers[soundId]
                        if (player != null && player.playWhenReady && playingQueue.contains(soundId)) {
                            updatePlayingState(soundId, true)
                        }
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                val isInQueue = playingQueue.contains(soundId)
                val player = remotePlayers[soundId]

                if (isInQueue && player != null) {
                    if (isPlaying) {
                        updatePlayingState(soundId, true)
                    } else if (player.playWhenReady) {
                        updatePlayingState(soundId, true)
                    } else {
                        updatePlayingState(soundId, false)
                    }
                } else if (!isPlaying) {
                    updatePlayingState(soundId, false)
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Logger.e(TAG, "$soundId 播放错误: ${error.message}")
                updatePlayingState(soundId, false)
                playingQueue.remove(soundId)
                callback?.onRemoteSoundPlaybackStateChanged(soundId, false)
            }
        }
    }

    /**
     * 播放网络音频
     */
    @UnstableApi
    fun playRemoteSound(
        context: Context,
        metadata: SoundMetadata,
        uri: Uri,
        maxConcurrentSounds: Int
    ): Boolean {
        val soundId = metadata.id

        ensureRemoteVolumeLoaded(context, soundId)

        if (!remoteVolumeSettings.containsKey(soundId)) {
            remoteVolumeSettings[soundId] = loadRemoteSoundVolume(context, soundId)
        }

        if (isPlayingRemoteSound(soundId)) {
            Logger.d(TAG, "$soundId 已经在播放中")
            return true
        }

        // 检查是否已达到最大播放数量
        if (playingQueue.size >= maxConcurrentSounds) {
            val oldestSoundId = playingQueue.poll()
            if (oldestSoundId != null) {
                pauseRemoteSound(oldestSoundId)
                Logger.d(TAG, "已达到最大播放数量，停止最早播放的远程声音: $oldestSoundId")
            }
        }

        // 释放旧播放器
        val existingPlayer = remotePlayers[soundId]
        if (existingPlayer != null) {
            try {
                Logger.d(TAG, "播放器 $soundId 已存在，释放后重新创建")
                existingPlayer.playWhenReady = false
                existingPlayer.stop()
                existingPlayer.release()
            } catch (e: Exception) {
                Logger.w(TAG, "释放旧播放器 $soundId 失败: ${e.message}")
            }
            remotePlayers.remove(soundId)
        }

        // 初始化播放器
        if (remotePlayers[soundId] == null) {
            try {
                val player = ExoPlayer.Builder(context).build().apply {
                    addListener(createRemotePlayerListener(soundId))
                }
                remotePlayers[soundId] = player
                updatePlayingState(soundId, false)
                Logger.d(TAG, "$soundId 播放器初始化成功")
            } catch (e: Exception) {
                Logger.e(TAG, "初始化 $soundId 播放器失败", e)
                return false
            }
        }

        val player = remotePlayers[soundId]
        if (player == null) {
            Logger.e(TAG, "播放器 $soundId 初始化失败，无法播放")
            return false
        }

        remoteMetadataCache[soundId] = Pair(metadata, uri)

        try {
            val dataSourceFactory = DefaultDataSource.Factory(context)

            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(uri))

            player.setMediaSource(mediaSource)
            player.repeatMode = Player.REPEAT_MODE_ONE
            player.volume = remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME

            player.prepare()
            player.playWhenReady = true
            updatePlayingState(soundId, true)
            playingQueue.offer(soundId)

            callback?.onRemoteSoundPlaybackStateChanged(soundId, true)

            Logger.d(TAG, "$soundId 开始播放")
            return true
        } catch (e: Exception) {
            Logger.e(TAG, "播放 $soundId 声音失败", e)
            updatePlayingState(soundId, false)
            return false
        }
    }

    /**
     * 暂停网络音频
     */
    fun pauseRemoteSound(soundId: String) {
        try {
            remotePlayers[soundId]?.apply {
                try {
                    playWhenReady = false
                    stop()
                    release()
                } catch (e: Exception) {
                    Logger.w(TAG, "释放播放器 $soundId 时出错: ${e.message}")
                }
            }
            remotePlayers.remove(soundId)
            updatePlayingState(soundId, false)
            playingQueue.remove(soundId)

            callback?.onRemoteSoundPlaybackStateChanged(soundId, false)

            Logger.d(TAG, "$soundId 已暂停并释放播放器")
        } catch (e: Exception) {
            Logger.e(TAG, "暂停 $soundId 失败: ${e.message}")
            remotePlayers.remove(soundId)
            updatePlayingState(soundId, false)
        }
    }

    /**
     * 暂停所有远程声音
     */
    fun pauseAllRemoteSounds() {
        val remotePlayerIds = remotePlayers.keys.toList()
        remotePlayerIds.forEach { soundId ->
            pauseRemoteSound(soundId)
        }
        playingQueue.clear()
        Logger.d(TAG, "所有远程声音已暂停")
    }

    /**
     * 检查网络音频是否正在播放
     */
    fun isPlayingRemoteSound(soundId: String): Boolean {
        return remotePlayingStatesInternal[soundId] == true
    }

    /**
     * 设置网络音频音量
     */
    fun setRemoteVolume(soundId: String, volume: Float) {
        val coercedVolume = volume.coerceIn(0f, 1f)
        remoteVolumeSettings[soundId] = coercedVolume
        remotePlayers[soundId]?.volume = coercedVolume

        applicationContext?.let { context ->
            PreferencesManager.saveRemoteSoundVolume(context, soundId, coercedVolume)
        }
    }

    /**
     * 获取网络音频音量
     */
    fun getRemoteVolume(soundId: String): Float {
        if (!remoteVolumeLoaded.contains(soundId)) {
            applicationContext?.let { context ->
                ensureRemoteVolumeLoaded(context, soundId)
            }
        }
        return remoteVolumeSettings[soundId] ?: DEFAULT_VOLUME
    }

    /**
     * 确保指定远程音频的音量已从 SharedPreferences 加载
     */
    private fun ensureRemoteVolumeLoaded(context: Context, soundId: String) {
        if (!remoteVolumeLoaded.contains(soundId)) {
            val savedVolume = PreferencesManager.getRemoteSoundVolume(
                context,
                soundId,
                DEFAULT_VOLUME
            )
            remoteVolumeSettings[soundId] = savedVolume
            remoteVolumeLoaded.add(soundId)
            Logger.d(TAG, "加载远程音频 $soundId 的保存音量: $savedVolume")
        }
    }

    /**
     * 从 SharedPreferences 加载远程声音的音量设置
     */
    private fun loadRemoteSoundVolume(context: Context, soundId: String): Float {
        ensureRemoteVolumeLoaded(context, soundId)
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
            remotePlayingStatesInternal.remove(soundId)
            _playingStates.value = remotePlayingStatesInternal.toMap()
            _hasAnyPlaying.value = remotePlayingStatesInternal.values.any { it }
            remoteVolumeSettings.remove(soundId)
            Logger.d(TAG, "成功释放 $soundId 播放器资源")
        } catch (e: Exception) {
            Logger.e(TAG, "释放 $soundId 播放器资源失败: ${e.message}")
            remotePlayers.remove(soundId)
            remotePlayingStatesInternal.remove(soundId)
            _playingStates.value = remotePlayingStatesInternal.toMap()
            _hasAnyPlaying.value = remotePlayingStatesInternal.values.any { it }
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
            Logger.d(TAG, "已释放所有网络音频播放器资源")
        } catch (e: Exception) {
            Logger.e(TAG, "释放所有网络音频播放器资源失败: ${e.message}")
        }
    }

    /**
     * 获取正在播放的远程声音ID列表
     */
    fun getPlayingRemoteSoundIds(): List<String> {
        val stateBasedIds = remotePlayingStatesInternal.filter { it.value }.keys.toList()
        if (stateBasedIds.isNotEmpty()) {
            return stateBasedIds
        }
        val playingIds = mutableListOf<String>()
        remotePlayers.forEach { (soundId, player) ->
            if (player != null && player.playWhenReady && player.isPlaying) {
                playingIds.add(soundId)
            }
        }
        return playingIds
    }

    /**
     * 获取远程音频的元数据和URI（用于恢复播放）
     */
    fun getRemoteMetadata(soundId: String): Pair<SoundMetadata, Uri>? {
        return remoteMetadataCache[soundId]
    }

    /**
     * 检查是否有任何远程声音正在播放
     */
    fun hasAnyPlayingSounds(): Boolean {
        return remotePlayingStatesInternal.values.any { it }
    }
}
