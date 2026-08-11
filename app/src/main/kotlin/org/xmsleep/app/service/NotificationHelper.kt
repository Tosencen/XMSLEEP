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
import org.xmsleep.app.i18n.LanguageManager

object NotificationHelper {

    private const val CHANNEL_ID = "music_playback_channel"

    const val NOTIFICATION_ID = 1001

    const val ACTION_PLAY_PAUSE = "org.xmsleep.app.ACTION_PLAY_PAUSE"
    const val ACTION_STOP = "org.xmsleep.app.ACTION_STOP"
    const val ACTION_NOTIFICATION_DISMISSED = "org.xmsleep.app.ACTION_NOTIFICATION_DISMISSED"

    /**
     * 获取跟随应用语言的 Context（Service 自身不走 Compose 本地化包装）
     */
    private fun localizedContext(context: Context): Context =
        LanguageManager.createLocalizedContext(context, LanguageManager.getCurrentLanguage(context))

    fun createNotificationChannel(context: Context) {
        val lc = localizedContext(context)
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, lc.getString(R.string.music_playback_channel_name), importance).apply {
            description = lc.getString(R.string.music_playback_channel_description)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
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

        val lc = localizedContext(context)
        val title = buildString {
            append("XMSLEEP")
            if (!timeLeftText.isNullOrEmpty()) {
                append("  ·  $timeLeftText")
            }
        }
        val statusText = if (isPlaying) lc.getString(R.string.playing) else lc.getString(R.string.paused)
        val content = buildString {
            append(statusText)
            if (playingSoundsCount > 0) {
                append(" · ").append(lc.resources.getQuantityString(R.plurals.playing_audio_count, playingSoundsCount, playingSoundsCount))
            }
        }

        val playPauseIcon = if (isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
        val playPauseLabel = if (isPlaying) lc.getString(R.string.pause) else lc.getString(R.string.play)

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
            Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_stop), lc.getString(R.string.notification_quit), stopPendingIntent).build()
        )

        return builder.build()
    }
}
