package org.xmsleep.app.audio

import io.mockk.mockk
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * AudioManager.stopAllSounds 测试
 *
 * 验证"停止所有播放"会委托给所有子播放器：
 *  - 内置声音 LocalSoundPlayer.stopAllSounds
 *  - 远程声音 RemoteSoundPlayer.pauseAllRemoteSounds
 *  - 电台回调 onStopRadioRequested
 *  - 冥想 MeditationPlayerManager.stop
 *
 * 这是"一键关闭"弹窗的核心逻辑，防止回归。
 */
class AudioManagerStopAllTest {

    private lateinit var audioManager: AudioManager
    private lateinit var localSoundPlayer: LocalSoundPlayer
    private lateinit var remoteSoundPlayer: RemoteSoundPlayer
    private lateinit var musicServiceManager: MusicServiceManager
    private lateinit var meditationPlayer: org.xmsleep.app.meditation.MeditationPlayerManager

    private fun setField(obj: Any, name: String, value: Any) {
        val field = obj.javaClass.getDeclaredField(name)
        field.isAccessible = true
        field.set(obj, value)
    }

    private fun replaceSingleton(clazz: Class<*>, instance: Any?) {
        val field = clazz.getDeclaredField("instance")
        field.isAccessible = true
        field.set(null, instance)
    }

    @Before
    fun setUp() {
        replaceSingleton(AudioManager::class.java, null)
        audioManager = AudioManager.getInstance()

        // 用 mock 替换子播放器，避免依赖真实播放器
        localSoundPlayer = mockk(relaxed = true)
        remoteSoundPlayer = mockk(relaxed = true)
        musicServiceManager = mockk(relaxed = true)
        meditationPlayer = mockk(relaxed = true)
        replaceSingleton(org.xmsleep.app.meditation.MeditationPlayerManager::class.java, meditationPlayer)

        setField(audioManager, "localSoundPlayer", localSoundPlayer)
        setField(audioManager, "remoteSoundPlayer", remoteSoundPlayer)
        setField(audioManager, "musicServiceManager", musicServiceManager)
    }

    @After
    fun tearDown() {
        replaceSingleton(AudioManager::class.java, null)
        replaceSingleton(org.xmsleep.app.meditation.MeditationPlayerManager::class.java, null)
    }

    @Test
    fun `stopAllSounds should stop local sound player`() {
        audioManager.stopAllSounds()
        verify { localSoundPlayer.stopAllSounds() }
    }

    @Test
    fun `stopAllSounds should pause remote sound player`() {
        audioManager.stopAllSounds()
        verify { remoteSoundPlayer.pauseAllRemoteSounds() }
    }

    @Test
    fun `stopAllSounds should reset paused state`() {
        audioManager.stopAllSounds()
        verify { musicServiceManager.setPausedState(false) }
    }

    @Test
    fun `stopAllSounds should invoke radio stop callback`() {
        var radioStopped = false
        audioManager.setOnStopRadioRequested { radioStopped = true }
        audioManager.stopAllSounds()
        assertTrue(radioStopped)
    }

    @Test
    fun `stopAllSounds should stop meditation player`() {
        audioManager.stopAllSounds()
        verify { meditationPlayer.stop() }
    }

    @Test
    fun `stopAllSounds without radio callback should not throw`() {
        audioManager.setOnStopRadioRequested(null)
        audioManager.stopAllSounds()
        verify { localSoundPlayer.stopAllSounds() }
    }

    @Test
    fun `paused state should be false after stopAllSounds`() {
        audioManager.stopAllSounds()
        assertFalse(audioManager.isPausedState)
    }
}
