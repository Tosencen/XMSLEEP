package org.xmsleep.app.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Binder
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import org.xmsleep.app.MainActivity
import org.xmsleep.app.R
import org.xmsleep.app.audio.AggregatePlayer
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.meditation.MeditationPlayerManager
import org.xmsleep.app.timer.TimerManager
import org.xmsleep.app.utils.Logger
import java.util.Collections
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
    
    // 保存最后一次播放的音频状态，用于暂停/恢复（线程安全）
    private val lastPlayingLocalSounds: MutableSet<AudioManager.Sound> = Collections.synchronizedSet(mutableSetOf())
    private val lastPlayingRemoteSoundIds: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
    
    // 恢复播放标志：恢复期间不要更新保存的播放列表
    private var isRestoring = false
    
    // 暂停标志：暂停期间（逐个暂停触发回调时）不要覆盖保存的播放列表
    private var isPausing = false
    
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
        
        override fun onTimerFinished(durationMinutes: Int) {
            // 定时到点：淡出 5 秒后停止，避免突然中断
            audioManager.fadeOutAndStopAll()
            timeLeftText = null
            updateNotification()
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
        
        // 聚合播放器的命令回调
        aggregatePlayer.onPlayPauseRequested = ::handlePlayPause
        aggregatePlayer.onStopRequested = ::handleStop

        // 设置媒体卡片封面为 App logo
        // 注意：ic_launcher 是 adaptive icon（XML），BitmapFactory.decodeResource 无法解码，
        // 需通过 Drawable 绘制成 Bitmap。
        try {
            val logo = ContextCompat.getDrawable(this, R.mipmap.ic_launcher)
            if (logo != null) {
                val size = 192
                val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                logo.setBounds(0, 0, size, size)
                logo.draw(canvas)
                val stream = java.io.ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                aggregatePlayer.artworkData = stream.toByteArray()
                stream.close()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "设置媒体封面失败: ${e.message}")
        }
        
        // 初始化 MediaSession（设置 SessionActivity，点击系统媒体卡片时打开 App）
        mediaSession = MediaSession.Builder(this, aggregatePlayer)
            .setSessionActivity(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
        
        // 注册定时器监听器
        timerManager.addListener(timerListener)
        
        // 查询当前实际播放状态，确保初始通知正确
        val localAudioPlayer = org.xmsleep.app.audio.LocalAudioPlayer.getInstance()
        val localAudioCount = localAudioPlayer.playingAudioIds.value.size
        isPlaying = audioManager.hasAnyPlayingSounds() || localAudioCount > 0
        playingSoundsCount = audioManager.getPlayingSounds().size + audioManager.getPlayingRemoteSoundIds().size + localAudioCount
        
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
        // 用 START_NOT_STICKY 而非 START_STICKY：
        // START_STICKY 在服务被杀后会以 null intent 重启，走到上面的 else 分支重建前台通知，
        // 表现为用户点了停止、通知却"阴魂不散"；且 null intent 无法恢复播放内容，恢复也是空的。
        // 播放状态由 MusicServiceManager / 定时器自行管理，无需系统代为重启。
        return START_NOT_STICKY
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
        
        // 更新 MediaSession 播放状态（使用 string resource 拼接副标题，跟随系统语言）
        aggregatePlayer.onPlaybackChanged(playing, formatSubtitle(soundDescriptions))
        
        // 关键修复：恢复期间不要重新保存播放列表，避免覆盖之前保存的列表
        if (isRestoring) {
            updateNotification()
            return
        }
        
        // 如果有音频播放，保存当前播放列表（暂停中不覆盖，避免 pauseAllSounds 回调串扰）
        if (!isPausing && playing && soundsCount > 0) {
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
                // 如果倒计时处于暂停状态，在时间后加上本地化"已暂停"标记
                if (timerManager.isTimerPaused.value) {
                    val lc = LanguageManager.createLocalizedContext(this, LanguageManager.getCurrentLanguage(this))
                    timeLeftText = "$timeLeftText ${lc.getString(R.string.timer_paused_suffix)}"
                }
            }
        }
        
        updateNotification()
    }
    
    /**
     * 处理播放/暂停按钮点击
     * 同时控制声音/电台（AudioManager）与冥想播放器（MeditationPlayerManager）
     */
    private fun handlePlayPause() {
        val meditation = MeditationPlayerManager.getInstance()
        val meditationPlaying = meditation.isPlaying.value
        val meditationPaused = meditation.isPaused.value
        val localAudioPlayer = org.xmsleep.app.audio.LocalAudioPlayer.getInstance()

        if (isPlaying || meditationPlaying) {
            // 当前正在暂停
            audioManager.setRadioWasPlaying(audioManager.radioPlaying.value)
            lastPlayingLocalSounds.clear()
            lastPlayingLocalSounds.addAll(audioManager.getPlayingSounds())

            lastPlayingRemoteSoundIds.clear()
            lastPlayingRemoteSoundIds.addAll(audioManager.getPlayingRemoteSoundIds())

            isPausing = true
            try {
                // 暂停本地音频文件（保存播放位置）
                localAudioPlayer.pauseAll()

                audioManager.pauseAllSounds()

                // 暂停冥想（冥想播放器状态变化会自动同步回通知）
                if (meditationPlaying) {
                    meditation.pause()
                }

                // 暂停倒计时
                if (timerManager.isTimerActive.value) {
                    timerManager.pauseTimer()
                }
            } finally {
                isPausing = false
            }

            isPlaying = false
        } else {
            // 当前已暂停，恢复上次播放的音频
            if (lastPlayingLocalSounds.isEmpty() && lastPlayingRemoteSoundIds.isEmpty() && !audioManager.isRadioWasPlaying() && !meditationPaused && !localAudioPlayer.hasPausedAudios()) {
                // 没有可恢复的音频，关闭服务
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            var restoredAnything = false

            // 关键修复：设置恢复标志，防止恢复过程中重新保存播放列表
            isRestoring = true

            try {
                // ① 优先恢复本地音频文件（互斥优先级最高，避免被白噪音/远程音频抢占）
                if (localAudioPlayer.resumeAll(applicationContext)) {
                    restoredAnything = true
                }

                // ② 恢复电台
                if (audioManager.isRadioWasPlaying()) {
                    audioManager.setRadioWasPlaying(false)
                    audioManager.resumeRadio()
                    restoredAnything = true
                }

                // ③ 恢复本地白噪音（副本避免 ConcurrentModificationException）
                val soundsToRestore = lastPlayingLocalSounds.toList()
                soundsToRestore.forEach { sound ->
                    try {
                        audioManager.playSound(applicationContext ?: return, sound)
                        restoredAnything = true
                    } catch (e: Exception) {
                        Logger.e(TAG, "恢复本地播放 $sound 失败: ${e.message}")
                    }
                }

                // ④ 恢复远程音频（使用缓存的元数据和URI）
                val remoteSoundsToRestore = lastPlayingRemoteSoundIds.toList()
                remoteSoundsToRestore.forEach { soundId ->
                    try {
                        val metadataAndUri = audioManager.getRemoteMetadata(soundId)
                        if (metadataAndUri != null) {
                            val (metadata, uri) = metadataAndUri
                            audioManager.playRemoteSound(applicationContext ?: return, metadata, uri)
                            restoredAnything = true
                        } else {
                            Logger.w(TAG, "无法恢复远程音频 $soundId：元数据不存在")
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, "恢复远程播放 $soundId 失败: ${e.message}")
                    }
                }

                // ⑤ 恢复冥想（仅当冥想处于暂停状态时）
                if (meditationPaused) {
                    if (meditation.resume(applicationContext)) {
                        restoredAnything = true
                    }
                }

                // ⑥ 恢复倒计时
                if (timerManager.isTimerActive.value && timerManager.isTimerPaused.value) {
                    timerManager.resumeTimer()
                }

                isPlaying = restoredAnything
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
        
        // 停止本地音频文件（LocalAudioPlayer 单独管理，通知栏停止必须一并停止）
        org.xmsleep.app.audio.LocalAudioPlayer.getInstance().stopAllAudios()
        
        // 取消倒计时
        timerManager.cancelTimer()
        
        // 清理保存的播放列表，防止服务意外重启时恢复过期音频
        lastPlayingLocalSounds.clear()
        lastPlayingRemoteSoundIds.clear()
        audioManager.setRadioWasPlaying(false)
        
        // 停止前台服务
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    /**
     * 根据当前 locale 拼接 MediaSession 副标题。
     * - 0 个：空
     * - 1~3 个：使用 " + " 拼接
     * - 超过 3 个：前 2 个 + 本地化的「其他 N 个」
     */
    private fun formatSubtitle(descriptions: List<String>): String {
        return when {
            descriptions.isEmpty() -> ""
            descriptions.size <= 3 -> descriptions.joinToString(" + ")
            else -> {
                val first = descriptions.take(2).joinToString(" + ")
                val othersCount = descriptions.size - 2
                val others = resources.getQuantityString(R.plurals.aggregate_others_format, othersCount, othersCount)
                "$first + $others"
            }
        }
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
