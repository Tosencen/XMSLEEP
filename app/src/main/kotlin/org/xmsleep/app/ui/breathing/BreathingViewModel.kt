package org.xmsleep.app.ui.breathing

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 呼吸练习 ViewModel - 仅管理呼吸法与时长选择。
 * 计时与相位状态机已迁入 BreathingService（前台服务，支持息屏续播），
 * 运行时状态请观察 BreathingSession。
 */
class BreathingViewModel : ViewModel() {

    // 默认呼吸方法（用于初始化，后续会被真实方法替换）
    private val defaultMethod = BreathingMethod(
        id = "sleep_478",
        name = "4-7-8",
        subtitle = "",
        inhale = 4,
        hold = 7,
        exhale = 8,
        holdAfter = 0,
        defaultDurationMinutes = 10,
        isPrimary = true,
        description = "",
        steps = emptyList(),
        tags = emptyList()
    )

    private val _currentMethod = MutableStateFlow(defaultMethod)
    val currentMethod = _currentMethod.asStateFlow()

    private val _durationMinutes = MutableStateFlow(defaultMethod.defaultDurationMinutes)
    val durationMinutes = _durationMinutes.asStateFlow()

    /**
     * 选择呼吸法
     */
    fun selectMethod(method: BreathingMethod) {
        _currentMethod.value = method
        _durationMinutes.value = method.defaultDurationMinutes
    }

    /**
     * 设置时长（分钟）
     */
    fun setDuration(minutes: Int) {
        _durationMinutes.value = minutes
    }
}
