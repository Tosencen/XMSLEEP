package org.xmsleep.app.meditation

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.xmsleep.app.utils.Logger

/**
 * 冥想音频播放管理器（单例）
 * 管理冥想音频的播放状态，支持跨页面保持播放
 */
class MeditationPlayerManager private constructor() {

    companion object {
        private const val TAG = "MeditationPlayerManager"

        @Volatile
        private var instance: MeditationPlayerManager? = null

        fun getInstance(): MeditationPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: MeditationPlayerManager().also { instance = it }
            }
        }
    }

    private var exoPlayer: ExoPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private val audioManager = MutableStateFlow<SystemAudioManager?>(null)

    // 播放状态
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _currentCategoryId = MutableStateFlow<String?>(null)
    val currentCategoryId: StateFlow<String?> = _currentCategoryId.asStateFlow()

    private val _currentTime = MutableStateFlow(0L)
    val currentTime: StateFlow<Long> = _currentTime.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // 外部停止回调（用于互斥）
    private var onStopRequested: (() -> Unit)? = null

    fun setOnStopRequested(callback: () -> Unit) {
        onStopRequested = callback
    }

    /**
     * 初始化播放器（需要Context获取音频服务）
     */
    fun initialize(context: Context) {
        if (exoPlayer == null) {
            val httpDataSourceFactory = DefaultHttpDataSource.Factory().apply {
                setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                setDefaultRequestProperties(
                    mapOf(
                        "Referer" to "https://www.bilibili.com",
                        "Origin" to "https://www.bilibili.com"
                    )
                )
            }
            exoPlayer = ExoPlayer.Builder(context.applicationContext)
                .setMediaSourceFactory(DefaultMediaSourceFactory(httpDataSourceFactory))
                .build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            _duration.value = duration
                        }
                        if (playbackState == Player.STATE_ENDED) {
                            _isPlaying.value = false
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Logger.e(TAG, "播放错误: ${error.message} cause=${error.cause?.message}")
                        _isPlaying.value = false
                    }
                })
            }
            audioManager.value = context.getSystemService(Context.AUDIO_SERVICE) as? SystemAudioManager
        }
    }

    /**
     * 播放冥想音频
     */
    fun play(context: Context, categoryId: String, sessionId: String, audioUrl: String) {
        initialize(context)

        // 通知外部停止其他音频（互斥）
        onStopRequested?.invoke()

        // 请求音频焦点
        if (!requestAudioFocus(context)) {
            Logger.w(TAG, "无法获取音频焦点")
            return
        }

        _currentCategoryId.value = categoryId
        _currentSessionId.value = sessionId
        _isPlaying.value = true

        exoPlayer?.let { player ->
            player.repeatMode = if (_isLoop.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            player.setMediaItem(MediaItem.fromUri(audioUrl))
            player.prepare()
            player.play()
        }

        Logger.d(TAG, "开始播放冥想音频: $sessionId")
    }

    /**
     * 暂停播放
     */
    fun pause() {
        _isPlaying.value = false
        exoPlayer?.playWhenReady = false
        abandonAudioFocus()
        Logger.d(TAG, "暂停冥想播放")
    }

    /**
     * 恢复播放
     */
    fun resume(context: Context) {
        if (!requestAudioFocus(context)) return
        exoPlayer?.playWhenReady = true
        Logger.d(TAG, "恢复冥想播放")
    }

    /**
     * 停止播放
     */
    fun stop() {
        exoPlayer?.stop()
        _isPlaying.value = false
        _currentSessionId.value = null
        _currentCategoryId.value = null
        _currentTime.value = 0L
        _duration.value = 0L
        abandonAudioFocus()
        Logger.d(TAG, "停止冥想播放")
    }

    /**
     * 跳转到指定位置
     */
    fun seekTo(positionMs: Long) {
        exoPlayer?.seekTo(positionMs)
        _currentTime.value = positionMs
    }

    /**
     * 更新当前播放位置（由外部定时调用）
     */
    fun updatePosition(positionMs: Long) {
        _currentTime.value = positionMs
    }

    /**
     * 获取当前播放位置
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }

    /**
     * 获取总时长
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }

    /**
     * 检查是否正在播放指定的冥想音频
     */
    fun isPlayingSession(sessionId: String): Boolean {
        return _isPlaying.value && _currentSessionId.value == sessionId
    }

    fun hasMedia(): Boolean {
        return exoPlayer?.mediaItemCount?.let { it > 0 } == true
    }

    /**
     * 是否单曲循环
     */
    private val _isLoop = MutableStateFlow(false)
    val isLoop: StateFlow<Boolean> = _isLoop.asStateFlow()

    fun toggleLoop() {
        val next = !_isLoop.value
        _isLoop.value = next
        exoPlayer?.repeatMode = if (next) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        Logger.d(TAG, "循环播放: $next")
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(context: Context): Boolean {
        val am = audioManager.value ?: return false
        val focusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setOnAudioFocusChangeListener { }
            .build()

        val result = am.requestAudioFocus(focusRequest)
        audioFocusRequest = focusRequest
        return result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    /**
     * 释放音频焦点
     */
    private fun abandonAudioFocus() {
        audioFocusRequest?.let { request ->
            audioManager.value?.abandonAudioFocusRequest(request)
        }
        audioFocusRequest = null
    }

    /**
     * 释放资源
     */
    fun release() {
        stop()
        exoPlayer?.release()
        exoPlayer = null
        instance = null
    }
}
