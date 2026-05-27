package org.xmsleep.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import androidx.media3.session.MediaSession
import org.xmsleep.app.MainActivity
import org.xmsleep.app.R

object NotificationHelper {

    private const val CHANNEL_ID = "music_playback_channel"
    private const val CHANNEL_NAME = "音乐播放"
    private const val CHANNEL_DESCRIPTION = "显示正在播放的音乐和控制按钮"

    const val NOTIFICATION_ID = 1001

    const val ACTION_PLAY_PAUSE = "org.xmsleep.app.ACTION_PLAY_PAUSE"
    const val ACTION_STOP = "org.xmsleep.app.ACTION_STOP"
    const val ACTION_NOTIFICATION_DISMISSED = "org.xmsleep.app.ACTION_NOTIFICATION_DISMISSED"

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

    fun buildNotification(
        context: Context,
        isPlaying: Boolean,
        playingSoundsCount: Int,
        timeLeftText: String? = null,
        mediaSession: MediaSession? = null
    ): Notification {
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, MusicService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(context, MusicService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(
            context, 2, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = buildString {
            append("XMSLEEP")
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

        val playPauseIcon = if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
        val playPauseLabel = if (isPlaying) "暂停" else "播放"

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setShowWhen(false)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setCategory(Notification.CATEGORY_SERVICE)

        if (mediaSession != null) {
            val platformToken = mediaSession.getPlatformToken()
            if (platformToken is android.media.session.MediaSession.Token) {
                builder.setStyle(
                    Notification.MediaStyle()
                        .setMediaSession(platformToken)
                        .setShowActionsInCompactView(0, 1)
                )
            }
        }

        builder.addAction(
            Notification.Action.Builder(Icon.createWithResource(context, playPauseIcon), playPauseLabel, playPausePendingIntent).build()
        )
        builder.addAction(
            Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_stop), "退出", stopPendingIntent).build()
        )

        return builder.build()
    }
}
