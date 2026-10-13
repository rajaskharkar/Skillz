package com.kingkharnivore.skillz.ui.screen.flow

import com.kingkharnivore.skillz.data.model.entity.FlowPlanEntity
import com.kingkharnivore.skillz.data.model.entity.HorizonKind
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.model.state.flow.*
import org.junit.Assert.*
import org.junit.Test

class ActivityTitleSuggestionTest {
    private val habit = FlowPlanEntity(id = 7, title = "Brush teeth", tagId = 2)
    private val tags = listOf(TagEntity(id = 2, name = "ready"))

    @Test fun selectingEitherActivityKindAppliesEverySavedFlowModeAndLink() {
        for (kind in listOf(HorizonKind.HABIT, HorizonKind.PLAN)) {
            for (mode in FlowMode.entries) {
                val saved = habit.copy(kind = kind, mode = mode)
                val state = FlowUiState(title = "bru", mode = FlowMode.POWER, isSurgeOn = true, surgePlannedMs = 60_000)
                val selected = state.withActivityTitle(activityTitleSuggestions(listOf(saved), tags, state).single())
                assertEquals(kind, selected.originPlanKind)
                assertEquals(7L, selected.originPlanId)
                assertEquals(mode, selected.mode)
                assertEquals("ready", selected.tagName)
                assertEquals(mode != FlowMode.SOFT, selected.isSurgeOn)
                assertEquals(if (mode == FlowMode.SOFT) null else 60_000L, selected.surgePlannedMs)
            }
        }
    }

    @Test fun activeOrPausedTimerOnlySuggestsActivitiesOfItsLockedType() {
        val activities = FlowMode.entries.mapIndexed { index, mode -> habit.copy(id = index + 1L, mode = mode) }
        for (state in listOf(
            FlowUiState(title = "bru", isInFlowMode = true),
            FlowUiState(title = "bru", stopwatch = StopwatchState(isRunning = true)),
            FlowUiState(title = "bru", stopwatch = StopwatchState(elapsedMs = 10_000))
        )) {
            val suggestions = activityTitleSuggestions(activities, tags, state)
            assertEquals(listOf(FlowMode.FLOW), suggestions.map { it.mode })
            assertEquals(state, state.withActivityTitle(ActivityTitleSuggestion(9, "Other", "ready", mode = FlowMode.SOFT)))
        }
    }

    @Test fun softConfirmationOnlyAppliesToAnUnlockedNonSoftFlowWithAnArcBonus() {
        val base = FlowUiState(isInArc = true, arcMultiplier = 1.0)
        assertFalse(base.requiresSoftArcConfirmation())
        assertFalse(base.copy(arcMultiplier = null).requiresSoftArcConfirmation())
        assertFalse(base.copy(isInArc = false, arcMultiplier = 1.3).requiresSoftArcConfirmation())
        val activeBonus = base.copy(arcMultiplier = 1.3)
        assertTrue(activeBonus.requiresSoftArcConfirmation())
        assertTrue(activeBonus.copy(mode = FlowMode.POWER).requiresSoftArcConfirmation())
        assertFalse(activeBonus.copy(mode = FlowMode.SOFT).requiresSoftArcConfirmation())
        assertFalse(activeBonus.copy(stopwatch = StopwatchState(elapsedMs = 60_000)).requiresSoftArcConfirmation())
    }

    private fun selected() = FlowUiState(stopwatch = StopwatchState(isRunning = true, elapsedMs = 60_000))
        .withActivityTitle(ActivityTitleSuggestion(7, "Brush teeth", "ready"))

    @Test fun editingTitleDetachesHabitEvenWhenTagStaysTheSame() {
        val changed = selected().withEditedTitle("Read a book")
        assertNull(changed.originPlanId)
        assertNull(changed.originPlanKind)
        assertEquals("ready", changed.tagName)
        assertEquals(selected().stopwatch, changed.stopwatch)
        assertNull(changed.withEditedTitle("Brush teeth").originPlanId)
    }

    @Test fun editingTagDetachesHabitEvenWhenTitleStaysTheSame() {
        val changed = selected().withEditedTagName("reading")
        assertNull(changed.originPlanId)
        assertEquals("Brush teeth", changed.title)
        assertNull(changed.withEditedTagName("ready").originPlanId)
    }

    @Test fun resettingFlowDetachesHabitAndStopsTimer() {
        val reset = selected().withResetStopwatch()
        assertNull(reset.originPlanId)
        assertNull(reset.originPlanKind)
        assertEquals(StopwatchState(isRunning = false, elapsedMs = 0), reset.stopwatch)
        assertNull(reset.withEditedTitle("Brush teeth").withEditedTagName("ready").originPlanId)
    }

