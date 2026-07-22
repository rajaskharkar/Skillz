package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureCatalog
import com.kingkharnivore.skillz.utils.shell.CreatureEconomy
import com.kingkharnivore.skillz.utils.shell.CreatureMasteryTier
import com.kingkharnivore.skillz.utils.shell.CreatureSourceType
import com.kingkharnivore.skillz.utils.shell.ChestSortOption
import com.kingkharnivore.skillz.utils.shell.ChestFilterOption
import com.kingkharnivore.skillz.utils.shell.StillwaterCatalog
import com.kingkharnivore.skillz.utils.shell.StillwaterVessel
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet
import com.kingkharnivore.skillz.ui.screen.shell.ux.isActiveChestCreature
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.domain.achievement.Level99AchievementPreview
import com.kingkharnivore.skillz.domain.achievement.BadgeDefinitionResolver
import com.kingkharnivore.skillz.domain.achievement.BadgeRequirement
import java.util.Locale
import kotlin.math.roundToInt

internal data class ChestInventoryStackUiModel(
    val creatureId: String,
    val creatureName: String,
    val level: Int,
    val count: Int,
    val iconKey: String,
    val isStillwaterExclusive: Boolean = false,
    val totalValuePearls: Int = 0,
    val newestAcquiredAtMs: Long = 0L,
    val oldestAcquiredAtMs: Long = 0L,
    val recentActivityAtMs: Long = 0L,
    val speciesMasteryCount: Int = 0
)

