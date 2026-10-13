package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.green.PlantSpecimenEntity
import com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator
import com.kingkharnivore.skillz.domain.green.GreenCatalogue
import com.kingkharnivore.skillz.utils.shell.*
import kotlin.test.*

class ChestPlantInventoryTest {
    private fun plant(id: String, species: String = "daisy", level: Int = 1, time: Long = 1) =
        PlantSpecimenEntity(id, species, level, time, positionKey = id, createdOrder = time, investedDrops = 1800)

    @Test fun eachPlantHasOneEnvironmentAndTheGreenRealmWithNoCreatureLeakage() {
        val realms = listOf(ChestFilterOption.Sea, ChestFilterOption.Land, ChestFilterOption.Red, ChestFilterOption.Green)
        val environments = ChestFilterOption.entries.filter { it != ChestFilterOption.All && !it.isProgress && it !in realms }
        GreenCatalogue.species.forEach { species ->
            assertTrue(ChestFilterOption.All.matchesEnvironment(species))
            assertEquals(listOf(ChestFilterOption.Green), realms.filter { it.matchesEnvironment(species) })
            assertEquals(listOf(ChestFilterOption.fromKey(species.environment.id)), environments.filter { it.matchesEnvironment(species) })
        }
        CreatureCatalog.all.forEach { assertFalse(ChestFilterOption.Green.matchesEnvironment(it)) }
    }

    @Test fun plantsShareProgressFiltersAndKeepIndependentCopies() {
        val plants = listOf(plant("young"), plant("near", level = 98), plant("mastered", level = 99), plant("fern", "fern", 90))
        assertEquals(plants, filterChestPlants(plants, ChestFilters()))
        assertEquals(listOf("near"), filterChestPlants(plants,
            ChestFilters(ChestFilterOption.Garden, ChestFilterOption.ClosestToMastery)).map { it.id })
        assertEquals(listOf("mastered"), filterChestPlants(plants,
            ChestFilters(ChestFilterOption.Green, ChestFilterOption.Mastered)).map { it.id })
        assertEquals(listOf("fern"), filterChestPlants(plants,
            ChestFilters(ChestFilterOption.Woodlands, ChestFilterOption.NotMastered)).map { it.id })
        assertTrue(filterChestPlants(plants, ChestFilters(ChestFilterOption.Red)).isEmpty())
    }

    @Test fun globalSortInterleavesPlantsAndCreaturesWithoutCombiningSpecimens() {
        val plants = listOf(plant("first", level = 50, time = 2), plant("second", level = 50, time = 3), plant("last", "fern", 90, 4))
        val creature = ChestInventoryStackUiModel("creature_chicken", "Chicken", 75, 2, "", newestAcquiredAtMs = 1)
        val entries = buildChestEntries(listOf(creature), plants, plants, mapOf("daisy" to "Daisy", "fern" to "Fern"), ChestSortOption.Level)
        assertEquals(listOf(90, 75, 50, 50), entries.map { it.sortValues.level })
        assertEquals(4, entries.map { it.key }.distinct().size)
        assertEquals(setOf("first", "second", "last"), entries.mapNotNull { it.plant?.id }.toSet())
        assertEquals(listOf(1L, 2L, 3L, 4L), buildChestEntries(listOf(creature.copy(oldestAcquiredAtMs = 1)), plants,
            plants, emptyMap(), ChestSortOption.OldestArrival).map { it.sortValues.oldestAcquiredAtMs })
    }

    @Test fun trackedPlantGoalsOnlyIncludeCopiesThatCanAdvanceThem() {
        val plants = listOf(plant("daisy"), plant("fern", "fern"))
        val badges = BadgeDashboardCalculator.calculate(emptyList(), emptyList(), emptyList(), emptyList(),
            emptyList(), emptyList(), emptyList(), greenPlants = plants).badges.map {
            it.copy(tracked = it.badgeId == "green_v1_garden_flourish")
        }
        val filters = ChestFilters(ChestFilterOption.Green, ChestFilterOption.NeededForTrackedBadges)
        assertEquals(listOf("daisy"), filterChestPlants(plants, filters, badges).map { it.id })
        assertTrue(filterChestPlants(plants + plant("grown", level = 90), filters, badges).isEmpty())
    }
}
