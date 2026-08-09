package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class RecurringObjectiveStatsTest {
    private val calculator = ObjectiveProgressCalculator()
    private val day = 86_400_000L

    @Test fun oldMissCannotOverwriteLaterCurrentStreak() {
        val objective = objective()
        val completions = listOf(completion(0), completion(2), completion(3))
        val stats = RecurringObjectiveStatsCalculator.derive(
            objective, completions, emptyList(), Instant.ofEpochMilli(3 * day + 1),
            ZoneOffset.UTC, calculator
        )
        assertEquals(2, stats.currentStreak)
        assertEquals(2, stats.maxStreak)
        assertEquals(3, stats.totalCompletions)
        assertEquals(1, RecurringObjectiveStatsCalculator.streakBefore(
            objective, completions, emptyList(), 3 * day
        ))
    }

    private fun objective() = ObjectiveEntity(
        id = 1, journeyId = 1, journeyNameSnapshot = "Journey", periodType = "daily",
        objectiveType = "recurring", targetDurationMs = 60_000, startAtMs = 0,
        currentStreak = 99, maxStreak = 99, totalCompletions = 99, createdAt = 0, updatedAt = 0
    )

    private fun completion(cycle: Int) = ObjectiveCompletionEntity(
        id = cycle + 1L, objectiveId = 1, journeyId = 1, journeyNameSnapshot = "Journey",
        periodType = "daily", objectiveType = "recurring", periodStartMs = cycle * day,
        periodEndMs = (cycle + 1) * day, completedAt = cycle * day + 1,
        achievedDurationMs = 60_000, targetDurationMs = 60_000, baseRewardPearls = 1,
        streakBeforeCompletion = 0, streakMultiplier = 1.0, finalRewardPearls = 1,
        badgeKey = "objective_badge_1_daily", badgeLabelSnapshot = "ignored"
    )
}
