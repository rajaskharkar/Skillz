package com.kingkharnivore.skillz.utils.arc

import com.kingkharnivore.skillz.ui.model.ArcRuntimeState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ArcContinuationResolverTest {
    private val end = 1_000_000L

    @Test
    fun `between Flow countdown and start eligibility share all age tier boundaries`() {
        val hour = 60 * 60_000L
        listOf(
            0L to 5L, 24 * hour - 1 to 5L, 24 * hour to 30L,
            72 * hour - 1 to 30L, 72 * hour to 60L, 100 * hour to 60L
        ).forEach { (age, minutes) ->
            val candidate = arc().copy(startedAtMs = end - age, pauseUsedMs = hour)
            val budget = minutes * 60_000L
            assertEquals(budget, ArcContinuationResolver.remainingMs(candidate, end))
            assertEquals(1L, ArcContinuationResolver.remainingMs(candidate, end + budget - 1))
            assertSame(candidate, resolve(candidate, end + budget))
            assertSame(candidate, ArcContinuationResolver.resolveArcForNewFlow(candidate, null, end + budget))
            assertEquals(0L, ArcContinuationResolver.remainingMs(candidate, end + budget))
            assertNull(resolve(candidate, end + budget + 1))
            assertNull(ArcContinuationResolver.resolveArcForNewFlow(candidate, null, end + budget + 1))
        }
    }

    @Test
    fun `waiting across an age threshold cannot extend or revive between Flow grace`() {
        val hour = 60 * 60_000L
        listOf(24 * hour to 5L, 72 * hour to 30L).forEach { (threshold, minutes) ->
            val candidate = arc().copy(startedAtMs = end - threshold + 60_000L)
            assertEquals((minutes - 2) * 60_000L, ArcContinuationResolver.remainingMs(candidate, end + 120_000L))
            assertNull(resolve(candidate, end + minutes * 60_000L + 1))
            assertNull(resolve(candidate, end + 3 * hour))
        }
    }

    @Test
    fun `empty recently ended Arc cannot continue without a completed Flow`() {
        assertNull(resolve(arc().copy(sessionCountInArc = 0), end))
    }

    @Test
    fun `regular Flow resumes same Arc through inclusive five minute boundary`() {
        val arc = arc()
        listOf(0L, 1_000L, 2 * 60_000L, 4 * 60_000L + 59_000L, 5 * 60_000L)
            .forEach { gap ->
                assertSame(arc, resolve(arc, end + gap))
            }
    }

    @Test
    fun `Flow after boundary does not resurrect Arc`() {
        assertNull(resolve(arc(), end + ArcRules.GRACE_WINDOW_MS + 1L))
        assertNull(resolve(arc(), end + 6 * 60_000L))
    }

    @Test
    fun `future previous end timestamp is rejected`() {
        assertNull(resolve(arc(), end - 1L))
    }

    @Test
    fun `active eligible Arc wins over an older recently ended Arc`() {
        val active = arc(id = 9L)
        val older = arc(id = 4L).copy(lastSessionEndTimeMs = end - 1_000L)
        assertSame(
            active,
            ArcContinuationResolver.resolveArcForNewFlow(active, older, end + 1_000L)
        )
    }

    @Test
    fun `eligible completed Arc wins over a pre-created empty Arc`() {
        val empty = arc(id = 9L).copy(sessionCountInArc = 0)
        val completed = arc(id = 4L)

        assertSame(
            completed,
            ArcContinuationResolver.resolveArcForNewFlow(empty, completed, end + 1_000L)
        )
    }

    @Test
    fun `continuation preserves identity progression and accumulated state`() {
        val original = arc()
        val resumed = resolve(original, end + 2 * 60_000L)!!
        assertSame(original, resumed)
        assertEquals(42L, resumed.arcId)
        assertEquals(1.7, resumed.multiplier, 0.0)
        assertEquals(6, resumed.sessionCountInArc)
        assertEquals(123_000L, resumed.progressMs)
    }

    @Test
    fun `Soft Flow resets only multiplier and remains in same Arc progression`() {
        val original = resolve(arc(), end + 2 * 60_000L)!!
        val completedSoft = original.resetMultiplierForSoftFlow().afterCompletedSoftFlow(end + 3 * 60_000L)

        assertEquals(original.arcId, completedSoft.arcId)
        assertEquals(ArcRuntimeState.BASE_MULTIPLIER, completedSoft.multiplier, 0.0)
        assertEquals(original.sessionCountInArc + 1, completedSoft.sessionCountInArc)
        assertSame(completedSoft, resolve(completedSoft, end + 4 * 60_000L))
    }

    private fun resolve(recent: ArcRuntimeState, start: Long) =
        ArcContinuationResolver.resolveArcForNewFlow(null, recent, start)

    private fun arc(id: Long = 42L) = ArcRuntimeState(
        arcId = id,
        isPending = false,
        multiplier = 1.7,
        progressMs = 123_000L,
        lastSessionEndTimeMs = end,
        sessionCountInArc = 6
    )
}
