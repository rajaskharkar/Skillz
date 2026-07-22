package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel

enum class BadgeUiCategory { ALL, FLOW, ARC, CREATURES, MASTERY, COLLECTIONS, STILLWATER, MOVEMENT, SURGE, SPECIAL }
enum class BadgeSort { RECOMMENDED, RECENTLY_EARNED, RECENTLY_ADVANCED, HIGHEST_COUNT, CLOSEST_MILESTONE, ALPHABETICAL }
enum class BadgeDisabledReason { COMPLETE, NO_NEXT_MILESTONE, CREATURE_NOT_OWNED, REGION_LOCKED, VESSEL_LOCKED, EMPTY_ROSTER, UNSUPPORTED_DESTINATION }
data class AchievementAccessState(
    val unlockedBlueZones: Set<CreatureZone> = CreatureZone.entries.toSet(),
    val unlockedStillwaterVessels: Set<StillwaterVessel> = StillwaterVessel.entries.toSet()
)
sealed interface BadgeActionDestination {
    data object Flow : BadgeActionDestination
    data object Arc : BadgeActionDestination
    data object MovementInfo : BadgeActionDestination
    data class BadgeDetails(val badgeId: String) : BadgeActionDestination
    data class CollectionDetails(val collectionId: String) : BadgeActionDestination
    data class ChestSpecies(val speciesId: String) : BadgeActionDestination
    data class BlueRegion(val collectionId: String, val speciesId: String? = null) : BadgeActionDestination
    data class StillwaterVessel(val collectionId: String, val speciesId: String? = null) : BadgeActionDestination
    data class BeyondBlue(val collectionId: String, val speciesId: String) : BadgeActionDestination
}

data class BadgeProgressModel(
    val badgeId: String,
    val count: Int,
    val earned: Boolean,
    val category: BadgeUiCategory,
    val progress: Int,
    val target: Int,
    val remaining: Int,
    val milestone: MilestoneProgress,
    val firstEarnedAt: Long? = null,
    val lastAdvancedAt: Long? = null,
    val pinnedOrder: Int? = null,
    val tracked: Boolean = false,
    val collectionProgress: CollectionProgress? = null,
    val highestCreatureLevel: Int? = null,
    val action: BadgeActionDestination = BadgeActionDestination.BadgeDetails(badgeId),
    val newlyEarned: Boolean = false,
    val recentlyUpdated: Boolean = false,
    val importance: Int = 0,
    val canTrack: Boolean = false,
    val canNavigate: Boolean = false,
    val canProgressNow: Boolean = false,
    val disabledReason: BadgeDisabledReason? = null,
    val acquisitionAction: BadgeActionDestination? = null,
    val pinnable: Boolean = true,
    val goalType: BadgeGoalType = BadgeGoalType.REPEATABLE_MILESTONE
)

data class BadgeDashboard(
    val badges: List<BadgeProgressModel>,
    val collections: List<CollectionProgress>,
    val uniqueEarned: Int,
    val totalMasteries: Int,
    val completedCollections: Int,
    val recommendations: List<BadgeProgressModel>,
    val level99Previews: Map<String, Level99AchievementPreview>
)
data class Level99AchievementPreview(
    val speciesId: String,
    val resultingSpeciesMasteryCount: Int,
    val firstSpeciesMastery: Boolean,
    val regionCollectionId: String,
    val regionalMasteredAfter: Int,
    val regionalTotal: Int,
    val completesRegion: Boolean,
    val completesBlue: Boolean,
    val completesAllWaters: Boolean,
    val restoresRegionRoster: Boolean = false,
    val restoresBlueRoster: Boolean = false,
    val restoresAllWatersRoster: Boolean = false,
    val milestones: List<Int>,
    val stillwaterMasteredAfter: Int? = null,
    val stillwaterTotal: Int? = null,
    val completesStillwater: Boolean = false,
    val restoresStillwaterRoster: Boolean = false
)

