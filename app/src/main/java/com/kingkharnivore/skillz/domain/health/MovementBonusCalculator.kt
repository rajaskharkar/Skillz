package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.data.model.entity.health.MovementDataSourceType

class MovementBonusCalculator {
    fun calculateMovementPoints(steps: Long): Long = steps.coerceAtLeast(0L) / STEPS_PER_POINT

    fun selectAwardedMovement(
        previouslyAwardedSteps: Long = 0L,
        phoneEstimatedSteps: Long? = null,
        healthConnectSteps: Long? = null,
        reconciledHealthConnect: Boolean = false
    ): AwardedMovement {
        val previous = previouslyAwardedSteps.coerceAtLeast(0L)
        val phone = phoneEstimatedSteps?.coerceAtLeast(0L) ?: 0L
        val health = healthConnectSteps?.coerceAtLeast(0L) ?: 0L
        val finalSteps = maxOf(previous, phone, health)
        val source = when {
            finalSteps <= 0L -> MovementDataSourceType.NONE
            health >= finalSteps && reconciledHealthConnect -> MovementDataSourceType.HEALTH_CONNECT_RECONCILED
            health >= finalSteps -> MovementDataSourceType.HEALTH_CONNECT
            phone >= finalSteps -> MovementDataSourceType.PHONE_SENSOR
            else -> MovementDataSourceType.NONE
        }
        return AwardedMovement(
            finalAwardedSteps = finalSteps.takeIf { it > 0L },
            finalAwardedMovementPoints = calculateMovementPoints(finalSteps),
            movementDataSource = source,
            phoneEstimatedMovementPoints = calculateMovementPoints(phone),
            healthConnectMovementPoints = calculateMovementPoints(health)
        )
    }

    companion object {
        const val STEPS_PER_POINT: Long = 25L
    }
}

data class AwardedMovement(
    val finalAwardedSteps: Long?,
    val finalAwardedMovementPoints: Long,
    val movementDataSource: MovementDataSourceType,
    val phoneEstimatedMovementPoints: Long,
    val healthConnectMovementPoints: Long
)

data class MovementBonusEligibilityInput(
    val movementBonusEnabled: Boolean,
    val healthConnectAvailable: Boolean,
    val readStepsPermissionGranted: Boolean,
    val isRegularPointEligibleFlow: Boolean,
    val isSoftFlow: Boolean,
    val phoneStepTrackingAvailable: Boolean = false,
    val activityRecognitionPermissionGranted: Boolean = false
)

class MovementBonusEligibilityPolicy {
    fun isEligible(input: MovementBonusEligibilityInput): Boolean =
        input.movementBonusEnabled &&
            (input.healthConnectAvailable && input.readStepsPermissionGranted ||
                input.phoneStepTrackingAvailable && input.activityRecognitionPermissionGranted) &&
            input.isRegularPointEligibleFlow &&
            !input.isSoftFlow
}

