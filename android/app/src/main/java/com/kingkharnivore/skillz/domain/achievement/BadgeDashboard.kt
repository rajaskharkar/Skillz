package com.kingkharnivore.skillz.domain.achievement

import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureStatus

enum class BadgeUiCategory { ALL, FLOW, ARC, CREATURES, MASTERY, COLLECTIONS, STILLWATER, MOVEMENT, SURGE, SPECIAL }
enum class BadgeSort { RECOMMENDED, RECENTLY_EARNED, RECENTLY_ADVANCED, HIGHEST_COUNT, CLOSEST_MILESTONE, ALPHABETICAL }

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
    val action: String = "badges"
)

data class BadgeDashboard(
    val badges: List<BadgeProgressModel>,
    val collections: List<CollectionProgress>,
    val uniqueEarned: Int,
    val totalMasteries: Int,
    val completedCollections: Int,
    val recommendations: List<BadgeProgressModel>
)

object BadgeDashboardCalculator {
    fun calculate(
        earned: List<UserBadgeEntity>,
        instances: List<UserShellFindInstanceEntity>,
        discoveries: List<CreatureDiscoveryEntity>,
        masteries: List<CreatureMasteryEventEntity>,
        completions: List<CollectionCompletionEntity>,
        pins: List<BadgePinEntity>,
        tracking: List<BadgeTrackingEntity>
    ): BadgeDashboard {
        val earnedById = earned.associateBy { it.badgeId }
        val pinOrder = pins.associate { it.badgeId to it.pinOrder }
        val tracked = tracking.map { it.badgeId }.toSet()
        val discovered = discoveries.map { it.speciesId }.toSet()
        val mastered = masteries.map { it.speciesId }.toSet()
        val activeLevels = instances.filter { it.creatureStatus == CreatureStatus.ACTIVE }
            .groupBy({ it.findId }, { it.animalLevel })
        val historical = completions.groupBy { it.collectionId }.mapValues { (_, rows) ->
            rows.mapNotNull { runCatching { BadgeRequirement.valueOf(it.completionType) }.getOrNull() }.toSet()
        }
        val collectionProgress = CollectionCatalog.collections.map {
            CollectionProgressCalculator.calculate(it, discovered, activeLevels, mastered, historical[it.collectionId].orEmpty())
        }
        val collectionById = collectionProgress.associateBy { it.collectionId }
        val definitions = (AchievementBadgeCatalog.definitions + earned.mapNotNull { row ->
            if (AchievementBadgeCatalog.byId.containsKey(row.badgeId)) null else legacyDefinition(row.badgeId)
        }).distinctBy { it.badgeId }
        val badges = definitions.map { definition ->
            val stored = earnedById[definition.badgeId]
            val collection = definition.collectionId?.let(collectionById::get)
            val speciesLevels = definition.speciesId?.let { activeLevels[it].orEmpty() }.orEmpty()
            val computed = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> if (collection?.collectorEarned == true) 1 else 0
                BadgeRequirement.CURATOR -> if (collection?.curatorEarned == true) 1 else 0
                BadgeRequirement.COMPLETIONIST -> if (collection?.completionistEarned == true) 1 else 0
                BadgeRequirement.EXACT_COUNT -> stored?.count ?: when (definition.badgeId) {
                    "mastery_first" -> if (masteries.isEmpty()) 0 else 1
                    "mastery_circle" -> masteries.size
                    "mastery_variety" -> mastered.size
                    "variety_collector" -> discovered.size
                    else -> definition.speciesId?.let { species -> masteries.count { it.speciesId == species } } ?: 0
                }
            }
            val target = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> collection?.totalParticipatingSpecies ?: 1
                BadgeRequirement.CURATOR -> collection?.totalParticipatingSpecies ?: 1
                BadgeRequirement.COMPLETIONIST -> collection?.totalParticipatingSpecies ?: 1
                BadgeRequirement.EXACT_COUNT -> MilestoneEngine.evaluate(computed, thresholds = definition.milestones).nextThreshold ?: computed.coerceAtLeast(1)
            }
            val progress = when (definition.requirement) {
                BadgeRequirement.COLLECTOR -> collection?.discoveredSpeciesCount ?: 0
                BadgeRequirement.CURATOR -> collection?.currentlyOwnedSpeciesCount ?: 0
                BadgeRequirement.COMPLETIONIST -> collection?.masteredSpeciesCount ?: 0
                BadgeRequirement.EXACT_COUNT -> computed
            }
            BadgeProgressModel(definition.badgeId, computed, computed > 0, category(definition), progress,
                target, (target - progress).coerceAtLeast(0), MilestoneEngine.evaluate(computed, thresholds = definition.milestones),
                stored?.firstEarnedAt, stored?.lastEarnedAt, pinOrder[definition.badgeId],
                definition.badgeId in tracked && !(definition.countType == BadgeCountType.ONE_TIME && computed > 0),
                collection, speciesLevels.maxOrNull(), action(definition))
        }
        return BadgeDashboard(badges, collectionProgress, badges.count { it.earned }, masteries.size,
            completions.map { it.collectionId to it.completionType }.distinct().size, RecommendationEngine.recommend(badges))
    }

    private fun legacyDefinition(id: String) = AchievementBadgeDefinition(id, when {
        "arc" in id -> BadgeFamily.ARC
        "surge" in id -> BadgeFamily.SURGE
        "movement" in id -> BadgeFamily.MOVEMENT
        else -> BadgeFamily.FLOW
    }, BadgeCountType.REPEATABLE, BadgeRequirement.EXACT_COUNT)

    private fun category(def: AchievementBadgeDefinition) = when {
        def.speciesId != null -> BadgeUiCategory.MASTERY
        def.collectionId == "collection_stillwater" -> BadgeUiCategory.STILLWATER
        def.collectionId != null -> BadgeUiCategory.COLLECTIONS
        def.family == BadgeFamily.FLOW || def.family == BadgeFamily.SOFT_FLOW -> BadgeUiCategory.FLOW
        def.family == BadgeFamily.ARC -> BadgeUiCategory.ARC
        def.family == BadgeFamily.MOVEMENT -> BadgeUiCategory.MOVEMENT
        def.family == BadgeFamily.SURGE -> BadgeUiCategory.SURGE
        def.family == BadgeFamily.MASTERY -> BadgeUiCategory.MASTERY
        else -> BadgeUiCategory.SPECIAL
    }
    private fun action(def: AchievementBadgeDefinition) = when {
        def.speciesId != null -> "chest"
        def.collectionId == "collection_stillwater" -> "stillwater"
        def.collectionId != null -> "blue"
        def.family == BadgeFamily.FLOW -> "flow"
        def.family == BadgeFamily.ARC -> "arc"
        else -> "badges"
    }
}

object RecommendationEngine {
    fun recommend(badges: List<BadgeProgressModel>, limit: Int = 3): List<BadgeProgressModel> {
        val eligible = badges.filter { it.remaining > 0 && it.target > 0 }
        val result = eligible.filter { it.tracked }.sortedWith(compareBy<BadgeProgressModel> { it.category }.thenBy { it.badgeId }).toMutableList()
        val usedCategories = result.mapTo(mutableSetOf()) { it.category }
        eligible.filterNot { it.tracked }.sortedWith(compareBy<BadgeProgressModel> {
            it.remaining.toDouble() / it.target
        }.thenByDescending { it.progress }.thenBy { it.badgeId }).forEach { candidate ->
            if (result.size >= limit) return@forEach
            if (candidate.category !in usedCategories || eligible.none { it.category !in usedCategories }) {
                result += candidate
                usedCategories += candidate.category
            }
        }
        return result.distinctBy { it.badgeId }.take(limit)
    }
}
