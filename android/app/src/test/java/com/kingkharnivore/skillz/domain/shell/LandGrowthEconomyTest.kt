package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class LandGrowthEconomyTest {
    // Premium growth anchors, independent of the Arc's Flow count and release/trade value.
    private val flagshipBases = linkedMapOf(
        "creature_chicken" to 720,
        "creature_deer" to 1_200,
        "creature_camel" to 1_500,
        "creature_moose" to 2_400,
        "creature_tiger" to 3_000
    )

    @Test fun everyArcFlagshipUsesItsZonesPremiumGrowthTier() {
        val flagships = CreatureCatalog.all.filter { it.sourceType == CreatureSourceType.ARC_EARNED }
        assertEquals(flagshipBases.keys, flagships.map { it.creatureId }.toSet())
        flagships.forEach { creature ->
            val premiumPeer = LandCreatureCatalog.main.filter {
                it.zone == creature.zone && it.sourceType == CreatureSourceType.BEYOND_BLUE
            }.maxBy { requireNotNull(it.pearlPrice) }
            assertEquals(creature.creatureId, flagshipBases.getValue(creature.creatureId), CreatureEconomy.baseGrowthCost(creature.creatureId))
            assertEquals(CreatureEconomy.baseGrowthCost(premiumPeer.creatureId), CreatureEconomy.baseGrowthCost(creature.creatureId))
        }
        assertEquals(listOf(819, 1_365, 1_707, 2_731, 3_414), flagshipBases.keys.map { CreatureEconomy.growthCostPearls(it, 1) })
    }

    @Test fun tigerMatchesLionAtEveryUpgradeableLevel() {
        for (level in 1 until CreatureEconomy.MAX_CREATURE_LEVEL) {
            assertEquals("Level $level", CreatureEconomy.growthCostPearls("creature_lion", level),
                CreatureEconomy.growthCostPearls("creature_tiger", level))
        }
        assertEquals(3_414, CreatureEconomy.growthCostPearls("creature_tiger", 1))
    }

    @Test fun flagshipGrowthRisesAndCumulativeInvestmentUsesTheSameCurve() {
        flagshipBases.keys.forEach { id ->
            val costs = (1 until CreatureEconomy.MAX_CREATURE_LEVEL).map { CreatureEconomy.growthCostPearls(id, it) }
            assertTrue(id, costs.zipWithNext().all { (before, after) -> after > before })
            assertEquals(0L, CreatureEconomy.cumulativeGrowthCostPearls(id, 1))
            assertEquals(costs.sumOf { it.toLong() }, CreatureEconomy.cumulativeGrowthCostPearls(id, 99))
            // Existing release semantics recover a fraction of the catalog's cumulative growth value.
            assertEquals(CreatureEconomy.canonicalPearlValue(id) + (costs.sumOf { it.toLong() } * 0.35).toInt(),
                CreatureEconomy.releaseValuePearls(id, 99))
        }
    }

    @Test fun growthDoesNotChangeArcAcquisitionOrBaseReleaseAndTradeValues() {
        val releases = listOf(30, 90, 180, 360, 720)
        flagshipBases.keys.forEachIndexed { index, id ->
            val creature = CreatureCatalog.require(id)
            assertEquals(CreatureRequirement.ArcDepth((index + 1) * 3), creature.requirement)
            assertNull(creature.requirementMinutes)
            assertNull(creature.flowTimeValueMinutes)
            assertNull(creature.pearlPrice)
            assertEquals(releases[index], CreatureEconomy.releaseValuePearls(id, 1))
            assertEquals(releases[index] / 2, CreatureEconomy.beyondBlueTradeContributionMinutes(id, 99))
            assertThrows(IllegalArgumentException::class.java) {
                CreatureEconomy.quoteBeyondBluePayment(id, 0, Int.MAX_VALUE)
            }
        }
    }

    @Test fun regularFlowDurationNeverAwardsArcFlagships() {
        for (minutes in 0..300) {
            assertTrue(CreatureEconomy.creaturesForRegularFlowMinutes(minutes).none { it.creatureId in flagshipBases })
        }
    }
}
