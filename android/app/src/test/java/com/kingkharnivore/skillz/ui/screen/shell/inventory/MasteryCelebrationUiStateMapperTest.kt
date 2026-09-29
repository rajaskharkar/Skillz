package com.kingkharnivore.skillz.ui.screen.shell.inventory

import com.kingkharnivore.skillz.data.model.entity.shell.MasteryCelebrationEventEntity
import com.kingkharnivore.skillz.domain.achievement.BadgeRequirement
import com.kingkharnivore.skillz.domain.achievement.CelebrationLifecycle
import com.kingkharnivore.skillz.domain.achievement.CelebrationStage
import com.kingkharnivore.skillz.domain.achievement.CollectionCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MasteryCelebrationUiStateMapperTest {
    @Test fun firstMasteryIncrementsSpeciesTotalUniqueAndCollections() {
        val state = MasteryCelebrationUiStateMapper.map(event())

        assertEquals(MasteryCountChange(0, 1), state.speciesMasteries)
        assertEquals(MasteryCountChange(0, 1), state.totalMasteries)
        assertEquals(MasteryCountChange(0, 1), state.uniqueSpecies)
        assertTrue(state.collections.all { it.progress == MasteryCountChange(0, 1) })
        assertEquals(
            listOf("mastery_first", "mastery_species_focus_minnow", "mastery_variety", "mastery_circle"),
            state.newlyEarnedBadgeIds
        )
        assertNull(state.speciesMilestone)
    }

    @Test fun repeatMasteryDoesNotIncreaseUniqueSpeciesOrCollectionSpecies() {
        val state = MasteryCelebrationUiStateMapper.map(
            event(
                speciesCount = 2,
                totalMasteries = 6,
                uniqueSpecies = 4,
                regionMastered = 3,
                newlyEarned = ""
            )
        )

        assertEquals(MasteryCountChange(1, 2), state.speciesMasteries)
        assertEquals(MasteryCountChange(5, 6), state.totalMasteries)
        assertEquals(MasteryCountChange(4, 4), state.uniqueSpecies)
        assertTrue(state.collections.all { !it.progress.changed })
        assertTrue(state.newlyEarnedBadgeIds.isEmpty())
    }

    @Test fun realSpeciesMilestoneIsShownButFirstCountIsNotDuplicatedAsMilestone() {
        val milestone = MasteryCelebrationUiStateMapper.map(
            event(
                speciesCount = 5,
                totalMasteries = 9,
                uniqueSpecies = 3,
                milestones = "mastery_species_focus_minnow:5,mastery_circle:5"
            )
        )
        val first = MasteryCelebrationUiStateMapper.map(
            event(milestones = "mastery_species_focus_minnow:1,mastery_circle:1")
        )

        assertEquals(5, milestone.speciesMilestone)
        assertNull(first.speciesMilestone)
    }

    @Test fun firstMasteryOfAnotherSpeciesAdvancesUniqueAndRelevantCollections() {
        val state = MasteryCelebrationUiStateMapper.map(
            event(
                speciesId = "focus_seahorse",
                regionId = "blue_deeper_reef",
                speciesCount = 1,
                totalMasteries = 2,
                uniqueSpecies = 2,
                regionMastered = 1,
                blueMastered = 2,
                allMastered = 2,
                newlyEarned = "mastery_species_focus_seahorse"
            )
        )

        assertEquals(MasteryCountChange(1, 2), state.uniqueSpecies)
        assertEquals(
            listOf("blue_deeper_reef", "collection_the_blue", "collection_all_waters"),
            state.collections.map { it.collectionId }
        )
        assertTrue(state.collections.all { it.progress.changed })
    }

    @Test fun landMasteryDoesNotLabelSeaSnapshotCountsAsLandProgress() {
        val state = MasteryCelebrationUiStateMapper.map(event(
            speciesId = "creature_tiger", regionId = "blue_great_wild",
            blueMastered = 8, allMastered = 12,
            newlyEarned = "mastery_species_creature_tiger"
        ).copy(sourceId = "ARC_EARNED"))
        assertEquals(listOf("blue_great_wild"), state.collections.map { it.collectionId })
        assertEquals(listOf("mastery_species_creature_tiger"), state.newlyEarnedBadgeIds)
    }

    @Test fun restorativeLandMasteryKeepsVesselAndSharedStillwaterProgress() {
        val state = MasteryCelebrationUiStateMapper.map(event(
            speciesId = "creature_peacock", regionId = "stillwater_pasture",
            newlyEarned = "mastery_species_creature_peacock"
        ).copy(sourceId = "RESTORATIVE_LAND", stillwaterMastered = 1))
        assertEquals(listOf("stillwater_pasture", "collection_stillwater"), state.collections.map { it.collectionId })
    }

    @Test fun noAchievementsProducesNoPlaceholderState() {
        val state = MasteryCelebrationUiStateMapper.map(event(speciesCount = 3, newlyEarned = ""))
        assertTrue(state.newlyEarnedBadgeIds.isEmpty())
        assertFalse(state.uniqueSpecies.changed)
    }

    @Test fun creaturePluralizationHandlesRepositoryStyleNames() {
        assertEquals("Minnows", pluralizeEnglishCreatureName("Minnow"))
        assertEquals("Seahorses", pluralizeEnglishCreatureName("Seahorse"))
        assertEquals("Clownfish", pluralizeEnglishCreatureName("Clownfish"))
        assertEquals("Octopuses", pluralizeEnglishCreatureName("Octopus"))
    }

    private fun event(
        speciesId: String = "focus_minnow",
        regionId: String = "blue_sunlit_reef",
        speciesCount: Int = 1,
        totalMasteries: Int = 1,
        uniqueSpecies: Int = 1,
        regionMastered: Int = 1,
        blueMastered: Int = regionMastered,
        allMastered: Int = regionMastered,
        newlyEarned: String = "mastery_circle,mastery_species_focus_minnow,mastery_variety,mastery_first",
        milestones: String = "mastery_species_focus_minnow:1"
    ) = MasteryCelebrationEventEntity(
        eventId = "event", transactionId = "transaction", creatureInstanceId = "creature",
        speciesId = speciesId, artworkKey = "creature_icon_$speciesId", regionId = regionId,
        sourceId = "FLOW_EARNED", previousLevel = 98, newLevel = 99,
        speciesMasteryCount = speciesCount, totalMasteries = totalMasteries,
        uniqueMasteredSpecies = uniqueSpecies, regionalDiscovered = regionMastered,
        regionalTotal = completionistTotal(regionId), regionalMastered = regionMastered,
        regionalCollectorEarned = false, regionalCompletionistEarned = false,
        blueMastered = blueMastered, blueTotal = completionistTotal("collection_the_blue"),
        stillwaterMastered = 0, stillwaterTotal = completionistTotal("collection_stillwater"),
        allWatersMastered = allMastered, allWatersTotal = completionistTotal("collection_all_waters"),
        newlyEarnedBadgeIds = newlyEarned, advancedBadgeIds = "",
        milestonesReached = milestones, originDestination = "BLUE", createdAt = 1L,
        lifecycleState = CelebrationLifecycle.PRESENTING.name,
        presentationStage = CelebrationStage.LEVEL_TRANSITION.name
    )

    private fun completionistTotal(collectionId: String): Int =
        CollectionCatalog.byId.getValue(collectionId).eligibleRoster(BadgeRequirement.COMPLETIONIST).size
}
