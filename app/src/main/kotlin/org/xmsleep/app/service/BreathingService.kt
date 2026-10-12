package org.xmsleep.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import kotlinx.coroutines.*
import org.xmsleep.app.MainActivity
import org.xmsleep.app.R
import org.xmsleep.app.i18n.LanguageManager
import org.xmsleep.app.preferences.PreferencesManager

class BreathingService : Service() {

    companion object {
        const val CHANNEL_ID = "breathing_service_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_STOP = "stop_breathing"

        var isRunning = false
            private set
    }

    private var breathPlayer: MediaPlayer? = null
    private var voicePlayer: MediaPlayer? = null
    private var mediaSession: MediaSession? = null
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var serviceScope: CoroutineScope? = null
    private var job: Job? = null

    private var totalTime = 0L
    private var methodInhale = 4
    private var methodHold = 7
    private var methodExhale = 8
    private var methodHoldAfter = 0

    private fun localizedContext(): Context =
        LanguageManager.createLocalizedContext(this, LanguageManager.getCurrentLanguage(this))

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        mediaSession = MediaSession(this, "BreathingService").apply {
            setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS or MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_STOP)
                    .setState(PlaybackState.STATE_PLAYING, 0, 1f)
                    .build()
            )
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopBreathing()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                totalTime = intent?.getLongExtra("total_time", 5 * 60 * 1000L) ?: 5 * 60 * 1000L
                methodInhale = intent?.getIntExtra("inhale", 4) ?: 4
                methodHold = intent?.getIntExtra("hold", 7) ?: 7
                methodExhale = intent?.getIntExtra("exhale", 8) ?: 8
                methodHoldAfter = intent?.getIntExtra("hold_after", 0) ?: 0
                startForeground(NOTIFICATION_ID, createNotification(totalTime))
                startBreathing()
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val lc = localizedContext()
        val channel = NotificationChannel(
            CHANNEL_ID,
            lc.getString(R.string.breathing_exercise),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = lc.getString(R.string.breathing_channel_description)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(timeLeft: Long): android.app.Notification {
        val stopIntent = Intent(this, BreathingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lc = localizedContext()
        val minutes = (timeLeft / 1000) / 60
        val seconds = (timeLeft / 1000) % 60
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val phaseText = when (BreathingSession.phase.value) {
            BreathPhase.INHALE -> lc.getString(R.string.inhale)
            BreathPhase.HOLD -> lc.getString(R.string.hold)
            BreathPhase.EXHALE -> lc.getString(R.string.exhale)
            BreathPhase.HOLD_AFTER -> lc.getString(R.string.hold_after)
        }.removeSuffix("…").removeSuffix("...")

        val builder = android.app.Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(lc.getString(R.string.breathing_notification_title))
            .setContentText("$phaseText · ${lc.getString(R.string.breathing_notification_time_left, timeText)}")
            .setSmallIcon(R.drawable.ic_media_play)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setVisibility(android.app.Notification.VISIBILITY_PUBLIC)

        mediaSession?.let {
            builder.setStyle(
                android.app.Notification.MediaStyle()
                    .setMediaSession(it.sessionToken)
            )
        }

        builder.addAction(
            android.app.Notification.Action.Builder(
                android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_stop),
                lc.getString(R.string.stop),
                stopPendingIntent
            ).build()
        )
        return builder.build()
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return false
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS -> {
                        stopBreathing()
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                }
            }
            .build()
        audioFocusRequest = focusRequest
        return am.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        audioFocusRequest = null
    }

    private fun ensurePlayers() {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        if (breathPlayer == null) {
            breathPlayer = MediaPlayer().apply { setAudioAttributes(attributes) }
        }
        if (voicePlayer == null) {
            voicePlayer = MediaPlayer().apply { setAudioAttributes(attributes) }
        }
    }

    private fun playAsset(player: MediaPlayer?, path: String) {
        if (player == null) return
        try {
            player.reset()
            assets.openFd(path).use { afd ->
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            }
            player.prepare()
            player.start()
        } catch (_: Exception) { }
    }

    private fun voiceAssetPath(phase: BreathPhase): String? {
        if (!PreferencesManager.getBreathingVoiceEnabled(this)) return null
        val lang = LanguageManager.getCurrentLanguage(this).code
        val name = when (phase) {
            BreathPhase.INHALE -> "inhale"
            BreathPhase.HOLD, BreathPhase.HOLD_AFTER -> "hold"
            BreathPhase.EXHALE -> "exhale"
        }
        return "breathing/voice/$lang/$name.mp3"
    }

    private fun onPhaseEnter(phase: BreathPhase) {
        BreathingSession.phase.value = phase
        ensurePlayers()
        // 人声提示（每轮都播，设置可关）
        voiceAssetPath(phase)?.let { playAsset(voicePlayer, it) }
        // 呼吸环境音（吸/呼时播放，屏息时暂停）
        when (phase) {
            BreathPhase.INHALE -> playAsset(breathPlayer, "breathing/breath-in.ogg")
            BreathPhase.EXHALE -> playAsset(breathPlayer, "breathing/breath-out.ogg")
            else -> breathPlayer?.pause()
        }
        updateNotification(BreathingSession.timeLeftMillis.value)
    }

    private fun startBreathing() {
        isRunning = true
        requestAudioFocus()
        serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        BreathingSession.isActive.value = true
        BreathingSession.breathCount.value = 0
        BreathingSession.timeLeftMillis.value = totalTime

        job = serviceScope?.launch {
            try {
                while (BreathingSession.timeLeftMillis.value > 0 && isRunning) {
                    // 吸气
                    onPhaseEnter(BreathPhase.INHALE)
                    for (i in methodInhale downTo 1) {
                        BreathingSession.phaseCountdown.value = i
                        if (!tick()) return@launch
                    }
                    // 屏息（如果有的话）
                    if (methodHold > 0) {
                        onPhaseEnter(BreathPhase.HOLD)
                        for (i in methodHold downTo 1) {
                            BreathingSession.phaseCountdown.value = i
                            if (!tick()) return@launch
                        }
                    }
                    // 呼气
                    onPhaseEnter(BreathPhase.EXHALE)
                    for (i in methodExhale downTo 1) {
                        BreathingSession.phaseCountdown.value = i
                        if (!tick()) return@launch
                    }
                    // 呼气后屏息（箱式呼吸）
                    if (methodHoldAfter > 0) {
                        onPhaseEnter(BreathPhase.HOLD_AFTER)
                        for (i in methodHoldAfter downTo 1) {
                            BreathingSession.phaseCountdown.value = i
                            if (!tick()) return@launch
                        }
                    }
                    BreathingSession.breathCount.value = BreathingSession.breathCount.value + 1
                }
            } finally {
                // 自然结束 / 手动停止 / 取消都统一收尾，复位会话状态
                stopBreathing()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    /** 秒 tick：扣减总时长并刷新通知；返回 false 表示应中止循环 */
    private suspend fun tick(): Boolean {
        if (!isRunning) return false
        delay(1000L)
        if (!isRunning) return false
        val next = BreathingSession.timeLeftMillis.value - 1000L
        BreathingSession.timeLeftMillis.value = next.coerceAtLeast(0L)
        updateNotification(BreathingSession.timeLeftMillis.value)
        return next > 0
    }

    private fun updateNotification(timeLeft: Long) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createNotification(timeLeft))
    }

    private fun stopBreathing() {
        isRunning = false
        job?.cancel()
        serviceScope?.cancel()
        breathPlayer?.release()
        breathPlayer = null
        voicePlayer?.release()
        voicePlayer = null
        abandonAudioFocus()
        mediaSession?.release()
        mediaSession = null
        BreathingSession.reset()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopBreathing()
    }
}
