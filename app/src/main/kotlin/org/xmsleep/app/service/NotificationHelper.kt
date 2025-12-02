package org.xmsleep.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import org.xmsleep.app.MainActivity
import org.xmsleep.app.R

/**
 * 通知助手类
 * 负责创建和管理应用的通知
 */
object NotificationHelper {
    
    private const val CHANNEL_ID = "music_playback_channel"
    private const val CHANNEL_NAME = "音乐播放"
    private const val CHANNEL_DESCRIPTION = "显示正在播放的音乐和控制按钮"
    
    const val NOTIFICATION_ID = 1001
    
    // 通知动作
    const val ACTION_PLAY_PAUSE = "org.xmsleep.app.ACTION_PLAY_PAUSE"
    const val ACTION_STOP = "org.xmsleep.app.ACTION_STOP"
    const val ACTION_NOTIFICATION_DISMISSED = "org.xmsleep.app.ACTION_NOTIFICATION_DISMISSED"
    
    /**
     * 创建通知渠道（仅在 Android 8.0+ 需要）
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 构建通知
     * @param context 上下文
     * @param isPlaying 是否正在播放
     * @param playingSoundsCount 正在播放的音频数量
     * @param timeLeftText 倒计时剩余时间文本（如果有）
     */
    fun buildNotification(
        context: Context,
        isPlaying: Boolean,
        playingSoundsCount: Int,
        timeLeftText: String? = null
    ): Notification {
        createNotificationChannel(context)
        
        // 点击通知打开应用
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 播放/暂停按钮
        val playPauseIntent = Intent(context, MusicService::class.java).apply {
            action = ACTION_PLAY_PAUSE
        }
        val playPausePendingIntent = PendingIntent.getService(
            context,
            0,
            playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 停止按钮
        val stopIntent = Intent(context, MusicService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知内容
        val title = buildString {
            append("XMSLEEP")
            // 倒计时显示在标题右边
            if (!timeLeftText.isNullOrEmpty()) {
                append("  ·  $timeLeftText")
            }
        }
        val statusText = if (isPlaying) "正在播放" else "已暂停"
        val content = buildString {
            append(statusText)
            if (playingSoundsCount > 0) {
                append(" · $playingSoundsCount 个音频")
            }
        }
        
        // 播放/暂停按钮图标和文本
        val playPauseIcon = if (isPlaying) {
            android.R.drawable.ic_media_pause
        } else {
            android.R.drawable.ic_media_play
        }
        val playPauseText = if (isPlaying) "暂停" else "播放"
        
        // 使用自定义布局确保按钮始终显示
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification) // 使用应用图标的矢量版本（纯白色）
            .setContentIntent(contentPendingIntent)
            .setOngoing(true) // 设置为持续通知，不能被滑动清除
            .setSilent(true) // 静默通知，不发出声音
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(playPauseIcon, playPauseText, playPausePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "退出", stopPendingIntent)
        
        // 设置自定义样式，使按钮在折叠状态下也显示（通过设置为DecoratedCustomViewStyle）
        // 关键：使用setStyle并设置自定义视图样式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
        }
        
        return builder.build()
    }
}
