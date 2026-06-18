package com.kingkharnivore.skillz.viewmodel

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IdeaGroveActiveFlowStateTest {
    @Test
    fun nullOngoingSessionIsNotActive() {
        val ongoing: OngoingSessionEntity? = null
        assertFalse(ongoing.isMeaningfulActiveFlow())
    }

    @Test
    fun zeroTimeDraftIsNotActive() {
        assertFalse(draft().isMeaningfulActiveFlow())
    }

    @Test
    fun zeroTimePulseOriginDraftIsNotActiveAndIsAbandoned() {
        val ongoing = draft(originPulseId = 42L)

        assertFalse(ongoing.isMeaningfulActiveFlow())
        assertTrue(ongoing.isAbandonedPulseOriginDraft())
    }

    @Test
    fun runningOngoingSessionIsActive() {
        assertTrue(draft(isRunning = true).isMeaningfulActiveFlow())
    }

    @Test
    fun flowModeOngoingSessionIsActive() {
        assertTrue(draft(isInFlowMode = true).isMeaningfulActiveFlow())
    }

    @Test
    fun elapsedOngoingSessionIsActive() {
        assertTrue(draft(accumulatedBeforeStartMs = 1L).isMeaningfulActiveFlow())
    }

    @Test
    fun baseStartOngoingSessionIsActive() {
        assertTrue(draft(baseStartTimeMs = 1L).isMeaningfulActiveFlow())
    }

    private fun draft(
        isRunning: Boolean = false,
        isInFlowMode: Boolean = false,
        accumulatedBeforeStartMs: Long = 0L,
        baseStartTimeMs: Long? = null,
        originPulseId: Long? = null
    ) = OngoingSessionEntity(
        flowInstanceId = "flow",
        title = "",
        description = "",
        tagName = "",
        isInFlowMode = isInFlowMode,
        isRunning = isRunning,
        baseStartTimeMs = baseStartTimeMs,
        accumulatedBeforeStartMs = accumulatedBeforeStartMs,
        originPulseId = originPulseId
    )
}
