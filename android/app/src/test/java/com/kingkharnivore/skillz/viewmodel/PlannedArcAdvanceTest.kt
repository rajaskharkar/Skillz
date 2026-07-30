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
}
