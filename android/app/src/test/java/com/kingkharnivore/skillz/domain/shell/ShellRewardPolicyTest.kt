package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.utils.shell.ShellRewardPolicy
import com.kingkharnivore.skillz.utils.shell.*
import com.kingkharnivore.skillz.model.FlowMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

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
    fun landEncountersAreAddedToSeaRewardsInBothActiveModes() {
        val zones = mapOf(45 to CreatureZone.GOLDEN_FIELDS, 75 to CreatureZone.ANCIENT_WOODS,
            105 to CreatureZone.OPEN_SANDS, 135 to CreatureZone.HIGH_PEAKS, 150 to CreatureZone.GREAT_WILD)
        for (mode in listOf(FlowMode.FLOW, FlowMode.POWER)) {
            zones.forEach { (minutes, zone) ->
                val sea = CreatureEconomy.creaturesForRegularFlowMinutes(minutes)
                    .flatMap { reward -> List(reward.quantity) { reward.creatureId } }
                val candidates = LandCreatureCatalog.main.filter { it.zone == zone && it.isAvailable }.map { it.creatureId }.toSet()
                val drawn = (0 until 100).map { seed ->
                    val rewards = ShellRewardPolicy.milestoneFindsForMinutes(minutes, mode, Random(seed))
                    assertEquals(sea, rewards.dropLast(1))
                    rewards.last().also { assertTrue(it in candidates) }
                }
                assertTrue("The encounter must vary across draws", drawn.toSet().size > 1)
            }
        }
    }

    @Test
    fun onlyPowerReceivesOneRandomDinosaurFromTheMatchingPeriod() {
        val eras = mapOf(30 to RedEra.TRIASSIC, 60 to RedEra.JURASSIC, 120 to RedEra.CRETACEOUS)
        eras.forEach { (minutes, era) ->
            val regular = ShellRewardPolicy.milestoneFindsForMinutes(minutes)
            val candidates = RedCreatureCatalog.entries.filter { it.era == era && it.active }.map { it.id }.toSet()
            val drawn = (0 until 100).map { seed ->
                val rewards = ShellRewardPolicy.milestoneFindsForMinutes(minutes, FlowMode.POWER, Random(seed))
                assertEquals(regular, rewards.dropLast(1))
                rewards.last().also { assertTrue(it in candidates) }
            }
            assertTrue(drawn.toSet().size > 1)
        }
        assertEquals(listOf(ShellContentCatalog.FOCUS_MINNOW),
            ShellRewardPolicy.milestoneFindsForMinutes(10, FlowMode.POWER))
    }

    @Test
    fun softFlowsNeverGrantCreaturesAndOtherDurationsKeepExistingSeaRewards() {
        for (minutes in 0..180) {
            assertTrue(ShellRewardPolicy.milestoneFindsForMinutes(minutes, FlowMode.SOFT).isEmpty())
        }
        for (minutes in listOf(-1, 0, 9, 29, 31, 44, 46, 59, 61, 74, 76, 104, 106, 119, 121, 134, 136, 149, 151, 180)) {
            val sea = CreatureEconomy.creaturesForRegularFlowMinutes(minutes)
                .flatMap { reward -> List(reward.quantity) { reward.creatureId } }
            assertEquals(sea, ShellRewardPolicy.milestoneFindsForMinutes(minutes))
            assertEquals(sea, ShellRewardPolicy.milestoneFindsForMinutes(minutes, FlowMode.POWER))
        }
    }
}
