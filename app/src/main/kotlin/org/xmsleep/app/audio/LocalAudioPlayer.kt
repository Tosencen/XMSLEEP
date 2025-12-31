package org.xmsleep.app.audio

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * 本地音频播放器单例
 * 支持同时播放多个本地音频（最多10个），类似远程声音的混音功能
 */
class LocalAudioPlayer private constructor() {
    
    private val TAG = "LocalAudioPlayer"
    
    companion object {
        private const val MAX_CONCURRENT_AUDIOS = 10 // 最多同时播放10个音频
        
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
    
    // 播放顺序队列，用于限制最多同时播放的音频数量
    private val playingQueue = java.util.concurrent.ConcurrentLinkedQueue<Long>()
    
    // 当前正在播放的音频ID列表（用于UI显示）
    private val _playingAudioIds = MutableStateFlow<Set<Long>>(emptySet())
    val playingAudioIds: StateFlow<Set<Long>> = _playingAudioIds.asStateFlow()
    
    // 兼容旧API：返回第一个正在播放的音频ID
    val playingAudioId: StateFlow<Long?> = MutableStateFlow(null)
    
    // 默认音量（50%）
    private var _currentVolume = MutableStateFlow(0.5f)
    val currentVolume: StateFlow<Float> = _currentVolume.asStateFlow()
    
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
            
            // 检查是否达到最大并发数
            if (playingQueue.size >= MAX_CONCURRENT_AUDIOS) {
                // 移除最早播放的音频
                val oldestAudioId = playingQueue.poll()
                if (oldestAudioId != null) {
                    stopAudio(oldestAudioId)
                    Log.d(TAG, "达到最大并发数，停止最早的音频: $oldestAudioId")
                }
            }
            
            // 缓存 URI，用于恢复播放
            audioUriCache[audioId] = audioUri.toString()
            
            // 加载保存的音量（如果没有保存则使用默认值）
            if (!volumeSettings.containsKey(audioId)) {
                val savedVolume = org.xmsleep.app.preferences.PreferencesManager.getLocalAudioVolume(
                    context,
                    audioId,
                    _currentVolume.value
                )
                volumeSettings[audioId] = savedVolume
            }
            
            // 立即更新状态，提供即时反馈
            playingStates[audioId] = true
            playingQueue.offer(audioId)
            updatePlayingAudioIds()
            
            // 创建新的 MediaPlayer
            val mediaPlayer = MediaPlayer().apply {
                try {
                    setDataSource(context, audioUri)
                    isLooping = true
                    val volume = volumeSettings[audioId] ?: _currentVolume.value
                    setVolume(volume, volume)
                    
                    setOnPreparedListener {
                        try {
                            start()
                            Log.d(TAG, "音频准备完成并开始播放: $audioId, 当前播放数: ${playingQueue.size}")
                        } catch (e: Exception) {
                            Log.e(TAG, "启动播放失败: audioId=$audioId", e)
                            playingStates.remove(audioId)
                            playingQueue.remove(audioId)
                            updatePlayingAudioIds()
                            onError("启动播放失败")
                        }
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        Log.e(TAG, "播放错误: audioId=$audioId, what=$what, extra=$extra")
                        onError("播放失败")
                        playingStates.remove(audioId)
                        mediaPlayers.remove(audioId)
                        playingQueue.remove(audioId)
                        updatePlayingAudioIds()
                        true
                    }
                    
                    setOnCompletionListener {
                        // 循环播放，不应该触发这个回调
                        Log.d(TAG, "音频播放完成（不应该发生）: $audioId")
                    }
                    
                    // 使用异步准备，避免阻塞UI
                    prepareAsync()
                    Log.d(TAG, "开始准备音频: $audioId（异步）")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "设置音频源失败: audioId=$audioId", e)
                    playingStates.remove(audioId)
                    playingQueue.remove(audioId)
                    updatePlayingAudioIds()
                    onError("设置音频源失败: ${e.message}")
                    throw e
                }
            }
            
