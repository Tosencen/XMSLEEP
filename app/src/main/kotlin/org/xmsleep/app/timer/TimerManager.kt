package org.xmsleep.app.timer

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 全局倒计时管理器
 * 负责管理应用中的倒计时，确保在应用生命周期中保持倒计时状态
 * 适配Compose，使用协程和StateFlow
 */
class TimerManager private constructor() {

    private val TAG = "TimerManager"

    // 协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // 倒计时相关状态
    private var timerEndTime: Long = 0
    private var currentTimerMinutes: Int = 0
    private var _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    // 剩余时间（毫秒）
    private var _timeLeftMillis = MutableStateFlow(0L)
    val timeLeftMillis: StateFlow<Long> = _timeLeftMillis.asStateFlow()

    // 倒计时监听器列表（线程安全）
    private val listeners = mutableSetOf<TimerListener>()

    /**
     * 倒计时监听器接口
     */
    interface TimerListener {
        fun onTimerTick(timeLeftMillis: Long)
        fun onTimerFinished()
    }

    /**
     * 添加倒计时监听器
     */
    fun addListener(listener: TimerListener) {
        listeners.add(listener)

        // 如果倒计时正在进行，立即通知新的监听器当前状态
        if (_isTimerActive.value) {
            val timeLeft = timerEndTime - System.currentTimeMillis()
            if (timeLeft > 0) {
                listener.onTimerTick(timeLeft)
            }
        }
    }

    /**
     * 移除倒计时监听器
     */
    fun removeListener(listener: TimerListener) {
        listeners.remove(listener)
    }

    /**
     * 开始倒计时
     */
    fun startTimer(durationMinutes: Int) {
        try {
            // 取消之前的倒计时
            cancelTimer(notifyListeners = false)

            if (durationMinutes <= 0) {
                return
            }

            // 保存当前倒计时设置
            currentTimerMinutes = durationMinutes
            _isTimerActive.value = true

            // 计算结束时间
            val durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
            timerEndTime = System.currentTimeMillis() + durationMillis
            _timeLeftMillis.value = durationMillis

            // 开始倒计时任务
            scope.launch {
                startTimerLoop()
            }

            // 通知所有监听器倒计时开始
            val timeLeftMillis = timerEndTime - System.currentTimeMillis()
            for (listener in listeners) {
                listener.onTimerTick(timeLeftMillis)
            }

            Log.d(TAG, "倒计时已开始: $durationMinutes 分钟")
        } catch (e: Exception) {
            Log.e(TAG, "启动倒计时失败: ${e.message}")
            // 出现异常时，确保重置倒计时状态
            resetTimerState()
        }
    }

    /**
     * 取消倒计时
     * @param notifyListeners 是否通知监听器，默认为true
     */
    fun cancelTimer(notifyListeners: Boolean = true) {
        try {
            _isTimerActive.value = false
            currentTimerMinutes = 0
            timerEndTime = 0
            _timeLeftMillis.value = 0

            // 通知所有监听器倒计时已取消
            if (notifyListeners) {
                for (listener in listeners) {
                    listener.onTimerFinished()
                }
            }

            Log.d(TAG, "倒计时已取消，通知监听器: $notifyListeners")
        } catch (e: Exception) {
            Log.e(TAG, "取消倒计时失败: ${e.message}")
        }
    }

    /**
     * 获取当前倒计时分钟数
     */
    fun getCurrentTimerMinutes(): Int {
        return currentTimerMinutes
    }

    /**
     * 获取剩余时间（毫秒）
     */
    fun getTimeLeftMillis(): Long {
        return if (_isTimerActive.value) {
            val timeLeft = timerEndTime - System.currentTimeMillis()
            if (timeLeft > 0) timeLeft else 0
        } else {
            0
        }
    }

    /**
     * 倒计时循环
     */
    private suspend fun startTimerLoop() {
        while (_isTimerActive.value) {
            val now = System.currentTimeMillis()
            val timeLeft = timerEndTime - now

            if (timeLeft <= 0) {
                // 倒计时结束
                finishTimer()
                break
            } else {
                // 更新剩余时间（在主线程上更新StateFlow，确保触发Compose重组）
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    _timeLeftMillis.value = timeLeft
                }

                // 通知所有监听器倒计时更新
                listeners.forEach { listener ->
                    listener.onTimerTick(timeLeft)
                }

                // 每秒更新一次
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    /**
     * 完成倒计时
     */
    private fun finishTimer() {
        _isTimerActive.value = false
        currentTimerMinutes = 0
        timerEndTime = 0
        _timeLeftMillis.value = 0

        // 通知所有监听器倒计时结束
        for (listener in listeners) {
            listener.onTimerFinished()
        }
    }

    /**
     * 重置倒计时状态
     */
    private fun resetTimerState() {
        _isTimerActive.value = false
        currentTimerMinutes = 0
        timerEndTime = 0
        _timeLeftMillis.value = 0
    }

    /**
     * 释放资源
     */
    fun releaseResources() {
        try {
            cancelTimer(notifyListeners = false)
            listeners.clear()
            Log.d(TAG, "TimerManager资源已释放")
        } catch (e: Exception) {
            Log.e(TAG, "释放TimerManager资源失败: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: TimerManager? = null

        fun getInstance(): TimerManager {
            return instance ?: synchronized(this) {
                instance ?: TimerManager().also { instance = it }
            }
        }

        /**
         * 重置单例实例
         */
        fun resetInstance() {
            synchronized(this) {
                instance?.releaseResources()
                instance = null
            }
        }
    }
}

