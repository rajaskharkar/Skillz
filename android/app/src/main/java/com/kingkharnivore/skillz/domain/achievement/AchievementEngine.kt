package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterCatalog
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeCountFloorEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity
import java.security.MessageDigest

enum class BadgeFamily { SPECIES_MASTERY, MASTERY, COLLECTION, FLOW, SOFT_FLOW, ARC, MOVEMENT, SURGE }
enum class BadgeCountType { ONE_TIME, REPEATABLE }
enum class BadgeRequirement { EXACT_COUNT, COLLECTOR, CURATOR, COMPLETIONIST }

data class SpeciesMasteryEvidence(
    val speciesId: String,
    val verifiedIndividualCount: Int,
    val legacyMinimumCount: Int,
    val effectiveLifetimeCount: Int,
    val hasEverBeenMastered: Boolean,
    val currentLevel99Count: Int = 0,
    val firstMasteryAt: Long? = null,
    val latestMasteryAt: Long? = null
)

/** One count-floor rule shared by persistence, dashboards, previews, and celebrations. */
object MasteryEvidenceCalculator {
    fun effectiveCount(verifiedCount: Int, floor: BadgeCountFloorEntity?): Int = floor?.let {
        maxOf(verifiedCount, it.minimumCount + (verifiedCount - it.verifiedCountAtReconciliation).coerceAtLeast(0))
    } ?: verifiedCount

    fun bySpecies(
        events: List<CreatureMasteryEventEntity>,
        floors: List<BadgeCountFloorEntity>,
        ownedLevels: Map<String, List<Int>> = emptyMap()
    ): Map<String, SpeciesMasteryEvidence> {
        val eventsBySpecies = events.groupBy { it.speciesId }
        val floorsBySpecies = floors.mapNotNull { floor ->
            val speciesId = floor.speciesId ?: floor.badgeId.removePrefix("mastery_species_")
                .takeIf { floor.badgeId.startsWith("mastery_species_") }
            speciesId?.let { it to floor }
        }.toMap()
        return (CreatureCatalog.all.map { it.creatureId } + eventsBySpecies.keys + floorsBySpecies.keys)
            .distinct().associateWith { speciesId ->
                val speciesEvents = eventsBySpecies[speciesId].orEmpty()
                val floor = floorsBySpecies[speciesId]
                val effective = effectiveCount(speciesEvents.size, floor)
                SpeciesMasteryEvidence(
                    speciesId = speciesId,
                    verifiedIndividualCount = speciesEvents.size,
                    legacyMinimumCount = floor?.minimumCount ?: 0,
                    effectiveLifetimeCount = effective,
                    hasEverBeenMastered = effective > 0,
                    currentLevel99Count = ownedLevels[speciesId].orEmpty().count { it >= 99 },
                    firstMasteryAt = speciesEvents.minOfOrNull { it.achievedAt },
                    latestMasteryAt = speciesEvents.maxOfOrNull { it.achievedAt }
                )
            }
    }
}

data class AchievementBadgeDefinition(
    val badgeId: String,
    val family: BadgeFamily,
    val countType: BadgeCountType,
    val requirement: BadgeRequirement,
    val milestones: List<Int> = DEFAULT_MILESTONES,
    val speciesId: String? = null,
    val collectionId: String? = null,
    val hiddenUntilEarned: Boolean = false,
    val pinnable: Boolean = true,
    val trackable: Boolean = true,
    val importance: Int = 0,
    val navigationDestination: String? = null
) {
    companion object { val DEFAULT_MILESTONES = listOf(1, 5, 10, 25, 50, 100, 250, 500, 1_000) }
}

object AchievementBadgeCatalog {
    val definitions: List<AchievementBadgeDefinition> = buildList {
        CreatureCatalog.all.forEach { creature ->
            add(AchievementBadgeDefinition("mastery_species_${creature.creatureId}", BadgeFamily.SPECIES_MASTERY, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT, speciesId = creature.creatureId))
        }
        add(AchievementBadgeDefinition("mastery_first", BadgeFamily.MASTERY, BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT))
        add(AchievementBadgeDefinition("mastery_circle", BadgeFamily.MASTERY, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT))
        add(AchievementBadgeDefinition("mastery_variety", BadgeFamily.MASTERY, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT))
        add(AchievementBadgeDefinition("variety_collector", BadgeFamily.COLLECTION, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT))
        add(AchievementBadgeDefinition("stillwater_first_catch", BadgeFamily.COLLECTION, BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT, collectionId = "collection_stillwater"))
        add(AchievementBadgeDefinition("stillwater_variety", BadgeFamily.COLLECTION, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT, collectionId = "collection_stillwater"))
        add(AchievementBadgeDefinition("stillwater_mastery", BadgeFamily.MASTERY, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT, collectionId = "collection_stillwater"))
        CollectionCatalog.collections.forEach { collection ->
            listOf(BadgeRequirement.COLLECTOR, BadgeRequirement.CURATOR, BadgeRequirement.COMPLETIONIST).forEach { requirement ->
                val importance = when {
                    collection.collectionId == "collection_all_waters" && requirement == BadgeRequirement.COMPLETIONIST -> 100
                    collection.collectionId in setOf("collection_the_blue", "collection_stillwater") && requirement == BadgeRequirement.COMPLETIONIST -> 80
                    requirement == BadgeRequirement.COMPLETIONIST -> 60
                    requirement == BadgeRequirement.CURATOR -> 40
                    else -> 30
                }
                add(AchievementBadgeDefinition("${collection.collectionId}_${requirement.name.lowercase()}", BadgeFamily.COLLECTION, BadgeCountType.ONE_TIME, requirement, collectionId = collection.collectionId, importance = importance))
            }
        }
        listOf("across_the_depths", "one_from_every_water", "keeper_of_the_blue").forEach {
            add(AchievementBadgeDefinition(it, BadgeFamily.COLLECTION, BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT))
        }
        // Existing IDs remain canonical and are evaluated from their existing reliable ledgers.
        listOf("badge_flow_10_min", "badge_flow_30_min", "badge_flow_60_min", "badge_flow_120_min").forEach {
            add(AchievementBadgeDefinition(it, BadgeFamily.FLOW, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT))
        }
    }
    val byId = definitions.associateBy { it.badgeId }
}

