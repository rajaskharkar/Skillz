package com.kingkharnivore.skillz.ui.navigation

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.domain.anchor.effectiveFlowElapsedMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveFlowHeroPolicyTest {
    @Test fun noOngoingSessionHidesHero() {
        assertFalse(shouldShowStoryActiveFlowHero(null, now = 10_000L))
    }

    @Test fun zeroTimeOngoingSessionHidesHero() {
        assertFalse(shouldShowStoryActiveFlowHero(draft(isInFlowMode = true), now = 10_000L))
    }

    @Test fun anchorEnabledZeroTimeOngoingSessionStillHidesHero() {
        assertFalse(
            shouldShowStoryActiveFlowHero(
                draft(isInFlowMode = true, anchorEnabledForFlow = true),
                now = 10_000L
            )
        )
    }

    @Test fun nonFlowModeOngoingSessionHidesHeroEvenWithElapsedTime() {
        assertFalse(shouldShowStoryActiveFlowHero(draft(accumulatedBeforeStartMs = 10_000L), now = 10_000L))
    }

    @Test fun runningFlowWithMeaningfulElapsedShowsHero() {
        val ongoing = draft(isInFlowMode = true, isRunning = true, baseStartTimeMs = 8_000L)

        assertEquals(2_000L, effectiveFlowElapsedMs(ongoing, now = 10_000L))
        assertTrue(shouldShowStoryActiveFlowHero(ongoing, now = 10_000L))
    }

    @Test fun pausedFlowWithAccumulatedElapsedShowsHero() {
        assertTrue(shouldShowStoryActiveFlowHero(draft(isInFlowMode = true, accumulatedBeforeStartMs = 2_000L), now = 10_000L))
    }

    @Test fun activeFlowHeroIgnoresAnchorRuntimeFieldsWhenElapsedIsMeaningful() {
        val ongoing = draft(
            isInFlowMode = true,
            accumulatedBeforeStartMs = 2_000L,
            anchorEnabledForFlow = true,
            anchorPaused = true,
            anchorBreakStartedAtMs = 1L,
            anchorBreakEndsAtMs = 61_000L,
            anchorReturnPanelPending = true,
            anchorUsageAccessRevoked = true
        )

        assertTrue(shouldShowStoryActiveFlowHero(ongoing, now = 10_000L))
    }

    private fun draft(
        isInFlowMode: Boolean = false,
        isRunning: Boolean = false,
        baseStartTimeMs: Long? = null,
        accumulatedBeforeStartMs: Long = 0L,
        anchorEnabledForFlow: Boolean = false,
        anchorPaused: Boolean = false,
        anchorBreakStartedAtMs: Long? = null,
        anchorBreakEndsAtMs: Long? = null,
        anchorReturnPanelPending: Boolean = false,
        anchorUsageAccessRevoked: Boolean = false
    ) = OngoingSessionEntity(
        flowInstanceId = "flow",
        title = "",
        description = "",
        tagName = "",
        isInFlowMode = isInFlowMode,
        isRunning = isRunning,
        baseStartTimeMs = baseStartTimeMs,
        accumulatedBeforeStartMs = accumulatedBeforeStartMs,
        anchorEnabledForFlow = anchorEnabledForFlow,
        anchorPaused = anchorPaused,
        anchorBreakStartedAtMs = anchorBreakStartedAtMs,
        anchorBreakEndsAtMs = anchorBreakEndsAtMs,
        anchorReturnPanelPending = anchorReturnPanelPending,
        anchorUsageAccessRevoked = anchorUsageAccessRevoked
    )
}
