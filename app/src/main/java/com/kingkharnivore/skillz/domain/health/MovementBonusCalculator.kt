package com.kingkharnivore.skillz.domain.health

class MovementBonusCalculator {
    fun calculateMovementPoints(steps: Long): Long = steps.coerceAtLeast(0L) / STEPS_PER_POINT

    companion object {
        const val STEPS_PER_POINT: Long = 25L
    }
}

data class MovementBonusEligibilityInput(
    val movementBonusEnabled: Boolean,
    val healthConnectAvailable: Boolean,
    val readStepsPermissionGranted: Boolean,
    val isRegularPointEligibleFlow: Boolean,
    val isSoftFlow: Boolean
)

class MovementBonusEligibilityPolicy {
    fun isEligible(input: MovementBonusEligibilityInput): Boolean =
        input.movementBonusEnabled &&
            input.healthConnectAvailable &&
            input.readStepsPermissionGranted &&
            input.isRegularPointEligibleFlow &&
            !input.isSoftFlow
}

data class FlowRewardBreakdown(
    val baseFlowPoints: Long,
    val pulseBonusPoints: Long = 0L,
    val surgeBonusPoints: Long = 0L,
    val otherPreMultiplierBonusPoints: Long = 0L,
    val movementPoints: Long = 0L,
    val preMultiplierTotal: Long,
    val arcMultiplier: Double = 1.0,
    val streakMultiplier: Double = 1.0,
    val otherMultiplier: Double = 1.0,
    val arcBonusPoints: Long = 0L,
    val finalScyraPoints: Long,
    val pearlsEarned: Long
)

object MovementRewardRecalculator {
    fun withMovementPoints(
        baseFlowPoints: Long,
        pulseBonusPoints: Long = 0L,
        surgeBonusPoints: Long = 0L,
        otherPreMultiplierBonusPoints: Long = 0L,
        movementPoints: Long,
        arcMultiplier: Double = 1.0,
        streakMultiplier: Double = 1.0,
        otherMultiplier: Double = 1.0,
        pearlEligible: Boolean
    ): FlowRewardBreakdown {
        val preMultiplierTotal = baseFlowPoints + pulseBonusPoints + surgeBonusPoints +
            otherPreMultiplierBonusPoints + movementPoints
        val multiplier = arcMultiplier * streakMultiplier * otherMultiplier
        val finalScyraPoints = kotlin.math.round(preMultiplierTotal * multiplier).toLong()
        val arcBonusPoints = (finalScyraPoints - preMultiplierTotal).coerceAtLeast(0L)
        return FlowRewardBreakdown(
            baseFlowPoints = baseFlowPoints,
            pulseBonusPoints = pulseBonusPoints,
            surgeBonusPoints = surgeBonusPoints,
            otherPreMultiplierBonusPoints = otherPreMultiplierBonusPoints,
            movementPoints = movementPoints,
            preMultiplierTotal = preMultiplierTotal,
            arcMultiplier = arcMultiplier,
            streakMultiplier = streakMultiplier,
            otherMultiplier = otherMultiplier,
            arcBonusPoints = arcBonusPoints,
            finalScyraPoints = finalScyraPoints,
            pearlsEarned = if (pearlEligible) finalScyraPoints else 0L
        )
    }
}

data class FlowActiveInterval(
    val startTimeMs: Long,
    val endTimeMs: Long
)

object FlowActiveIntervalNormalizer {
    fun normalize(intervals: List<FlowActiveInterval>): List<FlowActiveInterval> {
        val sorted = intervals
            .filter { it.endTimeMs > it.startTimeMs }
            .sortedBy { it.startTimeMs }
        if (sorted.isEmpty()) return emptyList()
        val merged = mutableListOf<FlowActiveInterval>()
        for (interval in sorted) {
            val last = merged.lastOrNull()
            if (last == null || interval.startTimeMs > last.endTimeMs) {
                merged += interval
            } else {
                merged[merged.lastIndex] = last.copy(endTimeMs = maxOf(last.endTimeMs, interval.endTimeMs))
            }
        }
        return merged
    }
}