object BadgeDashboardCalculator {
    fun calculate(
        earned: List<UserBadgeEntity>,
        instances: List<UserShellFindInstanceEntity>,
        discoveries: List<CreatureDiscoveryEntity>,
        masteries: List<CreatureMasteryEventEntity>,
        completions: List<CollectionCompletionEntity>,
        pins: List<BadgePinEntity>,
        tracking: List<BadgeTrackingEntity>,
        countFloors: List<BadgeCountFloorEntity> = emptyList(),
        accessState: AchievementAccessState = AchievementAccessState()
    ): BadgeDashboard {
        val earnedById = earned.associateBy { it.badgeId }
        val pinOrder = pins.associate { it.badgeId to it.pinOrder }
        val tracked = tracking.map { it.badgeId }.toSet()
        val floors = countFloors.associateBy { it.badgeId }
        val discovered = discoveries.map { it.speciesId }.toSet()
        val activeLevels = instances.filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy({ it.findId }, { it.animalLevel })
        val masteryEvidence = MasteryEvidenceCalculator.bySpecies(masteries, countFloors, activeLevels)
        val mastered = masteryEvidence.filterValues { it.hasEverBeenMastered }.keys
        val eligibleMasterySpecies = CreatureCatalog.all.filter { it.isAvailable && it.participatesInCompletionist }.mapTo(mutableSetOf()) { it.creatureId }
        val eligibleCollectorSpecies = CreatureCatalog.all.filter { it.isAvailable && it.participatesInCollector }.mapTo(mutableSetOf()) { it.creatureId }
        val eligibleStillwaterSpecies = CreatureCatalog.stillwater.filter { it.isAvailable && it.participatesInCollector }.mapTo(mutableSetOf()) { it.creatureId }
        val historical = completions.groupBy { it.collectionId }.mapValues { (_, rows) ->
            rows.mapNotNull { runCatching { BadgeRequirement.valueOf(it.completionType) }.getOrNull() }.toSet()
        }
        val collectionProgress = CollectionCatalog.collections.map {
            CollectionProgressCalculator.calculate(it, discovered, activeLevels, masteryEvidence, historical[it.collectionId].orEmpty())
        }
        val collectionById = collectionProgress.associateBy { it.collectionId }
        val definitions = BadgeDefinitionResolver.allDefinitions(earned)
            .filter { BadgeDefinitionResolver.isUserVisible(it.badgeId) }
            .filter { !it.hiddenUntilEarned || (earnedById[it.badgeId]?.count ?: 0) > 0 }
        val badges = definitions.map { definition ->
            val stored = earnedById[definition.badgeId]
            val collection = definition.collectionId?.let(collectionById::get)
            val speciesLevels = definition.speciesId?.let { activeLevels[it].orEmpty() }.orEmpty()
            val verified = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> if (collection?.collectorEarned == true) 1 else 0
                BadgeRequirement.CURATOR -> if (collection?.curatorEarned == true) 1 else 0
                BadgeRequirement.COMPLETIONIST -> if (collection?.completionistEarned == true) 1 else 0
                BadgeRequirement.EXACT_COUNT -> when (definition.badgeId) {
                    "mastery_first" -> if (mastered.isEmpty()) 0 else 1
                    "mastery_circle" -> maxOf(
                        masteryEvidence.values.sumOf { it.effectiveLifetimeCount },
                        MasteryEvidenceCalculator.effectiveCount(masteries.size, floors["mastery_circle"])
                    )
                    "mastery_variety" -> mastered.intersect(eligibleMasterySpecies).size
                    "variety_collector" -> discovered.intersect(eligibleCollectorSpecies).size
                    "stillwater_first_catch" -> if (discovered.any { CreatureCatalog.get(it)?.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER }) 1 else 0
                    "stillwater_variety" -> discovered.intersect(eligibleStillwaterSpecies).size
                    "stillwater_mastery" -> maxOf(
                        masteryEvidence.values.filter { CreatureCatalog.get(it.speciesId)?.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER }.sumOf { it.effectiveLifetimeCount },
                        MasteryEvidenceCalculator.effectiveCount(masteries.count { CreatureCatalog.get(it.speciesId)?.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER }, floors["stillwater_mastery"])
                    )
                    "across_the_depths" -> if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") }.all { (collectionById[it.collectionId]?.discoveredSpeciesCount ?: 0) > 0 }) 1 else 0
                    "one_from_every_water" -> if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") || it.collectionId.startsWith("stillwater_") }.all { collection ->
                        collection.eligibleRoster(BadgeRequirement.COMPLETIONIST).any { masteryEvidence[it]?.hasEverBeenMastered == true }
                    }) 1 else 0
                    "keeper_of_the_blue" -> if (CollectionCatalog.collections.filter { it.collectionId.startsWith("blue_") }.all { collectionById[it.collectionId]?.collectorEarned == true }) 1 else 0
                    else -> definition.speciesId?.let { masteryEvidence[it]?.effectiveLifetimeCount ?: 0 } ?: (stored?.count ?: 0)
                }
            }
            val boundedRosterBadge = definition.badgeId in setOf("mastery_variety", "variety_collector", "stillwater_variety")
            val computed = if (boundedRosterBadge || definition.badgeId == "mastery_circle" || definition.speciesId != null || definition.badgeId == "stillwater_mastery") verified
                else MasteryEvidenceCalculator.effectiveCount(verified, floors[definition.badgeId])
            val target = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> collection?.totalParticipatingSpecies ?: 1
                BadgeRequirement.CURATOR -> collection?.totalParticipatingSpecies ?: 1
                BadgeRequirement.COMPLETIONIST -> collection?.totalCompletionistSpecies ?: 1
                BadgeRequirement.EXACT_COUNT -> if (definition.goalType == BadgeGoalType.HISTORICAL_COUNT_ONLY) 0
                    else MilestoneEngine.evaluate(computed, thresholds = definition.milestones).nextThreshold ?: computed.coerceAtLeast(1)
            }
            val progress = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> collection?.discoveredSpeciesCount ?: 0
                BadgeRequirement.CURATOR -> collection?.currentlyOwnedSpeciesCount ?: 0
                BadgeRequirement.COMPLETIONIST -> collection?.masteredSpeciesCount ?: 0
                BadgeRequirement.EXACT_COUNT -> computed
            }
            val currentRosterIncomplete = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> collection?.currentRosterCollectorComplete == false
                BadgeRequirement.CURATOR -> collection?.currentRosterCuratorComplete == false
                BadgeRequirement.COMPLETIONIST -> collection?.currentRosterCompletionistComplete == false
                BadgeRequirement.EXACT_COUNT -> false
            }
            val terminal = definition.countType == BadgeCountType.ONE_TIME && computed > 0 && !currentRosterIncomplete
            val hasNextMilestone = definition.countType == BadgeCountType.REPEATABLE &&
                definition.milestones.any { it > computed }
            val nonEmptyRoster = collection == null || when (definition.requirement) {
                BadgeRequirement.COMPLETIONIST -> collection.totalCompletionistSpecies > 0
                BadgeRequirement.COLLECTOR, BadgeRequirement.CURATOR -> collection.totalParticipatingSpecies > 0
                BadgeRequirement.EXACT_COUNT -> true
            }
            val ownsSpecies = definition.speciesId == null || speciesLevels.isNotEmpty()
            val destination = action(definition)
            val acquisition = if (!ownsSpecies) acquisitionAction(definition) else null
            val primaryAction = acquisition ?: destination
            val lockedReason = accessDisabledReason(primaryAction, accessState)
            val canNavigate = primaryAction !is BadgeActionDestination.BadgeDetails
            val canProgressNow = !terminal && nonEmptyRoster && (ownsSpecies || acquisition != null) && lockedReason == null &&
                (definition.countType == BadgeCountType.ONE_TIME || hasNextMilestone)
            val canTrack = definition.trackable && canProgressNow && canNavigate
            val disabledReason = when {
                terminal -> BadgeDisabledReason.COMPLETE
                !nonEmptyRoster -> BadgeDisabledReason.EMPTY_ROSTER
                lockedReason != null -> lockedReason
                !ownsSpecies && acquisition == null -> BadgeDisabledReason.CREATURE_NOT_OWNED
                definition.countType == BadgeCountType.REPEATABLE && !hasNextMilestone -> BadgeDisabledReason.NO_NEXT_MILESTONE
                !canNavigate -> BadgeDisabledReason.UNSUPPORTED_DESTINATION
                else -> null
            }
            BadgeProgressModel(definition.badgeId, computed, computed > 0, category(definition), progress,
                target, (target - progress).coerceAtLeast(0), MilestoneEngine.evaluate(computed, thresholds = definition.milestones),
                stored?.firstEarnedAt, stored?.lastEarnedAt, pinOrder[definition.badgeId],
                definition.badgeId in tracked,
                collection, speciesLevels.maxOrNull(), primaryAction,
                stored?.viewedAt == null && stored != null && stored.firstEarnedAt == stored.lastEarnedAt,
                stored?.viewedAt == null && stored != null && stored.lastEarnedAt > stored.firstEarnedAt,
                definition.importance, canTrack, canNavigate, canProgressNow, disabledReason,
                acquisition, definition.pinnable, definition.goalType)
        }
        val previewBySpecies = CreatureCatalog.all.associate { creature ->
            val region = collectionById.getValue(creature.primaryProgressCollectionId)
            val blue = collectionById.getValue("collection_the_blue")
            val all = collectionById.getValue("collection_all_waters")
            val stillwater = collectionById.getValue("collection_stillwater")
            val currentSpeciesCount = badges.firstOrNull { it.badgeId == "mastery_species_${creature.creatureId}" }?.count ?: 0
            val addsUnique = creature.creatureId !in mastered
            val afterCount = currentSpeciesCount + 1
            creature.creatureId to Level99AchievementPreview(creature.creatureId, afterCount,
                currentSpeciesCount == 0, creature.primaryProgressCollectionId,
                region.masteredSpeciesCount + if (addsUnique && creature.creatureId in region.missingMasteredSpeciesIds) 1 else 0,
                region.totalCompletionistSpecies,
                !region.completionistEarned && region.missingMasteredSpeciesIds == setOf(creature.creatureId),
                !blue.completionistEarned && blue.missingMasteredSpeciesIds == setOf(creature.creatureId),
                !all.completionistEarned && all.missingMasteredSpeciesIds == setOf(creature.creatureId),
                region.completionistEarned && !region.currentRosterCompletionistComplete && region.missingMasteredSpeciesIds == setOf(creature.creatureId),
                blue.completionistEarned && !blue.currentRosterCompletionistComplete && blue.missingMasteredSpeciesIds == setOf(creature.creatureId),
                all.completionistEarned && !all.currentRosterCompletionistComplete && all.missingMasteredSpeciesIds == setOf(creature.creatureId),
                AchievementBadgeDefinition.DEFAULT_MILESTONES.filter { it == afterCount },
                if (creature.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER) stillwater.masteredSpeciesCount + if (addsUnique && creature.creatureId in stillwater.missingMasteredSpeciesIds) 1 else 0 else null,
                if (creature.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER) stillwater.totalCompletionistSpecies else null,
                creature.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER && !stillwater.completionistEarned && stillwater.missingMasteredSpeciesIds == setOf(creature.creatureId),
                creature.sourceType == com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER && stillwater.completionistEarned && !stillwater.currentRosterCompletionistComplete && stillwater.missingMasteredSpeciesIds == setOf(creature.creatureId))
        }
        return BadgeDashboard(badges, collectionProgress, badges.count { it.earned },
            badges.firstOrNull { it.badgeId == "mastery_circle" }?.count ?: masteries.size,
            completions.filter { it.completionType == BadgeRequirement.COMPLETIONIST.name }.map { it.collectionId }.distinct().size,
            RecommendationEngine.recommend(badges), previewBySpecies)
    }

    private fun category(def: AchievementBadgeDefinition) = when {
        def.speciesId != null -> BadgeUiCategory.MASTERY
        def.collectionId == "collection_stillwater" || def.collectionId?.startsWith("stillwater_") == true -> BadgeUiCategory.STILLWATER
        def.collectionId != null -> BadgeUiCategory.COLLECTIONS
        def.family == BadgeFamily.FLOW || def.family == BadgeFamily.SOFT_FLOW -> BadgeUiCategory.FLOW
        def.family == BadgeFamily.ARC -> BadgeUiCategory.ARC
        def.family == BadgeFamily.MOVEMENT -> BadgeUiCategory.MOVEMENT
        def.family == BadgeFamily.SURGE -> BadgeUiCategory.SURGE
        def.family == BadgeFamily.MASTERY -> BadgeUiCategory.MASTERY
        else -> BadgeUiCategory.SPECIAL
    }
    private fun action(def: AchievementBadgeDefinition): BadgeActionDestination = when {
        def.speciesId != null -> BadgeActionDestination.ChestSpecies(def.speciesId)
        def.collectionId != null -> BadgeActionDestination.CollectionDetails(def.collectionId)
        def.badgeId in setOf("across_the_depths", "keeper_of_the_blue") -> BadgeActionDestination.CollectionDetails("collection_the_blue")
        def.badgeId == "one_from_every_water" -> BadgeActionDestination.CollectionDetails("collection_all_waters")
        def.family == BadgeFamily.FLOW || def.family == BadgeFamily.SOFT_FLOW || def.family == BadgeFamily.SURGE -> BadgeActionDestination.Flow
        def.family == BadgeFamily.ARC -> BadgeActionDestination.Arc
        def.family == BadgeFamily.MOVEMENT -> BadgeActionDestination.MovementInfo
        else -> BadgeActionDestination.BadgeDetails(def.badgeId)
    }
    private fun acquisitionAction(def: AchievementBadgeDefinition): BadgeActionDestination? = def.speciesId
        ?.let(CreatureCatalog::get)?.let { creature ->
            when (creature.sourceType) {
                com.kingkharnivore.skillz.utils.shell.CreatureSourceType.STILLWATER ->
                    BadgeActionDestination.StillwaterVessel(creature.primaryProgressCollectionId, creature.creatureId)
                com.kingkharnivore.skillz.utils.shell.CreatureSourceType.BEYOND_BLUE ->
                    BadgeActionDestination.BeyondBlue(creature.collectionId, creature.creatureId)
                else -> BadgeActionDestination.BlueRegion(creature.collectionId, creature.creatureId)
            }
        }
    private fun accessDisabledReason(action: BadgeActionDestination, access: AchievementAccessState): BadgeDisabledReason? = when (action) {
        is BadgeActionDestination.BlueRegion -> action.collectionId.removePrefix("blue_")
            .let { id -> CreatureZone.entries.firstOrNull { it.name.lowercase() == id } }
            ?.takeUnless { it in access.unlockedBlueZones }?.let { BadgeDisabledReason.REGION_LOCKED }
        is BadgeActionDestination.BeyondBlue -> action.collectionId.removePrefix("blue_")
            ?.let { id -> CreatureZone.entries.firstOrNull { it.name.lowercase() == id } }
            ?.takeUnless { it in access.unlockedBlueZones }?.let { BadgeDisabledReason.REGION_LOCKED }
        is BadgeActionDestination.StillwaterVessel -> action.collectionId.removePrefix("stillwater_")
            ?.let { id -> StillwaterVessel.entries.firstOrNull { it.name.lowercase() == id } }
            ?.takeUnless { it in access.unlockedStillwaterVessels }?.let { BadgeDisabledReason.VESSEL_LOCKED }
        else -> null
    }
}

object RecommendationEngine {
    fun recommend(badges: List<BadgeProgressModel>, limit: Int = 3): List<BadgeProgressModel> {
        val eligible = badges.filter { !it.tracked && it.goalType != BadgeGoalType.HISTORICAL_COUNT_ONLY &&
            it.canProgressNow && it.canNavigate && it.remaining > 0 && it.target > 0 &&
            (it.collectionProgress?.totalParticipatingSpecies ?: 1) > 0 }
        val result = mutableListOf<BadgeProgressModel>()
        val usedCategories = mutableSetOf<BadgeUiCategory>()
        eligible.sortedWith(compareBy<BadgeProgressModel> {
            it.remaining.toDouble() / it.target
        }.thenByDescending { it.importance }.thenByDescending { it.highestCreatureLevel ?: 0 }
            .thenByDescending { it.progress }.thenBy { it.badgeId }).forEach { candidate ->
            if (result.size >= limit) return@forEach
            if (candidate.category !in usedCategories || eligible.none { it.category !in usedCategories }) {
                result += candidate
                usedCategories += candidate.category
            }
        }
        return result.distinctBy { it.badgeId }.take(limit)
    }
}
