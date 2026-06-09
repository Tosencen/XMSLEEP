package org.xmsleep.app.audio

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * LocalSoundPlayer 单元测试
 */
class LocalSoundPlayerTest {

    private lateinit var localSoundPlayer: LocalSoundPlayer

    @Before
    fun setUp() {
        // 重置单例实例以便测试
        val instanceField = LocalSoundPlayer::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)

        localSoundPlayer = LocalSoundPlayer.getInstance()
    }

    @After
    fun tearDown() {
        // 清理单例实例
        val instanceField = LocalSoundPlayer::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun `getInstance should return same instance`() {
        val instance1 = LocalSoundPlayer.getInstance()
        val instance2 = LocalSoundPlayer.getInstance()
        assertSame(instance1, instance2)
    }

    @Test
    fun `isPlayingSound should return false initially`() {
        assertFalse(localSoundPlayer.isPlayingSound(AudioManager.Sound.HEAVY_RAIN))
    }

    @Test
    fun `getPlayingSounds should return empty list initially`() {
        val playingSounds = localSoundPlayer.getPlayingSounds()
        assertTrue(playingSounds.isEmpty())
    }

    @Test
    fun `hasAnyPlayingSounds should return false initially`() {
        assertFalse(localSoundPlayer.hasAnyPlayingSounds())
    }

    @Test
    fun `Sound enum should have correct display names`() {
        assertEquals("伞上雨声", AudioManager.Sound.UMBRELLA_RAIN.displayName)
        assertEquals("划船", AudioManager.Sound.ROWING.displayName)
        assertEquals("办公室", AudioManager.Sound.OFFICE.displayName)
        assertEquals("图书馆", AudioManager.Sound.LIBRARY.displayName)
        assertEquals("大雨", AudioManager.Sound.HEAVY_RAIN.displayName)
        assertEquals("打字机", AudioManager.Sound.TYPEWRITER.displayName)
        assertEquals("打雷", AudioManager.Sound.THUNDER.displayName)
        assertEquals("时钟", AudioManager.Sound.CLOCK.displayName)
        assertEquals("森林鸟鸣", AudioManager.Sound.FOREST_BIRDS.displayName)
        assertEquals("漂流", AudioManager.Sound.DRIFTING.displayName)
        assertEquals("篝火", AudioManager.Sound.CAMPFIRE.displayName)
        assertEquals("起风了", AudioManager.Sound.WIND.displayName)
        assertEquals("键盘", AudioManager.Sound.KEYBOARD.displayName)
        assertEquals("雪地徒步", AudioManager.Sound.SNOW_WALKING.displayName)
        assertEquals("早晨咖啡", AudioManager.Sound.MORNING_COFFEE.displayName)
        assertEquals("风车", AudioManager.Sound.WINDMILL.displayName)
        assertEquals("", AudioManager.Sound.NONE.displayName)
    }

    @Test
    fun `Sound enum should have 17 values`() {
        val soundValues = AudioManager.Sound.values()
        assertEquals(17, soundValues.size)
    }

    @Test
    fun `releasePlayer should not throw exception`() {
        // 测试释放不存在的播放器不会抛出异常
        localSoundPlayer.releasePlayer(AudioManager.Sound.HEAVY_RAIN)
    }

    @Test
    fun `releaseAllPlayers should not throw exception`() {
        // 测试释放所有播放器不会抛出异常
        localSoundPlayer.releaseAllPlayers()
    }

    @Test
    fun `pauseSound should not throw exception for non-playing sound`() {
        // 测试暂停未播放的声音不会抛出异常
        localSoundPlayer.pauseSound(AudioManager.Sound.HEAVY_RAIN)
    }

    @Test
    fun `pauseAllSounds should not throw exception`() {
        // 测试暂停所有声音不会抛出异常
        localSoundPlayer.pauseAllSounds()
    }

    @Test
    fun `stopAllSounds should not throw exception`() {
        // 测试停止所有声音不会抛出异常
        localSoundPlayer.stopAllSounds()
    }
}
