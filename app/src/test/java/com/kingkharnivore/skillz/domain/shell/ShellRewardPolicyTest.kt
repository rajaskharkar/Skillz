package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import org.junit.Assert.assertEquals
import org.junit.Test

class ShellRewardPolicyTest {
    @Test
    fun milestoneFindsScaleByRegularFlowDuration() {
        assertEquals(listOf(ShellContentCatalog.FOCUS_MINNOW), ShellRewardPolicy.milestoneFindsForMinutes(10))
        assertEquals(
            listOf(ShellContentCatalog.FOCUS_SEAHORSE),
            ShellRewardPolicy.milestoneFindsForMinutes(30)
        )
        assertEquals(listOf(ShellContentCatalog.FOCUS_MANTA), ShellRewardPolicy.milestoneFindsForMinutes(60))
        assertEquals(listOf(ShellContentCatalog.FOCUS_WHALE), ShellRewardPolicy.milestoneFindsForMinutes(120))
    }

}
