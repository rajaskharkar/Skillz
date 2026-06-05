package com.kingkharnivore.skillz.data.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PhoneStepEstimateAccumulatorTest {
    @Test fun finishFlowReturnsFinalStepsAndClearsStateForNextFlow() {
        val accumulator = PhoneStepEstimateAccumulator(PhoneStepSource.TYPE_STEP_COUNTER)
        accumulator.beginNewFlow()
        accumulator.startOrResumeTracking()
        accumulator.onStepCounter(1_000f)
        accumulator.onStepCounter(1_062f)

        assertEquals(62L, accumulator.finishFlowAndGetSteps())
        assertEquals(0L, accumulator.currentSteps())
        assertFalse(accumulator.isTracking)

        accumulator.beginNewFlow()
        assertEquals(0L, accumulator.currentSteps())
    }

    @Test fun pauseResumeExcludesPausedCounterSteps() {
        val accumulator = PhoneStepEstimateAccumulator(PhoneStepSource.TYPE_STEP_COUNTER)
        accumulator.beginNewFlow()
        accumulator.startOrResumeTracking()
        accumulator.onStepCounter(1_000f)
        accumulator.onStepCounter(1_030f)
        accumulator.pauseTracking()

        accumulator.onStepCounter(1_100f)
        assertEquals(30L, accumulator.currentSteps())

        accumulator.startOrResumeTracking()
        accumulator.onStepCounter(1_100f)
        accumulator.onStepCounter(1_120f)
        assertEquals(50L, accumulator.currentSteps())
    }

    @Test fun counterResetDoesNotProduceNegativeSteps() {
        val accumulator = PhoneStepEstimateAccumulator(PhoneStepSource.TYPE_STEP_COUNTER)
        accumulator.beginNewFlow()
        accumulator.startOrResumeTracking()
        accumulator.onStepCounter(1_000f)
        accumulator.onStepCounter(990f)

        assertEquals(0L, accumulator.currentSteps())
    }

    @Test fun detectorIncrementsOnlyWhileTracking() {
        val accumulator = PhoneStepEstimateAccumulator(PhoneStepSource.TYPE_STEP_DETECTOR)
        accumulator.onStepDetector()
        assertEquals(0L, accumulator.currentSteps())

        accumulator.startOrResumeTracking()
        repeat(3) { accumulator.onStepDetector() }
        assertEquals(3L, accumulator.currentSteps())

        accumulator.pauseTracking()
        accumulator.onStepDetector()
        assertEquals(3L, accumulator.currentSteps())
    }
}
