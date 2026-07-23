package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeCountFloorEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CollectionCompletionEntity
import org.junit.Assert.*
import org.junit.Test

class AchievementEngineTest {
    @Test fun speciesDiscoveryVisibilityDoesNotExposeBadgeBeforeEvidence() {
        val definition = AchievementBadgeDefinition(
            badgeId = "secret_species_badge",
            family = BadgeFamily.SPECIES_MASTERY,
            countType = BadgeCountType.REPEATABLE,
            requirement = BadgeRequirement.EXACT_COUNT,
            speciesId = "future_secret_species",
            visibility = BadgeVisibility.AFTER_SPECIES_DISCOVERY
        )
        val hidden = BadgeVisibilityContext(emptySet(), emptySet(), emptySet())
        val discovered = hidden.copy(discoveredSpeciesIds = setOf("future_secret_species"))

        assertFalse(BadgeVisibilityEvaluator.isVisible(definition, hidden))
        assertTrue(BadgeVisibilityEvaluator.isVisible(definition, discovered))
    }

    @Test fun afterEarnedVisibilityRequiresHistoricalEvidence() {
        val definition = AchievementBadgeDefinition(
            badgeId = "unavailable_species_badge",
            family = BadgeFamily.SPECIES_MASTERY,
            countType = BadgeCountType.REPEATABLE,
            requirement = BadgeRequirement.EXACT_COUNT,
            visibility = BadgeVisibility.AFTER_EARNED
        )
        val hidden = BadgeVisibilityContext(emptySet(), emptySet(), emptySet())

        assertFalse(BadgeVisibilityEvaluator.isVisible(definition, hidden))
        assertTrue(BadgeVisibilityEvaluator.isVisible(
            definition, hidden.copy(earnedBadgeIds = setOf(definition.badgeId))
        ))
    }
    @Test fun unknownBadgesAreHistoricalAndHaveNoFabricatedMilestones() {
        val definition = BadgeDefinitionResolver.resolve("legacy_unknown_badge")

        assertEquals(BadgeGoalType.HISTORICAL_COUNT_ONLY, definition.goalType)
        assertTrue(definition.milestones.isEmpty())
        assertFalse(definition.trackable)
    }
    @Test fun obsoleteDiscoveryBadgeIsPreservedButNotUserVisible() {
        assertNotNull(BadgeDefinitionResolver.resolve("badge_discovery"))
        assertFalse(BadgeDefinitionResolver.isUserVisible("badge_discovery"))
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

    @Test fun firstAndAggregateMasteryTimestampsUseQualifyingEvents() {
        val events = (1..6).map { index ->
            CreatureMasteryEventEntity("e$index", "i$index", "species-${index % 3}",
                achievedAt = index * 100L, levelUpTransactionId = if (index == 1) "backfill_i1" else "live-$index")
        } + CreatureMasteryEventEntity("late", "late", "unrelated", 9_999L, "live-late")

        assertEquals(100L, AchievementTimestampCalculator.firstMasteryTimestamp(events)?.timestamp)
        assertEquals(MasteryTimestampConfidence.ESTIMATED_FROM_ACQUISITION,
            AchievementTimestampCalculator.firstMasteryTimestamp(events)?.confidence)
        assertEquals(500L, AchievementTimestampCalculator.masteryThresholdTimestamp(events, 5)?.timestamp)
        assertEquals(300L, AchievementTimestampCalculator.masteryVarietyThresholdTimestamp(events, 3)?.timestamp)
    }

    @Test fun unknownMasteryTimeRemainsUnknownInsteadOfBecomingBackfillTime() {
        val unknown = CreatureMasteryEventEntity("unknown", "instance", "species", 0L, "backfill_instance")

        assertNull(AchievementTimestampCalculator.firstMasteryTimestamp(listOf(unknown)))
        assertNull(AchievementTimestampCalculator.masteryThresholdTimestamp(listOf(unknown), 1))
        assertEquals(MasteryTimestampConfidence.UNKNOWN,
            MasteryEvidenceCalculator.bySpecies(listOf(unknown), emptyList()).getValue("species").timestampConfidence)
    }

    @Test fun discoveryVarietyTimestampIgnoresUnrelatedLaterEvidence() {
        val discoveries = listOf(
            CreatureDiscoveryEntity("a", 100L, null, null, 100L),
            CreatureDiscoveryEntity("b", 200L, null, null, 200L),
            CreatureDiscoveryEntity("unrelated", 9_999L, null, null, 9_999L)
        )

        assertEquals(200L, AchievementTimestampCalculator.discoveryVarietyThresholdTimestamp(
            discoveries, threshold = 2, participatingSpecies = setOf("a", "b")
        )?.timestamp)
        assertNull(AchievementTimestampCalculator.discoveryVarietyThresholdTimestamp(
            discoveries, threshold = 3, participatingSpecies = setOf("a", "b")
        ))
    }

    @Test fun lockedOneTimeDashboardBadgeIsNotEarnedAndHasObjectiveTarget() {
        val badge = BadgeDashboardCalculator.calculate(
            earned = emptyList(), instances = emptyList(), discoveries = emptyList(),
            masteries = emptyList(), completions = emptyList(), pins = emptyList(), tracking = emptyList()
        ).badges.first { it.badgeId == "mastery_first" }

        assertFalse(badge.everEarned)
        assertFalse(badge.terminal)
        assertEquals(0, badge.currentProgress)
        assertEquals(1, badge.objectiveTarget)
        assertNull(badge.nextMilestoneTarget)
    }

    @Test fun specialCompletionDatesUseOnlyRequiredEvidence() {
        val species = CreatureCatalog.all.take(2)
        val collections = listOf(
            CollectionDefinition("blue_first", 1, listOf(species[0])),
            CollectionDefinition("blue_second", 1, listOf(species[1]))
        )
        val discoveries = listOf(
            CreatureDiscoveryEntity(species[0].creatureId, 100L, null, null, 100L),
            CreatureDiscoveryEntity(species[1].creatureId, 300L, null, null, 300L),
            CreatureDiscoveryEntity("unrelated", 9_999L, null, null, 9_999L)
        )
        val masteries = listOf(
            CreatureMasteryEventEntity("a", "a", species[0].creatureId, 200L, "live-a"),
            CreatureMasteryEventEntity("b", "b", species[1].creatureId, 400L, "live-b"),
            CreatureMasteryEventEntity("late", "late", "unrelated", 9_999L, "live-late")
        )
        val completions = collections.mapIndexed { index, collection ->
            CollectionCompletionEntity("c$index", collection.collectionId, BadgeRequirement.COLLECTOR.name,
                500L + index * 100, 1, "hash-$index", species[index].creatureId)
        } + CollectionCompletionEntity("late", "unrelated", BadgeRequirement.COLLECTOR.name,
            9_999L, 1, "late", "unrelated")

        assertEquals(300L, AchievementTimestampCalculator.acrossTheDepthsTimestamp(discoveries, collections)?.timestamp)
        assertEquals(400L, AchievementTimestampCalculator.oneFromEveryWaterTimestamp(masteries, collections)?.timestamp)
        assertEquals(600L, AchievementTimestampCalculator.keeperOfTheBlueTimestamp(
            completions, collections.mapTo(mutableSetOf()) { it.collectionId }
        )?.timestamp)
        assertNull(AchievementTimestampCalculator.keeperOfTheBlueTimestamp(
            completions.map { if (it.collectionId == "blue_first") it.copy(completedAt = 0L) else it },
            collections.mapTo(mutableSetOf()) { it.collectionId }
        ))
    }

    @Test fun boundedBadgeSeparatesHistoricalLifetimeFromCurrentRosterProgress() {
        val unavailableOrNonparticipating = CreatureCatalog.all.first()
        val stored = com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity(
            "mastery_variety", 7, 10L, 20L, false, viewedAt = 20L
        )
        val event = CreatureMasteryEventEntity(
            "historical", "instance", unavailableOrNonparticipating.creatureId, 10L, "live"
        )
        val badge = BadgeDashboardCalculator.calculate(
            earned = listOf(stored), instances = emptyList(), discoveries = emptyList(),
            masteries = listOf(event), completions = emptyList(), pins = emptyList(), tracking = emptyList()
        ).badges.first { it.badgeId == "mastery_variety" }

        assertEquals(7, badge.lifetimeCount)
        assertTrue(badge.currentProgress <= badge.objectiveTarget)
        assertTrue(badge.everEarned)
    }

    @Test fun stillwaterTimestampScopeIgnoresEarlierAndLaterBlueEvidence() {
        val stillwaterId = CreatureCatalog.stillwater.first().creatureId
        val blueId = CreatureCatalog.all.first { it.creatureId !in CreatureCatalog.stillwater.map { creature -> creature.creatureId } }.creatureId
        val discoveries = listOf(
            CreatureDiscoveryEntity(blueId, 10L, null, null, 10L),
            CreatureDiscoveryEntity(stillwaterId, 100L, null, null, 100L),
            CreatureDiscoveryEntity(blueId + "-later", 9_999L, null, null, 9_999L)
        )
        val masteries = listOf(
            CreatureMasteryEventEntity("blue", "blue", blueId, 20L, "live-blue"),
            CreatureMasteryEventEntity("still", "still", stillwaterId, 200L, "live-still"),
            CreatureMasteryEventEntity("blue-late", "blue-late", blueId, 9_999L, "live-blue-late")
        )

        val scoped = AchievementEvidenceScope.stillwater(discoveries, masteries)
        assertEquals(listOf(100L), scoped.discoveries.map { it.firstDiscoveredAt })
        assertEquals(listOf(200L), scoped.masteries.map { it.achievedAt })
        assertEquals(100L, AchievementTimestampCalculator.discoveryVarietyThresholdTimestamp(
            scoped.discoveries, 1, scoped.speciesIds
        )?.timestamp)
        assertEquals(200L, AchievementTimestampCalculator.masteryThresholdTimestamp(scoped.masteries, 1)?.timestamp)
        assertNull(AchievementTimestampCalculator.masteryThresholdTimestamp(scoped.masteries, 2))
    }

    @Test fun collectionCompletionIdentityIsStablePerRosterEdition() {
        val first = CollectionCompletionIdentity.forRoster("blue_open_blue", BadgeRequirement.COLLECTOR, "hash-a")
        assertEquals(first, CollectionCompletionIdentity.forRoster("blue_open_blue", BadgeRequirement.COLLECTOR, "hash-a"))
        assertTrue(first != CollectionCompletionIdentity.forRoster("blue_open_blue", BadgeRequirement.COLLECTOR, "hash-b"))
        assertTrue(first != CollectionCompletionIdentity.forRoster("blue_open_blue", BadgeRequirement.CURATOR, "hash-a"))
    }
}