@Composable
fun ShellChestScreen(
    uiState: ShellUiState,
    onReleaseCreaturesByLevel: (String, Map<Int, Int>) -> Unit,
    onLevelUpCreatureByLevel: (String, Int) -> Unit,
    onOpenBlue: () -> Unit,
    onSortOptionSelected: (ChestSortOption) -> Unit,
    onFilterSelected: (ChestFilterOption) -> Unit,
    focusSpeciesId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    var selectedStack by remember { mutableStateOf<ChestInventoryStackUiModel?>(null) }
    val masteryCounts = uiState.badgeDashboard?.badges?.mapNotNull { badge ->
        BadgeDefinitionResolver.resolve(badge.badgeId).speciesId?.let { it to badge.count }
    }?.toMap().orEmpty()
    val allStacks = remember(uiState.finds, uiState.chestSortOption, masteryCounts) {
        buildChestInventoryStacks(uiState.finds, uiState.chestSortOption, masteryCounts)
    }
    val neededForTrackedBadges = uiState.badgeDashboard?.badges?.filter { it.tracked }
        ?.flatMap { badge ->
            val definition = BadgeDefinitionResolver.resolve(badge.badgeId)
            definition.speciesId?.let { listOf(it) } ?: when (definition.requirement) {
                BadgeRequirement.COMPLETIONIST -> badge.collectionProgress?.missingMasteredSpeciesIds.orEmpty().toList()
                BadgeRequirement.CURATOR -> badge.collectionProgress?.speciesStates.orEmpty().filter { it.ownedCount == 0 }.map { it.speciesId }
                BadgeRequirement.COLLECTOR, BadgeRequirement.EXACT_COUNT -> emptyList()
            }
        }?.toSet().orEmpty()
    val stacks = remember(allStacks, uiState.chestFilter, neededForTrackedBadges) {
        allStacks.filter { stack -> when (uiState.chestFilter) {
            ChestFilterOption.All -> true
            ChestFilterOption.ClosestToMastery -> stack.level >= 90
            ChestFilterOption.Mastered -> stack.level >= 99
            ChestFilterOption.NotMastered -> stack.level < 99
            ChestFilterOption.NeededForTrackedBadges -> stack.creatureId in neededForTrackedBadges
            ChestFilterOption.SunlitReef -> CreatureCatalog.get(stack.creatureId)?.zone == CreatureZone.SUNLIT_REEF
            ChestFilterOption.DeeperReef -> CreatureCatalog.get(stack.creatureId)?.zone == CreatureZone.DEEPER_REEF
            ChestFilterOption.OpenBlue -> CreatureCatalog.get(stack.creatureId)?.zone == CreatureZone.OPEN_BLUE
            ChestFilterOption.GreatBlue -> CreatureCatalog.get(stack.creatureId)?.zone == CreatureZone.GREAT_BLUE
            ChestFilterOption.Fishbowl -> StillwaterCatalog.byId[stack.creatureId]?.vessel == StillwaterVessel.FISHBOWL
            ChestFilterOption.Aquarium -> StillwaterCatalog.byId[stack.creatureId]?.vessel == StillwaterVessel.AQUARIUM
            ChestFilterOption.Pond -> StillwaterCatalog.byId[stack.creatureId]?.vessel == StillwaterVessel.POND
            ChestFilterOption.Lake -> StillwaterCatalog.byId[stack.creatureId]?.vessel == StillwaterVessel.LAKE
        } }
    }
    val totalCreatureCount = stacks.sumOf { it.count }
    LaunchedEffect(focusSpeciesId, allStacks) {
        focusSpeciesId?.let { id ->
            allStacks.filter { it.creatureId == id }.maxWithOrNull(
                compareBy<ChestInventoryStackUiModel> { if (it.level < 99) 1 else 0 }.thenBy { it.level }
            )?.let { selectedStack = it; onFocusConsumed() }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoomHeader(title = R.string.shell_chest_title, body = R.string.shell_chest_body)
        if (allStacks.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_chest_inventory_stats, totalCreatureCount, stacks.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChestSortControl(selected = uiState.chestSortOption, onSelected = onSortOptionSelected)
                    ChestFilterControl(uiState.chestFilter, onFilterSelected)
                }
            }
        }

        if (stacks.isEmpty()) {
            if (allStacks.isEmpty()) EmptyChestState(onOpenBlue = onOpenBlue, modifier = Modifier.weight(1f))
            else FilteredChestEmptyState({ onFilterSelected(ChestFilterOption.All) }, Modifier.weight(1f),
                uiState.chestFilter == ChestFilterOption.NeededForTrackedBadges)
        } else {
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = GridCells.Adaptive(104.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = stacks,
                    key = { stack -> "${stack.creatureId}-${stack.level}" }
                ) { stack ->
                    ChestInventoryTile(
                        stack = stack,
                        onClick = { selectedStack = stack }
                    )
                }
            }
        }
    }

    selectedStack?.let { selected ->
        val stack = stacks.firstOrNull { it.creatureId == selected.creatureId && it.level == selected.level } ?: selected
        ChestStackDetailSheet(
            stack = stack,
            onDismiss = { selectedStack = null },
            pearlBalance = uiState.pearlBalance,
            level99Preview = uiState.badgeDashboard?.level99Previews?.get(stack.creatureId)?.takeIf { stack.level == 98 },
            onLevelUp = {
                onLevelUpCreatureByLevel(stack.creatureId, stack.level)
                selectedStack = null
            },
            onRelease = { releaseCount ->
                val safeCount = releaseCount.coerceIn(1, stack.count.coerceAtLeast(1))
                onReleaseCreaturesByLevel(stack.creatureId, chestReleaseSelection(stack, safeCount))
                selectedStack = null
            }
        )
    }
}

@Composable private fun ChestFilterControl(selected: ChestFilterOption, onSelected: (ChestFilterOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box { FilterChip(selected = selected != ChestFilterOption.All, onClick = { expanded = true }, label = { Text(stringResource(selected.labelRes)) }); DropdownMenu(expanded, { expanded = false }) { ChestFilterOption.entries.forEach { option -> DropdownMenuItem(text = { Text(stringResource(option.labelRes)) }, onClick = { expanded = false; onSelected(option) }) } } }
}

@Composable private fun FilteredChestEmptyState(onClear: () -> Unit, modifier: Modifier = Modifier, tracked: Boolean = false) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Text(stringResource(if (tracked) R.string.chest_filter_tracked_empty else R.string.chest_filter_empty), style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = onClear) { Text(stringResource(R.string.chest_filter_clear)) }
    }
}

