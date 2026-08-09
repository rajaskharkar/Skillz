package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveKind
import com.kingkharnivore.skillz.utils.shell.lookout.ObjectiveProgressCalculator
import java.time.Instant
import java.time.ZoneId

data class RecurringObjectiveStats(val currentStreak: Int, val maxStreak: Int, val totalCompletions: Int)

object RecurringObjectiveStatsCalculator {
    fun derive(
        objective: ObjectiveEntity,
        completions: List<ObjectiveCompletionEntity>,
        skipped: List<ObjectiveSkippedCycleEntity>,
        asOf: Instant,
        zoneId: ZoneId,
        calculator: ObjectiveProgressCalculator
    ): RecurringObjectiveStats {
        val relevant = completions.filter { it.objectiveId == objective.id }.sortedBy { it.periodStartMs }
        if (ObjectiveKind.fromStorage(objective.objectiveType) != ObjectiveKind.Recurring) {
            return RecurringObjectiveStats(0, 0, relevant.size)
        }
        val current = calculator.windowFor(objective, asOf, zoneId)
        var streak = 0
        var best = 0
        var previousEnd: Long? = null
        relevant.forEach { completion ->
            streak = if (previousEnd == completion.periodStartMs) streak + 1 else 1
            best = maxOf(best, streak)
            previousEnd = completion.periodEndMs
        }
        val currentStreak = relevant.lastOrNull()?.let { latest ->
            val currentSkipped = skipped.any {
                it.objectiveId == objective.id && it.periodStartMs == current.startMs
            }
            if (!currentSkipped && (latest.periodStartMs == current.startMs || latest.periodEndMs == current.startMs)) streak else 0
        } ?: 0
        return RecurringObjectiveStats(currentStreak, best, relevant.size)
    }

    fun streakBefore(
        objective: ObjectiveEntity,
        completions: List<ObjectiveCompletionEntity>,
        skipped: List<ObjectiveSkippedCycleEntity>,
        periodStartMs: Long
    ): Int {
        val relevant = completions.filter {
            it.objectiveId == objective.id && it.periodStartMs < periodStartMs
        }.sortedBy { it.periodStartMs }
        var streak = 0
        var previousEnd: Long? = null
        relevant.forEach { completion ->
            streak = if (previousEnd == completion.periodStartMs) streak + 1 else 1
            previousEnd = completion.periodEndMs
        }
        val boundarySkipped = skipped.any {
            it.objectiveId == objective.id && it.periodEndMs == periodStartMs
        }
        return if (!boundarySkipped && relevant.lastOrNull()?.periodEndMs == periodStartMs) streak else 0
    }
}
