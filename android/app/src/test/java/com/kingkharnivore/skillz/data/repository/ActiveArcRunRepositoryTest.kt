package com.kingkharnivore.skillz.data.repository

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Test

class ActiveArcRunRepositoryTest {
    @Test
    fun launchingPlannedArcPreservesExistingArcProgress() {
        val existing = ArcRuntimeState(
            arcId = 42L,
            isPending = false,
            multiplier = 1.5,
            progressMs = 0L,
            lastSessionEndTimeMs = 100L,
            sessionCountInArc = 5
        )

        val result = plannedArcRuntimeForLaunch(existing, nowMs = 200L)

        assertEquals(existing, result)
        assertEquals(6, result.sessionCountInArc + 1)
    }

    @Test
    fun launchingPlannedArcCreatesRuntimeWhenNoArcExists() {
        val result = plannedArcRuntimeForLaunch(existingArc = null, nowMs = 200L)

        assertEquals(200L, result.arcId)
        assertEquals(0, result.sessionCountInArc)
        assertEquals(1, result.sessionCountInArc + 1)
    }
}
