package org.xmsleep.app.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import org.xmsleep.app.audio.AggregatePlayer
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.utils.Logger
import java.util.concurrent.TimeUnit

/**
 * 音乐播放前台服务
 * 负责在通知栏显示播放控制、倒计时信息，以及 MediaSession 集成
 */
class MusicService : Service() {
    
    private val TAG = "MusicService"
    private val binder = MusicServiceBinder()
    
    private var isPlaying = false
    private var playingSoundsCount = 0
    private var timeLeftText: String? = null
    
    // 保存最后一次播放的音频状态，用于暂停/恢复
    private val lastPlayingLocalSounds = mutableSetOf<AudioManager.Sound>()
    private val lastPlayingRemoteSoundIds = mutableSetOf<String>()
    
    // 恢复播放标志：恢复期间不要更新保存的播放列表
    private var isRestoring = false
    
    // 标志位：是否正在停止服务（避免在停止时被重新启动）
    private var isStopping = false
    
    private val audioManager by lazy { AudioManager.getInstance() }
    private val timerManager by lazy { TimerManager.getInstance() }
    
    // MediaSession 集成
    private val aggregatePlayer = AggregatePlayer()
    private var mediaSession: MediaSession? = null
    
    // 定时器监听器
    private val timerListener = object : TimerManager.TimerListener {
        override fun onTimerTick(timeLeftMillis: Long) {
            timeLeftText = formatTime(timeLeftMillis)
            updateNotification()
        }
        
        override fun onTimerFinished() {
            // 倒计时结束，停止所有音频播放
            audioManager.stopAllSounds()
            timeLeftText = null
            updateNotification()
            // 停止服务
            stopForeground(true)
            stopSelf()
        }
        
        override fun onTimerCancelled() {
            // 倒计时被取消，只清除显示，不停止音频
            timeLeftText = null
            updateNotification()
        }
    }
    
    inner class MusicServiceBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // 初始化 MediaSession
        mediaSession = MediaSession.Builder(this, aggregatePlayer)
            .setCallback(object : MediaSession.Callback {
                override fun onPlayerCommandRequest(
                    session: MediaSession,
                    controllerInfo: MediaSession.ControllerInfo,
                    playerCommand: Int
                ): Int {
                    when (playerCommand) {
                        Player.COMMAND_PLAY_PAUSE -> handlePlayPause()
                        Player.COMMAND_STOP -> handleStop()
                    }
                    return SessionResult.RESULT_SUCCESS
                }
            })
            .build()
        
        // 注册定时器监听器
        timerManager.addListener(timerListener)
        
