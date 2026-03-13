package org.xmsleep.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import org.xmsleep.app.MainActivity
import org.xmsleep.app.R

class BreathingService : Service() {
    
    companion object {
        const val CHANNEL_ID = "breathing_service_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "stop_breathing"
        
        var isRunning = false
            private set
    }
    
    private var mediaPlayer: MediaPlayer? = null
    private var serviceScope: CoroutineScope? = null
    private var job: Job? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopBreathing()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                // 启动服务
                val totalTime = intent?.getLongExtra("total_time", 5 * 60 * 1000L) ?: 5 * 60 * 1000L
                startForeground(NOTIFICATION_ID, createNotification(totalTime))
                startBreathing(totalTime)
            }
        }
        return START_STICKY
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "呼吸练习",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "呼吸练习后台播放"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(timeLeft: Long): android.app.Notification {
        val stopIntent = Intent(this, BreathingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("呼吸练习中")
            .setContentText("剩余时间：$timeText")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(mainPendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "停止", stopPendingIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun startBreathing(totalTime: Long) {
        isRunning = true
        serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        
        var timeLeft = totalTime
        
        job = serviceScope?.launch {
            while (timeLeft > 0 && isRunning) {
                delay(1000)
                timeLeft -= 1000
                
                // 更新通知
                updateNotification(timeLeft)
            }
            
            // 时间到
            if (timeLeft <= 0) {
                stopBreathing()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }
    
    private fun updateNotification(timeLeft: Long) {
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(timeLeft))
    }
    
    private fun stopBreathing() {
        isRunning = false
        job?.cancel()
        serviceScope?.cancel()
        mediaPlayer?.release()
        mediaPlayer = null
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        stopBreathing()
    }
}