data class FlowRewardBreakdown(
    val nonMovementPreMultiplierPoints: Long,
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

/**
 * Central Movement Bonus reward formula.
 *
 * Scyra currently has an Arc multiplier only in the Flow completion path. Streak and
 * other multiplier hooks are intentionally represented here and persisted as 1.0 so
 * future multipliers plug into one formula instead of duplicating health-specific math.
 */
object MovementRewardRecalculator {
    fun withMovementPoints(
        nonMovementPreMultiplierPoints: Long,
        pulseBonusPoints: Long = 0L,
        surgeBonusPoints: Long = 0L,
        otherPreMultiplierBonusPoints: Long = 0L,
        movementPoints: Long,
        arcMultiplier: Double = 1.0,
        streakMultiplier: Double = 1.0,
        otherMultiplier: Double = 1.0,
        pearlEligible: Boolean
    ): FlowRewardBreakdown {
        val preMultiplierTotal = nonMovementPreMultiplierPoints + pulseBonusPoints + surgeBonusPoints +
            otherPreMultiplierBonusPoints + movementPoints
        val multiplier = arcMultiplier * streakMultiplier * otherMultiplier
        val finalScyraPoints = kotlin.math.round(preMultiplierTotal * multiplier).toLong()
        val arcBonusPoints = (finalScyraPoints - preMultiplierTotal).coerceAtLeast(0L)
        return FlowRewardBreakdown(
            nonMovementPreMultiplierPoints = nonMovementPreMultiplierPoints,
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

object FlowActiveIntervalCodec {
    fun encode(intervals: List<FlowActiveInterval>): String =
        FlowActiveIntervalNormalizer.normalize(intervals)
            .joinToString(separator = ";") { "${it.startTimeMs}-${it.endTimeMs}" }

    fun decode(encoded: String?): List<FlowActiveInterval> {
        if (encoded.isNullOrBlank()) return emptyList()
        return FlowActiveIntervalNormalizer.normalize(
            encoded.split(';').mapNotNull { part ->
                val pieces = part.split('-', limit = 2)
                val start = pieces.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
                val end = pieces.getOrNull(1)?.toLongOrNull() ?: return@mapNotNull null
                FlowActiveInterval(start, end)
            }
        )
    }
}

data class StoredMovementRewardContext(
    val nonMovementPreMultiplierPoints: Long,
    val pulseBonusPoints: Long = 0L,
    val surgeBonusPoints: Long = 0L,
    val otherPreMultiplierBonusPoints: Long = 0L,
    val existingMovementPoints: Long,
    val oldFinalScyraPoints: Long,
    val arcMultiplier: Double = 1.0,
    val streakMultiplier: Double = 1.0,
    val otherMultiplier: Double = 1.0,
    val pearlEligible: Boolean
)

data class DelayedMovementRewardResult(
    val newRawMovementPoints: Long,
    val newFinalScyraPoints: Long,
    val newPreMultiplierTotal: Long,
    val newArcBonusPoints: Long,
    val deltaScyraPoints: Long,
    val pearlDelta: Long,
    val pearlsEarned: Long
)

object DelayedMovementRewardPolicy {
    fun calculate(
        steps: Long,
        context: StoredMovementRewardContext,
        calculator: MovementBonusCalculator = MovementBonusCalculator()
    ): DelayedMovementRewardResult {
        val newRawMovementPoints = maxOf(
            context.existingMovementPoints,
            calculator.calculateMovementPoints(steps)
        )
        val recalculated = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = context.nonMovementPreMultiplierPoints,
            pulseBonusPoints = context.pulseBonusPoints,
            surgeBonusPoints = context.surgeBonusPoints,
            otherPreMultiplierBonusPoints = context.otherPreMultiplierBonusPoints,
            movementPoints = newRawMovementPoints,
            arcMultiplier = context.arcMultiplier,
            streakMultiplier = context.streakMultiplier,
            otherMultiplier = context.otherMultiplier,
            pearlEligible = context.pearlEligible
        )
        val delta = (recalculated.finalScyraPoints - context.oldFinalScyraPoints).coerceAtLeast(0L)
        return DelayedMovementRewardResult(
            newRawMovementPoints = newRawMovementPoints,
            newFinalScyraPoints = maxOf(context.oldFinalScyraPoints, recalculated.finalScyraPoints),
            newPreMultiplierTotal = recalculated.preMultiplierTotal,
            newArcBonusPoints = recalculated.arcBonusPoints,
            deltaScyraPoints = delta,
            pearlDelta = if (context.pearlEligible) delta else 0L,
            pearlsEarned = if (context.pearlEligible) maxOf(context.oldFinalScyraPoints, recalculated.pearlsEarned) else 0L
        )
    }
}

object MovementPearlDeltaKey {
    fun reason(sessionId: Long, movementPoints: Long, finalScyraPoints: Long): String =
        "movement_bonus_delta_session_${sessionId}_movement_${movementPoints}_final_${finalScyraPoints}"
}
