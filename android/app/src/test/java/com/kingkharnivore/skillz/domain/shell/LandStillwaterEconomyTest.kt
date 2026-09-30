package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class LandStillwaterEconomyTest {
    @Test fun landAcquisitionsParticipateInExistingStillwaterCollectionsAndEvidence() {
        val ids = com.kingkharnivore.skillz.domain.achievement.AchievementEvidenceScope.stillwater(emptyList(), emptyList()).speciesIds
        assertEquals(83, ids.size)
        assertTrue(ids.containsAll(LandCreatureCatalog.restorative.map { it.creatureId }))
        val collection = com.kingkharnivore.skillz.domain.achievement.CollectionCatalog.byId.getValue("collection_stillwater")
        assertNotNull(collection)
        assertEquals(83, CreatureCatalog.allStillwater.size)
        assertEquals(32, CreatureCatalog.stillwater.size)
    }

    @Test fun habitatCostsRequirementsAndConfirmationMatchSea() {
        assertEquals(listOf(15_000L, 25_000L, 45_000L, 60_000L, 75_000L), LandStillwaterHabitat.entries.map { it.dropCost })
        assertEquals(listOf(false, false, true, true, true), LandStillwaterHabitat.entries.map(::requiresStillwaterConfirmation))
        LandStillwaterHabitat.entries.forEach { habitat ->
            assertThrows(IllegalArgumentException::class.java) { validateStillwaterDraw(habitat, emptySet(), habitat.dropCost) }
            assertThrows(IllegalArgumentException::class.java) { validateStillwaterDraw(habitat, setOf(habitat.zone), habitat.dropCost - 1) }
            validateStillwaterDraw(habitat, setOf(habitat.zone), habitat.dropCost)
            LandStillwaterCatalog.creaturesFor(habitat).forEach {
                assertTrue(it.isAvailable)
                assertEquals(CreatureRequirement.Drops(habitat.dropCost), it.requirement)
                assertNull(it.pearlPrice)
                assertThrows(IllegalArgumentException::class.java) { CreatureEconomy.quoteBeyondBluePayment(it.creatureId, 0, Int.MAX_VALUE) }
            }
        }
    }

    @Test fun everySpeciesHasOneTierAndEveryHabitatHasAllFourPools() {
        assertEquals(LandCreatureCatalog.restorative.map { it.creatureId }.toSet(), LandStillwaterCatalog.rarityById.keys)
        LandStillwaterHabitat.entries.forEach { habitat ->
            val rarities = LandStillwaterCatalog.creaturesFor(habitat).map { LandStillwaterCatalog.rarityById[it.creatureId] }
            assertEquals(StillwaterRarity.entries.toSet(), rarities.toSet())
            assertEquals(1, rarities.count { it == StillwaterRarity.RARE })
            assertEquals(1, rarities.count { it == StillwaterRarity.MYTHIC })
        }
    }

    @Test fun everyPercentagePointUsesExistingRarityOddsAndStaysInItsHabitat() {
        LandStillwaterHabitat.entries.forEach { habitat ->
            val counts = mutableMapOf<StillwaterRarity, Int>()
            (0..99).forEach { percentile ->
                val random = object : Random() {
                    override fun nextBits(bitCount: Int) = 0
                    override fun nextInt(until: Int) = if (until == 100) percentile else 0
                }
                val creature = LandStillwaterCatalog.roll(habitat, random)
                assertEquals(habitat, creature.restorativeHabitat)
                val rarity = LandStillwaterCatalog.rarityById.getValue(creature.creatureId)
                counts[rarity] = (counts[rarity] ?: 0) + 1
            }
            assertEquals(mapOf(StillwaterRarity.COMMON to 60, StillwaterRarity.UNCOMMON to 30, StillwaterRarity.RARE to 8, StillwaterRarity.MYTHIC to 2), counts)
        }
    }

    @Test fun restorativeReleaseGrowthAndContributionMatchCorrespondingSeaVessel() {
        val pairs = listOf("creature_peacock" to "stillwater_clam", "creature_panda" to "stillwater_lionfish",
            "creature_addax" to "stillwater_barracuda", "creature_elephant" to "stillwater_coelacanth")
        pairs.forEach { (land, sea) ->
            for (level in listOf(1, 25, 98, 99)) {
                assertEquals(CreatureEconomy.releaseValuePearls(sea, level), CreatureEconomy.releaseValuePearls(land, level))
                assertEquals(CreatureEconomy.growthCostPearls(sea, level), CreatureEconomy.growthCostPearls(land, level))
                assertEquals(CreatureEconomy.beyondBlueTradeContributionMinutes(sea, level), CreatureEconomy.beyondBlueTradeContributionMinutes(land, level))
            }
        }
        assertEquals(500, CreatureEconomy.releaseValuePearls("creature_pika"))
    }

    @Test fun arcFlagshipsHavePremiumReleaseValuesWithoutBecomingPurchasable() {
        val sea = listOf("focus_minnow", "focus_seahorse", "focus_manta", "focus_whale")
        val land = listOf("creature_chicken", "creature_deer", "creature_camel", "creature_moose", "creature_tiger")
        assertEquals(listOf(20, 60, 120, 240), sea.map { CreatureEconomy.releaseValuePearls(it) })
        assertEquals(listOf(30, 90, 180, 360, 720), land.map { CreatureEconomy.releaseValuePearls(it) })
        sea.zip(land).forEach { (s, l) -> assertEquals(CreatureEconomy.releaseValuePearls(s) * 3 / 2, CreatureEconomy.releaseValuePearls(l)) }
        land.forEach { id ->
            val creature = CreatureCatalog.require(id)
            assertNull(creature.requirementMinutes)
            assertNull(creature.flowTimeValueMinutes)
            assertNull(creature.pearlPrice)
            assertThrows(IllegalArgumentException::class.java) { CreatureEconomy.quoteBeyondBluePayment(id, 0, Int.MAX_VALUE) }
            assertTrue(CreatureEconomy.releaseValuePearls(id, 99) > CreatureEconomy.releaseValuePearls(id))
        }
    }
}
