package org.xmsleep.app.ui.breathing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 呼吸练习 ViewModel - 管理呼吸状态和计时器
 */
class BreathingViewModel : ViewModel() {

    private val _isActive = MutableStateFlow(false)
    val isActive = _isActive.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds = _remainingSeconds.asStateFlow()

    private var timerJob: Job? = null

    // 默认 5 分钟呼吸练习
    fun startBreathing(totalSeconds: Int = 5 * 60) {
        if (_isActive.value) return

        _isActive.value = true
        _remainingSeconds.value = totalSeconds

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                _remainingSeconds.value -= 1
            }
            // 计时结束自动停止
            stopBreathing()
        }
    }

    fun stopBreathing() {
        timerJob?.cancel()
        timerJob = null
        _isActive.value = false
        _remainingSeconds.value = 0
    }

    override fun onCleared() {
        stopBreathing()
        super.onCleared()
    }
}