    @Test fun unchangedValuesRetainSelectionButReselectingIsRequiredAfterAnEdit() {
        val unchanged = selected().withEditedTitle("Brush teeth").withEditedTagName("ready")
        assertEquals(7L, unchanged.originPlanId)
        val reselected = unchanged.withEditedTitle("Read").withActivityTitle(ActivityTitleSuggestion(8, "Reading", "study"))
        assertEquals(8L, reselected.originPlanId)
        assertEquals("study", reselected.tagName)
    }

    @Test fun restoredHabitLinkIsDetachedByEditsWithoutDependingOnSuggestions() {
        val restored = FlowUiState(title = "Brush teeth", tagName = "ready", originPlanId = 7, originPlanKind = HorizonKind.HABIT)
        assertNull(restored.withEditedTitle("Read").originPlanId)
        assertNull(restored.withEditedTagName("study").originPlanId)
        assertNull(restored.withResetStopwatch().originPlanId)
    }

    @Test fun planJourneyLinkSurvivesEditsAndTimerReset() {
        val plan = FlowUiState(title = "Sort photos", tagName = "home", originPlanId = 9, originPlanKind = HorizonKind.PLAN)
        val changed = plan.withEditedTitle("Sorted first album").withEditedTagName("photos").withResetStopwatch()
        assertEquals(9L, changed.originPlanId)
        assertEquals(HorizonKind.PLAN, changed.originPlanKind)
    }

    @Test fun typingFindsHabitsIgnoringCaseAndWhitespaceWithoutLinkingThem() {
        val state = FlowUiState(title = " BRU ")
        val results = activityTitleSuggestions(listOf(habit), tags, state)
        assertEquals(listOf(ActivityTitleSuggestion(7, "Brush teeth", "ready")), results)
        assertNull(state.originPlanId)
        assertTrue(activityTitleSuggestions(listOf(habit), tags, state.copy(title = " ")).isEmpty())
        assertTrue(activityTitleSuggestions(listOf(habit), tags, state.copy(title = "Read")).isEmpty())
    }

    @Test fun includesActivePlansButExcludesArchivedAndCompletedActivities() {
        val results = activityTitleSuggestions(listOf(
            habit,
            habit.copy(id = 8, kind = HorizonKind.PLAN),
            habit.copy(id = 9, archived = true),
            habit.copy(id = 10, completedAt = 123)
        ), tags, FlowUiState(title = "bru"))
        assertEquals(listOf(7L, 8L), results.map { it.id })
        assertEquals(HorizonKind.PLAN, results.last().kind)
    }

    @Test fun selectionCopiesHabitTagAndIdAndPreservesRunningFlow() {
        val state = FlowUiState(title = "bru", tagName = "old", mode = FlowMode.POWER,
            stopwatch = StopwatchState(isRunning = true, elapsedMs = 90_000), isInArc = true)
        val selected = state.withActivityTitle(activityTitleSuggestions(listOf(habit.copy(mode = FlowMode.POWER)), tags, state).single())
        assertEquals("Brush teeth", selected.title)
        assertEquals("ready", selected.tagName)
        assertEquals(7L, selected.originPlanId)
        assertEquals(state.stopwatch, selected.stopwatch)
        assertEquals(FlowMode.POWER, selected.mode)
        assertTrue(selected.isInArc)
        assertTrue(activityTitleSuggestions(listOf(habit), tags, selected).isEmpty())
    }

    @Test fun sameTitleHabitsRemainDistinctAndSelectionReplacesPreviousLink() {
        val second = habit.copy(id = 8, tagId = 3)
        val state = FlowUiState(title = "bru", originPlanId = 7)
        val suggestions = activityTitleSuggestions(listOf(habit, second), tags + TagEntity(id = 3, name = "evening"), state)
        assertEquals(2, suggestions.size)
        val selected = state.withActivityTitle(suggestions.last())
        assertEquals(8L, selected.originPlanId)
        assertEquals("evening", selected.tagName)
    }

    @Test fun untaggedHabitClearsPreviousTagAndPrefixMatchesComeFirst() {
        val state = FlowUiState(title = "bru", tagName = "old")
        val suggestions = activityTitleSuggestions(listOf(habit.copy(id = 8, title = "Evening brushing"), habit.copy(tagId = null)), tags, state)
        assertEquals(listOf(7L, 8L), suggestions.map { it.id })
        assertEquals("", state.withActivityTitle(suggestions.first()).tagName)
    }
}
