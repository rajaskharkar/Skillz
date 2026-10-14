package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.domain.achievement.*
import org.junit.Assert.*
import org.junit.Test

class GreenBadgesTest {
    private fun badge(key: String) = GreenBadgeEvaluator.byId.getValue("green_v2_$key")
    private fun plants(id: String, level: Int, copies: Int) = List(copies) { PlantProgress(id,level) }
    private fun progress(key: String, plants: List<PlantProgress>) = GreenBadgeEvaluator.progress(badge(key),plants)
    @Test fun catalogueContainsStableIdsAndValidBotanicalRosters() {
        assertEquals(182,GreenBadgeEvaluator.definitions.size)
        assertEquals(182,GreenBadgeEvaluator.byId.size)
        assertEquals(81,GreenBadgeEvaluator.definitions.count { it.id.startsWith("green_v1_") })
        GreenBadgeEvaluator.definitions.forEach { b ->
            assertTrue(b.target > 0); assertTrue(b.requiredSpecies.isNotEmpty())
            assertTrue(GreenCatalogue.byId.keys.containsAll(b.requiredSpecies))
            assertEquals(b.achievementDefinition(),AchievementBadgeCatalog.definitions.first { it.badgeId == b.id })
        }
    }
    @Test fun growthMilestonesRequireExactBoundaryWithoutRequiringExpensiveSpecies() {
        listOf("young_promise" to 30,"taking_hold" to 45,"coming_alive" to 60,"full_form" to 90,"first_masterpiece" to 99).forEach { (id,level) ->
            assertEquals(0,progress(id,plants("daisy",level-1,1)))
            assertEquals(1,progress(id,plants("daisy",level,1)))
        }
        assertEquals(25,progress("living_landscape",plants("daisy",90,25)))
        assertEquals(0,progress("master_gardener",plants("daisy",98,25)))
    }
    @Test fun earlyThemedCollectionsAndEnvironmentTriosUseDistinctSpecies() {
        GreenBadgeEvaluator.themes.forEach { theme ->
            listOf("seedling" to 15, "rooted" to 45).forEach { (suffix, level) ->
                val b=badge("${theme.key}_$suffix")
                assertFalse(b.id in GreenBadgeEvaluator.eligible(theme.species.map { PlantProgress(it,level-1) }))
                assertTrue(b.id in GreenBadgeEvaluator.eligible(theme.species.map { PlantProgress(it,level) }))
            }
        }
        GreenEnvironment.entries.forEach { env ->
            val b=GreenBadgeEvaluator.byId.getValue("green_v3_${env.id}_15")
            val ids=GreenCatalogue.inEnvironment(env).take(3).map { it.id }
            assertEquals(1,GreenBadgeEvaluator.progress(b,plants(ids[0],15,3)))
            assertEquals(3,GreenBadgeEvaluator.progress(b,ids.map { PlantProgress(it,15) }))
        }
        assertEquals(0,progress("gentle_start",plants("daisy",4,1)))
        assertEquals(1,progress("gentle_start",plants("daisy",5,1)))
        assertEquals(0,progress("first_seedling",plants("daisy",14,1)))
        assertEquals(1,progress("first_seedling",plants("daisy",15,1)))
    }
    @Test fun careCountsWateringsNotCostOrSeedsAndCanBeReconstructedWithoutActionHistory() {
        assertEquals(0,progress("first_water",plants("daisy",1,10)))
        assertEquals(100,progress("well_tended",plants("daisy",51,2)))
        assertEquals(100,progress("well_tended",plants("magnolia",51,2)))
        assertEquals(98,progress("lifetime_care",plants("daisy",99,1)))
    }
    @Test fun environmentAndArchetypeBadgesRequireBreadthNotDuplicates() {
        assertEquals(1,progress("six_soils",plants("daisy",1,60)))
        assertEquals(0,progress("six_sanctuaries",plants("daisy",99,60)))
        val broad = GreenEnvironment.entries.flatMap { env -> GreenCatalogue.inEnvironment(env).take(3).map { PlantProgress(it.id,90) } }
        assertEquals(6,progress("six_sanctuaries",broad))
        assertEquals(5,progress("six_sanctuaries",broad.dropLast(1)))
        assertEquals(8,progress("many_ways_to_grow",GreenCatalogue.species.map { PlantProgress(it.id,90) }))
        assertEquals(1,progress("many_ways_to_grow",plants("daisy",90,60)))
    }
    @Test fun specimenFamiliesNeedSeparateCopiesAtTheRequiredLevel() {
        assertEquals(1,progress("second_spring",plants("daisy",90,1)+plants("daisy",89,1)))
        assertEquals(2,progress("second_spring",plants("daisy",90,2)))
        assertEquals(3,progress("family_portrait",plants("daisy",99,3)))
        val families = GreenCatalogue.species.take(5).flatMap { plants(it.id,90,2) }
        assertEquals(5,progress("many_generations",families))
        assertEquals(4,progress("many_generations",families.dropLast(1)))
    }
    @Test fun thematicCollectionsHaveDistinctFlourishAndMasteryAndKeepEarnedAwards() {
        GreenBadgeEvaluator.themes.forEach { theme ->
            val roster=theme.species.map { PlantProgress(it,90) }
            assertEquals(theme.species.size,progress("${theme.key}_flourish",roster))
            assertEquals(0,progress("${theme.key}_mastery",roster))
            val old=setOf("green_v2_${theme.key}_flourish")
            val expanded=listOf(badge("${theme.key}_flourish").copy(requiredSpecies=theme.species+"future",target=theme.species.size+1))
            assertEquals(old,GreenBadgeEvaluator.earned(emptyList(),old,expanded))
        }
    }
    @Test fun sharedDashboardPreservesPinsTrackingCountsAndOriginalAwardDates() {
        val b=GreenBadgeEvaluator.byId.getValue("green_v1_species_daisy_mastery")
        val award=GreenBadgeAwardEntity(b.id,100,1)
        val stored=UserBadgeEntity(b.id,3,100,200,false,viewedAt=250)
        val model=b.dashboardModel(emptyList(),award,stored,2,true)
        assertEquals(BadgeUiCategory.GREEN,model.category);assertEquals(3,model.count)
        assertEquals(100L,model.firstEarnedAt);assertEquals(200L,model.lastAdvancedAt)
        assertEquals(2,model.pinnedOrder);assertTrue(model.tracked);assertTrue(model.canTrack)
        assertEquals(5,model.target); assertEquals(2,model.remaining)
    }
    @Test fun actionsChooseMissingEnvironmentOrAnotherCopyRatherThanAlreadyFinishedPlant() {
        fun plant(id: String,level: Int) = PlantSpecimenEntity(id,id,level,1,positionKey=id,createdOrder=1,investedDrops=0)
        val owned=listOf(plant("daisy",99))
        val repeat=badge("second_spring").dashboardModel(owned,null,null,null,false).action as BadgeActionDestination.Green
        assertEquals("daisy",repeat.speciesId);assertTrue(repeat.plantSeed)
        val breadth=badge("six_soils").dashboardModel(owned,null,null,null,false).action as BadgeActionDestination.Green
        assertNotEquals(GreenEnvironment.GARDEN,GreenCatalogue.byId[breadth.speciesId]?.environment)
        val care=badge("well_tended").dashboardModel(listOf(plant("rose",45)),null,null,null,false).action as BadgeActionDestination.Green
        assertEquals("rose",care.speciesId);assertFalse(care.plantSeed)
    }
    @Test fun pairBadgeTargetsTheSpecificCopyStillBelowItsMilestone() {
        val ready = PlantSpecimenEntity("ready", "daisy", 45, 1, positionKey="ready", createdOrder=1, investedDrops=0)
        val growing = ready.copy(id="growing", level=29, positionKey="growing", createdOrder=2)
        val action = badge("side_by_side").dashboardModel(listOf(ready, growing), null, null, null, false).action as BadgeActionDestination.Green
        assertEquals("growing", action.specimenId)
        assertEquals("daisy", action.speciesId)
        assertFalse(action.plantSeed)
    }

}
