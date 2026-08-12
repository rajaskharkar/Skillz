package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArcFlowStartCoordinatorTest {
    private val end = 1_000_000L

    @Test
    fun `exact boundary remains resumed when internal start work crosses boundary`() = runBlocking {
        assertAcceptedAfterInternalWork(ArcRules.GRACE_WINDOW_MS, internalWorkMs = 500L)
    }

    @Test
    fun `just inside boundary remains resumed when internal start work crosses boundary`() = runBlocking {
        assertAcceptedAfterInternalWork(ArcRules.GRACE_WINDOW_MS - 1L, internalWorkMs = 500L)
    }

    @Test
    fun `start outside boundary does not resume`() = runBlocking {
        var now = end + ArcRules.GRACE_WINDOW_MS + 1L
        val store = Store(recent = arc())
        val coordinator = ArcFlowStartCoordinator(ArcContinuationLifecycle(store)) { now }

        val start = coordinator.start(isSoftFlow = false) { now += 500L }

        assertEquals(end + ArcRules.GRACE_WINDOW_MS + 1L, start.startedAtMs)
        assertNull(start.arc)
        assertNull(store.active)
    }

    private suspend fun assertAcceptedAfterInternalWork(gapMs: Long, internalWorkMs: Long) {
        var now = end + gapMs
        val store = Store(recent = arc())
        val coordinator = ArcFlowStartCoordinator(ArcContinuationLifecycle(store)) { now }

        val start = coordinator.start(isSoftFlow = false) {
            now += internalWorkMs
        }

        assertEquals(end + gapMs, start.startedAtMs)
        assertEquals(42L, start.arc?.arcId)
        assertEquals(5, start.arc?.sessionCountInArc)
        assertEquals(1.6, start.arc?.multiplier ?: 0.0, 0.0)
        assertEquals(42L, store.active?.arcId)
    }

    private fun arc() = ArcRuntimeState(42L, false, 1.6, 0L, end, 5)

    private class Store(
        var active: ArcRuntimeState? = null,
        var recent: ArcRuntimeState? = null
    ) : ArcContinuationStore {
        override suspend fun loadActive() = active
        override suspend fun saveActive(state: ArcRuntimeState) { active = state }
        override suspend fun clearActive() { active = null }
        override suspend fun loadRecentlyEnded() = recent
        override suspend fun saveRecentlyEnded(state: ArcRuntimeState, completedAtMs: Long) {
            recent = state
        }
        override suspend fun clearRecentlyEnded() { recent = null }
    }
}
