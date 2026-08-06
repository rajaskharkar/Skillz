package com.kingkharnivore.skillz.ui.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ArcRuntimeStateTest {
    private val activeArc = ArcRuntimeState(
        arcId = 42L,
        isPending = false,
        multiplier = 2.0,
        progressMs = 123L,
        lastSessionEndTimeMs = 1_000L,
        sessionCountInArc = 6,
        pauseUsedMs = 50L,
        pauseStartedAtMs = 75L
    )

    @Test
    fun resetMultiplierForSoftFlow_preservesAllContinuityState() {
        val reset = activeArc.resetMultiplierForSoftFlow()

        assertEquals(activeArc.copy(multiplier = 1.0), reset)
        assertEquals(42L, reset.arcId)
        assertEquals(6, reset.sessionCountInArc)
    }

    @Test
    fun completedSoftFlow_advancesSequenceAndKeepsResetMultiplier() {
        val completed = activeArc.resetMultiplierForSoftFlow()
            .afterCompletedSoftFlow(sessionEndTimeMs = 2_000L)

        assertEquals(42L, completed.arcId)
        assertEquals(7, completed.sessionCountInArc)
        assertEquals(1.0, completed.multiplier, 0.0)
        assertEquals(2_000L, completed.lastSessionEndTimeMs)
        assertEquals(activeArc.pauseUsedMs, completed.pauseUsedMs)
    }
}
