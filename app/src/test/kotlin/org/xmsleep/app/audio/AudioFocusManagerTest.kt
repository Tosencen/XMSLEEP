package org.xmsleep.app.audio

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * AudioFocusManager 单元测试
 * 注意：由于 Android 框架类难以在单元测试中 mock，这里只测试简单的状态管理
 */
class AudioFocusManagerTest {

    private lateinit var audioFocusManager: AudioFocusManager

    @Before
    fun setUp() {
        audioFocusManager = AudioFocusManager()
    }

    @After
    fun tearDown() {
        // 清理
    }

    @Test
    fun `hasAudioFocus should return false initially`() {
        assertFalse(audioFocusManager.hasAudioFocus())
    }

    @Test
    fun `callback should be settable`() {
        val callback = object : AudioFocusManager.Callback {
            override fun onAudioFocusLost() {}
            override fun onAudioFocusLostTransient() {}
            override fun onAudioFocusLostCanDuck() {}
            override fun onAudioFocusGained() {}
        }
        // 不应该抛出异常
        audioFocusManager.setCallback(callback)
    }
}
