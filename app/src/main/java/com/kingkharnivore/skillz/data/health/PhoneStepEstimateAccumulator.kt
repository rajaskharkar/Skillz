package com.kingkharnivore.skillz.data.health

internal class PhoneStepEstimateAccumulator(
    private val source: PhoneStepSource
) {
    var isTracking: Boolean = false
        private set

    private var accumulatedBeforePause = 0L
    private var baselineCounter: Float? = null
    private var latestCounter: Float? = null

    fun beginNewFlow() = clear()

    fun startOrResumeTracking() {
        isTracking = true
        baselineCounter = null
        latestCounter = null
    }

    fun pauseTracking() {
        accumulatedBeforePause = currentSteps()
        isTracking = false
        baselineCounter = null
        latestCounter = null
    }

    fun finishFlowAndGetSteps(): Long {
        val finalSteps = currentSteps()
        clear()
        return finalSteps
    }

    fun cancelAndReset() = clear()

    fun onStepCounter(counterValue: Float) {
        if (!isTracking || source != PhoneStepSource.TYPE_STEP_COUNTER) return
        val baseline = baselineCounter
        if (baseline == null || counterValue < baseline) {
            baselineCounter = counterValue
            latestCounter = counterValue
            return
        }
        latestCounter = counterValue
    }

    fun onStepDetector() {
        if (isTracking && source == PhoneStepSource.TYPE_STEP_DETECTOR) {
            accumulatedBeforePause += 1L
        }
    }

    fun currentSteps(): Long {
        val activeDelta = if (source == PhoneStepSource.TYPE_STEP_COUNTER) {
            val baseline = baselineCounter
            val latest = latestCounter
            if (baseline != null && latest != null && latest >= baseline) (latest - baseline).toLong() else 0L
        } else {
            0L
        }
        return (accumulatedBeforePause + activeDelta).coerceAtLeast(0L)
    }

    private fun clear() {
        isTracking = false
        accumulatedBeforePause = 0L
        baselineCounter = null
        latestCounter = null
    }
}
