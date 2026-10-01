package com.kingkharnivore.skillz.utils.arc

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArcPauseTimingTest {
    private val minute = 60_000L
    private val hour = 60 * minute
    private val pauseStart = 1_000_000_000L
    private val arc = ArcRuntimeState(42, false, 1.6, 0, pauseStart - minute, 9,
        pauseUsedMs = minute, pauseStartedAtMs = pauseStart, startedAtMs = pauseStart - 4 * hour)

    @Test fun timerPersistencePreservesOtherPreferencesAndCompletedArcHistory() = runBlocking {
        val store = MemoryPreferences()
        val language = stringPreferencesKey("app_language_tag")
        val legacyKey = longPreferencesKey("unrelated_existing_preference")
        store.data.value = preferencesOf(language to "ja", legacyKey to 12345L)
        val prefs = ArcPrefs(store)
        prefs.saveRecentlyEnded(arc.copy(arcId = 17), pauseStart)
        val completedHistory = prefs.loadRecentlyEnded()
        prefs.save(arc)
        prefs.save(ArcPauseTiming.endPause(arc, pauseStart + minute))
        prefs.clear()
        assertEquals("ja", store.data.value[language])
        assertEquals(12345L, store.data.value[legacyKey])
        assertEquals(completedHistory, prefs.loadRecentlyEnded())
    }

    @Test fun exitStoryReturnAndRepeatedRecreationKeepOriginalDeadline() = runBlocking {
        val store = MemoryPreferences()
        ArcPrefs(store).save(arc)
        val restored = ArcPauseTiming.restore(ArcPrefs(store).load(), ongoing())!!
        assertEquals(arc, restored)
        assertEquals(1 * minute, ArcPauseTiming.remainingMs(restored, pauseStart + 3 * minute))
        ArcPrefs(store).save(restored)
        val restoredAgain = ArcPauseTiming.restore(ArcPrefs(store).load(), ongoing())!!
        assertEquals(0L, ArcPauseTiming.remainingMs(restoredAgain, pauseStart + 5 * minute))
        assertEquals(0L, ArcPauseTiming.remainingMs(restoredAgain, pauseStart + 9 * minute))
        assertEquals(0L, ArcPauseTiming.remainingMs(restoredAgain, pauseStart + 30 * minute))
    }

    @Test fun resumePersistsUsedBudgetAndRemovesActivePauseTimestamp() = runBlocking {
        val store = MemoryPreferences()
        val prefs = ArcPrefs(store)
        prefs.save(arc)
        prefs.save(ArcPauseTiming.endPause(arc, pauseStart + 3 * minute))
        val resumed = ArcPauseTiming.restore(ArcPrefs(store).load(), ongoing().copy(isRunning = true))!!
        assertNull(resumed.pauseStartedAtMs)
        assertEquals(1 * minute, ArcPauseTiming.remainingMs(resumed, pauseStart + 50 * minute))
        prefs.save(resumed.copy(pauseStartedAtMs = pauseStart + 50 * minute))
        assertEquals(0L, ArcPauseTiming.remainingMs(prefs.load()!!, pauseStart + 51 * minute))
        prefs.clear()
        assertNull(prefs.load())
        prefs.save(arc.copy(arcId = 43, pauseUsedMs = 0, pauseStartedAtMs = null))
        assertEquals(5 * minute, ArcPauseTiming.remainingMs(prefs.load()!!, pauseStart + 60 * minute))
    }

    @Test fun legacyPausedFlowUsesLastActiveIntervalRatherThanScreenOpenTime() {
        val restored = ArcPauseTiming.restore(arc.copy(pauseStartedAtMs = null), ongoing())!!
        assertEquals(pauseStart, restored.pauseStartedAtMs)
        assertEquals(1 * minute, ArcPauseTiming.remainingMs(restored, pauseStart + 3 * minute))
    }

    @Test fun reconstructionFromOngoingSnapshotAlsoRestoresPauseStart() {
        val restored = ArcPauseTiming.restore(null, ongoing())!!
        assertEquals(pauseStart, restored.pauseStartedAtMs)
        assertEquals(2 * minute, ArcPauseTiming.remainingMs(restored, pauseStart + 3 * minute))
    }

    @Test fun completingPausedFlowStopsChargingDuringBetweenFlowGrace() {
        val completed = ArcPauseTiming.endPause(arc, pauseStart + 2 * minute)
        assertNull(completed.pauseStartedAtMs)
        assertEquals(3 * minute, completed.pauseUsedMs)
        assertEquals(2 * minute, ArcPauseTiming.remainingMs(completed, pauseStart + 6 * minute))
    }

    @Test fun allArcPauseTiersExpireAtTheirDeadlineRegardlessOfFlowCount() {
        listOf(0L to 5L, 3 * hour to 5L, 24 * hour to 30L, 72 * hour to 60L).forEach { (age, budgetMinutes) ->
            listOf(0, 2, 8, 99).forEach { count ->
                val state = arc.copy(sessionCountInArc = count, pauseUsedMs = 0, startedAtMs = pauseStart - age)
                assertEquals(1L, ArcPauseTiming.remainingMs(state, pauseStart + budgetMinutes * minute - 1))
                assertEquals(0L, ArcPauseTiming.remainingMs(state, pauseStart + budgetMinutes * minute))
            }
        }
    }

    @Test fun wallClockTierBoundariesAreInclusive() {
        listOf(
            3 * hour - 1 to 5L, 3 * hour to 5L,
            24 * hour - 1 to 5L, 24 * hour to 30L,
            72 * hour - 1 to 30L, 72 * hour to 60L
        ).forEach { (age, minutes) ->
            assertEquals(minutes * minute, ArcPauseTiming.budgetMs(arc.copy(startedAtMs = pauseStart - age), pauseStart))
        }
    }

    @Test fun eachPauseInFirstThreeHoursGetsFreshFiveMinutes() {
        val early = arc.copy(startedAtMs = pauseStart - hour, pauseStartedAtMs = null)
        val first = ArcPauseTiming.beginPause(early, pauseStart)
        assertEquals(5 * minute, ArcPauseTiming.remainingMs(first, pauseStart))
        val resumed = ArcPauseTiming.endPause(first, pauseStart + 4 * minute)
        val second = ArcPauseTiming.beginPause(resumed, pauseStart + 5 * minute)
        assertEquals(5 * minute, ArcPauseTiming.remainingMs(second, pauseStart + 5 * minute))
        assertEquals(0L, ArcPauseTiming.remainingMs(second, pauseStart + 10 * minute))
    }

    @Test fun pausesStartingAtThreeHoursShareTheFlowAllowance() {
        val state = arc.copy(startedAtMs = pauseStart - 3 * hour, pauseUsedMs = 0)
        val resumed = ArcPauseTiming.endPause(state, pauseStart + 2 * minute)
        val second = ArcPauseTiming.beginPause(resumed, pauseStart + 3 * minute)
        assertEquals(3 * minute, ArcPauseTiming.remainingMs(second, pauseStart + 3 * minute))
        assertEquals(0L, ArcPauseTiming.remainingMs(second, pauseStart + 6 * minute))
    }

    @Test fun earlyPausesDoNotConsumeTheLaterSharedAllowance() {
        val early = arc.copy(startedAtMs = pauseStart - 3 * hour + minute, pauseUsedMs = 0)
        val resumed = ArcPauseTiming.endPause(early, pauseStart + 2 * minute)
        val late = ArcPauseTiming.beginPause(resumed, pauseStart + 3 * minute)
        assertEquals(5 * minute, ArcPauseTiming.remainingMs(late, pauseStart + 3 * minute))
    }

    @Test fun elapsedWallClockCannotExtendOrReviveAnExistingPause() {
        listOf(24 * hour to 5L, 72 * hour to 30L).forEach { (threshold, budgetMinutes) ->
            val state = arc.copy(startedAtMs = pauseStart - threshold + minute, pauseUsedMs = 0)
            assertEquals((budgetMinutes - 2) * minute, ArcPauseTiming.remainingMs(state, pauseStart + 2 * minute))
            assertEquals(0L, ArcPauseTiming.remainingMs(state, pauseStart + 3 * hour))
            assertEquals(state, ArcPauseTiming.beginPause(state, pauseStart + 3 * hour))
        }
    }

    @Test fun restoreUsesFirstFlowTimestampForLegacyArc() {
        val restored = ArcPauseTiming.restore(arc.copy(startedAtMs = null), ongoing(), pauseStart - 72 * hour)!!
        assertEquals(pauseStart - 72 * hour, restored.startedAtMs)
        assertEquals(59 * minute, ArcPauseTiming.remainingMs(restored, pauseStart))
    }

    @Test fun unstartedPlannedFlowDoesNotStartArcAgeWhenOpened() {
        val restored = ArcPauseTiming.restore(null, ongoing().copy(
            accumulatedBeforeStartMs = 0, activeIntervalJson = null, arcSessionCountInArc = 0
        ))!!
        assertNull(restored.startedAtMs)
        assertNull(restored.pauseStartedAtMs)
    }

    @Test fun arcStartSurvivesActiveAndRecentlyEndedPersistenceAndIsCleared() = runBlocking {
        val prefs = ArcPrefs(MemoryPreferences())
        prefs.save(arc)
        assertEquals(arc.startedAtMs, prefs.load()!!.startedAtMs)
        prefs.saveRecentlyEnded(arc, pauseStart)
        assertEquals(arc.startedAtMs, prefs.loadRecentlyEnded()!!.startedAtMs)
        prefs.clear()
        prefs.clearRecentlyEnded()
        prefs.save(arc.copy(startedAtMs = null))
        prefs.saveRecentlyEnded(arc.copy(startedAtMs = null), pauseStart)
        assertNull(prefs.load()!!.startedAtMs)
        assertNull(prefs.loadRecentlyEnded()!!.startedAtMs)
    }

    private fun ongoing() = OngoingSessionEntity(
        flowInstanceId = "flow", title = "", description = "", tagName = "",
        isInFlowMode = false, isRunning = false, baseStartTimeMs = null,
        accumulatedBeforeStartMs = minute, arcId = 42, arcSessionCountInArc = 9,
        activeIntervalJson = "${pauseStart - minute}-$pauseStart"
    )

    private class MemoryPreferences : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            return transform(data.value).also { data.value = it }
        }
    }
}
