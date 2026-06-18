package com.kingkharnivore.skillz.utils.time

import org.junit.Assert.assertEquals
import org.junit.Test

class IdeaGroveDurationFormatterTest {
    @Test
    fun formatsFlowHistoryDurationsWithCompactUnits() {
        assertEquals("24m 12s", formatIdeaGroveDuration(24 * 60_000L + 12_000L))
        assertEquals("1h 3m 40s", formatIdeaGroveDuration(60 * 60_000L + 3 * 60_000L + 40_000L))
        assertEquals("5h 18m", formatIdeaGroveDuration(5 * 60 * 60_000L + 18 * 60_000L))
    }

    @Test
    fun formatsDurationForSpeech() {
        assertEquals(
            "1 hour 3 minutes 40 seconds",
            formatIdeaGroveDurationForSpeech(60 * 60_000L + 3 * 60_000L + 40_000L)
        )
    }
}
