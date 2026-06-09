package org.xmsleep.app.audio

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * AudioManager 单元测试
 */
class AudioManagerTest {

    private lateinit var audioManager: AudioManager

    @Before
    fun setUp() {
        // 重置单例实例以便测试
        val instanceField = AudioManager::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)

        audioManager = AudioManager.getInstance()
    }

    @After
    fun tearDown() {
        // 清理单例实例
        val instanceField = AudioManager::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun `getInstance should return same instance`() {
        val instance1 = AudioManager.getInstance()
        val instance2 = AudioManager.getInstance()
        assertSame(instance1, instance2)
    }

    @Test
    fun `isPlayingSound should return false initially`() {
        assertFalse(audioManager.isPlayingSound(AudioManager.Sound.HEAVY_RAIN))
    }

    @Test
    fun `getPlayingSounds should return empty list initially`() {
        val playingSounds = audioManager.getPlayingSounds()
        assertTrue(playingSounds.isEmpty())
    }

    @Test
    fun `hasAnyPlayingSounds should return false initially`() {
        assertFalse(audioManager.hasAnyPlayingSounds())
    }

    @Test
    fun `isPlayingRemoteSound should return false initially`() {
        assertFalse(audioManager.isPlayingRemoteSound("test_sound_id"))
    }

    @Test
    fun `getPlayingRemoteSoundIds should return empty list initially`() {
        val playingIds = audioManager.getPlayingRemoteSoundIds()
        assertTrue(playingIds.isEmpty())
    }

    @Test
    fun `Sound enum should have correct values`() {
        val soundValues = AudioManager.Sound.values()
        assertEquals(17, soundValues.size)
        assertEquals(AudioManager.Sound.NONE, AudioManager.Sound.valueOf("NONE"))
        assertEquals(AudioManager.Sound.UMBRELLA_RAIN, AudioManager.Sound.valueOf("UMBRELLA_RAIN"))
        assertEquals(AudioManager.Sound.HEAVY_RAIN, AudioManager.Sound.valueOf("HEAVY_RAIN"))
    }

    @Test
    fun `MAX_CONCURRENT_SOUNDS should be 10`() {
        assertEquals(10, AudioManager.MAX_CONCURRENT_SOUNDS)
    }

    @Test
    fun `releasePlayer should not throw exception`() {
        audioManager.releasePlayer(AudioManager.Sound.HEAVY_RAIN)
    }

    @Test
    fun `releaseRemotePlayer should not throw exception`() {
        audioManager.releaseRemotePlayer("test_sound_id")
    }

    @Test
    fun `stopAllSounds should not throw exception`() {
        audioManager.stopAllSounds()
    }

    @Test
    fun `pauseAllSounds should not throw exception`() {
        audioManager.pauseAllSounds()
    }

    @Test
    fun `pauseSound should not throw exception for non-playing sound`() {
        audioManager.pauseSound(AudioManager.Sound.HEAVY_RAIN)
    }

    @Test
    fun `pauseSound with NONE should call pauseAllSounds`() {
        audioManager.pauseSound(AudioManager.Sound.NONE)
    }

    @Test
    fun `getRemoteMetadata should return null for non-existent sound`() {
        val metadata = audioManager.getRemoteMetadata("non_existent_id")
        assertNull(metadata)
    }
}
