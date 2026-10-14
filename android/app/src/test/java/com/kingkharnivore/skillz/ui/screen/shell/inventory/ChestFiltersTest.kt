package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.utils.shell.*
import kotlin.test.*

class ChestFiltersTest {
    @Test fun legacySelectionsSurviveEitherFirstWrite() {
        ChestFilterOption.entries.forEach { option ->
            val migrated = ChestFilters.fromKeys(null, option.key)
            if (option.isProgress) {
                assertEquals(option, migrated.withEnvironment(ChestFilterOption.Jurassic).progress)
            } else assertEquals(option, migrated.withProgress(ChestFilterOption.NotMastered).environment)
            assertEquals(migrated, ChestFilters.fromKeys(migrated.environment.key, migrated.progress.key))
        }
        assertEquals(ChestFilterOption.NeededForTrackedBadges, ChestFilters.fromKeys(null,"tracked_mastery").progress)
        assertEquals(ChestFilters(), ChestFilters.fromKeys("unknown", "unknown"))
    }
    @Test fun everyCreatureHasOneExactEnvironmentAndOneRealm() {
        val realms = listOf(ChestFilterOption.Sea, ChestFilterOption.Land, ChestFilterOption.Red)
        val exact = ChestFilterOption.entries.filter { it != ChestFilterOption.All && !it.isProgress && it !in realms }
        CreatureCatalog.all.forEach { creature ->
            assertEquals(1, realms.count { it.matchesEnvironment(creature) }, creature.creatureId)
            assertEquals(1, exact.count { it.matchesEnvironment(creature) }, creature.creatureId)
            assertTrue(environmentOf(creature) in exact)
        }
    }
    @Test fun restorativeHabitatsDoNotLeakIntoParentRegions() {
        CreatureCatalog.all.filter { it.sourceType in setOf(CreatureSourceType.STILLWATER, CreatureSourceType.RESTORATIVE_LAND) }.forEach {
            assertFalse(ChestFilterOption.fromKey(it.zone.name.lowercase()).matchesEnvironment(it), it.creatureId)
        }
    }
    @Test fun environmentAndProgressIntersectWithoutChangingSortOrder() {
        val ids = listOf("jurassic_stegosaurus", "triassic_saturnalia", "creature_chicken", "focus_minnow")
        val stacks = ids.flatMap { id -> listOf(1,90,98,99).map { level -> ChestInventoryStackUiModel(id,id,level,1,id) } }
        assertEquals(listOf(90,98), filterChestInventoryStacks(stacks, ChestFilters(ChestFilterOption.Jurassic,ChestFilterOption.ClosestToMastery)).map { it.level })
        assertEquals(listOf("creature_chicken"), filterChestInventoryStacks(stacks,ChestFilters(ChestFilterOption.Land,ChestFilterOption.Mastered)).map { it.creatureId })
        assertEquals(3, filterChestInventoryStacks(stacks,ChestFilters(ChestFilterOption.Red,ChestFilterOption.NeededForTrackedBadges),setOf("jurassic_stegosaurus")).size)
    }
    @Test fun trackedLandAndBookMasteryGoalsUseSharedSpeciesRosters() {
        val dashboard=com.kingkharnivore.skillz.domain.achievement.BadgeDashboardCalculator.calculate(emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList(),emptyList())
        val land=dashboard.badges.map { it.copy(tracked=it.badgeId=="land_mastery_first") }
        assertEquals(LandCreatureCatalog.all.filter { it.isAvailable }.map { it.creatureId }.toSet(),speciesNeededForTrackedBadges(land))
        val book=dashboard.badges.map { it.copy(tracked=it.badgeId=="book_growing_complete") }
        assertEquals(CreatureCatalog.all.filter { it.isAvailable }.map { it.creatureId }.toSet(),speciesNeededForTrackedBadges(book))
    }

}
