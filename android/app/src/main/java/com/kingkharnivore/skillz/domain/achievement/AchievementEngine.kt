package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.utils.shell.StillwaterCatalog
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.data.model.entity.shell.BadgeCountFloorEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureMasteryEventEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CreatureDiscoveryEntity
import com.kingkharnivore.skillz.data.model.entity.shell.CollectionCompletionEntity
import com.kingkharnivore.skillz.data.model.entity.shell.UserBadgeEntity
import com.kingkharnivore.skillz.data.model.shell.BadgeCategory
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import java.security.MessageDigest

enum class BadgeFamily { SPECIES_MASTERY, MASTERY, COLLECTION, FLOW, SOFT_FLOW, ARC, MOVEMENT, SURGE }
enum class BadgeCountType { ONE_TIME, REPEATABLE }
enum class BadgeGoalType { ONE_TIME, REPEATABLE_MILESTONE, COLLECTION, SPECIES_MASTERY, HISTORICAL_COUNT_ONLY }
enum class BadgeRequirement { EXACT_COUNT, COLLECTOR, CURATOR, COMPLETIONIST }
enum class BadgeVisibility { ALWAYS, AFTER_SPECIES_DISCOVERY, AFTER_EARNED }
enum class AchievementTimestampConfidence { EXACT, ESTIMATED_FROM_ACQUISITION, UNKNOWN }

data class EvidenceTimestamp(
    val timestamp: Long,
    val confidence: AchievementTimestampConfidence
)

data class ScopedAchievementEvidence(
    val speciesIds: Set<String>,
    val discoveries: List<CreatureDiscoveryEntity>,
    val masteries: List<CreatureMasteryEventEntity>
)

object AchievementEvidenceScope {
    fun stillwater(
        discoveries: List<CreatureDiscoveryEntity>,
        masteries: List<CreatureMasteryEventEntity>
    ): ScopedAchievementEvidence {
        val ids = CreatureCatalog.stillwater.mapTo(mutableSetOf()) { it.creatureId }
        return ScopedAchievementEvidence(
            ids,
            discoveries.filter { it.speciesId in ids },
            masteries.filter { it.speciesId in ids }
        )
    }
}

object CollectionCompletionIdentity {
    fun forRoster(collectionId: String, requirement: BadgeRequirement, rosterHash: String): String =
        "$collectionId:${requirement.name}:$rosterHash"
}

/** Pure reconstruction rules for historical dates. A missing event never becomes "now". */
object AchievementTimestampCalculator {
    fun strongestConfidence(
        current: AchievementTimestampConfidence,
        candidate: AchievementTimestampConfidence
    ): AchievementTimestampConfidence {
        fun rank(value: AchievementTimestampConfidence) = when (value) {
            AchievementTimestampConfidence.UNKNOWN -> 0
            AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION -> 1
            AchievementTimestampConfidence.EXACT -> 2
        }
        return if (rank(candidate) > rank(current)) candidate else current
    }
    fun combineConfidence(evidence: List<EvidenceTimestamp?>): AchievementTimestampConfidence = when {
        evidence.any { it == null } -> AchievementTimestampConfidence.UNKNOWN
        evidence.any { it?.confidence == AchievementTimestampConfidence.UNKNOWN } -> AchievementTimestampConfidence.UNKNOWN
        evidence.any { it?.confidence == AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION } ->
            AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION
        else -> AchievementTimestampConfidence.EXACT
    }

    fun completionTimestamp(evidence: List<EvidenceTimestamp?>): EvidenceTimestamp? {
        val timestamp = evidence.mapNotNull { it }.maxOfOrNull { it.timestamp } ?: return null
        val confidence = combineConfidence(evidence)
        return if (confidence == AchievementTimestampConfidence.UNKNOWN) null else EvidenceTimestamp(timestamp, confidence)
    }
    private fun CreatureMasteryEventEntity.evidenceTimestamp(): EvidenceTimestamp? =
        achievedAt.takeIf { it > 0 }?.let { timestamp ->
            EvidenceTimestamp(
                timestamp,
                if (levelUpTransactionId.startsWith("backfill_"))
                    AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION
                else AchievementTimestampConfidence.EXACT
            )
        }

    fun firstMasteryTimestamp(events: List<CreatureMasteryEventEntity>): EvidenceTimestamp? =
        events.mapNotNull { it.evidenceTimestamp() }.minByOrNull { it.timestamp }

