package org.xmsleep.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 本地音频过滤逻辑测试
 *
 * - LocalAudioDurationFilter：时长区间边界（含 0/负值等未知时长）
 * - isHiddenSystemFolder：系统文件夹/隐藏目录识别
 */
class LocalAudioFilterTest {

    // === 时长过滤 ===

    @Test
    fun `ALL should match every duration including unknown`() {
        assertTrue(LocalAudioDurationFilter.ALL.matches(0L))
        assertTrue(LocalAudioDurationFilter.ALL.matches(-1L))
        assertTrue(LocalAudioDurationFilter.ALL.matches(1L))
        assertTrue(LocalAudioDurationFilter.ALL.matches(59_999L))
        assertTrue(LocalAudioDurationFilter.ALL.matches(60_000L))
        assertTrue(LocalAudioDurationFilter.ALL.matches(600_000L))
        assertTrue(LocalAudioDurationFilter.ALL.matches(3_600_000L))
    }

    @Test
    fun `UNDER_1_MIN should match durations below 60s`() {
        val filter = LocalAudioDurationFilter.UNDER_1_MIN
        assertFalse(filter.matches(0L))          // 未知时长不放行
        assertFalse(filter.matches(-5L))
        assertTrue(filter.matches(1L))
        assertTrue(filter.matches(30_000L))
        assertTrue(filter.matches(59_999L))
        assertFalse(filter.matches(60_000L))     // 边界：60s 归入 1-3 分钟
        assertFalse(filter.matches(61_000L))
    }

    @Test
    fun `MIN_1_3 should match durations from 60s to 180s`() {
        val filter = LocalAudioDurationFilter.MIN_1_3
        assertFalse(filter.matches(0L))
        assertFalse(filter.matches(59_999L))
        assertTrue(filter.matches(60_000L))
        assertTrue(filter.matches(120_000L))
        assertTrue(filter.matches(179_999L))
        assertFalse(filter.matches(180_000L))    // 边界：180s 归入 3-10 分钟
    }

    @Test
    fun `MIN_3_10 should match durations from 180s to 600s`() {
        val filter = LocalAudioDurationFilter.MIN_3_10
        assertFalse(filter.matches(179_999L))
        assertTrue(filter.matches(180_000L))
        assertTrue(filter.matches(300_000L))
        assertTrue(filter.matches(599_999L))
        assertFalse(filter.matches(600_000L))    // 边界：600s 归入 10 分钟以上
    }

    @Test
    fun `OVER_10_MIN should match durations at or above 600s`() {
        val filter = LocalAudioDurationFilter.OVER_10_MIN
        assertFalse(filter.matches(599_999L))
        assertTrue(filter.matches(600_000L))
        assertTrue(filter.matches(601_000L))
        assertTrue(filter.matches(3_600_000L))
    }

    @Test
    fun `unknown duration should only appear under ALL`() {
        val unknown = listOf(0L, -1L, -10_000L)
        for (duration in unknown) {
            assertTrue("duration=$duration should match ALL", LocalAudioDurationFilter.ALL.matches(duration))
            assertFalse(LocalAudioDurationFilter.UNDER_1_MIN.matches(duration))
            assertFalse(LocalAudioDurationFilter.MIN_1_3.matches(duration))
            assertFalse(LocalAudioDurationFilter.MIN_3_10.matches(duration))
            assertFalse(LocalAudioDurationFilter.OVER_10_MIN.matches(duration))
        }
    }

    // === 文件夹隐藏规则 ===

    @Test
    fun `system folders should be hidden`() {
        assertTrue(isHiddenSystemFolder("Recordings"))
        assertTrue(isHiddenSystemFolder("Ringtones"))
        assertTrue(isHiddenSystemFolder("Notifications"))
        assertTrue(isHiddenSystemFolder("Alarms"))
        assertTrue(isHiddenSystemFolder("Podcasts"))
        assertTrue(isHiddenSystemFolder("Call Recordings"))
        assertTrue(isHiddenSystemFolder("Voice Recorder"))
        assertTrue(isHiddenSystemFolder("WhatsApp Audio"))
    }

    @Test
    fun `hidden dot folders should be hidden`() {
        assertTrue(isHiddenSystemFolder(".thumbnails"))
        assertTrue(isHiddenSystemFolder(".Trash"))
        assertTrue(isHiddenSystemFolder(".android"))
    }

    @Test
    fun `normal folders should not be hidden`() {
        assertFalse(isHiddenSystemFolder("Music"))
        assertFalse(isHiddenSystemFolder("白噪音"))
        assertFalse(isHiddenSystemFolder("冥想"))
        assertFalse(isHiddenSystemFolder("Download"))
        assertFalse(isHiddenSystemFolder("雨声"))
    }

    @Test
    fun `system folder check should be case insensitive`() {
        assertTrue(isHiddenSystemFolder("recordings"))
        assertTrue(isHiddenSystemFolder("RECORDINGS"))
        assertTrue(isHiddenSystemFolder("ReCoRdInGs"))
    }
}
