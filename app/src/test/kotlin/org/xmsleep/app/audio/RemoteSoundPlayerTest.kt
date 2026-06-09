package org.xmsleep.app.audio

import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * RemoteSoundPlayer 单元测试
 */
class RemoteSoundPlayerTest {

    private lateinit var remoteSoundPlayer: RemoteSoundPlayer

    @Before
    fun setUp() {
        // 重置单例实例以便测试
        val instanceField = RemoteSoundPlayer::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)

        remoteSoundPlayer = RemoteSoundPlayer.getInstance()
    }

    @After
    fun tearDown() {
        // 清理单例实例
        val instanceField = RemoteSoundPlayer::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    @Test
    fun `getInstance should return same instance`() {
        val instance1 = RemoteSoundPlayer.getInstance()
        val instance2 = RemoteSoundPlayer.getInstance()
        assertSame(instance1, instance2)
    }

    @Test
    fun `isPlayingRemoteSound should return false initially`() {
        assertFalse(remoteSoundPlayer.isPlayingRemoteSound("test_sound_id"))
    }

    @Test
    fun `getPlayingRemoteSoundIds should return empty list initially`() {
        val playingIds = remoteSoundPlayer.getPlayingRemoteSoundIds()
        assertTrue(playingIds.isEmpty())
    }

    @Test
    fun `hasAnyPlayingSounds should return false initially`() {
        assertFalse(remoteSoundPlayer.hasAnyPlayingSounds())
    }

    @Test
    fun `getRemoteMetadata should return null for non-existent sound`() {
        val metadata = remoteSoundPlayer.getRemoteMetadata("non_existent_id")
        assertNull(metadata)
    }

    @Test
    fun `releaseRemotePlayer should not throw exception`() {
        // 测试释放不存在的播放器不会抛出异常
        remoteSoundPlayer.releaseRemotePlayer("test_sound_id")
    }

    @Test
    fun `releaseAllRemotePlayers should not throw exception`() {
        // 测试释放所有播放器不会抛出异常
        remoteSoundPlayer.releaseAllRemotePlayers()
    }

    @Test
    fun `pauseRemoteSound should not throw exception for non-playing sound`() {
        // 测试暂停未播放的声音不会抛出异常
        remoteSoundPlayer.pauseRemoteSound("test_sound_id")
        assertFalse(remoteSoundPlayer.isPlayingRemoteSound("test_sound_id"))
    }

    @Test
    fun `pauseAllRemoteSounds should not throw exception`() {
        // 测试暂停所有远程声音不会抛出异常
        remoteSoundPlayer.pauseAllRemoteSounds()
    }
}
