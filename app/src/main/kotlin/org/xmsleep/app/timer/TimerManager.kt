package org.xmsleep.app.timer

import android.os.Handler
import android.os.Looper
import org.xmsleep.app.analytics.AnalyticsLogger
import org.xmsleep.app.utils.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit

/**
 * 全局倒计时管理器
 * 使用 Handler + Runnable 驱动倒计时，比协程 delay 更可靠（不受 Android 后台/前台切换影响）
 */
class TimerManager private constructor() {

    private val TAG = "TimerManager"

    private val handler = Handler(Looper.getMainLooper())

    // 倒计时相关状态
    private var timerEndTime: Long = 0
    private var currentTimerMinutes: Int = 0
    private var _isTimerActive = MutableStateFlow(false)
    val isTimerActive: StateFlow<Boolean> = _isTimerActive.asStateFlow()

    // 倒计时暂停状态
    private var _isTimerPaused = MutableStateFlow(false)
    val isTimerPaused: StateFlow<Boolean> = _isTimerPaused.asStateFlow()
    private var pausedTimeLeft: Long = 0

    // 剩余时间（毫秒）
    private var _timeLeftMillis = MutableStateFlow(0L)
    val timeLeftMillis: StateFlow<Long> = _timeLeftMillis.asStateFlow()

    // 倒计时监听器列表
    private val listeners = CopyOnWriteArraySet<TimerListener>()

    // 在通知监听器之前执行的回调（用于声音日记等需要提前捕获状态的场景）
    private var beforeFinishCallback: ((Int) -> Unit)? = null

    // Handler 驱动的定时 Runnable
    private var tickRunnable: Runnable? = null
    private var finishRunnable: Runnable? = null
    private var hasFinished: Boolean = false

    fun setBeforeFinishCallback(callback: ((Int) -> Unit)?) {
        beforeFinishCallback = callback
    }

    interface TimerListener {
        fun onTimerTick(timeLeftMillis: Long)
        fun onTimerFinished(durationMinutes: Int = 0)
        fun onTimerCancelled() {}
    }

    fun addListener(listener: TimerListener) {
        listeners.add(listener)
        if (_isTimerActive.value) {
            val timeLeft = timerEndTime - System.currentTimeMillis()
            if (timeLeft > 0) {
                listener.onTimerTick(timeLeft)
            }
        }
    }

    fun removeListener(listener: TimerListener) {
        listeners.remove(listener)
    }

    fun startTimer(durationMinutes: Int) {
        try {
            cancelTimer(notifyListeners = false)

            if (durationMinutes <= 0) {
                return
            }

            AnalyticsLogger.logCountdownSet(durationMinutes)
            currentTimerMinutes = durationMinutes
            _isTimerActive.value = true
            hasFinished = false

            val durationMillis = TimeUnit.MINUTES.toMillis(durationMinutes.toLong())
            timerEndTime = System.currentTimeMillis() + durationMillis
            _timeLeftMillis.value = durationMillis

            // 用 Handler.postDelayed 精确安排到期回调
            val delayMillis = durationMillis
            finishRunnable = Runnable {
                Logger.d(TAG, "Handler 触发 finishTimer, duration=$durationMinutes")
                finishTimer()
            }
            handler.postDelayed(finishRunnable!!, delayMillis)

            // 每秒 tick 更新 UI
            scheduleNextTick()

            // 通知所有监听器倒计时开始
            for (listener in listeners) {
                listener.onTimerTick(durationMillis)
            }

            Logger.d(TAG, "倒计时已开始: $durationMinutes 分钟, delay=$delayMillis ms")
        } catch (e: Exception) {
            Logger.e(TAG, "启动倒计时失败: ${e.message}")
            resetTimerState()
        }
    }

    private fun scheduleNextTick() {
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = object : Runnable {
            override fun run() {
                if (!_isTimerActive.value || _isTimerPaused.value) return

                val timeLeft = timerEndTime - System.currentTimeMillis()
                if (timeLeft <= 0) {
                    // Handler 还没触发 finishRunnable，手动触发
                    if (!hasFinished) {
                        Logger.d(TAG, "tick 检测到 timeLeft<=0, 手动触发 finishTimer")
                        finishTimer()
                    }
                    return
                }

                _timeLeftMillis.value = timeLeft
                for (listener in listeners) {
                    listener.onTimerTick(timeLeft)
                }

                handler.postDelayed(this, 1000)
            }
        }
        handler.postDelayed(tickRunnable!!, 1000)
    }

