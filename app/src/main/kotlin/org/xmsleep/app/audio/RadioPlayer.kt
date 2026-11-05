package org.xmsleep.app.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.BehindLiveWindowException
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.xmsleep.app.audio.model.RadioStation
import org.xmsleep.app.utils.Logger

class RadioPlayer {
    private val TAG = "RadioPlayer"

    private var player: ExoPlayer? = null
    private var _currentStationId: String? = null
    val currentStationId: String? get() = _currentStationId
    private var appContext: Context? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private var volume = 0.5f
    private var retryCount = 0
    private var pendingStation: RadioStation? = null
    private var pendingContext: Context? = null

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            Logger.d(TAG, "state=$state buffering=${_isBuffering.value} playing=${_isPlaying.value}")
            when (state) {
                Player.STATE_BUFFERING -> {
                    _isBuffering.value = true
                }
                Player.STATE_READY -> {
                    _isBuffering.value = false
                    retryCount = 0
                    if (player?.playWhenReady == true) {
                        _isPlaying.value = true
                    }
                }
                Player.STATE_ENDED -> {
                    _isBuffering.value = false
                    _isPlaying.value = false
                }
                Player.STATE_IDLE -> {
                    _isBuffering.value = false
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            Logger.d(TAG, "onIsPlayingChanged=$isPlaying")
        }

        override fun onPlayerError(error: PlaybackException) {
            Logger.e(TAG, "播放错误: code=${error.errorCode} ${error.message}")
            if (error.cause is BehindLiveWindowException && retryCount < 5) {
                retryCount++
                Logger.d(TAG, "BehindLiveWindow, retry $retryCount...")
                val ctx = pendingContext ?: return
                val stn = pendingStation ?: return
                Handler(Looper.getMainLooper()).post { play(ctx, stn) }
                return
            }
            _isBuffering.value = false
            _isPlaying.value = false
        }
    }

    fun play(context: Context, station: RadioStation) {
        Logger.d(TAG, "play: ${station.name} (${station.url})")

        _currentStationId = station.id
        appContext = context.applicationContext
        pendingContext = context
        pendingStation = station

        releasePlayer()

        _isPlaying.value = true
        _isBuffering.value = true

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val p = ExoPlayer.Builder(context).build().apply {
            setAudioAttributes(audioAttrs, false)
            addListener(playerListener)
            repeatMode = Player.REPEAT_MODE_ONE
            setWakeMode(C.WAKE_MODE_NETWORK)
        }
        player = p

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(mapOf(
                "Referer" to "https://live.bilibili.com/",
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36",
                "Origin" to "https://live.bilibili.com"
            ))
        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val liveConfig = MediaItem.LiveConfiguration.Builder()
            .setTargetOffsetMs(2000)
            .build()
        val mediaItem = MediaItem.fromUri(Uri.parse(station.url))
            .buildUpon()
            .setLiveConfiguration(liveConfig)
            .build()

        val mediaSource = if (station.isHls || station.url.contains(".m3u8")) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }

        p.setMediaSource(mediaSource)
        p.volume = volume
        p.prepare()
        p.playWhenReady = true
    }

    fun pause() {
        player?.playWhenReady = false
        _isPlaying.value = false
    }

    fun togglePlayPause(context: Context) {
        val p = player ?: return
        p.playWhenReady = !p.playWhenReady
        _isPlaying.value = p.playWhenReady
    }

    fun stop() {
        _isPlaying.value = false
        _isBuffering.value = false
        val p = player ?: return
        p.stop()
        p.playWhenReady = false
    }

    fun setVolume(v: Float) {
        volume = v.coerceIn(0f, 1f)
        player?.volume = volume
    }

    fun getVolume(): Float = volume

    private fun releasePlayer() {
        try {
            player?.removeListener(playerListener)
            player?.stop()
            player?.release()
        } catch (e: Exception) {
            Logger.w(TAG, "释放播放器失败: ${e.message}")
        }
        player = null
    }

    fun release() {
        releasePlayer()
    }
}