    fun masteryThresholdTimestamp(
        events: List<CreatureMasteryEventEntity>,
        threshold: Int
    ): EvidenceTimestamp? {
        if (threshold <= 0 || events.size < threshold || events.any { it.achievedAt <= 0 }) return null
        return events.sortedBy(CreatureMasteryEventEntity::achievedAt).getOrNull(threshold - 1)
            ?.evidenceTimestamp()
    }

    fun masteryVarietyThresholdTimestamp(
        events: List<CreatureMasteryEventEntity>,
        threshold: Int
    ): EvidenceTimestamp? {
        val bySpecies = events.groupBy { it.speciesId }
        if (threshold <= 0 || bySpecies.size < threshold || bySpecies.values.any { firstMasteryTimestamp(it) == null }) return null
        return bySpecies.values.mapNotNull { firstMasteryTimestamp(it) }
            .sortedBy { it.timestamp }.getOrNull(threshold - 1)
    }

    fun discoveryVarietyThresholdTimestamp(
        discoveries: List<CreatureDiscoveryEntity>,
        threshold: Int,
        participatingSpecies: Set<String>? = null
    ): EvidenceTimestamp? = discoveries
        .filter { it.firstDiscoveredAt > 0 && (participatingSpecies == null || it.speciesId in participatingSpecies) }
        .distinctBy { it.speciesId }.sortedBy { it.firstDiscoveredAt }.getOrNull(threshold - 1)
        ?.let { EvidenceTimestamp(it.firstDiscoveredAt, AchievementTimestampConfidence.EXACT) }

    fun acrossTheDepthsTimestamp(
        discoveries: List<CreatureDiscoveryEntity>,
        requiredCollections: List<CollectionDefinition>
    ): EvidenceTimestamp? = requiredCollections.map { collection ->
        val roster = collection.eligibleRoster(BadgeRequirement.COLLECTOR)
        discoveries.filter { it.speciesId in roster && it.firstDiscoveredAt > 0 }.minOfOrNull { it.firstDiscoveredAt }
            ?: return null
    }.maxOrNull()?.let { EvidenceTimestamp(it, AchievementTimestampConfidence.EXACT) }

    fun oneFromEveryWaterTimestamp(
        masteries: List<CreatureMasteryEventEntity>,
        requiredCollections: List<CollectionDefinition>
    ): EvidenceTimestamp? {
        val requirements = requiredCollections.map { collection ->
            val roster = collection.eligibleRoster(BadgeRequirement.COMPLETIONIST)
            firstMasteryTimestamp(masteries.filter { it.speciesId in roster }) ?: return null
        }
        return requirements.maxByOrNull { it.timestamp }?.let { latest ->
            latest.copy(confidence = if (requirements.any { it.confidence == AchievementTimestampConfidence.UNKNOWN })
                AchievementTimestampConfidence.UNKNOWN else latest.confidence)
        }
    }

    fun keeperOfTheBlueTimestamp(
        completions: List<CollectionCompletionEntity>,
        requiredCollectionIds: Set<String>
    ): EvidenceTimestamp? {
        val required = requiredCollectionIds.map { id ->
            completions.filter {
                it.collectionId == id && it.completionType == BadgeRequirement.COLLECTOR.name &&
                    it.completedAt != null && it.completedAt > 0
            }.minByOrNull { it.completedAt ?: Long.MAX_VALUE } ?: return null
        }
        return completionTimestamp(required.map {
            it.completedAt?.let { timestamp -> EvidenceTimestamp(timestamp, it.timestampConfidence) }
        })
    }
}

data class BadgeVisibilityContext(
    val discoveredSpeciesIds: Set<String>,
    val earnedBadgeIds: Set<String>,
    val historicallyMasteredSpeciesIds: Set<String>
)

object BadgeVisibilityEvaluator {
    fun isVisible(definition: AchievementBadgeDefinition, context: BadgeVisibilityContext): Boolean {
        if (BadgeDefinitionResolver.isObsolete(definition.badgeId)) return false
        val species = definition.speciesId?.let(CreatureCatalog::get)
        val hasHistoricalEvidence = definition.badgeId in context.earnedBadgeIds ||
            definition.speciesId in context.historicallyMasteredSpeciesIds ||
            definition.speciesId in context.discoveredSpeciesIds
        if (species?.isAvailable == false) return hasHistoricalEvidence
        return when (definition.visibility) {
            BadgeVisibility.ALWAYS -> true
            BadgeVisibility.AFTER_SPECIES_DISCOVERY -> definition.speciesId in context.discoveredSpeciesIds || hasHistoricalEvidence
            BadgeVisibility.AFTER_EARNED -> hasHistoricalEvidence
        }
    }
}

