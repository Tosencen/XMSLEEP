package org.xmsleep.app.audio

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import org.xmsleep.app.preferences.PreferencesManager
import org.xmsleep.app.service.MusicService
import org.xmsleep.app.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 音乐服务管理器
 * 负责 MusicService 的管理、蓝牙耳机监听、电话状态监听和最近播放记录
 */
class MusicServiceManager private constructor() {

    companion object {
        private const val TAG = "MusicServiceManager"

        @Volatile
        private var instance: MusicServiceManager? = null

        fun getInstance(): MusicServiceManager {
            return instance ?: synchronized(this) {
                instance ?: MusicServiceManager().also { instance = it }
            }
        }
    }

    interface Callback {
        fun onPlayRecentSoundsRequested()
    }

    private var callback: Callback? = null
    private var applicationContext: Context? = null

    // MusicService 相关
    private var musicService: MusicService? = null
    private var isServiceBound = false

    // 蓝牙耳机管理器
    private val bluetoothHeadsetManager = BluetoothHeadsetManager.getInstance()

    // 标记是否因来电而暂停
    private var wasPausedByPhoneCall = false

    // 电话状态监听
    private var phoneStateListener: PhoneStateListener? = null
    private var telephonyManager: TelephonyManager? = null

    // 标记是否只是暂停状态（用于倒计时保持）
    var isPausedState = false
        private set

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? MusicService.MusicServiceBinder
            musicService = binder?.getService()
            isServiceBound = true
            // 重新同步当前播放状态（修复：服务启动时播放状态可能已丢失）
            val audioManager = AudioManager.getInstance()
            val isPlaying = audioManager.hasAnyPlayingSounds()
            val descriptions = audioManager.getActiveSoundDescriptions()
            val localCount = audioManager.getPlayingSounds().size
            val remoteCount = audioManager.getPlayingRemoteSoundIds().size
            musicService?.updatePlayingState(isPlaying, localCount + remoteCount, descriptions)
            Logger.d(TAG, "MusicService 已连接")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isServiceBound = false
            Logger.d(TAG, "MusicService 已断开")
        }
    }

    fun setCallback(callback: Callback) {
        this.callback = callback
    }

    fun setApplicationContext(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    /**
     * 初始化蓝牙耳机监听器
     */
    fun initializeBluetoothHeadsetListener(context: Context, onBluetoothDisconnected: () -> Unit) {
        try {
            bluetoothHeadsetManager.initialize(context) {
                Logger.d(TAG, "检测到蓝牙耳机断开，暂停所有音频")
                onBluetoothDisconnected()
            }
            Logger.d(TAG, "蓝牙耳机监听器初始化成功")
        } catch (e: Exception) {
            Logger.e(TAG, "初始化蓝牙耳机监听器失败: ${e.message}")
        }
    }

    /**
     * 注册电话状态监听
     */
    fun registerPhoneStateListener(context: Context) {
        try {
            telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            phoneStateListener = object : PhoneStateListener() {
                @Deprecated("Deprecated in Java")
                override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                    when (state) {
                        TelephonyManager.CALL_STATE_RINGING,
                        TelephonyManager.CALL_STATE_OFFHOOK -> {
                            if (hasAnyPlayingSounds()) {
                                wasPausedByPhoneCall = true
                                Logger.d(TAG, "检测到来电/通话，标记为因来电暂停")
                            }
                        }
                        TelephonyManager.CALL_STATE_IDLE -> {
                            Logger.d(TAG, "通话结束，等待音频焦点恢复")
                        }
                    }
                }
            }
            telephonyManager?.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            Logger.d(TAG, "电话状态监听器注册成功")
        } catch (e: Exception) {
            Logger.e(TAG, "注册电话状态监听器失败: ${e.message}")
        }
    }

    /**
     * 注销电话状态监听
     */
    fun unregisterPhoneStateListener() {
        try {
            phoneStateListener?.let { listener ->
                telephonyManager?.listen(listener, PhoneStateListener.LISTEN_NONE)
                phoneStateListener = null
                telephonyManager = null
                Logger.d(TAG, "电话状态监听器已注销")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "注销电话状态监听器失败: ${e.message}")
        }
    }

    /**
     * 启动音乐服务
     */
    fun startMusicService(context: Context) {
        try {
            if (applicationContext == null) {
                applicationContext = context.applicationContext
            }

            val serviceIntent = Intent(context, MusicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            context.bindService(
                serviceIntent,
                serviceConnection,
                Context.BIND_AUTO_CREATE
            )

            Logger.d(TAG, "MusicService 启动请求已发送")
        } catch (e: Exception) {
            Logger.e(TAG, "启动 MusicService 失败: ${e.message}", e)
        }
    }

    /**
     * 停止音乐服务
     */
    fun stopMusicService(context: Context) {
        try {
            if (isServiceBound) {
                context.unbindService(serviceConnection)
                isServiceBound = false
            }

            val serviceIntent = Intent(context, MusicService::class.java)
            context.stopService(serviceIntent)

            musicService = null
            Logger.d(TAG, "MusicService 已停止")
        } catch (e: Exception) {
            Logger.e(TAG, "停止 MusicService 失败: ${e.message}")
        }
    }

    /**
     * 获取当前活跃声音的描述列表（用于 MediaSession metadata）
     */
    fun getActiveSoundDescriptions(
        playingQueue: java.util.concurrent.ConcurrentLinkedQueue<*>
    ): List<String> {
        val descriptions = mutableListOf<String>()
        playingQueue.forEach { item ->
            when (item) {
                is PlayingItem.LocalSound -> descriptions.add(item.sound.displayName)
                is PlayingItem.RemoteSound -> {
                    val remotePlayer = RemoteSoundPlayer.getInstance()
                    val metadata = remotePlayer.getRemoteMetadata(item.soundId)?.first
                    descriptions.add(metadata?.name ?: item.soundId)
                }
            }
        }
        return descriptions
    }

    /**
     * 通知服务播放状态已改变
     */
    fun notifyServicePlayingStateChanged(
        isPlaying: Boolean,
        totalCount: Int,
        descriptions: List<String>
    ) {
        try {
            musicService?.updatePlayingState(isPlaying, totalCount, descriptions)
            Logger.d(TAG, "通知服务状态: isPlaying=$isPlaying, count=$totalCount")
        } catch (e: Exception) {
            Logger.e(TAG, "通知服务播放状态失败: ${e.message}")
        }
    }

    /**
     * 保存当前正在播放的声音列表
     */
    fun saveRecentPlayingSounds(
        localPlayer: LocalSoundPlayer,
        remotePlayer: RemoteSoundPlayer
    ) {
        try {
            val context = applicationContext ?: return

            Logger.d(TAG, "========== 开始保存最近播放记录 ==========")

            val playingLocalSounds = localPlayer.getPlayingSounds().map { it.name }
            val playingRemoteSounds = remotePlayer.getPlayingRemoteSoundIds()

            val hasAnyPlaying = playingLocalSounds.isNotEmpty() || playingRemoteSounds.isNotEmpty()

            if (hasAnyPlaying) {
                PreferencesManager.saveRecentLocalSounds(context, playingLocalSounds)
                PreferencesManager.saveRecentRemoteSounds(context, playingRemoteSounds)

                Logger.d(TAG, "✓ 已保存最近播放记录:")
                Logger.d(TAG, "  - 本地声音: ${playingLocalSounds.joinToString().ifEmpty { "无" }}")
                Logger.d(TAG, "  - 远程声音: ${playingRemoteSounds.joinToString().ifEmpty { "无" }}")
            } else {
                Logger.d(TAG, "✓ 当前没有正在播放的音频，保留之前的最近播放记录")
            }

            Logger.d(TAG, "========== 保存最近播放记录完成 ==========")
        } catch (e: Exception) {
            Logger.e(TAG, "保存最近播放声音失败", e)
        }
    }

    /**
     * 播放最近播放的声音
     */
    fun playRecentSounds(context: Context) {
        try {
            Logger.d(TAG, "开始播放最近的声音...")

            val recentLocalSounds = PreferencesManager.getRecentLocalSounds(context)
            Logger.d(TAG, "最近播放的本地声音数量: ${recentLocalSounds.size}")

            val localPlayer = LocalSoundPlayer.getInstance()
            recentLocalSounds.forEach { soundName ->
                try {
                    val sound = AudioManager.Sound.valueOf(soundName)
                    localPlayer.playSound(context, sound, AudioManager.MAX_CONCURRENT_SOUNDS)
                    Logger.d(TAG, "成功播放最近的本地声音: $soundName")
                } catch (e: Exception) {
                    Logger.e(TAG, "播放最近的本地声音 $soundName 失败: ${e.message}")
                }
            }

            val recentRemoteSounds = PreferencesManager.getRecentRemoteSounds(context)
            Logger.d(TAG, "最近播放的远程声音数量: ${recentRemoteSounds.size}")

            val remotePlayer = RemoteSoundPlayer.getInstance()
            CoroutineScope(Dispatchers.Main).launch {
                val resourceManager = AudioResourceManager.getInstance(context)
                recentRemoteSounds.forEach { soundId ->
                    try {
                        val metadata = withContext(Dispatchers.IO) {
                            resourceManager.getSoundMetadata(soundId)
                        }

                        if (metadata != null) {
                            val uri = withContext(Dispatchers.IO) {
                                resourceManager.getSoundUri(metadata)
                            }

                            if (uri != null) {
                                remotePlayer.playRemoteSound(context, metadata, uri, AudioManager.MAX_CONCURRENT_SOUNDS)
                                Logger.d(TAG, "成功播放最近的远程声音: $soundId")
                            } else {
                                Logger.w(TAG, "无法播放最近的远程声音 $soundId：URI 为空")
                            }
                        } else {
                            Logger.w(TAG, "无法播放最近的远程声音 $soundId：元数据不存在")
                        }
                    } catch (e: Exception) {
                        Logger.e(TAG, "播放最近的远程声音 $soundId 失败: ${e.message}")
                    }
                }
            }

            Logger.d(TAG, "播放最近声音完成")
        } catch (e: Exception) {
            Logger.e(TAG, "播放最近声音失败: ${e.message}")
        }
    }

    /**
     * 检查是否有最近播放的声音
     */
    fun hasRecentSounds(context: Context): Boolean {
        val recentLocalSounds = PreferencesManager.getRecentLocalSounds(context)
        val recentRemoteSounds = PreferencesManager.getRecentRemoteSounds(context)
        return recentLocalSounds.isNotEmpty() || recentRemoteSounds.isNotEmpty()
    }

    /**
     * 处理音频焦点恢复
     */
    fun onAudioFocusGained() {
        if (wasPausedByPhoneCall) {
            wasPausedByPhoneCall = false
            applicationContext?.let { context ->
                if (hasRecentSounds(context)) {
                    playRecentSounds(context)
                }
            }
        }
    }

    /**
     * 设置暂停状态
     */
    fun setPausedState(paused: Boolean) {
        isPausedState = paused
    }

    /**
     * 检查是否有任何声音正在播放
     */
    private fun hasAnyPlayingSounds(): Boolean {
        val localPlayer = LocalSoundPlayer.getInstance()
        val remotePlayer = RemoteSoundPlayer.getInstance()
        return localPlayer.hasAnyPlayingSounds() || remotePlayer.hasAnyPlayingSounds()
    }

    /**
     * 释放资源
     */
    fun release() {
        bluetoothHeadsetManager.release()
        unregisterPhoneStateListener()
        applicationContext = null
    }
}

/**
 * 播放队列项密封类
 */
sealed class PlayingItem {
    data class LocalSound(val sound: AudioManager.Sound) : PlayingItem()
    data class RemoteSound(val soundId: String) : PlayingItem()
}
