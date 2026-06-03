package com.kingkharnivore.skillz.domain.anchor

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnchorRuntimePolicyTest {
    @Test fun guideRunsOnlyForMeaningfulRunningGuideFlowWithUsageAccess() {
        assertTrue(AnchorRuntimePolicy.shouldRunGuide(input(mode = AnchorMode.GUIDE)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuide(input(mode = AnchorMode.GUIDE, usage = false)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuide(input(mode = AnchorMode.GUARD)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuide(input(entity = flow(accumulated = 0))))
        assertFalse(AnchorRuntimePolicy.shouldRunGuide(input(entity = flow(running = false))))
        assertFalse(AnchorRuntimePolicy.shouldRunGuide(input(selectedCount = 0)))
    }

    @Test fun guardRunsOnlyForMeaningfulRunningGuardFlowWithAccessibility() {
        assertTrue(AnchorRuntimePolicy.shouldRunGuard(input(mode = AnchorMode.GUARD, accessibility = true, usage = false)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuard(input(mode = AnchorMode.GUARD, accessibility = false)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuard(input(mode = AnchorMode.GUIDE, accessibility = true)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuard(input(entity = flow(pausedAnchor = true), mode = AnchorMode.GUARD, accessibility = true)))
        assertFalse(AnchorRuntimePolicy.shouldRunGuard(input(entity = flow(inBreak = true), mode = AnchorMode.GUARD, accessibility = true)))
    }

    private fun input(
        entity: OngoingSessionEntity? = flow(),
        mode: AnchorMode = AnchorMode.GUIDE,
        usage: Boolean = true,
        accessibility: Boolean = false,
        selectedCount: Int = 1
    ) = AnchorRuntimeInput(
        entity = entity,
        now = 10_000L,
        mode = mode,
        globallyEnabled = true,
        selectedPackageCount = selectedCount,
        usageAccessGranted = usage,
        accessibilityEnabled = accessibility
    )

    private fun flow(
        running: Boolean = true,
        accumulated: Long = 2_000L,
        pausedAnchor: Boolean = false,
        inBreak: Boolean = false
    ) = OngoingSessionEntity(
        flowInstanceId = "flow",
        title = "",
        description = "",
        tagName = "",
        isInFlowMode = true,
        isRunning = running,
        baseStartTimeMs = null,
        accumulatedBeforeStartMs = accumulated,
        anchorPaused = pausedAnchor,
        anchorBreakEndsAtMs = if (inBreak) 20_000L else null
    )
}
