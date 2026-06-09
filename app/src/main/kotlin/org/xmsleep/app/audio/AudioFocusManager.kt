package org.xmsleep.app.audio

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager as SystemAudioManager
import android.os.Build
import org.xmsleep.app.utils.Logger

/**
 * 音频焦点管理器
 * 负责音频焦点的申请、放弃和焦点变化处理
 */
class AudioFocusManager {

    companion object {
        private const val TAG = "AudioFocusManager"
    }

    interface Callback {
        fun onAudioFocusLost()
        fun onAudioFocusLostTransient()
        fun onAudioFocusLostCanDuck()
        fun onAudioFocusGained()
    }

    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var callback: Callback? = null

    private val audioFocusChangeListener = SystemAudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            SystemAudioManager.AUDIOFOCUS_LOSS -> {
                callback?.onAudioFocusLost()
                hasAudioFocus = false
            }
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                callback?.onAudioFocusLostTransient()
            }
            SystemAudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                callback?.onAudioFocusLostCanDuck()
            }
            SystemAudioManager.AUDIOFOCUS_GAIN -> {
                callback?.onAudioFocusGained()
                hasAudioFocus = true
            }
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun requestAudioFocus(context: Context): Boolean {
        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest = AudioFocusRequest.Builder(SystemAudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                val result = audioManager.requestAudioFocus(audioFocusRequest!!)
                hasAudioFocus = result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
                hasAudioFocus
            } else {
                @Suppress("DEPRECATION")
                val result = audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    SystemAudioManager.STREAM_MUSIC,
                    SystemAudioManager.AUDIOFOCUS_GAIN
                )
                hasAudioFocus = result == SystemAudioManager.AUDIOFOCUS_REQUEST_GRANTED
                hasAudioFocus
            }
        } catch (e: Exception) {
            Logger.e(TAG, "请求音频焦点失败", e)
            false
        }
    }

    fun abandonAudioFocus(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    audioManager.abandonAudioFocusRequest(it)
                }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(audioFocusChangeListener)
            }
            hasAudioFocus = false
        } catch (e: Exception) {
            Logger.e(TAG, "放弃音频焦点失败: ${e.message}")
        }
    }

    fun hasAudioFocus(): Boolean = hasAudioFocus
}
