package com.kingkharnivore.skillz.ui.navigation

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveFlowHeroPolicyTest {
    @Test
    fun activeFlowHeroIsVisibleWheneverOngoingSessionExists() {
        val ongoing = OngoingSessionEntity(
            flowInstanceId = "flow",
            title = "",
            description = "",
            tagName = "",
            isInFlowMode = false,
            isRunning = false
        )

        assertTrue(shouldShowStoryActiveFlowHero(ongoing))
    }

    @Test
    fun activeFlowHeroIgnoresAnchorRuntimeFields() {
        val ongoing = OngoingSessionEntity(
            flowInstanceId = "flow",
            title = "",
            description = "",
            tagName = "",
            isInFlowMode = false,
            isRunning = false,
            anchorEnabledForFlow = true,
            anchorDisabledForFlow = false,
            anchorPaused = true,
            anchorBreakStartedAtMs = 1L,
            anchorBreakEndsAtMs = 61_000L,
            anchorReturnPanelPending = true,
            anchorUsageAccessRevoked = true
        )

        assertTrue(shouldShowStoryActiveFlowHero(ongoing))
    }

    @Test
    fun activeFlowHeroIsHiddenWhenNoOngoingSessionExists() {
        assertFalse(shouldShowStoryActiveFlowHero(null))
    }
}
