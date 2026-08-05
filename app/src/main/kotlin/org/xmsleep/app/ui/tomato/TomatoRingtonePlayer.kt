package org.xmsleep.app.ui.tomato

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import org.xmsleep.app.R
import org.xmsleep.app.audio.AudioManager
import org.xmsleep.app.audio.LocalAudioPlayer
import org.xmsleep.app.utils.Logger

/**
 * 番茄时钟结束铃声选项
 */
object TomatoRingtone {
    /** 无铃声 */
    const val NONE = ""

    data class Option(val id: String, val resId: Int)

    val options: List<Option> = listOf(
        Option("ringtone_chime", R.raw.ringtone_chime),
        Option("ringtone_ding", R.raw.ringtone_ding),
        Option("ringtone_marimba", R.raw.ringtone_marimba),
        Option("ringtone_windchime", R.raw.ringtone_windchime),
    )

    fun resolveResId(id: String): Int? = options.firstOrNull { it.id == id }?.resId
}

/**
 * 番茄时钟结束铃声播放器（单例）
 * 铃声响起时停止白噪音/电台/冥想/本地音乐，播放一次后自动释放
 */
object TomatoRingtonePlayer {

    private const val TAG = "TomatoRingtonePlayer"

    /** 结束铃声播放次数 */
    private const val RING_TIMES = 3

    private var mediaPlayer: MediaPlayer? = null
    private var playCount = 0

    /** 试听播放器（试听一次，不打断其他音频） */
    private var previewPlayer: MediaPlayer? = null

    /**
     * 播放结束铃声（空铃声标识时静默忽略），响铃 RING_TIMES 次后自动释放
     */
    fun play(context: Context, ringtoneId: String) {
        val resId = TomatoRingtone.resolveResId(ringtoneId) ?: return
        stop()
        playCount = 0
        try {
            // 铃声与白噪音互斥：停止所有白噪音/电台/冥想
            AudioManager.getInstance().stopAllSounds()
            // 停止本地音乐
            try {
                LocalAudioPlayer.getInstance().stopAllAudios()
            } catch (e: Exception) {
                Logger.e(TAG, "停止本地音乐失败: ${e.message}")
            }

            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    context,
                    Uri.parse("android.resource://${context.packageName}/$resId")
                )
                isLooping = false
                setOnCompletionListener { it ->
                    playCount++
                    if (playCount < RING_TIMES) {
                        it.seekTo(0)
                        it.start()
                    } else {
                        mediaPlayer = null
                        it.release()
                    }
                }
                prepare()
                start()
            }
            mediaPlayer = player
            playCount = 1
            Logger.d(TAG, "结束铃声播放中(第 $playCount/$RING_TIMES 次): $ringtoneId")
        } catch (e: Exception) {
            Logger.e(TAG, "播放番茄结束铃声失败: ${e.message}", e)
            stop()
        }
    }

    /**
     * 停止并释放铃声
     */
    fun stop() {
        stopPreview()
        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "停止番茄铃声失败: ${e.message}")
        } finally {
            mediaPlayer = null
        }
    }

    /**
     * 试听结束铃声：播放一次，完成后自动释放并回调 onDone。
     * 不停止白噪音/电台/本地音乐，避免打断用户正在听的内容。
     */
    fun preview(context: Context, ringtoneId: String, onDone: () -> Unit = {}) {
        val resId = TomatoRingtone.resolveResId(ringtoneId) ?: return
        stopPreview()
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(
                    context,
                    Uri.parse("android.resource://${context.packageName}/$resId")
                )
                isLooping = false
                setOnCompletionListener {
                    previewPlayer = null
                    it.release()
                    onDone()
                }
                prepare()
                start()
            }
            previewPlayer = player
            Logger.d(TAG, "试听结束铃声: $ringtoneId")
        } catch (e: Exception) {
            Logger.e(TAG, "试听番茄结束铃声失败: ${e.message}", e)
            stopPreview()
            onDone()
        }
    }

    /**
     * 停止并释放试听播放器
     */
    fun stopPreview() {
        try {
            previewPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
            }
        } catch (e: Exception) {
            Logger.e(TAG, "停止番茄铃声试听失败: ${e.message}")
        } finally {
            previewPlayer = null
        }
    }
}
