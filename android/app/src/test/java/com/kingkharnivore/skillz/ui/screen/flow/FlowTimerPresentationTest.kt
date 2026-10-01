package com.kingkharnivore.skillz.ui.screen.flow

import com.kingkharnivore.skillz.model.state.flow.FlowUiState
import com.kingkharnivore.skillz.model.state.flow.StopwatchState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FlowTimerPresentationTest {
    private fun state(elapsedMs: Long, surge: Boolean = true, target: Long? = 30 * 60_000L) =
        FlowUiState(
            isInFlowMode = true,
            isSurgeOn = surge,
            surgePlannedMs = target,
            stopwatch = StopwatchState(isRunning = true, elapsedMs = elapsedMs)
        )

    @Test fun requiredTimerValues() = withEnglishLocale {
        assertEquals("10:00", state(600_000L, surge = false).mainTimerText)
        listOf(
            0L to "30:00",
            600_000L to "20:00",
            1_799_000L to "00:01",
            1_800_000L to "00:00",
            1_801_000L to "-00:01",
            2_100_000L to "-05:00"
        ).forEach { (elapsed, expected) ->
            assertEquals(expected, state(elapsed).mainTimerText)
        }
    }

    @Test fun fractionalTickerUpdatesHaveAFullZeroSecondAndNoNegativeZero() = withEnglishLocale {
        assertEquals(
            listOf("00:03", "00:02", "00:01", "00:00", "-00:01", "-00:02"),
            (-3L..2L).map { state(1_800_000L + it * 1000L + 427L).mainTimerText }
        )
        assertEquals("00:01", state(1_799_999L).mainTimerText)
        assertEquals("00:00", state(1_800_999L).mainTimerText)
    }

    @Test fun normalFlowKeepsCountingUpAndPreservesHourFormatting() = withEnglishLocale {
        assertEquals(listOf("00:00", "00:01", "00:02"),
            (0L..2L).map { state(it * 1000L + 427L, surge = false).mainTimerText })
        assertEquals("1:02:03", state(3_723_999L, surge = false).mainTimerText)
        assertEquals("-1:02:03", state(1_800_000L + 3_723_999L).mainTimerText)
        assertEquals("1:30:00", state(0L, target = 5_400_000L).mainTimerText)
    }

    @Test fun modeChangesAndRestoredInputsDerivePresentationWithoutChangingElapsedTime() = withEnglishLocale {
        val normal = state(2_100_123L, surge = false)
        val surge = normal.copy(isSurgeOn = true)
        assertEquals("35:00", normal.mainTimerText)
        assertEquals("-05:00", surge.mainTimerText)
        assertEquals(normal.stopwatch, surge.stopwatch)
        assertEquals("-05:00", state(2_100_123L).mainTimerText)
        assertEquals("-05:01", surge.copy(stopwatch = surge.stopwatch.copy(elapsedMs = 2_101_123L)).mainTimerText)
        assertEquals("-05:00", surge.copy(stopwatch = surge.stopwatch.copy(isRunning = false)).mainTimerText)
        assertEquals("10:00", state(600_000L, target = null).mainTimerText)
    }

    private fun withEnglishLocale(block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.US)
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
