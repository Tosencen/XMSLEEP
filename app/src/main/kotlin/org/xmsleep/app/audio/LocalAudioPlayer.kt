package org.xmsleep.app.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.MediaPlayer
import android.media.AudioManager as SystemAudioManager
import android.net.Uri
import org.xmsleep.app.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * 播放模式枚举
 */
enum class PlayMode {
    SEQUENTIAL, // 顺序播放：播完当前播下一首，播完最后一首停止
    SHUFFLE,    // 随机播放：随机顺序播放所有，播完后重新洗牌
    REPEAT_ONE  // 单曲循环：无限循环当前一首
}

/**
 * 本地音频播放器单例
 * 支持顺序/随机/单曲循环播放模式
 * 与白噪音互斥：播放本地音频时自动停止白噪音，反之亦然
 */
class LocalAudioPlayer private constructor() {
    
    private val TAG = "LocalAudioPlayer"
    
    companion object {
        @Volatile
        private var instance: LocalAudioPlayer? = null
        
        fun getInstance(): LocalAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: LocalAudioPlayer().also { instance = it }
            }
        }
    }
    
    // 存储每个音频的 MediaPlayer 实例（audioId -> MediaPlayer）
    private val mediaPlayers = ConcurrentHashMap<Long, MediaPlayer>()
    
    // 存储每个音频的播放状态（audioId -> isPlaying）
    private val playingStates = ConcurrentHashMap<Long, Boolean>()
    
    // 存储每个音频的音量设置（audioId -> volume）
    private val volumeSettings = ConcurrentHashMap<Long, Float>()
    
    // 存储每个音频的 URI（audioId -> URI字符串），用于恢复播放
    private val audioUriCache = ConcurrentHashMap<Long, String>()
    
    // 当前正在播放的音频ID列表（用于UI显示）
    private val _playingAudioIds = MutableStateFlow<Set<Long>>(emptySet())
    val playingAudioIds: StateFlow<Set<Long>> = _playingAudioIds.asStateFlow()
    
    // 默认音量（50%）
    private var _currentVolume = MutableStateFlow(0.5f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()
    
    // === 播放模式相关 ===
    
    // 当前播放模式
    private val _playMode = MutableStateFlow(PlayMode.SEQUENTIAL)
    val playMode: StateFlow<PlayMode> = _playMode.asStateFlow()
    
    // 播放列表（有序的音频ID列表）
    private val _playlist = MutableStateFlow<List<Long>>(emptyList())
    val playlist: StateFlow<List<Long>> = _playlist.asStateFlow()
    
    // 随机播放顺序（洗牌后的索引列表）
    private var shuffleOrder = mutableListOf<Int>()
    
    // 当前在播放列表中的位置
    private var currentPlayIndex = -1
    
    // 上下文引用，用于读取偏好设置
    private var appContext: Context? = null

    // === 音频焦点相关 ===
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    // 是否因焦点丢失而暂停（用于焦点恢复后自动续播）
    private var pausedByFocusLoss = false
    // 因焦点丢失而被暂停的音频ID集合
    private val pausedAudioIds = mutableSetOf<Long>()
    // 因焦点丢失（CAN_DUCK）而被调低音量的音频ID集合
    private val duckedAudioIds = mutableSetOf<Long>()

    private val audioFocusChangeListener = SystemAudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            SystemAudioManager.AUDIOFOCUS_LOSS -> {
                hasAudioFocus = false
                pausedByFocusLoss = true
                pausedAudioIds.clear()
                pausedAudioIds.addAll(playingStates.filter { it.value }.keys)
                pausedAudioIds.addAll(duckedAudioIds)
                duckedAudioIds.clear()
                stopAllAudios()
            }
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                pausedByFocusLoss = true
                pausedAudioIds.clear()
                pausedAudioIds.addAll(playingStates.filter { it.value }.keys)
                pausedAudioIds.addAll(duckedAudioIds)
                duckedAudioIds.clear()
                stopAllAudios()
            }
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                mediaPlayers.forEach { (audioId, mp) ->
                    duckedAudioIds.add(audioId)
                    try {
                        mp.setVolume(0.1f, 0.1f)
                    } catch (e: Exception) {
                        Logger.e(TAG, "降音失败: audioId=$audioId", e)
                    }
                }
            }
            SystemAudioManager.AUDIOFOCUS_GAIN -> {
                hasAudioFocus = true
                // 恢复被压低的音量
                mediaPlayers.forEach { (audioId, mp) ->
                    if (duckedAudioIds.contains(audioId)) {
                        val volume = volumeSettings[audioId] ?: _currentVolume.value
                        try {
                            mp.setVolume(volume, volume)
                        } catch (e: Exception) {
                            Logger.e(TAG, "恢复音量失败: audioId=$audioId", e)
                        }
                    }
                }
                duckedAudioIds.clear()

                // 恢复因瞬时焦点丢失而被暂停的音频
                if (pausedByFocusLoss) {
                    pausedByFocusLoss = false
                    val idsToRestore = pausedAudioIds.toList()
                    pausedAudioIds.clear()
                    idsToRestore.forEach { audioId ->
                        audioUriCache[audioId]?.let { uri ->
                            playAudio(appContext ?: return@let, audioId, Uri.parse(uri)) {}
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 设置播放列表（由UI调用，传入当前可见的音频列表）
     * 切换筛选后播放列表会缩小：正在播的音频不打断，只把“下一首”的索引重定位到新列表；
     * 若当前音频不在新列表中，则索引置 -1，播完当前后从新列表第一首开始。
     */
    fun setPlaylist(audios: List<Pair<Long, Uri>>) {
        // 记住当前播放的音频 ID
        val previousCurrentId = _playlist.value.getOrNull(currentPlayIndex)
        _playlist.value = audios.map { it.first }
        // 同时缓存URI
        audios.forEach { (id, uri) ->
            audioUriCache[id] = uri.toString()
        }
        // 在新列表中重新定位当前音频（不存在则为 -1）
        currentPlayIndex = previousCurrentId?.let { id -> audios.indexOfFirst { it.first == id } } ?: -1
        // 重新洗牌
        rebuildShuffleOrder()
        Logger.d(TAG, "设置播放列表: ${audios.size} 首")
    }
    
    /**
     * 重建随机播放顺序（Fisher-Yates洗牌）
     */
    private fun rebuildShuffleOrder() {
        val size = _playlist.value.size
        shuffleOrder = (0 until size).toMutableList()
        for (i in size - 1 downTo 1) {
            val j = Random.nextInt(i + 1)
            val temp = shuffleOrder[i]
            shuffleOrder[i] = shuffleOrder[j]
            shuffleOrder[j] = temp
        }
    }
    
    /**
     * 切换播放模式（顺序 → 随机 → 单曲循环 → 顺序）
     */
    fun cyclePlayMode(context: Context): PlayMode {
        val newMode = when (_playMode.value) {
            PlayMode.SEQUENTIAL -> PlayMode.SHUFFLE
            PlayMode.SHUFFLE -> PlayMode.REPEAT_ONE
            PlayMode.REPEAT_ONE -> PlayMode.SEQUENTIAL
        }
        setPlayMode(newMode, context)
        return newMode
    }
    
    /**
     * 设置播放模式
     */
    private fun setPlayMode(mode: PlayMode, context: Context? = null) {
        val ctx = context ?: appContext
        _playMode.value = mode
        
        // 持久化
        ctx?.let {
            org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioPlayMode(it, mode.name)
        }
        
        // 如果切换到随机模式，重新洗牌
        if (mode == PlayMode.SHUFFLE) {
            rebuildShuffleOrder()
        }
        
        // 更新当前播放音频的 isLooping 状态
        updateLoopingStates()
        
        Logger.d(TAG, "切换播放模式: $mode")
    }
    
    /**
     * 初始化：从偏好设置恢复播放模式
     */
    fun initPlayMode(context: Context) {
        appContext = context
        val savedMode = org.xmsleep.app.preferences.PreferencesManager.getLocalAudioPlayMode(context)
        _playMode.value = try {
            PlayMode.valueOf(savedMode)
        } catch (_: Exception) {
            PlayMode.SEQUENTIAL
        }
        Logger.d(TAG, "初始化播放模式: ${_playMode.value}")
    }
    
    /**
     * 更新所有播放中音频的 isLooping 状态
     */
    private fun updateLoopingStates() {
        val shouldLoop = _playMode.value == PlayMode.REPEAT_ONE
        mediaPlayers.values.forEach { mp ->
            try {
                mp.isLooping = shouldLoop
            } catch (e: Exception) {
                Logger.e(TAG, "更新 isLooping 失败", e)
            }
        }
    }
    
    /**
     * 播放或停止音频（切换）
     */
    fun toggleAudio(context: Context, audioId: Long, audioUri: Uri, onError: (String) -> Unit) {
        if (isAudioPlaying(audioId)) {
            stopAudio(audioId)
        } else {
            playAudio(context, audioId, audioUri, onError)
        }
    }
    
    /**
     * 播放音频
     */
    fun playAudio(context: Context, audioId: Long, audioUri: Uri, onError: (String) -> Unit) {
        try {
            // 如果已经在播放，先停止
            if (isAudioPlaying(audioId)) {
                stopAudio(audioId)
                return
            }
            
            // 互斥：停止所有白噪音/远程音频
            try {
                org.xmsleep.app.audio.AudioManager.getInstance().pauseAllSounds()
            } catch (e: Exception) {
                Logger.e(TAG, "停止白噪音失败", e)
            }
            
            // 缓存上下文
            if (appContext == null) appContext = context

            // 请求音频焦点
            if (!hasAudioFocus && !requestAudioFocus()) {
                Logger.w(TAG, "无法获取音频焦点，取消播放 audioId=$audioId")
                return
            }
            
            // 缓存 URI
            audioUriCache[audioId] = audioUri.toString()
            
            // 如果播放列表为空或不包含此音频，更新播放列表
            if (!_playlist.value.contains(audioId)) {
                val newList = _playlist.value + audioId
                _playlist.value = newList
                rebuildShuffleOrder()
            }
            
            // 更新当前播放索引
            currentPlayIndex = _playlist.value.indexOf(audioId)
            
            // 加载保存的音量
            if (!volumeSettings.containsKey(audioId)) {
                val savedVolume = org.xmsleep.app.preferences.PreferencesManager.getLocalAudioVolume(
                    context,
                    audioId,
                    _currentVolume.value
                )
                volumeSettings[audioId] = savedVolume
            }
            
            // 立即更新状态
            playingStates[audioId] = true
            updatePlayingAudioIds()
            
            // 停止之前播放的音频
            val previousAudioId = mediaPlayers.keys.firstOrNull { it != audioId }
            if (previousAudioId != null) {
                stopAudio(previousAudioId)
            }
            
            // 创建新的 MediaPlayer
            val shouldLoop = _playMode.value == PlayMode.REPEAT_ONE
            val mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(context, audioUri)
                    isLooping = shouldLoop
                    val volume = volumeSettings[audioId] ?: _currentVolume.value
                    setVolume(volume, volume)
                    
                    setOnPreparedListener {
                        try {
                            // 恢复播放位置
                            val savedPosition = org.xmsleep.app.preferences.PreferencesManager.getLocalAudioPosition(context, audioId)
                            if (savedPosition > 0) {
                                seekTo(savedPosition)
                                Logger.d(TAG, "恢复播放位置: audioId=$audioId, position=$savedPosition")
                            }
                            start()
                            Logger.d(TAG, "音频准备完成并开始播放: $audioId, 模式: ${_playMode.value}")
                        } catch (e: Exception) {
                            Logger.e(TAG, "启动播放失败: audioId=$audioId", e)
                            playingStates.remove(audioId)
                            updatePlayingAudioIds()
                            onError("启动播放失败")
                        }
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Logger.e(TAG, "播放错误: audioId=$audioId, what=$what, extra=$extra")
                        onError("播放失败")
                        playingStates.remove(audioId)
                        mediaPlayers.remove(audioId)
                        updatePlayingAudioIds()
                        true
                    }
                    
                    setOnCompletionListener {
                        Logger.d(TAG, "音频播放完成: $audioId, 模式: ${_playMode.value}")
                        onAudioCompleted(audioId)
                    }
                    
                    prepareAsync()
                    Logger.d(TAG, "开始准备音频: $audioId（异步）")
                    
                } catch (e: Exception) {
                    Logger.e(TAG, "设置音频源失败: audioId=$audioId", e)
                    playingStates.remove(audioId)
                    updatePlayingAudioIds()
                    onError("设置音频源失败: ${e.message}")
                    throw e
                }
            }
            
            mediaPlayers[audioId] = mediaPlayer
            
        } catch (e: Exception) {
            Logger.e(TAG, "播放失败: audioId=$audioId", e)
            onError("播放失败: ${e.message}")
            playingStates.remove(audioId)
            mediaPlayers.remove(audioId)
            updatePlayingAudioIds()
        }
    }
    
    /**
     * 音频播放完成回调 — 根据播放模式决定下一首
     */
    private fun onAudioCompleted(audioId: Long) {
        when (_playMode.value) {
            PlayMode.REPEAT_ONE -> {
                // isLooping = true 时不会触发此回调，但作为安全措施
                Logger.d(TAG, "单曲循环: 重新播放 $audioId")
                val uri = audioUriCache[audioId]?.let { Uri.parse(it) } ?: return
                stopAudio(audioId)
                playAudio(appContext ?: return, audioId, uri, {})
            }
            PlayMode.SEQUENTIAL -> {
                playNext()
            }
            PlayMode.SHUFFLE -> {
                playNext()
            }
        }
    }
    
    /**
     * 播放下一首
     */
    private fun playNext() {
        val list = _playlist.value
        if (list.isEmpty()) return
        
        // 如果当前没有播放，播放第一首
        if (currentPlayIndex < 0 || currentPlayIndex >= list.size) {
            val firstId = list[0]
            val firstUri = audioUriCache[firstId]?.let { Uri.parse(it) } ?: return
            stopAllAudios()
            playAudio(appContext ?: return, firstId, firstUri, {})
            return
        }
        
        val nextIndex = when (_playMode.value) {
            PlayMode.SHUFFLE -> {
                val currentShufflePos = shuffleOrder.indexOf(currentPlayIndex)
                val nextShufflePos = (currentShufflePos + 1) % shuffleOrder.size
                shuffleOrder[nextShufflePos]
            }
            PlayMode.SEQUENTIAL -> {
                val next = currentPlayIndex + 1
                if (next >= list.size) {
                    Logger.d(TAG, "顺序播放：已到末尾，停止")
                    stopAllAudios()
                    return
                }
                next
            }
            PlayMode.REPEAT_ONE -> currentPlayIndex
        }
        
        if (nextIndex < 0 || nextIndex >= list.size) return
        
        val nextAudioId = list[nextIndex]
        val nextUri = audioUriCache[nextAudioId]?.let { Uri.parse(it) } ?: return
        
        stopAllAudios()
        
        Logger.d(TAG, "播放下一首: index=$nextIndex, audioId=$nextAudioId")
        playAudio(appContext ?: return, nextAudioId, nextUri, {})
    }
    
    /**
     * 停止指定音频
     */
    fun stopAudio(audioId: Long) {
        try {
            val mediaPlayer = mediaPlayers[audioId]
            // 保存播放位置
            mediaPlayer?.let {
                try {
                    if (it.isPlaying) {
                        val position = it.currentPosition
                        org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioPosition(appContext ?: return, audioId, position)
                        Logger.d(TAG, "保存播放位置: audioId=$audioId, position=$position")
                    }
                } catch (_: Exception) {}
            }
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayers.remove(audioId)
            playingStates.remove(audioId)
            volumeSettings.remove(audioId)
            duckedAudioIds.remove(audioId)
            updatePlayingAudioIds()
            Logger.d(TAG, "停止播放音频: $audioId")

            // 没有其他音频播放时释放音频焦点
            if (mediaPlayers.isEmpty()) {
                abandonAudioFocus()
            }

            try {
                org.xmsleep.app.audio.AudioManager.getInstance().saveRecentPlayingSounds()
            } catch (e: Exception) {
                Logger.e(TAG, "保存最近播放记录失败: ${e.message}")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "停止失败: audioId=$audioId", e)
        }
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 淡出并停止所有本地音频文件（用于定时器到点平滑停止）
     * 只渐变播放器音量，不写入偏好设置。
     * @param durationMs 淡出时长（默认 5 秒）
     */
    fun fadeOutAndStopAll(durationMs: Long = 5_000L) {
        // MediaPlayer 实例最好在主线程访问，淡出协程切到 Main dispatcher
        scope.launch(Dispatchers.Main) {
            try {
                val playing = mediaPlayers.entries.filter { playingStates[it.key] == true }
                if (playing.isEmpty()) {
                    stopAllAudios()
                    return@launch
                }
                val originals = playing.associate { (audioId, _) ->
                    audioId to (volumeSettings[audioId] ?: _currentVolume.value)
                }
                val steps = 60L
                val stepMs = (durationMs / steps).coerceAtLeast(50L)
                for (i in 1..steps) {
                    val factor = 1f - i.toFloat() / steps.toFloat()
                    playing.forEach { (audioId, mp) ->
                        val orig = originals[audioId] ?: _currentVolume.value
                        try {
                            val v = (orig * factor).coerceIn(0f, 1f)
                            mp.setVolume(v, v)
                        } catch (e: Exception) {
                            Logger.e(TAG, "淡出音频音量失败: audioId=$audioId", e)
                        }
                    }
                    delay(stepMs)
                }
                stopAllAudios()
                Logger.d(TAG, "淡出完成，已停止所有音频")
            } catch (e: Exception) {
                Logger.e(TAG, "淡出所有音频时发生错误: ${e.message}", e)
                stopAllAudios()
            }
        }
    }

    /**
     * 停止所有音频
     */
    fun stopAllAudios() {
        try {
            val audioIds = mediaPlayers.keys.toList()
            audioIds.forEach { audioId ->
                stopAudio(audioId)
            }
            Logger.d(TAG, "停止所有音频")
        } catch (e: Exception) {
            Logger.e(TAG, "停止所有音频失败", e)
        }
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        val context = appContext ?: return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? SystemAudioManager ?: return false
        return try {
            audioFocusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            hasAudioFocus = result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
            hasAudioFocus
        } catch (e: Exception) {
            Logger.e(TAG, "请求音频焦点失败", e)
            false
        }
    }

    /**
     * 释放音频焦点
     */
    private fun abandonAudioFocus() {
        val context = appContext ?: return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? SystemAudioManager ?: return
        try {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
            audioFocusRequest = null
            hasAudioFocus = false
        } catch (e: Exception) {
            Logger.e(TAG, "释放音频焦点失败", e)
        }
    }
    
    /**
     * 设置指定音频的音量
     */
    fun setVolume(audioId: Long, volume: Float) {
        val coercedVolume = volume.coerceIn(0f, 1f)
        volumeSettings[audioId] = coercedVolume
        mediaPlayers[audioId]?.setVolume(coercedVolume, coercedVolume)
        
        try {
            val context = org.xmsleep.app.audio.AudioManager.getInstance().applicationContext
            context?.let {
                org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioVolume(
                    it,
                    audioId,
                    coercedVolume
                )
            }
        } catch (e: Exception) {
            Logger.e(TAG, "保存音频音量失败: audioId=$audioId", e)
        }
        
        Logger.d(TAG, "设置音频音量: audioId=$audioId, volume=$coercedVolume")
    }
    
    /**
     * 获取指定音频的音量
     */
    fun getVolume(audioId: Long): Float {
        return volumeSettings[audioId] ?: _currentVolume.value
    }
    
    /**
     * 检查指定音频是否正在播放
     */
    fun isAudioPlaying(audioId: Long): Boolean {
        return try {
            mediaPlayers[audioId]?.isPlaying == true
        } catch (e: Exception) {
            Logger.e(TAG, "isAudioPlaying 检查失败: audioId=$audioId", e)
            false
        }
    }
    
    /**
     * 检查是否有活跃的音频（播放中或暂停中）
     */
    fun hasActiveAudio(): Boolean {
        return mediaPlayers.isNotEmpty()
    }
    
    /**
     * 获取指定音频的当前播放位置和总时长
     */
    fun getAudioProgress(audioId: Long): Pair<Int, Int>? {
        return try {
            val mediaPlayer = mediaPlayers[audioId]
            if (mediaPlayer != null) {
                Pair(mediaPlayer.currentPosition, mediaPlayer.duration)
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.e(TAG, "获取播放进度失败: audioId=$audioId", e)
            null
        }
    }

    /**
     * 跳转到指定播放位置
     */
    fun seekTo(audioId: Long, positionMs: Int) {
        try {
            val mediaPlayer = mediaPlayers[audioId]
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.seekTo(positionMs.coerceIn(0, it.duration))
                    Logger.d(TAG, "跳转到位置: audioId=$audioId, position=$positionMs")
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "跳转失败: audioId=$audioId, position=$positionMs", e)
        }
    }

    /**
     * 更新正在播放的音频ID列表
     */
    private fun updatePlayingAudioIds() {
        val playingIds = playingStates.filter { it.value }.keys.toSet()
        _playingAudioIds.value = playingIds
    }
}