private val ChestFilterOption.labelRes: Int get() = when(this) {
    ChestFilterOption.All -> R.string.chest_filter_all
    ChestFilterOption.ClosestToMastery -> R.string.chest_filter_closest
    ChestFilterOption.Mastered -> R.string.chest_filter_mastered
    ChestFilterOption.NotMastered -> R.string.chest_filter_not_mastered
    ChestFilterOption.NeededForTrackedBadges -> R.string.chest_filter_needed_tracked
    ChestFilterOption.SunlitReef -> R.string.collection_sunlit_reef
    ChestFilterOption.DeeperReef -> R.string.collection_deeper_reef
    ChestFilterOption.OpenBlue -> R.string.collection_open_blue
    ChestFilterOption.GreatBlue -> R.string.collection_great_blue
    ChestFilterOption.Fishbowl -> R.string.collection_fishbowl
    ChestFilterOption.Aquarium -> R.string.collection_aquarium
    ChestFilterOption.Pond -> R.string.collection_pond
    ChestFilterOption.Lake -> R.string.collection_lake
}

internal fun buildChestInventoryStacks(
    finds: List<UserShellFindInstanceEntity>,
    sortOption: ChestSortOption = ChestSortOption.Level,
    masteryCounts: Map<String, Int> = emptyMap()
): List<ChestInventoryStackUiModel> =
    finds
        .asSequence()
        .filter(::isActiveChestCreature)
        .groupBy { instance -> instance.findId to instance.animalLevel.coerceAtLeast(1) }
        .mapNotNull { (key, creaturesAtLevel) ->
            val definition = ShellContentCatalog.find(key.first) ?: return@mapNotNull null
            val creature = CreatureCatalog.get(key.first)
            ChestInventoryStackUiModel(
                creatureId = key.first,
                creatureName = creature?.displayName ?: definitionTitleFallback(definition),
                level = key.second,
                count = creaturesAtLevel.size,
                iconKey = definition.iconKey,
                isStillwaterExclusive = creature?.sourceType == CreatureSourceType.STILLWATER,
                totalValuePearls = CreatureEconomy.releaseValuePearls(key.first, key.second) * creaturesAtLevel.size,
                newestAcquiredAtMs = creaturesAtLevel.maxOfOrNull { it.acquiredAt } ?: 0L,
                oldestAcquiredAtMs = creaturesAtLevel.minOfOrNull { it.acquiredAt } ?: 0L,
                recentActivityAtMs = creaturesAtLevel.maxOfOrNull { instance ->
                    maxOf(instance.acquiredAt, instance.lastActivityAt)
                } ?: 0L,
                speciesMasteryCount = masteryCounts[key.first] ?: 0
            )
        }
        .let { stacks -> sortChestInventoryStacks(stacks, sortOption) }

internal fun sortChestInventoryStacks(
    stacks: List<ChestInventoryStackUiModel>,
    sortOption: ChestSortOption
): List<ChestInventoryStackUiModel> = stacks.sortedWith(chestStackComparator(sortOption))

private fun chestStackComparator(sortOption: ChestSortOption): Comparator<ChestInventoryStackUiModel> {
    val levelNameTieBreakers = compareByDescending<ChestInventoryStackUiModel> { it.level }
        .thenBy { it.creatureName.lowercase(Locale.ROOT) }
        .thenBy { it.stableStackKey }
    val nameLevelTieBreakers = compareBy<ChestInventoryStackUiModel> { it.creatureName.lowercase(Locale.ROOT) }
        .thenByDescending { it.level }
        .thenBy { it.stableStackKey }

    return when (sortOption) {
        ChestSortOption.Level -> levelNameTieBreakers
        ChestSortOption.Recent -> compareByDescending<ChestInventoryStackUiModel> { it.recentActivityAtMs }
            .then(levelNameTieBreakers)
        ChestSortOption.NewestArrival -> compareByDescending<ChestInventoryStackUiModel> { it.newestAcquiredAtMs }
            .then(levelNameTieBreakers)
        ChestSortOption.OldestArrival -> compareBy<ChestInventoryStackUiModel> { it.oldestAcquiredAtMs }
            .then(levelNameTieBreakers)
        ChestSortOption.Alphabetical -> nameLevelTieBreakers
        ChestSortOption.Value -> compareByDescending<ChestInventoryStackUiModel> { it.totalValuePearls }
            .then(nameLevelTieBreakers)
        ChestSortOption.Count -> compareByDescending<ChestInventoryStackUiModel> { it.count }
            .then(nameLevelTieBreakers)
        ChestSortOption.ClosestToMastery -> compareBy<ChestInventoryStackUiModel> { (99 - it.level).coerceAtLeast(0) }
            .then(nameLevelTieBreakers)
        ChestSortOption.SpeciesMasteryCount -> compareByDescending<ChestInventoryStackUiModel> { it.speciesMasteryCount }
            .then(levelNameTieBreakers)
    }
}

