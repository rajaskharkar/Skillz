package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureRealm
import com.kingkharnivore.skillz.utils.shell.ShellRewardPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellRewardPolicyTest {
    @Test
    fun seaMilestonesAreUnchangedForRegularAndPowerFlows() {
        val cases = mapOf(
            10 to listOf(ShellContentCatalog.FOCUS_MINNOW),
            30 to listOf(ShellContentCatalog.FOCUS_SEAHORSE),
            60 to listOf(ShellContentCatalog.FOCUS_MANTA),
            120 to listOf(ShellContentCatalog.FOCUS_WHALE)
        )
        for (mode in listOf(FlowMode.FLOW, FlowMode.POWER)) {
            cases.forEach { (minutes, expected) ->
                assertEquals(expected, ShellRewardPolicy.milestoneFindsForMinutes(minutes, mode))
            }
        }
    }

    @Test
    fun formerLandDurationsKeepOnlyTheirExistingSeaRewards() {
        val minnow = ShellContentCatalog.FOCUS_MINNOW
        val seahorse = ShellContentCatalog.FOCUS_SEAHORSE
        val manta = ShellContentCatalog.FOCUS_MANTA
        val whale = ShellContentCatalog.FOCUS_WHALE
        val cases = mapOf(
            45 to listOf(seahorse, minnow),
            75 to listOf(manta, minnow),
            105 to listOf(manta, seahorse, minnow),
            135 to listOf(whale, minnow),
            150 to listOf(whale, whale, seahorse),
            180 to listOf(whale, whale, manta)
        )
        for (mode in listOf(FlowMode.FLOW, FlowMode.POWER)) {
            cases.forEach { (minutes, expected) ->
                assertEquals(expected, ShellRewardPolicy.milestoneFindsForMinutes(minutes, mode))
            }
        }
    }

    @Test
    fun noLandOrRedCreaturesAreGrantedAtOrBeyondAnyDuration() {
        for (minutes in -1..360) {
            val regular = ShellRewardPolicy.milestoneFindsForMinutes(minutes)
            assertTrue(regular.all { CreatureCatalog.require(it).realm == CreatureRealm.SEA })
            assertEquals(regular, ShellRewardPolicy.milestoneFindsForMinutes(minutes, FlowMode.POWER))
            assertTrue(ShellRewardPolicy.milestoneFindsForMinutes(minutes, FlowMode.SOFT).isEmpty())
        }
    }
}