fun boundedMilestones(totalEligible: Int): List<Int> {
    if (totalEligible <= 0) return emptyList()
    return (AchievementBadgeDefinition.DEFAULT_MILESTONES.filter { it <= totalEligible } + totalEligible)
        .distinct().sorted()
}

data class SpeciesMasteryEvidence(
    val speciesId: String,
    val verifiedIndividualCount: Int,
    val legacyMinimumCount: Int,
    val effectiveLifetimeCount: Int,
    val hasEverBeenMastered: Boolean,
    val currentLevel99Count: Int = 0,
    val firstMasteryAt: Long? = null,
    val latestMasteryAt: Long? = null,
    val timestampConfidence: AchievementTimestampConfidence = AchievementTimestampConfidence.UNKNOWN
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
                val earliest = speciesEvents.filter { it.achievedAt > 0 }.minByOrNull { it.achievedAt }
                SpeciesMasteryEvidence(
                    speciesId = speciesId,
                    verifiedIndividualCount = speciesEvents.size,
                    legacyMinimumCount = floor?.minimumCount ?: 0,
                    effectiveLifetimeCount = effective,
                    hasEverBeenMastered = effective > 0,
                    currentLevel99Count = ownedLevels[speciesId].orEmpty().count { it >= 99 },
                    firstMasteryAt = earliest?.achievedAt,
                    latestMasteryAt = speciesEvents.filter { it.achievedAt > 0 }.maxOfOrNull { it.achievedAt },
                    timestampConfidence = when {
                        earliest == null -> AchievementTimestampConfidence.UNKNOWN
                        earliest.levelUpTransactionId.startsWith("backfill_") -> AchievementTimestampConfidence.ESTIMATED_FROM_ACQUISITION
                        else -> AchievementTimestampConfidence.EXACT
                    }
                )
            }
    }
}

data class AchievementBadgeDefinition(
    val badgeId: String,
    val family: BadgeFamily,
    val countType: BadgeCountType,
    val requirement: BadgeRequirement,
    val milestones: List<Int> = if (countType == BadgeCountType.ONE_TIME) listOf(1) else DEFAULT_MILESTONES,
    val speciesId: String? = null,
    val collectionId: String? = null,
    val visibility: BadgeVisibility = BadgeVisibility.ALWAYS,
    val pinnable: Boolean = true,
    val trackable: Boolean = true,
    val importance: Int = 0,
    val navigationDestination: String? = null,
    val goalType: BadgeGoalType = when {
        speciesId != null -> BadgeGoalType.SPECIES_MASTERY
        requirement in setOf(BadgeRequirement.COLLECTOR, BadgeRequirement.CURATOR, BadgeRequirement.COMPLETIONIST) -> BadgeGoalType.COLLECTION
        countType == BadgeCountType.ONE_TIME -> BadgeGoalType.ONE_TIME
        else -> BadgeGoalType.REPEATABLE_MILESTONE
    }
) {
    companion object { val DEFAULT_MILESTONES = listOf(1, 5, 10, 25, 50, 100, 250, 500, 1_000) }
}