        // 启动前台服务
        startForegroundService()
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            NotificationHelper.ACTION_PLAY_PAUSE -> {
                handlePlayPause()
            }
            NotificationHelper.ACTION_STOP -> {
                handleStop()
            }
            else -> {
                // 首次启动服务
                startForegroundService()
            }
        }
        return START_STICKY
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // 释放 MediaSession
        mediaSession?.release()
        mediaSession = null
        
        // 移除定时器监听器
        timerManager.removeListener(timerListener)
        
        // 重置停止标志
        isStopping = false
    }
    
    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        val notification = NotificationHelper.buildNotification(
            context = this,
            isPlaying = isPlaying,
            playingSoundsCount = playingSoundsCount,
            timeLeftText = timeLeftText,
            mediaSession = mediaSession
        )
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        val notification = NotificationHelper.buildNotification(
            context = this,
            isPlaying = isPlaying,
            playingSoundsCount = playingSoundsCount,
            timeLeftText = timeLeftText,
            mediaSession = mediaSession
        )
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }
    
    /**
     * 更新播放状态
     */
    fun updatePlayingState(playing: Boolean, soundsCount: Int, soundDescriptions: List<String> = emptyList()) {
        // 如果正在停止服务，不再处理任何更新
        if (isStopping) {
            return
        }
        
        isPlaying = playing
        playingSoundsCount = soundsCount
        
        // 更新 MediaSession 播放状态
        aggregatePlayer.onPlaybackChanged(playing, soundDescriptions)
        
        // 关键修复：恢复期间不要重新保存播放列表，避免覆盖之前保存的列表
        if (isRestoring) {
            updateNotification()
            return
        }
        
        // 如果有音频播放，保存当前播放列表
        if (playing && soundsCount > 0) {
            lastPlayingLocalSounds.clear()
            lastPlayingLocalSounds.addAll(audioManager.getPlayingSounds())
            
            lastPlayingRemoteSoundIds.clear()
            lastPlayingRemoteSoundIds.addAll(audioManager.getPlayingRemoteSoundIds())
        }
        
        // 如果有倒计时，更新倒计时文本
        if (timerManager.isTimerActive.value) {
            val timeLeft = timerManager.getTimeLeftMillis()
            if (timeLeft > 0) {
                timeLeftText = formatTime(timeLeft)
                // 如果倒计时处于暂停状态，在时间后加上"已暂停"标记
                if (timerManager.isTimerPaused.value) {
                    timeLeftText = "$timeLeftText (已暂停)"
                }
            }
        }
        
        updateNotification()
    }
    
    /**
     * 处理播放/暂停按钮点击
     */
    private fun handlePlayPause() {
        if (isPlaying) {
            // 当前正在播放，执行暂停
            // 关键：在调用 pauseAllSounds() 之前先保存播放列表
            lastPlayingLocalSounds.clear()
            lastPlayingLocalSounds.addAll(audioManager.getPlayingSounds())
            
            lastPlayingRemoteSoundIds.clear()
            lastPlayingRemoteSoundIds.addAll(audioManager.getPlayingRemoteSoundIds())
            
            audioManager.pauseAllSounds()
            
            // 暂停倒计时
            if (timerManager.isTimerActive.value) {
                timerManager.pauseTimer()
            }
            
            isPlaying = false
        } else {
            // 当前已暂停，恢复上次播放的音频
            if (lastPlayingLocalSounds.isEmpty() && lastPlayingRemoteSoundIds.isEmpty()) {
                // 没有可恢复的音频，关闭服务
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }
            
            // 关键修复：设置恢复标志，防止恢复过程中重新保存播放列表
            isRestoring = true
            
            try {
                // 恢复本地音频（使用副本避免 ConcurrentModificationException）
                val soundsToRestore = lastPlayingLocalSounds.toList()
                soundsToRestore.forEach { sound ->
                    try {
                        audioManager.playSound(applicationContext ?: return, sound)
                    } catch (e: Exception) {
                        Logger.e(TAG, "恢复本地播放 $sound 失败: ${e.message}")
                    }
                }
                
                // 恢复远程音频（使用缓存的元数据和URI）
                val remoteSoundsToRestore = lastPlayingRemoteSoundIds.toList()
                remoteSoundsToRestore.forEach { soundId ->
                    try {
                        // 从 AudioManager 获取缓存的元数据和URI
                        val metadataAndUri = audioManager.getRemoteMetadata(soundId)
                        if (metadataAndUri != null) {
                            val (metadata, uri) = metadataAndUri
                            audioManager.playRemoteSound(applicationContext ?: return, metadata, uri)
                        } else {
                            Logger.w(TAG, "无法恢复远程音频 $soundId：元数据不存在")
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, "恢复远程播放 $soundId 失败: ${e.message}")
                    }
                }
                
                // 恢复倒计时
                if (timerManager.isTimerActive.value && timerManager.isTimerPaused.value) {
                    timerManager.resumeTimer()
                }
                
                isPlaying = true
            } finally {
                // 恢复完成后，清除恢复标志
                isRestoring = false
            }
        }
        
        // 更新通知
        updateNotification()
    }
    
    /**
     * 处理停止按钮点击（直接退出应用）
     */
    private fun handleStop() {
        // 停止所有音频
        audioManager.stopAllSounds()
        
        // 取消倒计时
        timerManager.cancelTimer()
        
        // 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        
        // 退出应用
        android.os.Process.killProcess(android.os.Process.myPid())
    }
    
    /**
     * 格式化时间（毫秒转为可读格式）
     */
    private fun formatTime(millis: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(millis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%02d:%02d", minutes, seconds)
        }
    }
}