            mediaPlayers[audioId] = mediaPlayer
            
        } catch (e: Exception) {
            Log.e(TAG, "播放失败: audioId=$audioId", e)
            onError("播放失败: ${e.message}")
            playingStates.remove(audioId)
            mediaPlayers.remove(audioId)
            playingQueue.remove(audioId)
            updatePlayingAudioIds()
        }
    }
    
    /**
     * 暂停音频（实际上是停止，因为本地音频不需要暂停功能）
     */
    fun pauseAudio() {
        // 停止所有音频
        stopAllAudios()
    }
    
    /**
     * 停止指定音频
     */
    fun stopAudio(audioId: Long) {
        try {
            val mediaPlayer = mediaPlayers.remove(audioId)
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            playingStates.remove(audioId)
            volumeSettings.remove(audioId)
            playingQueue.remove(audioId)
            updatePlayingAudioIds()
            Log.d(TAG, "停止播放音频: $audioId, 剩余播放数: ${playingQueue.size}")
            
            // 关键修复：单个本地音频文件停止后，保存当前正在播放的音频列表
            // 这样可以确保最近播放只包含当前正在播放的音频
            try {
                org.xmsleep.app.audio.AudioManager.getInstance().saveRecentPlayingSounds()
            } catch (e: Exception) {
                Log.e(TAG, "保存最近播放记录失败: ${e.message}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "停止失败: audioId=$audioId", e)
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
            Log.d(TAG, "停止所有音频")
        } catch (e: Exception) {
            Log.e(TAG, "停止所有音频失败", e)
        }
    }
    
    /**
     * 设置指定音频的音量
     */
    fun setVolume(audioId: Long, volume: Float) {
        val coercedVolume = volume.coerceIn(0f, 1f)
        volumeSettings[audioId] = coercedVolume
        mediaPlayers[audioId]?.setVolume(coercedVolume, coercedVolume)
        
        // 保存音量到 SharedPreferences
        try {
            // 需要 Context，从 AudioManager 获取
            val context = org.xmsleep.app.audio.AudioManager.getInstance().applicationContext
            context?.let {
                org.xmsleep.app.preferences.PreferencesManager.saveLocalAudioVolume(
                    it,
                    audioId,
                    coercedVolume
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存音频音量失败: audioId=$audioId", e)
        }
        
        Log.d(TAG, "设置音频音量: audioId=$audioId, volume=$coercedVolume")
    }
    
    /**
     * 设置默认音量（用于新播放的音频）
     */
    fun setVolume(volume: Float) {
        _currentVolume.value = volume
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
            val playing = mediaPlayers[audioId]?.isPlaying == true
            playing
        } catch (e: Exception) {
            Log.e(TAG, "isAudioPlaying 检查失败: audioId=$audioId", e)
            false
        }
    }
    
    /**
     * 检查是否有任何音频正在播放
     */
    fun isPlaying(): Boolean {
        return playingQueue.isNotEmpty()
    }
    
    /**
     * 检查是否有活跃的音频（播放中或暂停中）
     */
    fun hasActiveAudio(): Boolean {
        val hasActive = mediaPlayers.isNotEmpty()
        Log.d(TAG, "hasActiveAudio: $hasActive, 播放数: ${mediaPlayers.size}")
        return hasActive
    }
    
    /**
     * 获取当前播放的音频数量
     */
    fun getPlayingCount(): Int {
        return playingQueue.size
    }
    
    /**
     * 获取指定音频的 URI
     */
    fun getAudioUri(audioId: Long): Uri? {
        return audioUriCache[audioId]?.let { Uri.parse(it) }
    }
    
    /**
     * 获取所有正在播放的音频的 URI 映射（audioId -> URI字符串）
     */
    fun getPlayingAudioUris(): Map<Long, String> {
        val playingIds = playingAudioIds.value
        return audioUriCache.filter { (audioId, _) -> playingIds.contains(audioId) }
    }
    
    /**
     * 更新正在播放的音频ID列表
     */
    private fun updatePlayingAudioIds() {
        val playingIds = playingStates.filter { it.value }.keys.toSet()
        _playingAudioIds.value = playingIds
        // 更新兼容API
        (playingAudioId as MutableStateFlow).value = playingIds.firstOrNull()
    }
    
    /**
     * 释放所有资源
     */
    fun release() {
        stopAllAudios()
    }
}