data class CollectionDefinition(val collectionId: String, val rosterVersion: Int, val species: List<CreatureDefinition>, val allowEmptyCompletion: Boolean = false) {
    fun eligibleRoster(requirement: BadgeRequirement): List<String> = species.asSequence()
        .filter { it.isAvailable }
        .filter { creature -> when (requirement) {
            BadgeRequirement.COLLECTOR, BadgeRequirement.CURATOR -> creature.participatesInCollector
            BadgeRequirement.COMPLETIONIST -> creature.participatesInCompletionist
            BadgeRequirement.EXACT_COUNT -> false
        } }
        .map { it.creatureId }.distinct().sorted().toList()
    fun rosterHash(requirement: BadgeRequirement): String = MessageDigest.getInstance("SHA-256")
        .digest(eligibleRoster(requirement).joinToString("\n").toByteArray())
        .joinToString("") { "%02x".format(it) }
}

object CollectionCatalog {
    private val blueRegions = CreatureZone.values().map { zone ->
        CollectionDefinition("blue_${zone.name.lowercase()}", 1, CreatureCatalog.all.filter { it.sourceType != CreatureSourceType.STILLWATER && it.zone == zone })
    }
    private val stillwaterVessels = StillwaterVessel.values().map { vessel ->
        val ids = StillwaterCatalog.creaturesFor(vessel).map { it.creatureId }.toSet()
        CollectionDefinition("stillwater_${vessel.name.lowercase()}", 1, CreatureCatalog.stillwater.filter { it.creatureId in ids })
    }
    val collections = blueRegions + stillwaterVessels + listOf(
        CollectionDefinition("collection_stillwater", 1, CreatureCatalog.stillwater),
        CollectionDefinition("collection_the_blue", 1, CreatureCatalog.all.filter { it.sourceType != CreatureSourceType.STILLWATER }),
        CollectionDefinition("collection_all_waters", 1, CreatureCatalog.all)
    )
    val byId = collections.associateBy { it.collectionId }
}

data class CollectionProgress(
    val collectionId: String,
    val totalParticipatingSpecies: Int,
    val totalCompletionistSpecies: Int,
    val discoveredSpeciesCount: Int,
    val currentlyOwnedSpeciesCount: Int,
    val masteredSpeciesCount: Int,
    val collectorEarned: Boolean,
    val curatorEarned: Boolean,
    val completionistEarned: Boolean,
    val currentRosterCollectorComplete: Boolean,
    val currentRosterCuratorComplete: Boolean,
    val currentRosterCompletionistComplete: Boolean,
    val missingDiscoveredSpeciesIds: Set<String>,
    val missingCurrentlyOwnedSpeciesIds: Set<String>,
    val missingMasteredSpeciesIds: Set<String>,
    val closestCurrentCreatureToMastery: String?,
    val speciesStates: List<CollectionSpeciesProgress> = emptyList()
)
data class CollectionSpeciesProgress(
    val speciesId: String,
    val discovered: Boolean,
    val ownedCount: Int,
    val highestLevel: Int?,
    val mastered: Boolean,
    val secret: Boolean,
    val currentLevel99Count: Int = 0,
    val lifetimeMasteryCount: Int = 0,
    val firstMasteryAt: Long? = null,
    val latestMasteryAt: Long? = null,
    val requiredByCollector: Boolean = false,
    val requiredByCurator: Boolean = false,
    val requiredByCompletionist: Boolean = false,
    val sourceId: String? = null
)

