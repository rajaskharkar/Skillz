package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveCardState
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveKind
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectivePeriod
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveSourceFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val MINUTE = 60_000L

class ObjectiveProgressCalculatorTest {
    private val calculator = ObjectiveProgressCalculator()
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun dailyObjectiveRunsMidnightToMidnightAndLateCreationShowsShortTimeLeft() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, period = ObjectivePeriod.Daily, kind = ObjectiveKind.OneTime)
        val window = calculator.initialWindow(start, ObjectivePeriod.Daily, null, zone)
        assertEquals(start, window.startMs)
        assertEquals(LocalDate.of(2026, 6, 2).atStartOfDay(zone).toInstant().toEpochMilli(), window.endMs)
        val result = calculator.calculate(listOf(objective), emptyList(), emptyList(), emptyList(), LocalDate.of(2026, 6, 1).atTime(23, 30).atZone(zone).toInstant(), zone)
        assertEquals(ObjectiveCardState.InProgress, result.cards.single().state)
    }

    @Test
    fun futureStartDateAppearsUpcoming() {
        val tomorrow = LocalDate.of(2026, 6, 2).atStartOfDay(zone).toInstant().toEpochMilli()
        val result = calculator.calculate(listOf(objective(startAtMs = tomorrow)), emptyList(), emptyList(), emptyList(), LocalDate.of(2026, 6, 1).atTime(12, 0).atZone(zone).toInstant(), zone)
        assertEquals(ObjectiveCardState.Upcoming, result.cards.single().state)
    }

    @Test
    fun weeklyObjectiveRunsExactlySevenDaysFromStartDate() {
        val start = LocalDate.of(2026, 6, 3).atStartOfDay(zone).toInstant().toEpochMilli()
        val window = calculator.initialWindow(start, ObjectivePeriod.Weekly, DayOfWeek.SUNDAY.value, zone)
        assertEquals(LocalDate.of(2026, 6, 10).atStartOfDay(zone).toInstant().toEpochMilli(), window.endMs)
    }

    @Test
    fun monthlyObjectiveRunsThirtyDays() {
        val start = LocalDate.of(2026, 2, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val window = calculator.initialWindow(start, ObjectivePeriod.Monthly, null, zone)
        assertEquals(LocalDate.of(2026, 3, 3).atStartOfDay(zone).toInstant().toEpochMilli(), window.endMs)
    }

    @Test
    fun onlyCompletedEligibleNonSoftFlowsCountAndCompletionIsUnique() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, targetMinutes = 30)
        val flowTime = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val flows = listOf(flow(1, flowTime, 20 * MINUTE, false), flow(2, flowTime + MINUTE, 20 * MINUTE, true))
        val partial = calculator.calculate(listOf(objective), flows, emptyList(), emptyList(), Instant.ofEpochMilli(flowTime), zone)
        assertEquals(66, partial.cards.single().progressPercent)
        assertTrue(partial.completionsToGrant.isEmpty())
        val complete = calculator.calculate(listOf(objective), flows + flow(3, flowTime + 2 * MINUTE, 15 * MINUTE, false), emptyList(), emptyList(), Instant.ofEpochMilli(flowTime + 3 * MINUTE), zone)
        assertEquals(1, complete.completionsToGrant.size)
        assertEquals(35, complete.completionsToGrant.single().completion.baseRewardPearls)
        assertTrue(!complete.completionsToGrant.single().completion.pearlsClaimed)
        val existing = complete.completionsToGrant.single().completion
        val duplicate = calculator.calculate(listOf(objective), flows + flow(3, flowTime + 2 * MINUTE, 15 * MINUTE, false), listOf(existing), emptyList(), Instant.ofEpochMilli(flowTime + 4 * MINUTE), zone)
        assertTrue(duplicate.completionsToGrant.isEmpty())
    }


    @Test
    fun recurringDayOneHasNoStreakBonus() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, kind = ObjectiveKind.Recurring, targetMinutes = 30, currentStreak = 0)
        val end = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val grant = calculator.calculate(listOf(objective), listOf(flow(1, end, 30 * MINUTE, false)), emptyList(), emptyList(), Instant.ofEpochMilli(end), zone).completionsToGrant.single()
        assertEquals(0, grant.completion.streakBeforeCompletion)
        assertEquals(1.0, grant.completion.streakMultiplier, 0.0)
        assertEquals(30, grant.completion.finalRewardPearls)
    }

    @Test
    fun recurringDayTwoUsesTenPercentStreakBonus() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, kind = ObjectiveKind.Recurring, targetMinutes = 30, currentStreak = 1)
        val end = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val grant = calculator.calculate(listOf(objective), listOf(flow(1, end, 30 * MINUTE, false)), emptyList(), emptyList(), Instant.ofEpochMilli(end), zone).completionsToGrant.single()
        assertEquals(1, grant.completion.streakBeforeCompletion)
        assertEquals(1.1, grant.completion.streakMultiplier, 0.0)
        assertEquals(33, grant.completion.finalRewardPearls)
    }

    @Test
    fun overshootMinutesAreRewardedAndStreakBonusUsesOvershootAmount() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val oneTime = objective(startAtMs = start, targetMinutes = 10)
        val oneTimeGrant = calculator.calculate(listOf(oneTime), listOf(flow(1, end, 17 * MINUTE, false)), emptyList(), emptyList(), Instant.ofEpochMilli(end), zone).completionsToGrant.single()
        assertEquals(17, oneTimeGrant.completion.baseRewardPearls)
        assertEquals(17, oneTimeGrant.completion.finalRewardPearls)

        val recurring = objective(startAtMs = start, kind = ObjectiveKind.Recurring, targetMinutes = 10, currentStreak = 1)
        val recurringGrant = calculator.calculate(listOf(recurring), listOf(flow(1, end, 17 * MINUTE, false)), emptyList(), emptyList(), Instant.ofEpochMilli(end), zone).completionsToGrant.single()
        assertEquals(17, recurringGrant.completion.baseRewardPearls)
        assertEquals(18, recurringGrant.completion.finalRewardPearls)
    }

    @Test
    fun recurringUsesStreakBeforeIncrementWithoutMultiplierCap() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, kind = ObjectiveKind.Recurring, targetMinutes = 30, currentStreak = 20)
        val end = LocalDate.of(2026, 6, 1).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val grant = calculator.calculate(listOf(objective), listOf(flow(1, end, 30 * MINUTE, false)), emptyList(), emptyList(), Instant.ofEpochMilli(end), zone).completionsToGrant.single()
        assertEquals(20, grant.completion.streakBeforeCompletion)
        assertEquals(3.0, grant.completion.streakMultiplier, 0.0)
        assertEquals(90, grant.completion.finalRewardPearls)
        assertEquals(21, grant.newCurrentStreak)
    }

    @Test
    fun recurringMissedCycleResetsEffectiveStreakBeforeCompletionReward() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, kind = ObjectiveKind.Recurring, targetMinutes = 30, currentStreak = 5, totalCompletions = 12)
        val currentCycleEnd = LocalDate.of(2026, 6, 3).atTime(9, 0).atZone(zone).toInstant().toEpochMilli()
        val result = calculator.calculate(
            listOf(objective),
            listOf(flow(1, currentCycleEnd, 30 * MINUTE, false)),
            emptyList(),
            emptyList(),
            Instant.ofEpochMilli(currentCycleEnd),
            zone
        )
        val grant = result.completionsToGrant.single()
        assertTrue(result.streakResets.isEmpty())
        assertEquals(0, grant.completion.streakBeforeCompletion)
        assertEquals(1.0, grant.completion.streakMultiplier, 0.0)
        assertEquals(30, grant.completion.finalRewardPearls)
        assertEquals(1, grant.newCurrentStreak)
        assertEquals(5, grant.newMaxStreak)
        assertEquals(13, grant.newTotalCompletions)
    }

    @Test
    fun recurringStreakResetsWhenPreviousCycleMissedAndSkippedCycleIsHidden() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, kind = ObjectiveKind.Recurring, currentStreak = 2)
        val now = LocalDate.of(2026, 6, 3).atTime(9, 0).atZone(zone).toInstant()
        assertEquals(1, calculator.calculate(listOf(objective), emptyList(), emptyList(), emptyList(), now, zone).streakResets.size)
        val currentWindow = calculator.windowFor(objective, now, zone)
        val skipped = ObjectiveSkippedCycleEntity(objectiveId = objective.id, periodStartMs = currentWindow.startMs, periodEndMs = currentWindow.endMs, skippedAt = now.toEpochMilli())
        assertTrue(calculator.calculate(listOf(objective), emptyList(), emptyList(), listOf(skipped), now, zone).cards.isEmpty())
    }

    @Test
    fun badgeKeyUsesJourneyAndPeriod() {
        assertEquals("objective_badge_12_weekly", calculator.objectiveBadgeKey(12, ObjectivePeriod.Weekly))
    }

    @Test
    fun completionEvidenceMatchesDisplayedProgressEligibilityAndCrossingSession() {
        val start = LocalDate.of(2026, 6, 1).atStartOfDay(zone).toInstant().toEpochMilli()
        val objective = objective(startAtMs = start, targetMinutes = 60)
        val firstEnd = start + 30 * MINUTE
        val crossingEnd = start + 100 * MINUTE
        val flows = listOf(
            flow(1, firstEnd, 30 * MINUTE, false),
            flow(2, crossingEnd, 40 * MINUTE, false),
            flow(3, crossingEnd, 500 * MINUTE, true),
            ObjectiveSourceFlow(4, 99, start, crossingEnd, 500 * MINUTE, false)
        )
        val window = calculator.windowFor(objective, Instant.ofEpochMilli(crossingEnd), zone)
        val evidence = requireNotNull(calculator.completionEvidence(objective, flows, window))
        val card = calculator.calculate(
            listOf(objective), flows, emptyList(), emptyList(), Instant.ofEpochMilli(crossingEnd), zone
        ).cards.single()
        assertEquals(70 * MINUTE, evidence.achievedDurationMs)
        assertEquals(crossingEnd, evidence.completedAtMs)
        assertEquals(100, card.progressPercent)
    }

    private fun objective(
        id: Long = 7,
        startAtMs: Long,
        period: ObjectivePeriod = ObjectivePeriod.Daily,
        kind: ObjectiveKind = ObjectiveKind.OneTime,
        targetMinutes: Long = 30,
        currentStreak: Int = 0,
        totalCompletions: Int = currentStreak
    ) = ObjectiveEntity(
        id = id,
        journeyId = 12,
        journeyNameSnapshot = "Drums",
        periodType = period.storageValue,
        objectiveType = kind.storageValue,
        targetDurationMs = targetMinutes * MINUTE,
        startAtMs = startAtMs,
        weeklyBoundaryDay = if (period == ObjectivePeriod.Weekly) DayOfWeek.MONDAY.value else null,
        currentStreak = currentStreak,
        maxStreak = currentStreak,
        totalCompletions = totalCompletions,
        createdAt = startAtMs,
        updatedAt = startAtMs
    )

    private fun flow(id: Long, end: Long, duration: Long, soft: Boolean) =
        ObjectiveSourceFlow(id, 12, end - duration, end, duration, soft)
}
