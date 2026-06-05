package com.kingkharnivore.skillz.domain.health

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import com.kingkharnivore.skillz.data.model.entity.health.MovementDataSourceType
import org.junit.Test

class MovementBonusCalculatorTest {
    private val calculator = MovementBonusCalculator()

    @Test fun calculatesEveryTwentyFiveStepsUncapped() {
        mapOf(
            0L to 0L,
            24L to 0L,
            25L to 1L,
            49L to 1L,
            50L to 2L,
            99L to 3L,
            100L to 4L,
            342L to 13L,
            1_000L to 40L,
            10_000L to 400L,
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
        assertTrue(policy.isEligible(eligible.copy(healthConnectAvailable = false, readStepsPermissionGranted = false, phoneStepTrackingAvailable = true, activityRecognitionPermissionGranted = true)))
        assertFalse(policy.isEligible(eligible.copy(isSoftFlow = true)))
        assertFalse(policy.isEligible(eligible.copy(isRegularPointEligibleFlow = false)))
    }


    @Test fun sourceSelectionUsesMaxAndNeverSumsSources() {
        listOf(
            Triple(80L, 0L, MovementDataSourceType.PHONE_SENSOR) to (80L to 3L),
            Triple(0L, 240L, MovementDataSourceType.HEALTH_CONNECT) to (240L to 9L),
            Triple(100L, 80L, MovementDataSourceType.PHONE_SENSOR) to (100L to 4L),
            Triple(100L, 200L, MovementDataSourceType.HEALTH_CONNECT) to (200L to 8L),
            // Health Connect wins ties because it is the reconciled source.
            Triple(100L, 100L, MovementDataSourceType.HEALTH_CONNECT) to (100L to 4L),
            Triple(20L, 0L, MovementDataSourceType.PHONE_SENSOR) to (20L to 0L),
            Triple(0L, 24L, MovementDataSourceType.HEALTH_CONNECT) to (24L to 0L)
        ).forEach { (input, expected) ->
            val awarded = calculator.selectAwardedMovement(
                phoneEstimatedSteps = input.first,
                healthConnectSteps = input.second
            )
            assertEquals(expected.first.takeIf { it > 0L }, awarded.finalAwardedSteps)
            assertEquals(expected.second, awarded.finalAwardedMovementPoints)
            assertEquals(input.third, awarded.movementDataSource)
        }
    }

    @Test fun sourceSelectionNeverSubtractsPreviouslyAwardedSteps() {
        val lowerLaterData = calculator.selectAwardedMovement(
            previouslyAwardedSteps = 200,
            phoneEstimatedSteps = 100,
            healthConnectSteps = 80
        )
        assertEquals(200, lowerLaterData.finalAwardedSteps)
        assertEquals(8, lowerLaterData.finalAwardedMovementPoints)

        val higherHealthConnect = calculator.selectAwardedMovement(
            previouslyAwardedSteps = 100,
            phoneEstimatedSteps = 100,
            healthConnectSteps = 250,
            reconciledHealthConnect = true
        )
        assertEquals(250, higherHealthConnect.finalAwardedSteps)
        assertEquals(10, higherHealthConnect.finalAwardedMovementPoints)
        assertEquals(MovementDataSourceType.HEALTH_CONNECT_RECONCILED, higherHealthConnect.movementDataSource)
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
