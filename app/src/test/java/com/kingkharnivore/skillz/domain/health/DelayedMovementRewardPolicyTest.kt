package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.utils.health.DelayedMovementRewardPolicy
import com.kingkharnivore.skillz.utils.health.MovementBonusCalculator
import com.kingkharnivore.skillz.utils.health.MovementBonusEligibilityInput
import com.kingkharnivore.skillz.utils.health.MovementBonusEligibilityPolicy
import com.kingkharnivore.skillz.utils.health.MovementPearlDeltaKey
import com.kingkharnivore.skillz.utils.health.MovementRewardRecalculator
import com.kingkharnivore.skillz.utils.health.StoredMovementRewardContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DelayedMovementRewardPolicyTest {
    @Test fun noDataAtCompletionLaterStepsAddsPositiveDelta() {
        val result = DelayedMovementRewardPolicy.calculate(
            steps = 342,
            context = StoredMovementRewardContext(
                nonMovementPreMultiplierPoints = 42,
                existingMovementPoints = 0,
                oldFinalScyraPoints = 42,
                pearlEligible = true
            )
        )

        assertEquals(13, result.newRawMovementPoints)
        assertEquals(55, result.newFinalScyraPoints)
        assertEquals(13, result.deltaScyraPoints)
        assertEquals(13, result.pearlDelta)
    }

    @Test fun laterHigherStepsAddsOnlyAdditionalDelta() {
        val initial = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = 50,
            movementPoints = 4,
            arcMultiplier = 1.5,
            pearlEligible = true
        )

        val result = DelayedMovementRewardPolicy.calculate(
            steps = 200,
            context = StoredMovementRewardContext(
                nonMovementPreMultiplierPoints = 50,
                existingMovementPoints = 4,
                oldFinalScyraPoints = initial.finalScyraPoints,
                arcMultiplier = 1.5,
                pearlEligible = true
            )
        )

        assertEquals(8, result.newRawMovementPoints)
        assertEquals(87, result.newFinalScyraPoints)
        assertEquals(6, result.deltaScyraPoints)
        assertEquals(6, result.pearlDelta)
    }

    @Test fun laterLowerStepsSubtractsNothing() {
        val initial = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = 50,
            movementPoints = 8,
            arcMultiplier = 1.5,
            pearlEligible = true
        )

        val result = DelayedMovementRewardPolicy.calculate(
            steps = 100,
            context = StoredMovementRewardContext(
                nonMovementPreMultiplierPoints = 50,
                existingMovementPoints = 8,
                oldFinalScyraPoints = initial.finalScyraPoints,
                arcMultiplier = 1.5,
                pearlEligible = true
            )
        )

        assertEquals(8, result.newRawMovementPoints)
        assertEquals(initial.finalScyraPoints.toLong(), result.newFinalScyraPoints)
        assertEquals(0, result.deltaScyraPoints)
        assertEquals(0, result.pearlDelta)
    }

    @Test fun storedCompletionMultipliersAreUsedForDelayedDelta() {
        val result = DelayedMovementRewardPolicy.calculate(
            steps = 100,
            context = StoredMovementRewardContext(
                nonMovementPreMultiplierPoints = 40,
                existingMovementPoints = 0,
                oldFinalScyraPoints = 40,
                arcMultiplier = 1.25,
                streakMultiplier = 1.2,
                otherMultiplier = 1.1,
                pearlEligible = true
            )
        )

        // (40 + 4 movement) * 1.25 * 1.2 * 1.1 = 72.6, rounded to 73.
        assertEquals(73, result.newFinalScyraPoints)
        assertEquals(33, result.deltaScyraPoints)
    }

    @Test fun movementPointsApplyBeforeAllMultipliersAndPearlsCanBeDisabled() {
        val eligible = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = 40,
            movementPoints = 10,
            arcMultiplier = 1.2,
            streakMultiplier = 1.5,
            otherMultiplier = 1.1,
            pearlEligible = true
        )
        val notPearlEligible = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = 40,
            movementPoints = 10,
            arcMultiplier = 1.2,
            streakMultiplier = 1.5,
            otherMultiplier = 1.1,
            pearlEligible = false
        )

        assertEquals(99, eligible.finalScyraPoints)
        assertEquals(99, eligible.pearlsEarned)
        assertEquals(0, notPearlEligible.pearlsEarned)
    }

    @Test fun roundingIsDeterministic() {
        val reward = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = 1,
            movementPoints = 1,
            arcMultiplier = 1.25,
            pearlEligible = true
        )

        assertEquals(3, reward.finalScyraPoints)
    }

    @Test fun highStepCountsRemainUncapped() {
        assertEquals(400_000, MovementBonusCalculator().calculateMovementPoints(10_000_000))
    }

    @Test fun stablePearlDeltaKeyRepresentsAppliedFinalState() {
        val first = MovementPearlDeltaKey.reason(sessionId = 7, movementPoints = 13, finalScyraPoints = 92)
        val repeated = MovementPearlDeltaKey.reason(sessionId = 7, movementPoints = 13, finalScyraPoints = 92)
        val higher = MovementPearlDeltaKey.reason(sessionId = 7, movementPoints = 20, finalScyraPoints = 100)

        assertEquals(first, repeated)
        assertTrue(first != higher)
    }

    @Test fun softFlowEligibilityReturnsFalse() {
        assertEquals(
            false,
            MovementBonusEligibilityPolicy().isEligible(
                MovementBonusEligibilityInput(
                    movementBonusEnabled = true,
                    healthConnectAvailable = true,
                    readStepsPermissionGranted = true,
                    isRegularPointEligibleFlow = false,
                    isSoftFlow = true
                )
            )
        )
    }
}
