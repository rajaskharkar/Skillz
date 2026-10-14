package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.shell.lookout.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class ObjectiveTargetRewardTest {
    private val calculator = ObjectiveProgressCalculator()
    private val zone = ZoneId.of("America/Chicago")
    private val start = LocalDate.of(2026, 10, 13).atStartOfDay(zone).toInstant().toEpochMilli()
    private val minute = 60_000L
    private fun objective(mode: FlowMode = FlowMode.FLOW, target: Int? = 9 * 60) = ObjectiveEntity(
        id = 1, journeyId = 1, journeyNameSnapshot = "Practice", periodType = "daily", objectiveType = "recurring",
        targetDurationMs = 30 * minute, startAtMs = start, createdAt = start, updatedAt = start,
        flowMode = mode, targetMinuteOfDay = target
    )
    private fun flow(id: Long = 1, startMinute: Int = 8 * 60 + 45, duration: Int = 30, mode: FlowMode = FlowMode.FLOW) =
        ObjectiveSourceFlow(id, 1, start + startMinute * minute, start + (startMinute + duration) * minute,
            duration * minute, mode == FlowMode.SOFT, mode)
    private fun grant(objective: ObjectiveEntity, flows: List<ObjectiveSourceFlow>, streak: Int = 0) =
        calculator.windowFor(objective, Instant.ofEpochMilli(start), zone).let { window ->
            val evidence = requireNotNull(calculator.completionEvidence(objective, flows, window))
            calculator.buildGrant(objective, ObjectiveKind.Recurring, window, evidence.achievedDurationMs,
                evidence.completedAtMs, streak, evidence.startedAtMs, evidence.sourceSessionId, zone).completion
        }

    @Test fun startsBeforeTargetAndFinishesAfterStillDoublesWithStreak() {
        val reward = grant(objective(), listOf(flow()), 2)
        assertTrue(reward.startedBeforeTarget)
        assertEquals(1.2, reward.streakMultiplier, 0.0)
        assertEquals(72, reward.finalRewardPearls)
        assertEquals(72, reward.finalRewardPoints)
        assertEquals(1L, reward.sourceSessionId)
    }

    @Test fun exactTargetAndLateStartDoNotDoubleOrLoseCompletion() {
        listOf(9 * 60, 9 * 60 + 1).forEach { startMinute ->
            val reward = grant(objective(), listOf(flow(startMinute = startMinute)), 1)
            assertFalse(reward.startedBeforeTarget)
            assertEquals(33, reward.finalRewardPearls)
            assertEquals(33, reward.finalRewardPoints)
        }
    }

    @Test fun noTargetKeepsExistingRewardsAndStreak() {
        val reward = grant(objective(target = null), listOf(flow()), 3)
        assertEquals(39, reward.finalRewardPearls)
        assertFalse(reward.startedBeforeTarget)
        assertEquals(39, reward.finalRewardPoints)
    }

    @Test fun firstContributingFlowLocksEarlyStartAcrossSeveralSessions() {
        val reward = grant(objective(), listOf(flow(duration = 10), flow(id = 2, startMinute = 10 * 60, duration = 20)))
        assertTrue(reward.startedBeforeTarget)
        assertEquals(60, reward.finalRewardPearls)
        assertEquals(2L, reward.sourceSessionId)
    }

    @Test fun zeroDurationAndHistoricalFlowCannotUnlockBonus() {
        val zero = grant(objective(), listOf(flow(duration = 0), flow(id = 2, startMinute = 10 * 60)))
        assertFalse(zero.startedBeforeTarget)
        val retroactive = objective().copy(createdAt = start + 10 * 60 * minute)
        assertNull(calculator.completionEvidence(retroactive, listOf(flow()), calculator.windowFor(retroactive, Instant.ofEpochMilli(start), zone)))
    }

    @Test fun flowsStartedBeforeCreationCannotContributeEvenWhenTheyEndLater() {
        val obj = objective().copy(createdAt = start + (8 * 60 + 50) * minute)
        val old = flow(duration = 20)
        val fresh = flow(id = 2, startMinute = 8 * 60 + 55, duration = 10)
        val window = calculator.windowFor(obj, Instant.ofEpochMilli(start), zone)
        assertNull(calculator.completionEvidence(obj, listOf(old, fresh), window))
        val card = calculator.calculate(listOf(obj), listOf(old, fresh), emptyList(), emptyList(),
            Instant.ofEpochMilli(start + 10 * 60 * minute), zone).cards.single()
        assertEquals(10 * minute, card.progressDurationMs)
        val reward = grant(obj, listOf(old, fresh, flow(id = 3, startMinute = 10 * 60, duration = 20)))
        assertTrue(reward.startedBeforeTarget)
        assertEquals(30 * minute, reward.achievedDurationMs)
        assertEquals(60, reward.finalRewardPearls)
    }

    @Test fun softObjectiveCountsOnlySoftAndDoublesDrops() {
        val obj = objective(FlowMode.SOFT)
        assertNull(calculator.completionEvidence(obj, listOf(flow()), calculator.windowFor(obj, Instant.ofEpochMilli(start), zone)))
        val reward = grant(obj, listOf(flow(mode = FlowMode.SOFT)), 1)
        assertEquals(3960L, reward.finalRewardDrops)
        assertEquals(0, reward.finalRewardPearls)
        assertEquals(0, reward.finalRewardPebbles)
        assertEquals(0, reward.finalRewardPoints)
    }

    @Test fun untimedModesAwardTheirBaseCurrenciesAndPointsWithoutStackingBonuses() {
        val soft = grant(objective(FlowMode.SOFT, target = null), listOf(flow(mode = FlowMode.SOFT)), 1)
        assertEquals(1980L, soft.finalRewardDrops)
        assertEquals(0, soft.finalRewardPoints)
        assertEquals(0, soft.finalRewardPearls)
        assertEquals(0, soft.finalRewardPebbles)
        val power = grant(objective(FlowMode.POWER, target = null), listOf(flow(mode = FlowMode.POWER)), 1)
        assertEquals(33, power.finalRewardPoints)
        assertEquals(33, power.finalRewardPearls)
        assertEquals(33, power.finalRewardPebbles)
        assertEquals(0L, power.finalRewardDrops)
    }

    @Test fun powerObjectiveCountsOnlyPowerAndDoublesPebbles() {
        val reward = grant(objective(FlowMode.POWER), listOf(flow(mode = FlowMode.POWER)), 1)
        assertEquals(66, reward.finalRewardPebbles)
        assertEquals(66, reward.finalRewardPearls)
        assertEquals(66, reward.finalRewardPoints)
        assertEquals(0L, reward.finalRewardDrops)
        assertNull(calculator.completionEvidence(objective(), listOf(flow(mode = FlowMode.POWER)), calculator.windowFor(objective(), Instant.ofEpochMilli(start), zone)))
    }

    @Test fun targetFollowsLocalDayAcrossDaylightSavingAndRecurringCycles() {
        val dstStart = LocalDate.of(2026, 10, 30).atStartOfDay(zone).toInstant().toEpochMilli()
        val obj = objective().copy(periodType = "weekly", startAtMs = dstStart, targetDayOffset = 3)
        val window = calculator.windowFor(obj, Instant.ofEpochMilli(dstStart), zone)
        val expected = LocalDate.of(2026, 11, 2).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, calculator.targetAtMs(obj, window, zone))
        val next = calculator.windowFor(obj, Instant.ofEpochMilli(window.endMs), zone)
        assertEquals(LocalDate.of(2026, 11, 9).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(), calculator.targetAtMs(obj, next, zone))
        val monthly = obj.copy(periodType = "monthly", targetDayOffset = 29)
        assertEquals(LocalDate.of(2026, 11, 28).atTime(9, 0).atZone(zone).toInstant().toEpochMilli(),
            calculator.targetAtMs(monthly, calculator.windowFor(monthly, Instant.ofEpochMilli(dstStart), zone), zone))
    }

    @Test fun earlyBonusDoublesAlreadyRoundedStreakReward() {
        val reward = grant(objective().copy(targetDurationMs = 17 * minute), listOf(flow(duration = 17)), 1)
        assertEquals(36, reward.finalRewardPearls)
    }
}
