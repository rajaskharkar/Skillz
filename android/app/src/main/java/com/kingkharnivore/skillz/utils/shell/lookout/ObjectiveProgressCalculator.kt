package com.kingkharnivore.skillz.utils.shell.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectivePeriodTypes
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveSkippedCycleEntity
import com.kingkharnivore.skillz.data.model.entity.shell.ObjectiveTypes
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import kotlin.math.floor

private const val MILLIS_PER_MINUTE = 60_000L
enum class ObjectivePeriod(val storageValue: String, val label: String) {
    Daily(ObjectivePeriodTypes.DAILY, "Daily"),
    Weekly(ObjectivePeriodTypes.WEEKLY, "Weekly"),
    Monthly(ObjectivePeriodTypes.MONTHLY, "Monthly");

    companion object {
        fun fromStorage(value: String): ObjectivePeriod = entries.first { it.storageValue == value }
    }
}

enum class ObjectiveKind(val storageValue: String, val label: String) {
    OneTime(ObjectiveTypes.ONE_TIME, "One-time"),
    Recurring(ObjectiveTypes.RECURRING, "Recurring");

    companion object {
        fun fromStorage(value: String): ObjectiveKind = entries.first { it.storageValue == value }
    }
}

enum class ObjectiveCardState { Upcoming, InProgress, Completed }

data class ObjectiveSourceFlow(
    val id: Long,
    val journeyId: Long,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val isSoftMode: Boolean
)

data class ObjectiveWindow(val startMs: Long, val endMs: Long)

data class ObjectiveCardModel(
    val objective: ObjectiveEntity,
    val period: ObjectivePeriod,
    val kind: ObjectiveKind,
    val window: ObjectiveWindow,
    val state: ObjectiveCardState,
    val progressDurationMs: Long,
    val progressPercent: Int,
    val completion: ObjectiveCompletionEntity?,
    val effectiveCurrentStreak: Int = objective.currentStreak
)

data class ObjectiveCompletionGrant(
    val completion: ObjectiveCompletionEntity,
    val newCurrentStreak: Int?,
    val newMaxStreak: Int?,
    val newTotalCompletions: Int?
)

data class ObjectiveStreakReset(val objectiveId: Long)

data class ObjectiveCalculationResult(
    val cards: List<ObjectiveCardModel>,
    val completionsToGrant: List<ObjectiveCompletionGrant>,
    val streakResets: List<ObjectiveStreakReset>
)

class ObjectiveProgressCalculator @Inject constructor() {

