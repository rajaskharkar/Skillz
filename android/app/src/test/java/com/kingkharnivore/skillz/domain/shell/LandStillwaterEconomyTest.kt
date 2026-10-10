package com.kingkharnivore.skillz.domain.shell

import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class LandStillwaterEconomyTest {
    @Test fun legacyAcquisitionsRetainEvidenceAndParticipateInRegularBlueCollections() {
        val ids = com.kingkharnivore.skillz.domain.achievement.AchievementEvidenceScope.stillwater(emptyList(), emptyList()).speciesIds
        assertEquals(83, ids.size)
        assertTrue(ids.containsAll(LandCreatureCatalog.restorative.map { it.creatureId }))
        val collections = com.kingkharnivore.skillz.domain.achievement.CollectionCatalog.byId
        assertNull(collections["collection_stillwater"])
        assertTrue(collections.getValue("collection_living_earth").species.map { it.creatureId }.containsAll(ids))
        assertEquals(83, CreatureCatalog.allStillwater.size)
        assertEquals(32, CreatureCatalog.stillwater.size)
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
