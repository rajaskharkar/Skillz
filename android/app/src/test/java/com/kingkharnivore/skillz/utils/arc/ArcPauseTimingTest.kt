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
    private val pauseStart = 10_000_000L
    private val arc = ArcRuntimeState(42, false, 1.6, 0, pauseStart - minute, 9,
        pauseUsedMs = minute, pauseStartedAtMs = pauseStart)

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
        assertEquals(6 * minute, ArcPauseTiming.remainingMs(restored, pauseStart + 3 * minute))
        ArcPrefs(store).save(restored)
        val restoredAgain = ArcPauseTiming.restore(ArcPrefs(store).load(), ongoing())!!
        assertEquals(4 * minute, ArcPauseTiming.remainingMs(restoredAgain, pauseStart + 5 * minute))
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
        assertEquals(6 * minute, ArcPauseTiming.remainingMs(resumed, pauseStart + 50 * minute))
        prefs.save(resumed.copy(pauseStartedAtMs = pauseStart + 50 * minute))
        assertEquals(5 * minute, ArcPauseTiming.remainingMs(prefs.load()!!, pauseStart + 51 * minute))
        prefs.clear()
        assertNull(prefs.load())
        prefs.save(arc.copy(arcId = 43, pauseUsedMs = 0, pauseStartedAtMs = null))
        assertEquals(10 * minute, ArcPauseTiming.remainingMs(prefs.load()!!, pauseStart + 60 * minute))
    }

    @Test fun legacyPausedFlowUsesLastActiveIntervalRatherThanScreenOpenTime() {
        val restored = ArcPauseTiming.restore(arc.copy(pauseStartedAtMs = null), ongoing())!!
        assertEquals(pauseStart, restored.pauseStartedAtMs)
        assertEquals(6 * minute, ArcPauseTiming.remainingMs(restored, pauseStart + 3 * minute))
    }

    @Test fun reconstructionFromOngoingSnapshotAlsoRestoresPauseStart() {
        val restored = ArcPauseTiming.restore(null, ongoing())!!
        assertEquals(pauseStart, restored.pauseStartedAtMs)
        assertEquals(7 * minute, ArcPauseTiming.remainingMs(restored, pauseStart + 3 * minute))
    }

    @Test fun completingPausedFlowStopsChargingDuringBetweenFlowGrace() {
        val completed = ArcPauseTiming.endPause(arc, pauseStart + 2 * minute)
        assertNull(completed.pauseStartedAtMs)
        assertEquals(3 * minute, completed.pauseUsedMs)
        assertEquals(7 * minute, ArcPauseTiming.remainingMs(completed, pauseStart + 6 * minute))
    }

    @Test fun allArcPauseTiersExpireAtTheirDeadline() {
        listOf(1 to 2L, 4 to 5L, 9 to 10L).forEach { (count, budgetMinutes) ->
            val state = arc.copy(sessionCountInArc = count, pauseUsedMs = 0)
            assertEquals(1L, ArcPauseTiming.remainingMs(state, pauseStart + budgetMinutes * minute - 1))
            assertEquals(0L, ArcPauseTiming.remainingMs(state, pauseStart + budgetMinutes * minute))
        }
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
