package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.green.PlantSpecimenEntity
import com.kingkharnivore.skillz.domain.achievement.BadgeBookCollections
import com.kingkharnivore.skillz.domain.achievement.BadgeProgressModel
import com.kingkharnivore.skillz.domain.green.*
import com.kingkharnivore.skillz.ui.screen.shell.rooms.green.PlantArtwork
import com.kingkharnivore.skillz.utils.shell.*
import java.util.Locale

/** A specimen remains individually selectable, even beside another copy of the same plant. */
internal data class ChestInventoryEntry(
    val key: String,
    val sortValues: ChestInventoryStackUiModel,
    val plant: PlantSpecimenEntity? = null
)

internal fun buildChestEntries(
    stacks: List<ChestInventoryStackUiModel>,
    plants: List<PlantSpecimenEntity>,
    allPlants: List<PlantSpecimenEntity>,
    plantNames: Map<String, String>,
    sort: ChestSortOption,
    locale: Locale = Locale.getDefault()
): List<ChestInventoryEntry> {
    val masteries = allPlants.filter { it.level == 99 }.groupingBy { it.speciesId }.eachCount()
    val entries = stacks.map { ChestInventoryEntry("creature:${it.creatureId}:${it.level}", it) } +
        plants.map { plant ->
            ChestInventoryEntry("plant:${plant.id}", ChestInventoryStackUiModel(
                creatureId = plant.speciesId,
                creatureName = plantNames[plant.speciesId] ?: plant.speciesId,
                level = plant.level, count = 1, iconKey = "",
                // Plants return Drops; the sort key below uses their release amount.
                releaseSortValue = GreenEconomy.releaseReward(plant.investedDrops),
                newestAcquiredAtMs = plant.plantedAt, oldestAcquiredAtMs = plant.plantedAt,
                recentActivityAtMs = plant.lastWateredAt ?: plant.plantedAt,
                speciesMasteryCount = masteries[plant.speciesId] ?: 0
            ), plant)
        }
    val comparator = chestStackComparator(sort, locale)
    return entries.sortedWith(Comparator<ChestInventoryEntry> { left, right -> comparator.compare(left.sortValues, right.sortValues) }
        .thenBy { it.key })
}

internal fun filterChestPlants(
    plants: List<PlantSpecimenEntity>,
    filters: ChestFilters,
    badges: List<BadgeProgressModel> = emptyList(),
    history: List<PlantSpecimenEntity> = plants
): List<PlantSpecimenEntity> {
    val trackedMembers = badges.filter { it.tracked && !it.terminal }
        .flatMap { BadgeBookCollections.byAward[it.badgeId]?.memberIds.orEmpty() }.toSet()
    val goals = badges.filter { (it.tracked || it.badgeId in trackedMembers) && !it.terminal }
        .mapNotNull { GreenBadgeEvaluator.byId[it.badgeId] }
    val evidence = (history + plants).associateBy { it.id }.values.toList()
    val progress = evidence.map { PlantProgress(it.speciesId, it.level) }
    return plants.filter { plant ->
        val species = GreenCatalogue.byId[plant.speciesId] ?: return@filter false
        filters.environment.matchesEnvironment(species) && when (filters.progress) {
            ChestFilterOption.ClosestToMastery -> plant.level in 90..98
            ChestFilterOption.Mastered -> plant.level == 99
            ChestFilterOption.NotMastered -> plant.level < 99
            ChestFilterOption.NeededForTrackedBadges -> plant.level < 99 && goals.any { goal ->
                if (plant.speciesId !in goal.requiredSpecies) false else {
                    val current = GreenBadgeEvaluator.progress(goal, progress)
                    current < goal.target && GreenBadgeEvaluator.progress(goal,
                        progress.toMutableList().apply { this[evidence.indexOfFirst { it.id == plant.id }] = PlantProgress(plant.speciesId, 99) }) > current
                }
            }
            else -> true
        }
    }
}

@Composable
internal fun ChestPlantTile(plant: PlantSpecimenEntity, onClick: () -> Unit) {
    val species = GreenCatalogue.byId[plant.speciesId] ?: return
    val name = stringResource(species.nameRes)
    val description = stringResource(R.string.green_plant_description, name, plant.level,
        stringResource(PlantGrowthStageResolver.resolve(plant.level).nameRes))
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().testTag("chest-plant:${plant.id}").clickable(onClick = onClick)
            .semantics { contentDescription = description; role = Role.Button }
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PlantArtwork(species, plant.level, name, Modifier.size(60.dp))
            Text(name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ChestBadge(stringResource(R.string.shell_creature_level_short, plant.level))
        }
    }
}
