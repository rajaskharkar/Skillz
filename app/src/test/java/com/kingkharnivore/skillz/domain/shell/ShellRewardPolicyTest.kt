package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    @Test
    fun octopusRequiresThirdThirtyMinuteFlow() {
        assertFalse(ShellRewardPolicy.shouldDiscoverOctopus(minutes = 30, flow30BadgeCount = 1))
        assertFalse(ShellRewardPolicy.shouldDiscoverOctopus(minutes = 30, flow30BadgeCount = 2))
        assertTrue(ShellRewardPolicy.shouldDiscoverOctopus(minutes = 30, flow30BadgeCount = 3))
        assertFalse(ShellRewardPolicy.shouldDiscoverOctopus(minutes = 29, flow30BadgeCount = 3))
    }
}
