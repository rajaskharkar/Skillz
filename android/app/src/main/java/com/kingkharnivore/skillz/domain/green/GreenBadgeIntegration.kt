package com.kingkharnivore.skillz.domain.green

import com.kingkharnivore.skillz.domain.achievement.*
import com.kingkharnivore.skillz.data.model.entity.green.*
import com.kingkharnivore.skillz.data.model.entity.shell.*

/** Botanical criteria stay in Green; the common badge hub owns display, pins and tracking. */
fun GreenBadgeDefinition.achievementDefinition() = AchievementBadgeDefinition(
    id, if (requiredLevel == 99) BadgeFamily.MASTERY else BadgeFamily.COLLECTION,
    if (speciesId != null) BadgeCountType.REPEATABLE else BadgeCountType.ONE_TIME,
    BadgeRequirement.EXACT_COUNT,
    milestones = if (speciesId != null) listOf(1, 2, 3, 5, 10, 25, 50, 100) else listOf(target)
)

/** Shared completion rule for the dashboard and automatic tracking cleanup. */
fun GreenBadgeDefinition.isTerminal(count: Int): Boolean = count > 0 &&
    (speciesId == null || achievementDefinition().milestones.none { it > count })

fun GreenBadgeDefinition.dashboardModel(
    plants: List<PlantSpecimenEntity>, award: GreenBadgeAwardEntity?, stored: UserBadgeEntity?,
    pin: Int?, tracked: Boolean
): BadgeProgressModel {
    val progress = if (speciesId != null) maxOf(stored?.count ?: 0, if (award != null) 1 else 0, plants.count { it.speciesId == speciesId && it.level == 99 })
        else GreenBadgeEvaluator.progress(this, plants.map { PlantProgress(it.speciesId, it.level) })
    val earned = award != null || (stored?.count ?: 0) > 0 || progress >= target
    val count = maxOf(stored?.count ?: 0, if (speciesId != null) progress else if (earned) 1 else 0)
    val definition = achievementDefinition()
    val next = if (speciesId != null) definition.milestones.firstOrNull { it > count } else null
    val terminal = earned && isTerminal(count)
    val objective = next ?: if (speciesId != null) definition.milestones.last() else target
    val eligiblePlants = plants.filter { it.speciesId in requiredSpecies }
    val qualified = eligiblePlants.filter { it.level >= requiredLevel }.groupingBy { it.speciesId }.eachCount()
    val candidates = when (metric) {
        GreenBadgeMetric.SPECIES -> requiredSpecies.filter { it !in qualified }
        GreenBadgeMetric.ENVIRONMENTS -> {
            val completeEnvironments = qualified.keys.mapNotNull(GreenCatalogue.byId::get).groupingBy { it.environment }
                .eachCount().filterValues { it >= copiesPerSpecies }.keys
            requiredSpecies.filter { it !in qualified && GreenCatalogue.byId[it]?.environment !in completeEnvironments }
        }
        GreenBadgeMetric.ARCHETYPES -> {
            val completeArchetypes = qualified.keys.mapNotNull { GreenCatalogue.byId[it]?.archetype }.toSet()
            requiredSpecies.filter { GreenCatalogue.byId[it]?.archetype !in completeArchetypes }
        }
        GreenBadgeMetric.SAME_SPECIES_COPIES -> listOfNotNull(qualified.maxByOrNull { it.value }?.key
            ?: eligiblePlants.maxByOrNull { it.level }?.speciesId ?: requiredSpecies.firstOrNull())
        GreenBadgeMetric.SPECIES_FAMILIES -> requiredSpecies.filter { (qualified[it] ?: 0) < copiesPerSpecies }
            .sortedByDescending { qualified[it] ?: 0 }
        else -> requiredSpecies.toList()
    }.let { if (speciesId != null) listOf(speciesId) else it }
    val growing = eligiblePlants.filter { it.speciesId in candidates && it.level <
        (if (metric == GreenBadgeMetric.WATERINGS) 99 else requiredLevel) }.maxByOrNull { it.level }
    val targetId = growing?.speciesId ?: candidates.firstOrNull() ?: requiredSpecies.firstOrNull()
    val seed = growing == null
    return BadgeProgressModel(id, count, earned, BadgeUiCategory.GREEN, progress, objective,
        (objective - progress).coerceAtLeast(0), MilestoneEngine.evaluate(progress, thresholds = definition.milestones),
        firstEarnedAt = stored?.firstEarnedAt ?: award?.awardedAt,
        lastAdvancedAt = stored?.lastEarnedAt ?: award?.awardedAt,
        timestampConfidence = stored?.timestampConfidence ?: if (award != null) AchievementTimestampConfidence.EXACT else AchievementTimestampConfidence.UNKNOWN,
        pinnedOrder = pin, tracked = tracked,
        action = BadgeActionDestination.Green(environment?.id, targetId, seed, growing?.id),
        newlyEarned = stored?.viewedAt == null && stored?.isNew == true,
        canTrack = !terminal, canNavigate = true, canProgressNow = !terminal,
        disabledReason = if (terminal) BadgeDisabledReason.COMPLETE else null,
        goalType = definition.goalType, countType = definition.countType, terminal = terminal, nextMilestoneTarget = next)
}
