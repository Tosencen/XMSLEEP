package org.xmsleep.app.timer

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class TimerManagerTest {

    private lateinit var timerManager: TimerManager

    @Before
    fun setUp() {
        val instanceField = TimerManager::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)

        // Mock Handler construction so Looper.getMainLooper() doesn't crash in JVM tests
        mockkConstructor(android.os.Handler::class)
        every { anyConstructed<android.os.Handler>().postDelayed(any(), any<Long>()) } returns true
        every { anyConstructed<android.os.Handler>().removeCallbacks(any()) } returns Unit

        timerManager = TimerManager.getInstance()
    }

    @After
    fun tearDown() {
        timerManager.releaseResources()
        val instanceField = TimerManager::class.java.getDeclaredField("instance")
        instanceField.isAccessible = true
        instanceField.set(null, null)
        unmockkConstructor(android.os.Handler::class)
    }

    @Test
    fun `getInstance returns same instance`() {
        assertSame(timerManager, TimerManager.getInstance())
    }

    @Test
    fun `initial state - timer not active`() {
        assertFalse(timerManager.isTimerActive.value)
        assertFalse(timerManager.isTimerPaused.value)
        assertEquals(0L, timerManager.timeLeftMillis.value)
        assertEquals(0, timerManager.getCurrentTimerMinutes())
    }

    @Test
    fun `startTimer activates timer`() {
        timerManager.startTimer(5)

        assertTrue(timerManager.isTimerActive.value)
        assertEquals(5, timerManager.getCurrentTimerMinutes())
        assertTrue(timerManager.timeLeftMillis.value > 0)
    }

    @Test
    fun `startTimer with 0 does not activate`() {
        timerManager.startTimer(0)

        assertFalse(timerManager.isTimerActive.value)
    }

    @Test
    fun `startTimer with negative does not activate`() {
        timerManager.startTimer(-1)

        assertFalse(timerManager.isTimerActive.value)
    }

    @Test
    fun `cancelTimer deactivates timer`() {
        timerManager.startTimer(5)
        timerManager.cancelTimer()

        assertFalse(timerManager.isTimerActive.value)
        assertEquals(0L, timerManager.timeLeftMillis.value)
    }

    @Test
    fun `cancelTimer notifies listeners with onTimerCancelled`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener)
        timerManager.startTimer(5)
        timerManager.cancelTimer()

        verify { listener.onTimerCancelled() }
    }

    @Test
    fun `cancelTimer with notifyListeners=false does not notify`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener)
        timerManager.startTimer(5)
        timerManager.cancelTimer(notifyListeners = false)

        verify(exactly = 0) { listener.onTimerCancelled() }
    }

    @Test
    fun `finishTimer notifies listeners with onTimerFinished`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener)
        timerManager.startTimer(5)

        callFinishTimer()

        verify { listener.onTimerFinished(5) }
    }

    @Test
    fun `finishTimer sets timer inactive`() {
        timerManager.startTimer(5)

        callFinishTimer()

        assertFalse(timerManager.isTimerActive.value)
        assertEquals(0L, timerManager.timeLeftMillis.value)
        assertEquals(0, timerManager.getCurrentTimerMinutes())
    }

    @Test
    fun `finishTimer notifies all registered listeners`() {
        val listener1 = mockk<TimerManager.TimerListener>(relaxed = true)
        val listener2 = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener1)
        timerManager.addListener(listener2)
        timerManager.startTimer(10)

        callFinishTimer()

        verify { listener1.onTimerFinished(10) }
        verify { listener2.onTimerFinished(10) }
    }

    @Test
    fun `finishTimer only notifies once (hasFinished guard)`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener)
        timerManager.startTimer(5)

        callFinishTimer()
        callFinishTimer()

        verify(exactly = 1) { listener.onTimerFinished(5) }
    }

    @Test
    fun `finishTimer exception in one listener does not block others`() {
        val listener1 = mockk<TimerManager.TimerListener>(relaxed = true)
        val listener2 = mockk<TimerManager.TimerListener>(relaxed = true)
        every { listener1.onTimerFinished(any()) } throws RuntimeException("boom")
        timerManager.addListener(listener1)
        timerManager.addListener(listener2)
        timerManager.startTimer(5)

        callFinishTimer()

        verify { listener2.onTimerFinished(5) }
    }

    @Test
    fun `finishTimer exception in beforeFinishCallback does not block listeners`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.setBeforeFinishCallback { throw RuntimeException("callback boom") }
        timerManager.addListener(listener)
        timerManager.startTimer(5)

        callFinishTimer()

        verify { listener.onTimerFinished(5) }
    }

    @Test
    fun `pauseTimer pauses and saves remaining time`() {
        timerManager.startTimer(5)
        timerManager.pauseTimer()

        assertTrue(timerManager.isTimerPaused.value)
        assertTrue(timerManager.timeLeftMillis.value > 0)
    }

    @Test
    fun `resumeTimer resumes countdown`() {
        timerManager.startTimer(5)
        timerManager.pauseTimer()
        timerManager.resumeTimer()

        assertFalse(timerManager.isTimerPaused.value)
        assertTrue(timerManager.isTimerActive.value)
    }

    @Test
    fun `starting new timer cancels old one`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener)
        timerManager.startTimer(10)
        timerManager.startTimer(5)

        assertEquals(5, timerManager.getCurrentTimerMinutes())
        assertTrue(timerManager.isTimerActive.value)
    }

    @Test
    fun `addListener while timer active notifies with current tick`() {
        timerManager.startTimer(5)
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)

        timerManager.addListener(listener)

        verify { listener.onTimerTick(any()) }
    }

    @Test
    fun `releaseResources clears everything`() {
        val listener = mockk<TimerManager.TimerListener>(relaxed = true)
        timerManager.addListener(listener)
        timerManager.startTimer(5)

        timerManager.releaseResources()

        assertFalse(timerManager.isTimerActive.value)
    }

    @Test
    fun `handler postDelayed is called when timer starts`() {
        timerManager.startTimer(5)

        verify {
            anyConstructed<android.os.Handler>().postDelayed(any(), match { it > 0L })
        }
    }

    @Test
    fun `handler removeCallbacks is called when timer is cancelled`() {
        timerManager.startTimer(5)
        timerManager.cancelTimer()

        verify {
            anyConstructed<android.os.Handler>().removeCallbacks(any())
        }
    }

    @Test
    fun `getTimeLeftMillis returns 0 when timer not active`() {
        assertEquals(0L, timerManager.getTimeLeftMillis())
    }

    @Test
    fun `getTimeLeftMillis returns positive when timer active`() {
        timerManager.startTimer(5)
        assertTrue(timerManager.getTimeLeftMillis() > 0)
    }

    private fun callFinishTimer() {
        val method = TimerManager::class.java.getDeclaredMethod("finishTimer")
        method.isAccessible = true
        method.invoke(timerManager)
    }
}
