package org.xmsleep.app.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.timer.TimerManager
import java.util.concurrent.TimeUnit

/**
 * 音乐播放前台服务
 * 负责在通知栏显示播放控制和倒计时信息
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
    
    // 定时器监听器
    private val timerListener = object : TimerManager.TimerListener {
        override fun onTimerTick(timeLeftMillis: Long) {
            timeLeftText = formatTime(timeLeftMillis)
            updateNotification()
        }
        
        override fun onTimerFinished() {
            timeLeftText = null
            updateNotification()
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
        Log.d(TAG, "MusicService onCreate")
        
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
        Log.d(TAG, "MusicService onDestroy")
        
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
            timeLeftText = timeLeftText
        )
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        Log.d(TAG, "前台服务已启动")
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification() {
        val notification = NotificationHelper.buildNotification(
            context = this,
            isPlaying = isPlaying,
            playingSoundsCount = playingSoundsCount,
            timeLeftText = timeLeftText
        )
        
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }
    
    /**
     * 更新播放状态
     */
    fun updatePlayingState(playing: Boolean, soundsCount: Int) {
        // 如果正在停止服务，不再处理任何更新
        if (isStopping) {
            Log.d(TAG, "正在停止服务，忽略状态更新")
            return
        }
        
        isPlaying = playing
        playingSoundsCount = soundsCount
        
        // 关键修复：恢复期间不要重新保存播放列表，避免覆盖之前保存的列表
        if (isRestoring) {
            Log.d(TAG, "恢复播放中，跳过保存播放列表")
            updateNotification()
            return
        }
        
        // 如果有音频播放，保存当前播放列表
        if (playing && soundsCount > 0) {
            lastPlayingLocalSounds.clear()
            lastPlayingLocalSounds.addAll(audioManager.getPlayingSounds())
            
            lastPlayingRemoteSoundIds.clear()
            lastPlayingRemoteSoundIds.addAll(audioManager.getPlayingRemoteSoundIds())
            
            Log.d(TAG, "保存播放状态: 本地=${lastPlayingLocalSounds.size}, 远程=${lastPlayingRemoteSoundIds.size}")
        }
        
        // 如果有倒计时，更新倒计时文本
        if (timerManager.isTimerActive.value) {
            val timeLeft = timerManager.getTimeLeftMillis()
            if (timeLeft > 0) {
                timeLeftText = formatTime(timeLeft)
                // 如果倒计时处于暂停状态，在时间后加上“已暂停”标记
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
        Log.d(TAG, "处理播放/暂停按钮点击，当前状态: isPlaying=$isPlaying")
        
        if (isPlaying) {
            // 当前正在播放，执行暂停
            // 关键：在调用 pauseAllSounds() 之前先保存播放列表
            lastPlayingLocalSounds.clear()
            lastPlayingLocalSounds.addAll(audioManager.getPlayingSounds())
            
            lastPlayingRemoteSoundIds.clear()
            lastPlayingRemoteSoundIds.addAll(audioManager.getPlayingRemoteSoundIds())
            
            Log.d(TAG, "保存播放状态: 本地=${lastPlayingLocalSounds.size}, 远程=${lastPlayingRemoteSoundIds.size}")
            Log.d(TAG, "暂停所有音频")
            
            audioManager.pauseAllSounds()
            
            // 暂停倒计时
            if (timerManager.isTimerActive.value) {
                timerManager.pauseTimer()
                Log.d(TAG, "倒计时已暂停")
            }
            
            isPlaying = false
        } else {
            // 当前已暂停，恢复上次播放的音频
            Log.d(TAG, "恢复播放: 本地=${lastPlayingLocalSounds.size}, 远程=${lastPlayingRemoteSoundIds.size}")
            
            if (lastPlayingLocalSounds.isEmpty() && lastPlayingRemoteSoundIds.isEmpty()) {
                // 没有可恢复的音频，关闭服务
                Log.d(TAG, "没有可恢复的音频，关闭服务")
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
                        Log.d(TAG, "恢复本地播放: $sound")
                    } catch (e: Exception) {
                        Log.e(TAG, "恢复本地播放 $sound 失败: ${e.message}")
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
                            Log.d(TAG, "恢复远程播放: $soundId")
                        } else {
                            Log.w(TAG, "无法恢复远程音频 $soundId：元数据不存在")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "恢复远程播放 $soundId 失败: ${e.message}")
                    }
                }
                
                // 恢复倒计时
                if (timerManager.isTimerActive.value && timerManager.isTimerPaused.value) {
                    timerManager.resumeTimer()
                    Log.d(TAG, "倒计时已恢复")
                }
                
                isPlaying = true
            } finally {
                // 恢复完成后，清除恢复标志
                isRestoring = false
                Log.d(TAG, "恢复播放完成")
            }
        }
        
        // 更新通知
        updateNotification()
    }
    
    /**
     * 处理停止按钮点击（直接退出应用）
     */
    private fun handleStop() {
        Log.d(TAG, "处理停止按钮点击 - 退出应用")
        
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
