package org.xmsleep.app.audio

import android.net.Uri
import io.mockk.mockk
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * LocalAudioPlayer.setPlaylist 播放列表重定位测试
 *
 * 核心场景：切换文件夹/时长筛选后播放列表会缩小，
 * 正在播的音频不打断，只把“下一首”索引重定位到新列表；
 * 若当前音频不在新列表中，索引置 -1（播完当前后从新列表第一首开始）。
 */
class LocalAudioPlayerPlaylistTest {

    private lateinit var player: LocalAudioPlayer

    private fun currentPlayIndex(): Int {
        val field = LocalAudioPlayer::class.java.getDeclaredField("currentPlayIndex")
        field.isAccessible = true
        return field.getInt(player)
    }

    private fun playlist(): List<Long> {
        val field = LocalAudioPlayer::class.java.getDeclaredField("_playlist")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(player) as kotlinx.coroutines.flow.MutableStateFlow<List<Long>>
        return flow.value
    }

    private fun setPlaylistValue(ids: List<Long>) {
        val field = LocalAudioPlayer::class.java.getDeclaredField("_playlist")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(player) as kotlinx.coroutines.flow.MutableStateFlow<List<Long>>
        flow.value = ids
    }

    private fun setCurrentPlayIndex(index: Int) {
        val field = LocalAudioPlayer::class.java.getDeclaredField("currentPlayIndex")
        field.isAccessible = true
        field.setInt(player, index)
    }

    @Before
    fun setUp() {
        val instanceField = LocalAudioPlayer::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
        player = LocalAudioPlayer.getInstance()
    }

    @After
    fun tearDown() {
        val instanceField = LocalAudioPlayer::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
    }

    private fun uri(id: Long): Uri = mockk(relaxed = true)

    @Test
    fun `setPlaylist with empty list keeps index at -1`() {
        player.setPlaylist(emptyList())
        assertEquals(-1, currentPlayIndex())
        assertEquals(0, playlist().size)
    }

    @Test
    fun `setPlaylist when nothing playing sets index to -1`() {
        setCurrentPlayIndex(-1)
        player.setPlaylist(listOf(1L to uri(1), 2L to uri(2), 3L to uri(3)))
        assertEquals(-1, currentPlayIndex())
        assertEquals(listOf(1L, 2L, 3L), playlist())
    }

    @Test
    fun `setPlaylist relocates index when current audio stays in new list`() {
        // 当前正在播第 2 首（索引 1，音频 id=2）
        setPlaylistValue(listOf(1L, 2L, 3L))
        setCurrentPlayIndex(1)
        // 筛选后列表收缩为 [2, 3]
        player.setPlaylist(listOf(2L to uri(2), 3L to uri(3)))
        // 当前音频 2 在新列表索引 0
        assertEquals(0, currentPlayIndex())
        assertEquals(listOf(2L, 3L), playlist())
    }

    @Test
    fun `setPlaylist keeps last position when current audio is last item`() {
        setPlaylistValue(listOf(1L, 2L, 3L))
        setCurrentPlayIndex(2)
        player.setPlaylist(listOf(3L to uri(3)))
        assertEquals(0, currentPlayIndex())
        assertEquals(listOf(3L), playlist())
    }

    @Test
    fun `setPlaylist resets index to -1 when current audio filtered out`() {
        setPlaylistValue(listOf(1L, 2L, 3L))
        setCurrentPlayIndex(1)
        // 当前音频 2 被过滤掉，只剩 [3]
        player.setPlaylist(listOf(3L to uri(3)))
        assertEquals(-1, currentPlayIndex())
        assertEquals(listOf(3L), playlist())
    }

    @Test
    fun `setPlaylist resets index to -1 when all audio filtered out`() {
        setPlaylistValue(listOf(1L, 2L))
        setCurrentPlayIndex(0)
        player.setPlaylist(emptyList())
        assertEquals(-1, currentPlayIndex())
        assertEquals(0, playlist().size)
    }

    @Test
    fun `setPlaylist replacing same ids keeps original index`() {
        setPlaylistValue(listOf(10L, 20L, 30L))
        setCurrentPlayIndex(2)
        player.setPlaylist(listOf(10L to uri(10), 20L to uri(20), 30L to uri(30)))
        assertEquals(2, currentPlayIndex())
        assertEquals(listOf(10L, 20L, 30L), playlist())
    }

    @Test
    fun `setPlaylist caches uris for restore`() {
        player.setPlaylist(listOf(7L to uri(7)))
        val cacheField = LocalAudioPlayer::class.java.getDeclaredField("audioUriCache")
        cacheField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val cache = cacheField.get(player) as java.util.concurrent.ConcurrentHashMap<Long, String>
        assertEquals(1, cache.size)
        assertEquals(true, cache.containsKey(7L))
    }
}