object AchievementBadgeCatalog {
    val definitions: List<AchievementBadgeDefinition> = buildList {
        CreatureCatalog.all.forEach { creature ->
            add(AchievementBadgeDefinition(
                "mastery_species_${creature.creatureId}", BadgeFamily.SPECIES_MASTERY,
                BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT,
                speciesId = creature.creatureId,
                visibility = when {
                    !creature.isAvailable -> BadgeVisibility.AFTER_EARNED
                    creature.secretUntilDiscovered -> BadgeVisibility.AFTER_SPECIES_DISCOVERY
                    else -> BadgeVisibility.ALWAYS
                }
            ))
        }
        add(AchievementBadgeDefinition("mastery_first", BadgeFamily.MASTERY, BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT))
        add(AchievementBadgeDefinition("mastery_circle", BadgeFamily.MASTERY, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT))
        add(AchievementBadgeDefinition("mastery_variety", BadgeFamily.MASTERY, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT,
            milestones = boundedMilestones(CreatureCatalog.all.count { it.isAvailable && it.participatesInCompletionist })))
        add(AchievementBadgeDefinition("variety_collector", BadgeFamily.COLLECTION, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT,
            milestones = boundedMilestones(CreatureCatalog.all.count { it.isAvailable && it.participatesInCollector })))
        add(AchievementBadgeDefinition("stillwater_first_catch", BadgeFamily.COLLECTION, BadgeCountType.ONE_TIME, BadgeRequirement.EXACT_COUNT, collectionId = "collection_stillwater"))
        add(AchievementBadgeDefinition("stillwater_variety", BadgeFamily.COLLECTION, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT,
            milestones = boundedMilestones(CreatureCatalog.stillwater.count { it.isAvailable && it.participatesInCollector }), collectionId = "collection_stillwater"))
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

/** The sole resolver for static, historical, and unknown persisted badge IDs. */
object BadgeDefinitionResolver {
    /** Persisted for migration safety, but belongs to the retired discovery-journal system. */
    private val obsoleteBadgeIds = setOf("badge_discovery")

    fun isObsolete(badgeId: String): Boolean = badgeId in obsoleteBadgeIds

    fun isUserVisible(badgeId: String): Boolean = !isObsolete(badgeId) &&
        resolve(badgeId).goalType != BadgeGoalType.HISTORICAL_COUNT_ONLY

    fun resolve(badgeId: String): AchievementBadgeDefinition {
        AchievementBadgeCatalog.byId[badgeId]?.let { return it }
        val legacy = ShellContentCatalog.badge(badgeId)
        val family = when (legacy?.category) {
            BadgeCategory.FLOW -> BadgeFamily.FLOW
            BadgeCategory.SOFT_FLOW -> BadgeFamily.SOFT_FLOW
            BadgeCategory.ARC -> BadgeFamily.ARC
            BadgeCategory.SURGE -> BadgeFamily.SURGE
            BadgeCategory.DISCOVERY, BadgeCategory.PULSE, null -> BadgeFamily.COLLECTION
        }
        // Historical-only definitions have reliable earned counts but no reconstructable next goal.
        return AchievementBadgeDefinition(
            badgeId = badgeId,
            family = family,
            countType = BadgeCountType.REPEATABLE,
            requirement = BadgeRequirement.EXACT_COUNT,
            milestones = emptyList(),
            trackable = false,
            navigationDestination = "badge_details",
            goalType = BadgeGoalType.HISTORICAL_COUNT_ONLY
        )
    }

    fun allDefinitions(earnedBadges: List<UserBadgeEntity> = emptyList()): List<AchievementBadgeDefinition> =
        (AchievementBadgeCatalog.definitions + earnedBadges.map { resolve(it.badgeId) }).distinctBy { it.badgeId }
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
    val speciesStates: List<CollectionSpeciesProgress> = emptyList(),
    val historicalCompletions: Map<BadgeRequirement, CollectionCompletionEvidence> = emptyMap()
)
data class CollectionCompletionEvidence(
    val completedAt: Long?,
    val timestampConfidence: AchievementTimestampConfidence
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
    val sourceId: String? = null,
    val timestampConfidence: AchievementTimestampConfidence = AchievementTimestampConfidence.UNKNOWN,
    val action: CollectionSpeciesAction = CollectionSpeciesAction.None
)

data class SpecialBadgeRequirement(val collectionId: String, val complete: Boolean)
sealed interface SpecialBadgeProgress {
    val requirements: List<SpecialBadgeRequirement>
    data class AcrossTheDepths(override val requirements: List<SpecialBadgeRequirement>) : SpecialBadgeProgress
    data class OneFromEveryWater(override val requirements: List<SpecialBadgeRequirement>) : SpecialBadgeProgress
    data class KeeperOfTheBlue(override val requirements: List<SpecialBadgeRequirement>) : SpecialBadgeProgress
}

sealed interface CollectionSpeciesAction {
    data class ViewInChest(val speciesId: String) : CollectionSpeciesAction
    data class OpenBlueRegion(val speciesId: String, val collectionId: String) : CollectionSpeciesAction
    data class OpenStillwaterVessel(val speciesId: String, val collectionId: String) : CollectionSpeciesAction
    data class OpenBeyondBlue(val speciesId: String, val collectionId: String) : CollectionSpeciesAction
    data object None : CollectionSpeciesAction
}

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
                    sourceId = creature.sourceType.name,
                    timestampConfidence = evidence?.timestampConfidence ?: AchievementTimestampConfidence.UNKNOWN,
                    action = when {
                        creature.secretUntilDiscovered && creature.creatureId !in discovered -> CollectionSpeciesAction.None
                        levels.isNotEmpty() -> CollectionSpeciesAction.ViewInChest(creature.creatureId)
                        creature.sourceType == CreatureSourceType.STILLWATER -> CollectionSpeciesAction.OpenStillwaterVessel(creature.creatureId, creature.primaryProgressCollectionId)
                        creature.sourceType == CreatureSourceType.BEYOND_BLUE -> CollectionSpeciesAction.OpenBeyondBlue(creature.creatureId, creature.collectionId)
                        else -> CollectionSpeciesAction.OpenBlueRegion(creature.creatureId, creature.collectionId)
                    })
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
