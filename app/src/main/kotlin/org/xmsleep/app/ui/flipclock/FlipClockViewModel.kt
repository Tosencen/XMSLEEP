package org.xmsleep.app.ui.flipclock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 翻页时钟状态
 */
data class FlipClockUiState(
    val hours: Int = 0,
    val minutes: Int = 0,
    val seconds: Int = 0,
    val isCountdownMode: Boolean = false,
    val countdownRemainingMillis: Long = 0L,
    val isCountdownActive: Boolean = false,
    val showDate: Boolean = false,
    val selectedFont: ClockFont = ClockFont.OSWALD
)

/**
 * 翻页时钟 ViewModel
 */
class FlipClockViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FlipClockUiState())
    val uiState: StateFlow<FlipClockUiState> = _uiState.asStateFlow()

    private var clockJob: Job? = null
    private var countdownJob: Job? = null

    init {
        startClock()
    }

    /**
     * 启动实时时钟
     */
    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            while (true) {
                val calendar = Calendar.getInstance()
                _uiState.value = _uiState.value.copy(
                    hours = calendar.get(Calendar.HOUR_OF_DAY),
                    minutes = calendar.get(Calendar.MINUTE),
                    seconds = calendar.get(Calendar.SECOND)
                )
                delay(1000L)
            }
        }
    }

    /**
     * 开始倒计时
     * @param minutes 倒计时分钟数
     */
    fun startCountdown(minutes: Int) {
        countdownJob?.cancel()
        val totalMillis = minutes * 60 * 1000L

        _uiState.value = _uiState.value.copy(
            isCountdownMode = true,
            countdownRemainingMillis = totalMillis,
            isCountdownActive = true
        )

        countdownJob = viewModelScope.launch {
            var remaining = totalMillis
            while (remaining > 0) {
                delay(1000L)
                remaining -= 1000L
                _uiState.value = _uiState.value.copy(
                    countdownRemainingMillis = remaining.coerceAtLeast(0L)
                )
            }

            // 倒计时结束
            _uiState.value = _uiState.value.copy(
                isCountdownActive = false,
                hours = 0,
                minutes = 0,
                seconds = 0
            )
        }
    }

    /**
     * 取消倒计时
     */
    fun cancelCountdown() {
        countdownJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isCountdownMode = false,
            countdownRemainingMillis = 0L,
            isCountdownActive = false
        )
        // 恢复显示实时时间
        startClock()
    }

    /**
     * 切换日期显示
     */
    fun toggleDate() {
        _uiState.value = _uiState.value.copy(
            showDate = !_uiState.value.showDate
        )
    }

    /**
     * 设置字体
     */
    fun setFont(font: ClockFont) {
        _uiState.value = _uiState.value.copy(
            selectedFont = font
        )
    }

    /**
     * 获取倒计时剩余的时、分、秒
     */
    fun getCountdownTime(): Triple<Int, Int, Int> {
        val remaining = _uiState.value.countdownRemainingMillis
        val hours = (remaining / 3600000).toInt()
        val minutes = ((remaining % 3600000) / 60000).toInt()
        val seconds = ((remaining % 60000) / 1000).toInt()
        return Triple(hours, minutes, seconds)
    }

    override fun onCleared() {
        super.onCleared()
        clockJob?.cancel()
        countdownJob?.cancel()
    }
}
