package com.kingkharnivore.skillz.domain.lookout

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.shell.lookout.*
import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class SagaObjectiveCardsTest {
    private val calc = ObjectiveProgressCalculator()
    private val zone = ZoneId.of("UTC")
    private val start = Instant.parse("2026-10-13T00:00:00Z").toEpochMilli()
    private fun objective(id: Long = 1, mode: FlowMode = FlowMode.FLOW) = ObjectiveEntity(
        id = id, journeyId = id, journeyNameSnapshot = "Journey $id", periodType = "daily", objectiveType = "one_time",
        targetDurationMs = 60_000, startAtMs = start, createdAt = start, updatedAt = start, flowMode = mode
    )
    @Test fun allModesAndCurrentProgressAreShownTogether() {
        val objectives = FlowMode.entries.mapIndexed { index, mode -> objective(index + 1L, mode) }
        val flows = objectives.map { ObjectiveSourceFlow(it.id, it.journeyId, start, start + 30_000, 30_000, it.flowMode == FlowMode.SOFT, it.flowMode) }
        val cards = sagaObjectiveCards(objectives, flows, emptyList(), emptyList(), Instant.ofEpochMilli(start + 40_000), zone, calc)
        assertEquals(FlowMode.entries, cards.map { it.objective.flowMode })
        assertEquals(listOf(50, 50, 50), cards.map { it.progressPercent })
    }
    @Test fun completedOneTimeObjectiveRemainsVisibleAfterItsPeriodEnds() {
        val obj = objective()
        val window = calc.windowFor(obj, Instant.ofEpochMilli(start), zone)
        val reward = calc.buildGrant(obj, ObjectiveKind.OneTime, window, 60_000, start + 60_000, 0).completion.copy(id = 1)
        val card = sagaObjectiveCards(listOf(obj), emptyList(), listOf(reward), emptyList(), Instant.ofEpochMilli(window.endMs + 1), zone, calc).single()
        assertEquals(ObjectiveCardState.Completed, card.state)
        assertEquals(100, card.progressPercent)
        assertEquals(1L, card.completion?.id)
    }
    @Test fun previousUnclaimedCycleCoexistsWithCurrentRecurringProgress() {
        val obj = objective().copy(objectiveType = "recurring")
        val window = calc.windowFor(obj, Instant.ofEpochMilli(start), zone)
        val reward = calc.buildGrant(obj, ObjectiveKind.Recurring, window, 60_000, start + 60_000, 0).completion.copy(id = 1)
        val cards = sagaObjectiveCards(listOf(obj), emptyList(), listOf(reward), emptyList(), Instant.ofEpochMilli(window.endMs + 1), zone, calc)
        assertEquals(listOf(ObjectiveCardState.InProgress, ObjectiveCardState.Completed), cards.map { it.state })
        assertEquals(1, sagaObjectiveCards(listOf(obj), emptyList(), listOf(reward.copy(pearlsClaimed = true)), emptyList(), Instant.ofEpochMilli(window.endMs + 1), zone, calc).size)
    }
    @Test fun skippedAndExpiredObjectivesDoNotAppearAsRunnable() {
        val obj = objective()
        val window = calc.windowFor(obj, Instant.ofEpochMilli(start), zone)
        val skipped = ObjectiveSkippedCycleEntity(objectiveId = 1, periodStartMs = window.startMs, periodEndMs = window.endMs, skippedAt = start)
        assertEquals(ObjectiveCardState.Skipped, sagaObjectiveCards(listOf(obj), emptyList(), emptyList(), listOf(skipped), Instant.ofEpochMilli(start + 1), zone, calc).single().state)
        assertEquals(ObjectiveCardState.Expired, sagaObjectiveCards(listOf(obj), emptyList(), emptyList(), emptyList(), Instant.ofEpochMilli(window.endMs + 1), zone, calc).single().state)
        assertTrue(sagaObjectiveCards(listOf(obj.copy(isArchived = true)), emptyList(), emptyList(), emptyList(), Instant.ofEpochMilli(start), zone, calc).isEmpty())
    }
}
