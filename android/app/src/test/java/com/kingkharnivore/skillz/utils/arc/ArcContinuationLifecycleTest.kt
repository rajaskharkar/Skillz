package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ArcContinuationLifecycleTest {
    private val end = 10_000_000L

    @Test
    fun `planned Arc clock starts with first Flow and is preserved on subsequent starts`() = runBlocking {
        val store = FakeStore(active = finalArc().copy(sessionCountInArc = 0))
        val lifecycle = ArcContinuationLifecycle(store)
        val first = lifecycle.resolveForFlowStart(end, isSoftFlow = false)!!
        assertEquals(end, first.startedAtMs)
        store.active = first.copy(sessionCountInArc = 1, lastSessionEndTimeMs = end + 60_000L)
        assertEquals(end, lifecycle.resolveForFlowStart(end + 120_000L, isSoftFlow = true)!!.startedAtMs)
    }

    @Test
    fun `legacy active and recent Arcs recover original start from saved Flows`() = runBlocking {
        for (recent in listOf(false, true)) {
            val store = if (recent) FakeStore(recent = finalArc()) else FakeStore(active = finalArc())
            val lifecycle = ArcContinuationLifecycle(store) { id ->
                assertEquals(123L, id)
                end - 1_000_000L
            }
            assertEquals(end - 1_000_000L, lifecycle.resolveForFlowStart(end, isSoftFlow = false)!!.startedAtMs)
        }
    }

    @Test
    fun `each new regular or Soft Flow resets pause accounting for active and recent Arcs`() = runBlocking {
        for (isSoft in listOf(false, true)) {
            for (isRecent in listOf(false, true)) {
                val previous = finalArc().copy(pauseUsedMs = 240_000L, pauseStartedAtMs = end - 60_000L)
                val store = if (isRecent) FakeStore(recent = previous) else FakeStore(active = previous)

                val started = ArcContinuationLifecycle(store)
                    .resolveForFlowStart(end + 60_000L, isSoftFlow = isSoft)!!

                assertEquals(0L, started.pauseUsedMs)
                assertNull(started.pauseStartedAtMs)
                assertEquals(previous.arcId, started.arcId)
                assertEquals(previous.sessionCountInArc, started.sessionCountInArc)
                assertEquals(started, store.active)
            }
        }
    }

    @Test
    fun `completed Arc resumes with final state at every eligible boundary`() = runBlocking {
        listOf(0L, 2 * 60_000L, 4 * 60_000L + 59_000L, ArcRules.GRACE_WINDOW_MS)
            .forEach { gap ->
                val store = FakeStore(active = finalArc())
                val lifecycle = ArcContinuationLifecycle(store)

                lifecycle.completeArc(store.active!!, end)
                val resumed = lifecycle.resolveForFlowStart(end + gap, isSoftFlow = false)

                assertEquals(123L, resumed?.arcId)
                assertEquals(5, resumed?.sessionCountInArc)
                assertEquals(1.6, resumed?.multiplier ?: 0.0, 0.0)
                assertEquals(6, resumed!!.sessionCountInArc + 1)
                assertSame(resumed, store.active)
                assertNull(store.recent)
            }
    }

    @Test
    fun `completed Arc expires only when Flow actually starts after boundary`() = runBlocking {
        listOf(ArcRules.GRACE_WINDOW_MS + 1L, 6 * 60_000L).forEach { gap ->
            val store = FakeStore(active = finalArc())
            val lifecycle = ArcContinuationLifecycle(store)
            lifecycle.completeArc(store.active!!, end)

            assertNull(lifecycle.resolveForFlowStart(end + gap, isSoftFlow = false))
            assertNull(store.active)
            assertNull(store.recent)
        }
    }

    @Test
    fun `opening screen does not promote or mutate completed candidate`() = runBlocking {
        val completed = finalArc()
        val store = FakeStore(active = completed)
        val lifecycle = ArcContinuationLifecycle(store)
        lifecycle.completeArc(completed, end)

        // ViewModel/screen creation only reads active state; no start transition occurs.
        assertNull(store.loadActive())
        assertEquals(completed, store.loadRecentlyEnded())
        assertFalse(store.operations.contains("saveActive"))

        // Opening at +4m cannot make a later +6m Start eligible.
        assertNull(lifecycle.resolveForFlowStart(end + 6 * 60_000L, isSoftFlow = false))
    }

    @Test
    fun `Soft preparation leaves candidate multiplier unchanged and Start resets only multiplier`() = runBlocking {
        val completed = finalArc()
        val store = FakeStore(active = completed)
        val lifecycle = ArcContinuationLifecycle(store)
        lifecycle.completeArc(completed, end)

        assertEquals(1.6, store.recent!!.multiplier, 0.0)
        val started = lifecycle.resolveForFlowStart(end + 2 * 60_000L, isSoftFlow = true)!!

        assertEquals(completed.arcId, started.arcId)
        assertEquals(completed.sessionCountInArc, started.sessionCountInArc)
        assertEquals(ArcRuntimeState.BASE_MULTIPLIER, started.multiplier, 0.0)
    }

    @Test
    fun `Soft completion can be followed by regular continuation from reset state`() = runBlocking {
        val store = FakeStore(active = finalArc())
        val lifecycle = ArcContinuationLifecycle(store)
        lifecycle.completeArc(store.active!!, end)
        val softStarted = lifecycle.resolveForFlowStart(end + 60_000L, isSoftFlow = true)!!
        val softCompleted = softStarted.afterCompletedSoftFlow(end + 2 * 60_000L)
        store.active = softCompleted

        lifecycle.completeArc(softCompleted, end + 2 * 60_000L)
        val regular = lifecycle.resolveForFlowStart(end + 3 * 60_000L, isSoftFlow = false)!!

        assertEquals(123L, regular.arcId)
        assertEquals(6, regular.sessionCountInArc)
        assertEquals(ArcRuntimeState.BASE_MULTIPLIER, regular.multiplier, 0.0)
    }

    @Test
    fun `completion persists final candidate before clearing active state`() = runBlocking {
        val store = FakeStore(active = finalArc())
        ArcContinuationLifecycle(store).completeArc(store.active!!, end)

        assertEquals(listOf("saveRecent", "clearActive"), store.operations)
        assertEquals(5, store.recent?.sessionCountInArc)
        assertEquals(end, store.recent?.lastSessionEndTimeMs)
    }

    @Test
    fun `duplicate Start resolution keeps one active Arc identity`() = runBlocking {
        val store = FakeStore(active = finalArc())
        val lifecycle = ArcContinuationLifecycle(store)
        lifecycle.completeArc(store.active!!, end)

        val first = lifecycle.resolveForFlowStart(end + 1L, isSoftFlow = false)
        val second = lifecycle.resolveForFlowStart(end + 1L, isSoftFlow = false)

        assertEquals(first, second)
        assertEquals(123L, store.active?.arcId)
        assertNull(store.recent)
    }

    private fun finalArc() = ArcRuntimeState(
        arcId = 123L,
        isPending = false,
        multiplier = 1.6,
        progressMs = 0L,
        lastSessionEndTimeMs = end,
        sessionCountInArc = 5
    )

    private class FakeStore(
        var active: ArcRuntimeState? = null,
        var recent: ArcRuntimeState? = null
    ) : ArcContinuationStore {
        val operations = mutableListOf<String>()

        override suspend fun loadActive() = active
        override suspend fun saveActive(state: ArcRuntimeState) {
            operations += "saveActive"
            active = state
        }
        override suspend fun clearActive() {
            operations += "clearActive"
            active = null
        }
        override suspend fun loadRecentlyEnded() = recent
        override suspend fun saveRecentlyEnded(state: ArcRuntimeState, completedAtMs: Long) {
            operations += "saveRecent"
            recent = state
        }
        override suspend fun clearRecentlyEnded() {
            operations += "clearRecent"
            recent = null
        }
    }
}
