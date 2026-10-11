package org.xmsleep.app.service

import kotlinx.coroutines.flow.MutableStateFlow

/** 呼吸相位 */
enum class BreathPhase {
    INHALE,      // 吸气
    HOLD,        // 屏息
    EXHALE,      // 呼气
    HOLD_AFTER   // 呼气后屏息（箱式呼吸）
}

/**
 * 呼吸练习会话状态：Service 写入、Screen 观察。
 * 相位状态机与音频播放全部运行在 BreathingService（前台服务）内，
 * 息屏/后台不受 Compose 重组影响，保证续播。
 */
object BreathingSession {
    val isActive = MutableStateFlow(false)
    val phase = MutableStateFlow(BreathPhase.INHALE)
    val phaseCountdown = MutableStateFlow(0)
    val breathCount = MutableStateFlow(0)
    val timeLeftMillis = MutableStateFlow(0L)

    fun reset() {
        isActive.value = false
        phase.value = BreathPhase.INHALE
        phaseCountdown.value = 0
        breathCount.value = 0
        timeLeftMillis.value = 0L
    }
}
