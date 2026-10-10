package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.utils.shell.*
import org.junit.Assert.*
import org.junit.Test

class GreenDomainTest {
    @Test fun catalogueHasEveryExplicitTierAndSevenStableAssets() {
        assertEquals(6, GreenEnvironment.entries.size)
        assertEquals(60, GreenCatalogue.species.size)
        assertEquals(60, GreenCatalogue.byId.size)
        GreenEnvironment.entries.forEach { environment ->
            val species = GreenCatalogue.inEnvironment(environment)
            assertEquals(10, species.size)
            assertEquals((1..10).toSet(), species.map { it.tier }.toSet())
            species.forEach {
                assertEquals(7, it.stageAssetKeys.distinct().size)
                assertEquals("plant_${it.id}_stage_01", it.assetKey(1))
                assertEquals("plant_${it.id}_stage_07", it.assetKey(90))
                assertEquals(it.assetKey(90), it.assetKey(99))
            }
        }
        assertEquals(setOf("magnolia","giant_sequoia","kapok","victoria_lily","baobab","bristlecone_pine"), GreenCatalogue.species.filter { it.landmark }.map { it.id }.toSet())
    }
    @Test fun economyMatchesEveryPublishedTotal() {
        val seeds = listOf(1800L,2700L,3600L,5400L,7200L,10800L,14400L,21600L,32400L,43200L)
        val bases = listOf(300L,450L,600L,900L,1200L,1800L,2400L,3600L,5400L,7200L)
        val grown = listOf(196800L,294800L,393100L,589950L,786050L,1179100L,1572350L,2358400L,3537800L,4717150L)
        val mastered = listOf(242450L,363250L,484400L,726900L,968650L,1453050L,1937500L,2906200L,4359550L,5812750L)
        (1..10).forEach { tier ->
            assertEquals(seeds[tier-1], GreenEconomy.seedCost(tier))
            assertEquals(bases[tier-1], GreenEconomy.waterCost(tier,1))
            assertEquals(grown[tier-1], GreenEconomy.totalCost(tier,90))
            assertEquals(mastered[tier-1], GreenEconomy.totalCost(tier,99))
            (1..98).forEach { level ->
                val cost = GreenEconomy.waterCost(tier,level)
                assertEquals(0L,cost%50)
                assertEquals(cost, GreenEconomy.waterCost(tier,level))
                if(level<98) assertTrue(cost <= GreenEconomy.waterCost(tier,level+1))
            }
        }
        assertEquals(listOf(7200L,30950L,69150L,115100L,128400L), listOf(1,30,60,90,98).map { GreenEconomy.waterCost(10,it) })
    }
    @Test fun everyStageBoundaryAndMasteryAssetAreExact() {
        val bounds = listOf(1 to PlantGrowthStage.SPROUT,14 to PlantGrowthStage.SPROUT,15 to PlantGrowthStage.SEEDLING,29 to PlantGrowthStage.SEEDLING,
            30 to PlantGrowthStage.YOUNG,44 to PlantGrowthStage.YOUNG,45 to PlantGrowthStage.ROOTED,59 to PlantGrowthStage.ROOTED,
            60 to PlantGrowthStage.THRIVING,74 to PlantGrowthStage.THRIVING,75 to PlantGrowthStage.MATURE,89 to PlantGrowthStage.MATURE,
            90 to PlantGrowthStage.FULLY_GROWN,98 to PlantGrowthStage.FULLY_GROWN,99 to PlantGrowthStage.MASTERED)
        bounds.forEach { (level,stage) -> assertEquals(stage,PlantGrowthStageResolver.resolve(level)) }
        assertEquals(6,PlantGrowthStageResolver.resolve(99).assetIndex)
        assertEquals(90,PlantGrowthStageResolver.nextVisibleGrowth(89)); assertNull(PlantGrowthStageResolver.nextVisibleGrowth(90))
        listOf(0,100,-1).forEach { assertThrows(IllegalArgumentException::class.java) { PlantGrowthStageResolver.resolve(it) } }
        listOf(0,99,100).forEach { assertThrows(IllegalArgumentException::class.java) { GreenEconomy.waterCost(1,it) } }
        listOf(0,11).forEach { assertThrows(IllegalArgumentException::class.java) { GreenEconomy.seedCost(it) } }
    }
    @Test fun badgesRequireUniqueSpeciesAndAllThreeCompletionThresholds() {
        val garden=GreenCatalogue.inEnvironment(GreenEnvironment.GARDEN)
        assertFalse("green_v1_garden_catalogue" in GreenBadgeEvaluator.eligible(List(10){PlantProgress("daisy",90)}))
        val planted=garden.map { PlantProgress(it.id,1) }
        assertEquals(setOf("green_v1_garden_catalogue"),GreenBadgeEvaluator.eligible(planted).filter { it.startsWith("green_v1_") }.toSet())
        assertEquals(setOf("green_v1_garden_catalogue","green_v1_garden_flourish"), GreenBadgeEvaluator.eligible(planted.map { it.copy(level=90) }).filter { it.startsWith("green_v1_") }.toSet())
        val mastered=GreenCatalogue.species.map { PlantProgress(it.id,99) }
        assertEquals(81,GreenBadgeEvaluator.eligible(mastered).count { it.startsWith("green_v1_") })
        val almost=mastered.map { if(it.speciesId=="daisy") it.copy(level=98) else it }
        assertFalse("green_v1_all_mastery" in GreenBadgeEvaluator.eligible(almost))
        assertTrue("green_v1_all_flourish" in GreenBadgeEvaluator.eligible(almost))
        val expanded=GreenBadgeEvaluator.definitions.map { it.copy(requiredSpecies=it.requiredSpecies+"future_species") }
        val old=GreenBadgeEvaluator.eligible(mastered)
        assertEquals(old, GreenBadgeEvaluator.earned(emptyList(),old,expanded))
    }
    @Test fun allHeritageCreaturesAreDeterministicallyAvailableInBlueWithoutDuplicateIds() {
        assertEquals(83,CreatureCatalog.allStillwater.size)
        assertEquals(CreatureCatalog.all.size,CreatureCatalog.byId.size)
        CreatureCatalog.allStillwater.forEach { creature ->
            assertEquals(CreatureSourceType.BEYOND_BLUE,creature.sourceType)
            assertTrue(creature in CreatureCatalog.beyondBlue)
            assertEquals("blue_${creature.zone.name.lowercase()}",creature.collectionId)
            assertTrue(CreatureEconomy.quoteBeyondBluePayment(creature.creatureId,0,Int.MAX_VALUE).canEncounter)
        }
    }
    @Test fun softFlowDropEarningAndOrdinaryRewardsStayUnchanged() {
        listOf(-1L,0L,1L,300L,900L,1800L,3600L).forEach { assertEquals(it.coerceAtLeast(0),calculateDropsForSoftFlow(it)) }
        assertTrue(CreatureEconomy.creaturesForRegularFlowMinutes(120,true).isEmpty())
        assertEquals(listOf(CreatureReward("focus_whale",1)),CreatureEconomy.creaturesForRegularFlowMinutes(120))
    }
}