    fun calculate(
        objectives: List<ObjectiveEntity>,
        flows: List<ObjectiveSourceFlow>,
        completions: List<ObjectiveCompletionEntity>,
        skippedCycles: List<ObjectiveSkippedCycleEntity>,
        now: Instant,
        zoneId: ZoneId
    ): ObjectiveCalculationResult {
        val completionByCycle = completions.associateBy { cycleKey(it.objectiveId, it.periodStartMs, it.periodEndMs) }
        val skippedByCycle = skippedCycles.map { cycleKey(it.objectiveId, it.periodStartMs, it.periodEndMs) }.toSet()
        val cards = mutableListOf<ObjectiveCardModel>()
        val grants = mutableListOf<ObjectiveCompletionGrant>()
        val resets = mutableListOf<ObjectiveStreakReset>()
        val nowMs = now.toEpochMilli()

        objectives.filterNot { it.isArchived }.forEach { objective ->
            val period = ObjectivePeriod.fromStorage(objective.periodType)
            val kind = ObjectiveKind.fromStorage(objective.objectiveType)
            val window = currentOrInitialWindow(objective, period, kind, now, zoneId)
            val key = cycleKey(objective.id, window.startMs, window.endMs)
            if (key in skippedByCycle) return@forEach

            val completion = completionByCycle[key]
            val progressMs = if (completion != null) completion.achievedDurationMs else progressFor(objective, flows, window)
            val progressPercent = percent(progressMs, objective.targetDurationMs)
            val shouldResetBeforeCurrentCycle = kind == ObjectiveKind.Recurring &&
                objective.currentStreak > 0 &&
                shouldResetStreakBeforeCurrentCycle(objective, period, completions, window, zoneId)
            val effectiveCurrentStreak = if (shouldResetBeforeCurrentCycle && completion == null) 0 else objective.currentStreak
            var grantsCurrentCycle = false

            if (completion == null && nowMs >= window.startMs && progressMs >= objective.targetDurationMs) {
                val achievedAtCompletionMs = achievedDurationAtFirstCompletion(objective, flows, window)
                grants += buildGrant(objective, kind, window, achievedAtCompletionMs, nowMs, effectiveCurrentStreak)
                grantsCurrentCycle = true
            }

            when {
                completion != null && nowMs < window.endMs -> cards += ObjectiveCardModel(objective, period, kind, window, ObjectiveCardState.Completed, progressMs, 100, completion, effectiveCurrentStreak)
                nowMs < window.startMs -> cards += ObjectiveCardModel(objective, period, kind, window, ObjectiveCardState.Upcoming, 0L, 0, null, effectiveCurrentStreak)
                nowMs < window.endMs -> cards += ObjectiveCardModel(objective, period, kind, window, ObjectiveCardState.InProgress, progressMs, progressPercent, null, effectiveCurrentStreak)
                kind == ObjectiveKind.Recurring -> {
                    val currentWindow = windowForCycleContaining(objective, period, now, zoneId)
                    if (currentWindow != window) {
                        val currentKey = cycleKey(objective.id, currentWindow.startMs, currentWindow.endMs)
                        if (currentKey !in skippedByCycle) {
                            val currentCompletion = completionByCycle[currentKey]
                            val currentProgress = currentCompletion?.achievedDurationMs ?: progressFor(objective, flows, currentWindow)
                            val currentPercent = if (currentCompletion != null) 100 else percent(currentProgress, objective.targetDurationMs)
                            val currentShouldResetBeforeCycle = objective.currentStreak > 0 &&
                                shouldResetStreakBeforeCurrentCycle(objective, period, completions, currentWindow, zoneId)
                            val currentEffectiveStreak = if (currentShouldResetBeforeCycle && currentCompletion == null) 0 else objective.currentStreak
                            var grantsFallbackCycle = false
                            if (currentCompletion == null && currentProgress >= objective.targetDurationMs) {
                                val achievedAtCompletionMs = achievedDurationAtFirstCompletion(objective, flows, currentWindow)
                                grants += buildGrant(objective, kind, currentWindow, achievedAtCompletionMs, nowMs, currentEffectiveStreak)
                                grantsFallbackCycle = true
                            }
                            cards += ObjectiveCardModel(
                                objective,
                                period,
                                kind,
                                currentWindow,
                                if (currentCompletion != null) ObjectiveCardState.Completed else ObjectiveCardState.InProgress,
                                currentProgress,
                                currentPercent,
                                currentCompletion,
                                currentEffectiveStreak
                            )
                            if (currentShouldResetBeforeCycle && currentCompletion == null && !grantsFallbackCycle) {
                                resets += ObjectiveStreakReset(objective.id)
                            }
                        }
                    }
                }
            }

            if (shouldResetBeforeCurrentCycle && completion == null && !grantsCurrentCycle) {
                resets += ObjectiveStreakReset(objective.id)
            }
        }

        return ObjectiveCalculationResult(
            cards = cards.distinctBy { it.objective.id to it.window.startMs },
            completionsToGrant = grants.distinctBy { cycleKey(it.completion.objectiveId, it.completion.periodStartMs, it.completion.periodEndMs) },
            streakResets = resets.distinctBy { it.objectiveId }
        )
    }

    fun windowFor(objective: ObjectiveEntity, now: Instant, zoneId: ZoneId): ObjectiveWindow =
        currentOrInitialWindow(objective, ObjectivePeriod.fromStorage(objective.periodType), ObjectiveKind.fromStorage(objective.objectiveType), now, zoneId)

    fun initialWindow(startAtMs: Long, period: ObjectivePeriod, weeklyBoundaryDay: Int?, zoneId: ZoneId): ObjectiveWindow {
        val start = Instant.ofEpochMilli(startAtMs).atZone(zoneId).toLocalDate().atStartOfDay(zoneId)
        val end = when (period) {
            ObjectivePeriod.Daily -> start.plusDays(1)
            ObjectivePeriod.Monthly -> start.plusDays(30)
            ObjectivePeriod.Weekly -> start.plusDays(7)
        }
        return ObjectiveWindow(start.toInstant().toEpochMilli(), end.toInstant().toEpochMilli())
    }

    fun objectiveBadgeKey(journeyId: Long, period: ObjectivePeriod): String =
        "objective_badge_${journeyId}_${period.storageValue}"

    private fun currentOrInitialWindow(
        objective: ObjectiveEntity,
        period: ObjectivePeriod,
        kind: ObjectiveKind,
        now: Instant,
        zoneId: ZoneId
    ): ObjectiveWindow {
        val initial = initialWindow(objective.startAtMs, period, objective.weeklyBoundaryDay, zoneId)
        if (kind == ObjectiveKind.OneTime || now.toEpochMilli() < initial.endMs) return initial
        return windowForCycleContaining(objective, period, now, zoneId)
    }

    private fun windowForCycleContaining(objective: ObjectiveEntity, period: ObjectivePeriod, now: Instant, zoneId: ZoneId): ObjectiveWindow {
        var window = initialWindow(objective.startAtMs, period, objective.weeklyBoundaryDay, zoneId)
        val nowMs = now.toEpochMilli()
        while (nowMs >= window.endMs) {
            val nextStart = Instant.ofEpochMilli(window.endMs).atZone(zoneId)
            val nextEnd = when (period) {
                ObjectivePeriod.Daily -> nextStart.plusDays(1)
                ObjectivePeriod.Weekly -> nextStart.plusDays(7)
                ObjectivePeriod.Monthly -> nextStart.plusDays(30)
            }
            window = ObjectiveWindow(nextStart.toInstant().toEpochMilli(), nextEnd.toInstant().toEpochMilli())
        }
        return window
    }

