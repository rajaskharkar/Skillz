package com.kingkharnivore.skillz.domain.health

import com.kingkharnivore.skillz.utils.health.MovementBonusCalculator
import com.kingkharnivore.skillz.utils.health.MovementBonusEligibilityInput
import com.kingkharnivore.skillz.utils.health.MovementBonusEligibilityPolicy
import com.kingkharnivore.skillz.utils.health.MovementRewardRecalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MovementBonusCalculatorTest {
    private val calculator = MovementBonusCalculator()

    @Test fun calculatesEveryHundredStepsUncapped() {
        // Match the shipped 100-step policy shown in the Movement Bonus UI.
        mapOf(
            0L to 0L,
            25L to 0L,
            99L to 0L,
            100L to 1L,
            199L to 1L,
            200L to 2L,
            342L to 3L,
            1_000L to 10L,
            10_000L to 100L,
            10_000_000L to 100_000L,
            Long.MAX_VALUE to 92_233_720_368_547_758L,
            -10L to 0L
        ).forEach { (steps, points) ->
            assertEquals(points, calculator.calculateMovementPoints(steps))
        }
    }

    @Test fun eligibilityRequiresEnabledAvailablePermissionAndRegularFlow() {
        val policy = MovementBonusEligibilityPolicy()
        val eligible = MovementBonusEligibilityInput(true, true, true, true, false)
        assertTrue(policy.isEligible(eligible))
        assertFalse(policy.isEligible(eligible.copy(movementBonusEnabled = false)))
        assertFalse(policy.isEligible(eligible.copy(readStepsPermissionGranted = false)))
        assertFalse(policy.isEligible(eligible.copy(healthConnectAvailable = false)))
        assertFalse(policy.isEligible(eligible.copy(isSoftFlow = true)))
        assertFalse(policy.isEligible(eligible.copy(isRegularPointEligibleFlow = false)))
    }

    @Test fun movementPointsAreIncludedBeforeMultipliersAndPearls() {
        val reward = MovementRewardRecalculator.withMovementPoints(
            nonMovementPreMultiplierPoints = 42,
            pulseBonusPoints = 0,
            surgeBonusPoints = 15,
            movementPoints = 13,
            arcMultiplier = 1.2,
            streakMultiplier = 1.0,
            pearlEligible = true
        )
        assertEquals(70, reward.preMultiplierTotal)
        assertEquals(84, reward.finalScyraPoints)
        assertEquals(84, reward.pearlsEarned)
    }

    @Test fun softFlowsDoNotProducePearlsOrMovementRewards() {
        val policy = MovementBonusEligibilityPolicy()
        assertFalse(policy.isEligible(MovementBonusEligibilityInput(true, true, true, false, true)))
    }
}