private val ChestInventoryStackUiModel.stableStackKey: String
    get() = "$creatureId-$level"

private fun definitionTitleFallback(definition: ShellFindDefinition): String = definition.findId
    .removePrefix("creature_")
    .removePrefix("focus_")
    .split('_')
    .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase() } }


@Composable
private fun ChestSortControl(
    selected: ChestSortOption,
    onSelected: (ChestSortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = true,
            onClick = { expanded = true },
            label = { Text(stringResource(R.string.shell_chest_sort_selected, stringResource(selected.labelRes))) },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondary
            )
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ChestSortOption.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    leadingIcon = {
                        if (option == selected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        if (option != selected) {
                            onSelected(option)
                        }
                    }
                )
            }
        }
    }
}

private val ChestSortOption.labelRes: Int
    get() = when (this) {
        ChestSortOption.Level -> R.string.shell_chest_sort_level
        ChestSortOption.Recent -> R.string.shell_chest_sort_recent
        ChestSortOption.NewestArrival -> R.string.shell_chest_sort_newest_arrival
        ChestSortOption.OldestArrival -> R.string.shell_chest_sort_oldest_arrival
        ChestSortOption.Alphabetical -> R.string.shell_chest_sort_alphabetical
        ChestSortOption.Value -> R.string.shell_chest_sort_value
        ChestSortOption.Count -> R.string.shell_chest_sort_count
        ChestSortOption.ClosestToMastery -> R.string.shell_chest_sort_closest_mastery
        ChestSortOption.SpeciesMasteryCount -> R.string.shell_chest_sort_species_mastery
    }

@Composable
private fun EmptyChestState(onOpenBlue: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.shell_chest_empty_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.shell_chest_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
        )
        Button(onClick = onOpenBlue) {
            Text(stringResource(R.string.shell_chest_empty_action))
        }
    }
}

