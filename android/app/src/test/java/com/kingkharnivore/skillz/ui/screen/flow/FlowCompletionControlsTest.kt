package com.kingkharnivore.skillz.ui.screen.flow

import org.junit.Assert.assertEquals
import org.junit.Test

class FlowCompletionControlsTest {
    @Test fun standaloneSoftHasOnlySaveControl() {
        assertEquals(
            FlowCompletionControls.StandaloneSoft,
            flowCompletionControls(isSoftMode = true, isArcLinked = false)
        )
    }

    @Test fun arcLinkedSoftHasArcActions() {
        assertEquals(
            FlowCompletionControls.ArcActions,
            flowCompletionControls(isSoftMode = true, isArcLinked = true)
        )
    }

    @Test fun regularArcFlowStillHasArcActions() {
        assertEquals(
            FlowCompletionControls.ArcActions,
            flowCompletionControls(isSoftMode = false, isArcLinked = true)
        )
    }

    @Test fun standaloneRegularKeepsRegularActions() {
        assertEquals(
            FlowCompletionControls.RegularActions,
            flowCompletionControls(isSoftMode = false, isArcLinked = false)
        )
    }

    @Test fun terminalWithoutRewardSelectsRecoveredExit() {
        assertEquals(true, shouldRecoverTerminalExit(exitAfterReward = true, hasReward = false))
    }

    @Test fun terminalWithLiveRewardDoesNotAutoExit() {
        assertEquals(false, shouldRecoverTerminalExit(exitAfterReward = true, hasReward = true))
    }

    @Test fun terminalShellEntryConsumesExit() {
        assertEquals(
            RewardShellEntry.ConsumeTerminalExit,
            rewardShellEntry(exitAfterReward = true)
        )
    }

    @Test fun continuationShellEntryPreservesHandoff() {
        assertEquals(
            RewardShellEntry.PreserveContinuation,
            rewardShellEntry(exitAfterReward = false)
        )
    }

    @Test fun terminalShellEntryRemovesCompletedFlowDestination() {
        assertEquals(
            ShellNavigationMode.RemoveCompletedFlow,
            shellNavigationMode(RewardShellEntry.ConsumeTerminalExit)
        )
    }

    @Test fun continuationShellEntryPreservesPreparedFlowDestination() {
        assertEquals(
            ShellNavigationMode.PreservePreparedFlow,
            shellNavigationMode(RewardShellEntry.PreserveContinuation)
        )
    }
}
