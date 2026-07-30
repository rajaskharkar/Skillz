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
}
