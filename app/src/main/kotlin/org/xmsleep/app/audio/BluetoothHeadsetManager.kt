package org.xmsleep.app.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager as SystemAudioManager
import android.os.Build
import org.xmsleep.app.utils.Logger

/**
 * 蓝牙耳机管理器
 * 监听蓝牙耳机连接/断开事件，在断开时自动暂停音频播放
 */
class BluetoothHeadsetManager private constructor() {
    
    companion object {
        private const val TAG = "BluetoothHeadsetManager"
        
        @Volatile
        private var instance: BluetoothHeadsetManager? = null
        
        fun getInstance(): BluetoothHeadsetManager {
            return instance ?: synchronized(this) {
                instance ?: BluetoothHeadsetManager().also { instance = it }
            }
        }
    }
    
    private var context: Context? = null
    private var isReceiverRegistered = false
    private var onHeadsetDisconnected: (() -> Unit)? = null
    
    // 蓝牙耳机断开广播接收器
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(
                        BluetoothProfile.EXTRA_STATE,
                        BluetoothProfile.STATE_DISCONNECTED
                    )
                    
                    when (state) {
                        BluetoothProfile.STATE_DISCONNECTED -> {
                            Logger.d(TAG, "蓝牙耳机已断开")
                            onHeadsetDisconnected?.invoke()
                        }
                        BluetoothProfile.STATE_CONNECTED -> {
                            Logger.d(TAG, "蓝牙耳机已连接")
                        }
                    }
                }
                
                // 监听音频输出设备变化（更通用的方式）
                SystemAudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    // 当音频输出设备断开时（包括蓝牙耳机、有线耳机等）
                    Logger.d(TAG, "音频输出设备已断开（ACTION_AUDIO_BECOMING_NOISY）")
                    onHeadsetDisconnected?.invoke()
                }
            }
        }
    }
    
    /**
     * 初始化并开始监听
     * @param context 应用上下文
     * @param onDisconnected 耳机断开时的回调
     */
    fun initialize(context: Context, onDisconnected: () -> Unit) {
        this.context = context.applicationContext
        this.onHeadsetDisconnected = onDisconnected
        
        registerReceiver()
    }
    
    /**
     * 注册广播接收器
     */
    private fun registerReceiver() {
        if (isReceiverRegistered) {
            Logger.d(TAG, "广播接收器已注册，跳过")
            return
        }
        
        try {
            val filter = IntentFilter().apply {
                // 监听蓝牙耳机连接状态变化
                addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
                // 监听音频输出设备变化（更通用，包括有线耳机）
                addAction(SystemAudioManager.ACTION_AUDIO_BECOMING_NOISY)
            }
            
            context?.registerReceiver(bluetoothReceiver, filter)
            isReceiverRegistered = true
            Logger.d(TAG, "蓝牙耳机监听器已注册")
        } catch (e: Exception) {
            Logger.e(TAG, "注册广播接收器失败: ${e.message}")
        }
    }
    
    /**
     * 注销广播接收器
     */
    fun unregister() {
        if (!isReceiverRegistered) {
            return
        }
        
        try {
            context?.unregisterReceiver(bluetoothReceiver)
            isReceiverRegistered = false
            Logger.d(TAG, "蓝牙耳机监听器已注销")
        } catch (e: Exception) {
            Logger.e(TAG, "注销广播接收器失败: ${e.message}")
        }
    }
    
    /**
     * 检查当前是否连接了蓝牙耳机
     */
    @SuppressLint("MissingPermission")
    fun isBluetoothHeadsetConnected(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as SystemAudioManager
                val devices = audioManager.getDevices(SystemAudioManager.GET_DEVICES_OUTPUTS)
                
                devices.any { device ->
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                }
            } else {
                // Android 6.0 以下使用 BluetoothAdapter
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                @Suppress("WrongConstant")
                val connected = bluetoothAdapter?.isEnabled == true &&
                    bluetoothAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) == BluetoothProfile.STATE_CONNECTED
                connected
            }
        } catch (e: Exception) {
            Logger.e(TAG, "检查蓝牙耳机连接状态失败: ${e.message}")
            false
        }
    }
    
    /**
     * 释放资源
     */
    fun release() {
        unregister()
        context = null
        onHeadsetDisconnected = null
    }
}
