package com.kingkharnivore.skillz.viewmodel

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannedArcLaunchTest {
    @Test
    fun zeroTimerDraftAllowsPlannedArcLaunch() {
        assertTrue(canLaunchPlannedArc(ongoing(accumulatedMs = 0L)))
    }

    @Test
    fun absentOngoingFlowAllowsPlannedArcLaunch() {
        assertTrue(canLaunchPlannedArc(null))
    }

    @Test
    fun elapsedTimerBlocksPlannedArcLaunch() {
        assertFalse(canLaunchPlannedArc(ongoing(accumulatedMs = 1L)))
    }

    @Test
    fun runningTimerBlocksPlannedArcLaunch() {
        assertFalse(
            canLaunchPlannedArc(
                ongoing(accumulatedMs = 0L, isRunning = true, baseStartTimeMs = 100L)
            )
        )
    }

    @Test
    fun plannedSurgeSetupKeepsTargetMinutes() {
        assertEquals(
            25,
            plannedSurgeMinutes(
                targetMinutes = 25,
                launchWithSurge = true,
                isSoftMode = false
            )
        )
    }

    @Test
    fun plannedSurgeSetupIsDisabledForSoftFlow() {
        assertNull(
            plannedSurgeMinutes(
                targetMinutes = 25,
                launchWithSurge = true,
                isSoftMode = true
            )
        )
    }

    private fun ongoing(
        accumulatedMs: Long,
        isRunning: Boolean = false,
        baseStartTimeMs: Long? = null
    ) = OngoingSessionEntity(
        flowInstanceId = "flow",
        title = "",
        description = "",
        tagName = "",
        isInFlowMode = isRunning,
        isRunning = isRunning,
        baseStartTimeMs = baseStartTimeMs,
        accumulatedBeforeStartMs = accumulatedMs
    )
}
