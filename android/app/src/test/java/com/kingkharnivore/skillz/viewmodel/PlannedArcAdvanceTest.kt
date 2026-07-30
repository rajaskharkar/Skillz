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
    fun arcLinkedSoftFlowPreservesContinueChoice() {
        assertEquals(
            FlowEndAction.CONTINUE_ARC,
            resolveFlowEndMode(
                requested = FlowEndAction.CONTINUE_ARC,
                isSoftMode = true,
                isInArc = true
            )
        )
    }

    @Test
    fun arcLinkedSoftFlowPreservesCompleteChoice() {
        assertEquals(
            FlowEndAction.COMPLETE_ARC,
            resolveFlowEndMode(
                requested = FlowEndAction.COMPLETE_ARC,
                isSoftMode = true,
                isInArc = true
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
                isInArc = false
            )
        )
    }
}
