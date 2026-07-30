package com.kingkharnivore.skillz.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedArcAdvanceTest {
    @Test
    fun nonFinalStepAdvances() {
        assertEquals(
            PlannedArcAdvanceResult.Advanced,
            plannedArcAdvanceResult(currentStepIndex = 1, totalSteps = 3)
        )
    }

    @Test
    fun finalStepCompletesInsteadOfRepeating() {
        assertEquals(
            PlannedArcAdvanceResult.Completed,
            plannedArcAdvanceResult(currentStepIndex = 2, totalSteps = 3)
        )
    }

    @Test
    fun nonFinalPlannedSoftFlowWaitsForContinuation() {
        assertEquals(
            FlowEndAction.CONTINUE_ARC,
            resolveFlowEndMode(
                requested = FlowEndAction.SAVE_FLOW,
                isSoftMode = true,
                plannedCurrentStepIndex = 0,
                plannedTotalSteps = 2
            )
        )
    }

    @Test
    fun finalPlannedSoftFlowCompletesArc() {
        assertEquals(
            FlowEndAction.COMPLETE_ARC,
            resolveFlowEndMode(
                requested = FlowEndAction.SAVE_FLOW,
                isSoftMode = true,
                plannedCurrentStepIndex = 1,
                plannedTotalSteps = 2
            )
        )
    }

    @Test
    fun standaloneSoftFlowKeepsSaveBehavior() {
        assertEquals(
            FlowEndAction.SAVE_FLOW,
            resolveFlowEndMode(
                requested = FlowEndAction.SAVE_FLOW,
                isSoftMode = true,
                plannedCurrentStepIndex = null,
                plannedTotalSteps = null
            )
        )
    }
}
