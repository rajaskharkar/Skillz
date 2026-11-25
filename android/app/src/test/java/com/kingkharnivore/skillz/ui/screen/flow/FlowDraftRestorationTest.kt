package com.kingkharnivore.skillz.ui.screen.flow

import com.kingkharnivore.skillz.data.model.entity.OngoingSessionEntity
import com.kingkharnivore.skillz.model.state.flow.shouldApplyLaunchToDraft
import org.junit.Assert.*
import org.junit.Test

class FlowDraftRestorationTest {
    private val ideaDraft = OngoingSessionEntity(flowInstanceId = "idea-draft", title = "Edited step", description = "",
        tagName = "Design", originPulseId = 7, isInFlowMode = false, isRunning = false,
        baseStartTimeMs = null, accumulatedBeforeStartMs = 0)

    @Test fun returningToAutofilledIdeaRestoresItsIdleDraft() {
        assertFalse(shouldApplyLaunchToDraft(ideaDraft, hasLaunchOverrides = false, launchAlreadyApplied = false))
    }
    @Test fun recreatingGroveLaunchKeepsEditedIdeaAndOriginalFlowInstance() {
        assertFalse(shouldApplyLaunchToDraft(ideaDraft, hasLaunchOverrides = true, launchAlreadyApplied = true))
    }
    @Test fun freshExplicitLaunchCanReplaceAnIdleIdeaDraft() {
        assertTrue(shouldApplyLaunchToDraft(ideaDraft, hasLaunchOverrides = true, launchAlreadyApplied = false))
    }
    @Test fun explicitLaunchCannotOverwriteRunningPausedOrFocusFlows() {
        listOf(ideaDraft.copy(isRunning = true), ideaDraft.copy(isInFlowMode = true),
            ideaDraft.copy(baseStartTimeMs = 1), ideaDraft.copy(accumulatedBeforeStartMs = 1)).forEach {
            assertFalse(shouldApplyLaunchToDraft(it, hasLaunchOverrides = true, launchAlreadyApplied = false))
        }
    }
}
