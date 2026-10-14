package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.utils.shell.lookout.*
import java.time.Instant
import java.time.ZoneId

/** Sagas keeps ended one-time Objectives and outstanding earlier rewards visible. */
fun sagaObjectiveCards(
    objectives: List<ObjectiveEntity>, flows: List<ObjectiveSourceFlow>,
    completions: List<ObjectiveCompletionEntity>, skipped: List<ObjectiveSkippedCycleEntity>,
    now: Instant, zone: ZoneId, calculator: ObjectiveProgressCalculator
): List<ObjectiveCardModel> {
    val current = calculator.calculate(objectives, flows, completions, skipped, now, zone).cards
        .associateBy { it.objective.id }
    return objectives.filterNot { it.isArchived }.flatMap { objective ->
        val window = calculator.windowFor(objective, now, zone)
        val completion = completions.firstOrNull { it.objectiveId == objective.id && it.periodStartMs == window.startMs && it.periodEndMs == window.endMs }
        val wasSkipped = skipped.any { it.objectiveId == objective.id && it.periodStartMs == window.startMs && it.periodEndMs == window.endMs }
        val progress = calculator.eligibleFlowsFor(objective, flows, window).sumOf { it.durationMs.coerceAtLeast(0) }
        val card = current[objective.id] ?: ObjectiveCardModel(
            objective, ObjectivePeriod.fromStorage(objective.periodType), ObjectiveKind.fromStorage(objective.objectiveType),
            window, when { completion != null -> ObjectiveCardState.Completed; wasSkipped -> ObjectiveCardState.Skipped; else -> ObjectiveCardState.Expired },
            completion?.achievedDurationMs ?: progress,
            if (completion != null) 100 else (progress * 100.0 / objective.targetDurationMs.coerceAtLeast(1)).toInt().coerceIn(0, 100), completion
        )
        listOf(card) + completions.filter {
            it.objectiveId == objective.id && !it.pearlsClaimed && it.id != card.completion?.id
        }.map { reward -> card.copy(
            window = ObjectiveWindow(reward.periodStartMs, reward.periodEndMs), state = ObjectiveCardState.Completed,
            progressDurationMs = reward.achievedDurationMs, progressPercent = 100, completion = reward
        ) }
    }
}