    fun cancelTimer(notifyListeners: Boolean = true) {
        try {
            _isTimerActive.value = false
            _isTimerPaused.value = false
            currentTimerMinutes = 0
            timerEndTime = 0
            pausedTimeLeft = 0
            _timeLeftMillis.value = 0
            hasFinished = false

            // 移除所有待执行的 Handler 回调
            tickRunnable?.let { handler.removeCallbacks(it) }
            tickRunnable = null
            finishRunnable?.let { handler.removeCallbacks(it) }
            finishRunnable = null

            if (notifyListeners) {
                for (listener in listeners) {
                    listener.onTimerCancelled()
                }
            }

            Logger.d(TAG, "倒计时已取消，通知监听器: $notifyListeners")
        } catch (e: Exception) {
            Logger.e(TAG, "取消倒计时失败: ${e.message}")
        }
    }

    fun pauseTimer() {
        if (!_isTimerActive.value || _isTimerPaused.value) return

        // 移除 Handler 回调
        tickRunnable?.let { handler.removeCallbacks(it) }
        finishRunnable?.let { handler.removeCallbacks(it) }

        _isTimerPaused.value = true
        pausedTimeLeft = timerEndTime - System.currentTimeMillis()
        if (pausedTimeLeft < 0) pausedTimeLeft = 0

        Logger.d(TAG, "倒计时已暂停，剩余时间: ${pausedTimeLeft}ms")
    }

    fun resumeTimer() {
        if (!_isTimerActive.value || !_isTimerPaused.value) return

        _isTimerPaused.value = false
        hasFinished = false
        timerEndTime = System.currentTimeMillis() + pausedTimeLeft

        // 重新安排 Handler 回调
        finishRunnable = Runnable {
            Logger.d(TAG, "Handler(恢复后)触发 finishTimer")
            finishTimer()
        }
        handler.postDelayed(finishRunnable!!, pausedTimeLeft)

        scheduleNextTick()

        Logger.d(TAG, "倒计时已恢复，剩余时间: ${pausedTimeLeft}ms")
    }

    fun getCurrentTimerMinutes(): Int = currentTimerMinutes

    fun getTimeLeftMillis(): Long {
        return if (_isTimerActive.value) {
            if (_isTimerPaused.value) {
                pausedTimeLeft
            } else {
                val timeLeft = timerEndTime - System.currentTimeMillis()
                if (timeLeft > 0) timeLeft else 0
            }
        } else {
            0
        }
    }

    private fun finishTimer() {
        if (hasFinished) {
            Logger.d(TAG, "finishTimer 已执行过，跳过重复调用")
            return
        }
        hasFinished = true

        val completedDuration = currentTimerMinutes
        Logger.d(TAG, "finishTimer 开始, completedDuration=$completedDuration, listenerCount=${listeners.size}")

        _isTimerActive.value = false
        currentTimerMinutes = 0
        timerEndTime = 0
        _timeLeftMillis.value = 0

        // 移除所有 Handler 回调
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
        finishRunnable?.let { handler.removeCallbacks(it) }
        finishRunnable = null

        try {
            beforeFinishCallback?.invoke(completedDuration)
        } catch (e: Exception) {
            Logger.e(TAG, "beforeFinishCallback 执行失败: ${e.message}", e)
        }

        try {
            val listenersSnapshot = listeners.toList()
            for (listener in listenersSnapshot) {
                try {
                    Logger.d(TAG, "通知监听器: ${listener.javaClass.simpleName}")
                    listener.onTimerFinished(completedDuration)
                } catch (e: Exception) {
                    Logger.e(TAG, "通知监听器倒计时结束失败: ${e.message}", e)
                }
            }
            Logger.d(TAG, "倒计时结束，已通知 ${listenersSnapshot.size} 个监听器")
        } catch (e: Exception) {
            Logger.e(TAG, "完成倒计时时发生错误: ${e.message}", e)
        }
    }

    private fun resetTimerState() {
        _isTimerActive.value = false
        _isTimerPaused.value = false
        currentTimerMinutes = 0
        timerEndTime = 0
        pausedTimeLeft = 0
        _timeLeftMillis.value = 0
        hasFinished = false
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
        finishRunnable?.let { handler.removeCallbacks(it) }
        finishRunnable = null
    }

    fun releaseResources() {
        try {
            cancelTimer(notifyListeners = false)
            listeners.clear()
            Logger.d(TAG, "TimerManager资源已释放")
        } catch (e: Exception) {
            Logger.e(TAG, "释放TimerManager资源失败: ${e.message}")
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

        fun resetInstance() {
            synchronized(this) {
                instance?.releaseResources()
                instance = null
            }
        }
    }
}

