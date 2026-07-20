package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import java.security.MessageDigest

enum class BadgeFamily { SPECIES_MASTERY, MASTERY, COLLECTION, FLOW, SOFT_FLOW, ARC, MOVEMENT, SURGE }
enum class BadgeCountType { ONE_TIME, REPEATABLE }
enum class BadgeRequirement { EXACT_COUNT, COLLECTOR, CURATOR, COMPLETIONIST }

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
        CollectionCatalog.collections.forEach { collection ->
            listOf(BadgeRequirement.COLLECTOR, BadgeRequirement.CURATOR, BadgeRequirement.COMPLETIONIST).forEach { requirement ->
                add(AchievementBadgeDefinition("${collection.collectionId}_${requirement.name.lowercase()}", BadgeFamily.COLLECTION, BadgeCountType.ONE_TIME, requirement, collectionId = collection.collectionId))
            }
        }
        // Existing IDs remain canonical and are evaluated from their existing reliable ledgers.
        listOf("badge_flow_10_min", "badge_flow_30_min", "badge_flow_60_min", "badge_flow_120_min").forEach {
            add(AchievementBadgeDefinition(it, BadgeFamily.FLOW, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT))
        }
    }
    val byId = definitions.associateBy { it.badgeId }
}

data class CollectionDefinition(val collectionId: String, val rosterVersion: Int, val species: List<CreatureDefinition>) {
    val rosterHash: String = MessageDigest.getInstance("SHA-256")
        .digest(species.map { it.creatureId }.sorted().joinToString("\n").toByteArray())
        .joinToString("") { "%02x".format(it) }
}

object CollectionCatalog {
    private val blueRegions = CreatureZone.values().map { zone ->
        CollectionDefinition("blue_${zone.name.lowercase()}", 1, CreatureCatalog.all.filter { it.sourceType != CreatureSourceType.STILLWATER && it.zone == zone })
    }
    val collections = blueRegions + listOf(
        CollectionDefinition("collection_stillwater", 1, CreatureCatalog.stillwater),
        CollectionDefinition("collection_the_blue", 1, CreatureCatalog.all.filter { it.sourceType != CreatureSourceType.STILLWATER }),
        CollectionDefinition("collection_all_waters", 1, CreatureCatalog.all)
    )
    val byId = collections.associateBy { it.collectionId }
}

data class CollectionProgress(
    val collectionId: String,
    val totalParticipatingSpecies: Int,
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
    val closestCurrentCreatureToMastery: String?
)

object CollectionProgressCalculator {
    fun calculate(definition: CollectionDefinition, discovered: Set<String>, ownedLevels: Map<String, List<Int>>, mastered: Set<String>, historicalTypes: Set<BadgeRequirement> = emptySet()): CollectionProgress {
        val collectorRoster = definition.species.filter { it.participatesInCollector && it.isAvailable }.map { it.creatureId }.toSet()
        val masteryRoster = definition.species.filter { it.participatesInCompletionist && it.isAvailable }.map { it.creatureId }.toSet()
        val owned = ownedLevels.filterValues { it.isNotEmpty() }.keys
        val missingDiscovered = collectorRoster - discovered
        val missingOwned = collectorRoster - owned
        val missingMastered = masteryRoster - mastered
        return CollectionProgress(definition.collectionId, collectorRoster.size, (collectorRoster intersect discovered).size,
            (collectorRoster intersect owned).size, (masteryRoster intersect mastered).size,
            missingDiscovered.isEmpty() || BadgeRequirement.COLLECTOR in historicalTypes,
            missingOwned.isEmpty() || BadgeRequirement.CURATOR in historicalTypes,
            missingMastered.isEmpty() || BadgeRequirement.COMPLETIONIST in historicalTypes,
            missingDiscovered.isEmpty(), missingOwned.isEmpty(), missingMastered.isEmpty(),
            missingDiscovered, missingOwned, missingMastered,
            ownedLevels.filterKeys { it in missingMastered }.maxByOrNull { it.value.maxOrNull() ?: 0 }?.key)
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

enum class AchievementChangeType { BADGE_NEWLY_EARNED, COUNT_INCREASED, MILESTONE_REACHED, COLLECTION_PROGRESS_CHANGED, SPECIES_MASTERY_RECORDED, REGIONAL_COMPLETIONIST_COMPLETED, BLUE_COMPLETIONIST_COMPLETED, ALL_WATERS_COMPLETIONIST_COMPLETED }
data class AchievementChange(val type: AchievementChangeType, val badgeId: String? = null, val speciesId: String? = null, val collectionId: String? = null, val previousCount: Int = 0, val exactCount: Int = 0, val milestone: Int? = null)
data class AchievementResult(val eventId: String, val committed: Boolean, val changes: List<AchievementChange>, val pearlCost: Int = 0, val resultingLevel: Int? = null)