object CollectionProgressCalculator {
    fun calculate(definition: CollectionDefinition, discovered: Set<String>, ownedLevels: Map<String, List<Int>>, mastered: Set<String>, historicalTypes: Set<BadgeRequirement> = emptySet()): CollectionProgress =
        calculate(definition, discovered, ownedLevels, mastered.associateWith {
            SpeciesMasteryEvidence(it, 1, 0, 1, true)
        }, historicalTypes)

    fun calculate(definition: CollectionDefinition, discovered: Set<String>, ownedLevels: Map<String, List<Int>>, masteryEvidence: Map<String, SpeciesMasteryEvidence>, historicalTypes: Set<BadgeRequirement> = emptySet()): CollectionProgress {
        val collectorRoster = definition.eligibleRoster(BadgeRequirement.COLLECTOR).toSet()
        val masteryRoster = definition.eligibleRoster(BadgeRequirement.COMPLETIONIST).toSet()
        val owned = ownedLevels.filterValues { it.isNotEmpty() }.keys
        val missingDiscovered = collectorRoster - discovered
        val missingOwned = collectorRoster - owned
        val mastered = masteryEvidence.filterValues { it.hasEverBeenMastered }.keys
        val missingMastered = masteryRoster - mastered
        val collectorComplete = (collectorRoster.isNotEmpty() || definition.allowEmptyCompletion) && missingDiscovered.isEmpty()
        val curatorComplete = (collectorRoster.isNotEmpty() || definition.allowEmptyCompletion) && missingOwned.isEmpty()
        val masteryComplete = (masteryRoster.isNotEmpty() || definition.allowEmptyCompletion) && missingMastered.isEmpty()
        return CollectionProgress(definition.collectionId, collectorRoster.size, masteryRoster.size, (collectorRoster intersect discovered).size,
            (collectorRoster intersect owned).size, (masteryRoster intersect mastered).size,
            collectorComplete || (collectorRoster.isNotEmpty() && BadgeRequirement.COLLECTOR in historicalTypes),
            curatorComplete || (collectorRoster.isNotEmpty() && BadgeRequirement.CURATOR in historicalTypes),
            masteryComplete || (masteryRoster.isNotEmpty() && BadgeRequirement.COMPLETIONIST in historicalTypes),
            collectorComplete, curatorComplete, masteryComplete,
            missingDiscovered, missingOwned, missingMastered,
            ownedLevels.filterKeys { it in missingMastered }.maxByOrNull { it.value.maxOrNull() ?: 0 }?.key,
            definition.species.filter { it.isAvailable }.distinctBy { it.creatureId }.map { creature ->
                val levels = ownedLevels[creature.creatureId].orEmpty()
                val evidence = masteryEvidence[creature.creatureId]
                CollectionSpeciesProgress(creature.creatureId, creature.creatureId in discovered,
                    levels.size, levels.maxOrNull(), creature.creatureId in mastered, creature.secretUntilDiscovered,
                    currentLevel99Count = evidence?.currentLevel99Count ?: levels.count { it >= 99 },
                    lifetimeMasteryCount = evidence?.effectiveLifetimeCount ?: 0,
                    firstMasteryAt = evidence?.firstMasteryAt, latestMasteryAt = evidence?.latestMasteryAt,
                    requiredByCollector = creature.creatureId in collectorRoster,
                    requiredByCurator = creature.creatureId in collectorRoster,
                    requiredByCompletionist = creature.creatureId in masteryRoster,
                    sourceId = creature.sourceType.name)
            })
    }
}

data class MilestoneProgress(val exactCount: Int, val currentThreshold: Int?, val nextThreshold: Int?, val progressToNext: Int, val newlyReachedThreshold: Int?)
object MilestoneEngine {
    fun evaluate(count: Int, previousCount: Int = count, thresholds: List<Int> = AchievementBadgeDefinition.DEFAULT_MILESTONES): MilestoneProgress {
        val sorted = thresholds.distinct().filter { it > 0 }.sorted()
        val current = sorted.lastOrNull { it <= count }
        val next = sorted.firstOrNull { it > count }
        val newly = sorted.lastOrNull { it in (previousCount + 1)..count }
        return MilestoneProgress(count, current, next, if (next == null) 0 else count - (current ?: 0), newly)
    }
}

enum class AchievementChangeType { BADGE_NEWLY_EARNED, COUNT_INCREASED, MILESTONE_REACHED, COLLECTION_PROGRESS_CHANGED, CURRENT_ROSTER_COMPLETED, SPECIES_MASTERY_RECORDED, REGIONAL_COMPLETIONIST_COMPLETED, BLUE_COMPLETIONIST_COMPLETED, ALL_WATERS_COMPLETIONIST_COMPLETED }
data class AchievementChange(val type: AchievementChangeType, val badgeId: String? = null, val speciesId: String? = null, val collectionId: String? = null, val previousCount: Int = 0, val exactCount: Int = 0, val milestone: Int? = null)
data class AchievementResult(val eventId: String, val committed: Boolean, val changes: List<AchievementChange>, val pearlCost: Int = 0, val resultingLevel: Int? = null)