@Composable
private fun ChestInventoryTile(stack: ChestInventoryStackUiModel, onClick: () -> Unit) {
    val description = stringResource(
        R.string.shell_chest_stack_a11y,
        stack.creatureName,
        stack.level,
        stack.count
    )
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .size(104.dp)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Box(Modifier.fillMaxSize().padding(8.dp)) {
            if (shouldShowChestCountBadge(stack.count)) {
                ChestBadge(
                    text = stringResource(R.string.shell_chest_count_badge, stack.count),
                    modifier = Modifier.align(Alignment.TopEnd)
                )
            }
            ShellObjectIcon(
                iconKey = stack.iconKey,
                modifier = Modifier.size(54.dp).align(Alignment.Center)
            )
            ChestBadge(
                text = when {
                    stack.level >= 99 -> stringResource(R.string.chest_mastered)
                    stack.level == 98 -> stringResource(R.string.chest_one_to_mastery)
                    stack.level >= 95 -> stringResource(R.string.chest_near_mastery)
                    stack.level >= 90 -> stringResource(R.string.chest_levels_to_mastery, 99 - stack.level)
                    else -> stringResource(R.string.shell_creature_level_short, stack.level)
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun ChestBadge(text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChestStackDetailSheet(
    stack: ChestInventoryStackUiModel,
    pearlBalance: Int,
    level99Preview: Level99AchievementPreview?,
    onDismiss: () -> Unit,
    onLevelUp: () -> Unit,
    onRelease: (Int) -> Unit
) {
    var releaseCount by remember(stack.creatureId, stack.level, stack.count) { mutableIntStateOf(1) }
    var showReleaseConfirmation by remember(stack.creatureId, stack.level) { mutableStateOf(false) }
    var showLevelUpConfirmation by remember(stack.creatureId, stack.level) { mutableStateOf(false) }
    val safeReleaseCount = releaseCount.coerceIn(1, stack.count.coerceAtLeast(1))
    val releaseRewardPearls = chestReleaseRewardPearls(stack, safeReleaseCount)
    val rewardPreviewDescription = stringResource(
        R.string.shell_chest_release_reward_preview_a11y,
        safeReleaseCount,
        stack.level,
        stack.creatureName,
        stack.count,
        releaseRewardPearls
    )
    val releaseButtonDescription = stringResource(R.string.shell_chest_release_button_a11y)
    val levelUpCost = CreatureEconomy.growthCostPearls(stack.creatureId, stack.level)
    val isMaxLevel = stack.level >= CreatureEconomy.MAX_CREATURE_LEVEL
    val levelUpShortfall = (levelUpCost - pearlBalance).coerceAtLeast(0)
    val canAffordLevelUp = pearlBalance >= levelUpCost
    val levelUpStatus = when {
        isMaxLevel -> stringResource(R.string.shell_creature_level_up_unavailable_max)
        !canAffordLevelUp -> stringResource(
            R.string.shell_creature_level_up_unavailable_pearls,
            levelUpCost,
            levelUpShortfall
        )
        else -> stringResource(R.string.shell_creature_level_up_cost, levelUpCost)
    }
    val levelUpButtonDescription = when {
        isMaxLevel -> stringResource(R.string.shell_creature_level_up_mastered_a11y, stack.creatureName)
        !canAffordLevelUp -> stringResource(
            R.string.shell_creature_level_up_unavailable_pearls,
            levelUpCost,
            levelUpShortfall
        )
        else -> stringResource(
            R.string.shell_creature_level_up_a11y,
            stack.level,
            stack.creatureName,
            levelUpCost
        )
    }
    ScyraParchmentSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShellObjectIcon(stack.iconKey, Modifier.size(64.dp))
            Text(
                text = stack.creatureName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(stringResource(R.string.shell_chest_detail_level, stack.level))
            CreatureEconomy.creatureMasteryTier(stack.level)?.let { masteryTier ->
                Text(
                    text = stringResource(masteryTier.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(stringResource(R.string.shell_chest_detail_owned, stack.count))
            Text(stringResource(R.string.shell_chest_detail_source), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Text(
                text = stringResource(R.string.shell_creature_level_up),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(levelUpStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val levelButtonModifier = Modifier.fillMaxWidth().semantics { contentDescription = levelUpButtonDescription }
            if (canAffordLevelUp) {
                Button(onClick = { showLevelUpConfirmation = true }, enabled = !isMaxLevel, modifier = levelButtonModifier) {
                    Text(stringResource(R.string.shell_creature_level_up))
                }
            } else {
                OutlinedButton(onClick = { showLevelUpConfirmation = true }, enabled = !isMaxLevel, modifier = levelButtonModifier) {
                    Text(stringResource(R.string.shell_creature_view_requirements))
                }
            }
            Text(
                text = stringResource(R.string.shell_creature_release_action),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (stack.count > 1) {
                Text(
                    text = stringResource(R.string.shell_creature_release_selected_total, safeReleaseCount, stack.count),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = safeReleaseCount.toFloat(),
                    onValueChange = { value ->
                        releaseCount = value.roundToInt().coerceIn(1, stack.count)
                    },
                    valueRange = 1f..stack.count.toFloat(),
                    steps = (stack.count - 2).coerceAtLeast(0)
                )
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = rewardPreviewDescription }
            ) {
                Text(
                    text = stringResource(R.string.shell_creature_release_reward_preview, releaseRewardPearls),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_close))
                }
                Button(
                    onClick = { showReleaseConfirmation = true },
                    enabled = true,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { contentDescription = releaseButtonDescription }
                ) {
                    Text(stringResource(R.string.shell_creature_release_action))
                }
            }
        }
    }

    if (showLevelUpConfirmation) {
        ChestLevelUpConfirmationDialog(
            stack = stack,
            cost = levelUpCost,
            pearlBalance = pearlBalance,
            preview = level99Preview,
            canAfford = canAffordLevelUp,
            onDismiss = { showLevelUpConfirmation = false },
            onConfirm = {
                showLevelUpConfirmation = false
                onLevelUp()
            }
        )
    }

    if (showReleaseConfirmation) {
        ChestReleaseConfirmationDialog(
            stack = stack,
            releaseCount = safeReleaseCount,
            rewardPearls = releaseRewardPearls,
            onDismiss = { showReleaseConfirmation = false },
            onConfirm = {
                showReleaseConfirmation = false
                onRelease(safeReleaseCount)
            }
        )
    }
}

@Composable
private fun ChestLevelUpConfirmationDialog(
    stack: ChestInventoryStackUiModel,
    cost: Int,
    pearlBalance: Int,
    preview: Level99AchievementPreview?,
    canAfford: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val description = stringResource(
        R.string.shell_creature_level_up_confirm_a11y,
        stack.level,
        stack.creatureName,
        cost
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.shell_creature_level_up_confirm_title, stack.creatureName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.shell_creature_level_up_confirm_body, stack.level, stack.creatureName))
                Text(stringResource(R.string.shell_creature_level_up_confirm_cost, cost), fontWeight = FontWeight.SemiBold)
                if (preview != null) {
                    Text(stringResource(R.string.mastery_level_transition), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.level99_preview_balance, cost, pearlBalance, (cost - pearlBalance).coerceAtLeast(0)))
                    Text(stringResource(R.string.level99_preview_species_count, preview.resultingSpeciesMasteryCount))
                    if (preview.firstSpeciesMastery) Text(stringResource(R.string.level99_preview_first_species))
                    Text(stringResource(R.string.level99_preview_region, preview.regionalMasteredAfter, preview.regionalTotal))
                    preview.stillwaterMasteredAfter?.let { mastered -> Text(stringResource(R.string.level99_preview_stillwater, mastered, preview.stillwaterTotal ?: 0)) }
                    if (preview.completesStillwater) Text(stringResource(R.string.level99_preview_completes_stillwater))
                    if (preview.restoresStillwaterRoster) Text(stringResource(R.string.level99_preview_restores_stillwater))
                    if (preview.completesRegion) Text(stringResource(R.string.level99_preview_completes_region))
                    if (preview.completesBlue) Text(stringResource(R.string.level99_preview_completes_blue))
                    if (preview.completesAllWaters) Text(stringResource(R.string.level99_preview_completes_all))
                    if (preview.restoresRegionRoster || preview.restoresBlueRoster || preview.restoresAllWatersRoster) {
                        Text(stringResource(R.string.level99_preview_restores_roster))
                    }
                    preview.milestones.firstOrNull()?.let { Text(stringResource(R.string.level99_preview_milestone, it)) }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = canAfford) { Text(stringResource(R.string.shell_creature_level_up)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
        modifier = Modifier.semantics { contentDescription = description }
    )
}

@Composable
private fun ChestReleaseConfirmationDialog(
    stack: ChestInventoryStackUiModel,
    releaseCount: Int,
    rewardPearls: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val creatureName = stack.creatureName
    val body = if (releaseCount == 1) {
        stringResource(R.string.shell_creature_release_confirm_single_body, stack.level, stack.creatureName)
    } else {
        stringResource(R.string.shell_creature_release_confirm_bulk_body, releaseCount, stack.level, creatureName)
    }
    val description = stringResource(
        R.string.shell_creature_release_confirm_a11y,
        releaseCount,
        stack.level,
        creatureName,
        rewardPearls
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.shell_creature_release_confirm_title, stack.creatureName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body)
                Text(stringResource(R.string.shell_creature_release_confirm_reward, rewardPearls), fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text(stringResource(R.string.shell_creature_release_action)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) }
        },
        modifier = Modifier.semantics { contentDescription = description }
    )
}


private val CreatureMasteryTier.titleRes: Int
    get() = when (this) {
        CreatureMasteryTier.SEASONED -> R.string.shell_creature_mastery_seasoned
        CreatureMasteryTier.PROVEN -> R.string.shell_creature_mastery_proven
        CreatureMasteryTier.VETERAN -> R.string.shell_creature_mastery_veteran
        CreatureMasteryTier.ASCENDANT -> R.string.shell_creature_mastery_ascendant
        CreatureMasteryTier.MASTERED -> R.string.shell_creature_mastery_mastered
    }

internal fun chestReleaseSelection(stack: ChestInventoryStackUiModel, requestedCount: Int): Map<Int, Int> =
    mapOf(stack.level to requestedCount.coerceIn(1, stack.count.coerceAtLeast(1)))

internal fun chestReleaseRewardPearls(stack: ChestInventoryStackUiModel, requestedCount: Int): Int =
    CreatureEconomy.releaseValuePearls(stack.creatureId, stack.level) *
        requestedCount.coerceIn(1, stack.count.coerceAtLeast(1))

internal fun shouldShowChestCountBadge(count: Int): Boolean = count > 1


@Composable
fun ShellMetricPill(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}
