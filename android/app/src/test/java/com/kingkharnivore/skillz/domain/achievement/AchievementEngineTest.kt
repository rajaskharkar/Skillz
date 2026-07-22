package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeCountFloorEntity
import org.junit.Assert.*
import org.junit.Test

class AchievementEngineTest {
    @Test fun unknownBadgesAreHistoricalAndHaveNoFabricatedMilestones() {
        val definition = BadgeDefinitionResolver.resolve("legacy_unknown_badge")

        assertEquals(BadgeGoalType.HISTORICAL_COUNT_ONLY, definition.goalType)
        assertTrue(definition.milestones.isEmpty())
        assertFalse(definition.trackable)
    }
    @Test fun persistedBadgeDefinitionsUseTheSharedSafeResolver() {
        assertEquals(BadgeFamily.COLLECTION, BadgeDefinitionResolver.resolve("badge_discovery").family)
        assertFalse(BadgeDefinitionResolver.resolve("unknown_persisted_badge").trackable)
        assertSame(AchievementBadgeCatalog.byId.getValue("mastery_first"), BadgeDefinitionResolver.resolve("mastery_first"))
    }
    @Test fun legacyMasteryEvidenceCountsForLifetimeAndCollectionsWithoutFakeEvents() {
        val species = CreatureCatalog.all.first()
        val floor = BadgeCountFloorEntity("mastery_species_${species.creatureId}", species.creatureId, 2, 0, "legacy", 1L)
        val evidence = MasteryEvidenceCalculator.bySpecies(emptyList(), listOf(floor))
        assertEquals(2, evidence.getValue(species.creatureId).effectiveLifetimeCount)
        assertTrue(evidence.getValue(species.creatureId).hasEverBeenMastered)
        assertEquals(3, MasteryEvidenceCalculator.effectiveCount(1, floor))
        val progress = CollectionProgressCalculator.calculate(
            CollectionDefinition("test", 1, listOf(species)), setOf(species.creatureId), emptyMap(), evidence
        )
        assertTrue(progress.completionistEarned)
        assertEquals(2, progress.speciesStates.single().lifetimeMasteryCount)
    }
    @Test fun milestonesExposeExactCurrentNextAndNewlyReached() {
        val result = MilestoneEngine.evaluate(count = 11, previousCount = 9)
        assertEquals(11, result.exactCount)
        assertEquals(10, result.currentThreshold)
        assertEquals(25, result.nextThreshold)
        assertEquals(1, result.progressToNext)
        assertEquals(10, result.newlyReachedThreshold)
        assertNull(MilestoneEngine.evaluate(1_200).nextThreshold)
    }

    @Test fun regionCalculatesLifetimeCurrentAndMasteryIndependently() {
        val region = CollectionCatalog.byId.getValue("blue_sunlit_reef")
        val ids = region.species.map { it.creatureId }.toSet()
        val absent = ids.first()
        val progress = CollectionProgressCalculator.calculate(
            region, ids, ids.minus(absent).associateWith { listOf(1) }, ids.minus(absent)
        )
        assertTrue(progress.collectorEarned)
        assertFalse(progress.curatorEarned)
        assertFalse(progress.completionistEarned)
        assertEquals(setOf(absent), progress.missingCurrentlyOwnedSpeciesIds)
        assertEquals(setOf(absent), progress.missingMasteredSpeciesIds)
    }

    @Test fun historicalCompletionSurvivesRosterExpansion() {
        val original = CollectionCatalog.byId.getValue("collection_stillwater")
        val expanded = original.copy(species = original.species + CreatureCatalog.all.first())
        val oldIds = original.species.map { it.creatureId }.toSet()
        val progress = CollectionProgressCalculator.calculate(expanded, oldIds, emptyMap(), oldIds,
            setOf(BadgeRequirement.COLLECTOR, BadgeRequirement.COMPLETIONIST))
        assertTrue(progress.collectorEarned)
        assertTrue(progress.completionistEarned)
        assertFalse(progress.currentRosterCollectorComplete)
        assertFalse(progress.currentRosterCompletionistComplete)
    }

    @Test fun authoritativeCatalogHasStableUniqueIdsAndBadgePerSpecies() {
        assertEquals(CreatureCatalog.all.size, CreatureCatalog.all.map { it.creatureId }.distinct().size)
        assertTrue(CreatureCatalog.all.all { it.collectionId.isNotBlank() && it.titleRes != 0 })
        assertTrue(CreatureCatalog.all.all { AchievementBadgeCatalog.byId.containsKey("mastery_species_${it.creatureId}") })
    }

    @Test fun nonparticipatingSpeciesIsExcluded() {
        val species = CreatureCatalog.all.first().copy(participatesInCollector = false, participatesInCompletionist = false)
        val progress = CollectionProgressCalculator.calculate(CollectionDefinition("test", 1, listOf(species)), emptySet(), emptyMap(), emptySet())
        assertEquals(0, progress.totalParticipatingSpecies)
        assertFalse(progress.currentRosterCollectorComplete)
        assertFalse(progress.currentRosterCompletionistComplete)
    }
}