    private fun eligibleFlowsFor(objective: ObjectiveEntity, flows: List<ObjectiveSourceFlow>, window: ObjectiveWindow): List<ObjectiveSourceFlow> =
        flows.asSequence()
            .filter { it.journeyId == objective.journeyId }
            .filterNot { it.isSoftMode }
            .filter { it.endTimeMs in window.startMs until window.endMs }
            .sortedWith(compareBy<ObjectiveSourceFlow> { it.endTimeMs }.thenBy { it.id })
            .toList()

    private fun progressFor(objective: ObjectiveEntity, flows: List<ObjectiveSourceFlow>, window: ObjectiveWindow): Long =
        eligibleFlowsFor(objective, flows, window).sumOf { it.durationMs.coerceAtLeast(0L) }

    private fun achievedDurationAtFirstCompletion(objective: ObjectiveEntity, flows: List<ObjectiveSourceFlow>, window: ObjectiveWindow): Long {
        var total = 0L
        eligibleFlowsFor(objective, flows, window).forEach { flow ->
            total += flow.durationMs.coerceAtLeast(0L)
            if (total >= objective.targetDurationMs) return total
        }
        return total
    }

    private fun buildGrant(
        objective: ObjectiveEntity,
        kind: ObjectiveKind,
        window: ObjectiveWindow,
        achievedMs: Long,
        nowMs: Long,
        effectiveCurrentStreak: Int
    ): ObjectiveCompletionGrant {
        val period = ObjectivePeriod.fromStorage(objective.periodType)
        val base = floor(achievedMs / MILLIS_PER_MINUTE.toDouble()).toInt().coerceAtLeast(1)
        val streakBefore = if (kind == ObjectiveKind.Recurring) effectiveCurrentStreak else 0
        val multiplier = if (kind == ObjectiveKind.Recurring) 1.0 + streakBefore * 0.1 else 1.0
        val finalPearls = floor(base * multiplier).toInt()
        val newStreak = if (kind == ObjectiveKind.Recurring) effectiveCurrentStreak + 1 else null
        val badgeKey = objectiveBadgeKey(objective.journeyId, period)
        val completion = ObjectiveCompletionEntity(
            objectiveId = objective.id,
            journeyId = objective.journeyId,
            journeyNameSnapshot = objective.journeyNameSnapshot,
            periodType = objective.periodType,
            objectiveType = objective.objectiveType,
            periodStartMs = window.startMs,
            periodEndMs = window.endMs,
            completedAt = nowMs,
            achievedDurationMs = achievedMs,
            targetDurationMs = objective.targetDurationMs,
            baseRewardPearls = base,
            streakBeforeCompletion = streakBefore,
            streakMultiplier = multiplier,
            finalRewardPearls = finalPearls,
            badgeKey = badgeKey,
            badgeLabelSnapshot = "${objective.journeyNameSnapshot} ${period.label} Objective"
        )
        return ObjectiveCompletionGrant(
            completion = completion,
            newCurrentStreak = newStreak,
            newMaxStreak = newStreak?.let { maxOf(objective.maxStreak, it) },
            newTotalCompletions = newStreak?.let { objective.totalCompletions + 1 }
        )
    }

    private fun shouldResetStreakBeforeCurrentCycle(
        objective: ObjectiveEntity,
        period: ObjectivePeriod,
        completions: List<ObjectiveCompletionEntity>,
        current: ObjectiveWindow,
        zoneId: ZoneId
    ): Boolean {
        val previousEnd = current.startMs
        val previousStart = when (period) {
            ObjectivePeriod.Daily -> Instant.ofEpochMilli(previousEnd).atZone(zoneId).minusDays(1)
            ObjectivePeriod.Weekly -> Instant.ofEpochMilli(previousEnd).atZone(zoneId).minusDays(7)
            ObjectivePeriod.Monthly -> Instant.ofEpochMilli(previousEnd).atZone(zoneId).minusDays(30)
        }.toInstant().toEpochMilli()
        if (previousStart < initialWindow(objective.startAtMs, period, objective.weeklyBoundaryDay, zoneId).startMs) return false
        return completions.none { it.objectiveId == objective.id && it.periodStartMs == previousStart && it.periodEndMs == previousEnd }
    }

    private fun percent(progressMs: Long, targetMs: Long): Int =
        if (targetMs <= 0L) 0 else ((progressMs.toDouble() / targetMs) * 100).toInt().coerceIn(0, 100)

    private fun cycleKey(objectiveId: Long, startMs: Long, endMs: Long): String = "$objectiveId:$startMs:$endMs"
}
