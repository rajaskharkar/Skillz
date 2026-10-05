package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.platform.testTag
import com.kingkharnivore.skillz.utils.shell.ChestFilters
import com.kingkharnivore.skillz.utils.shell.environmentOf
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
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.ScyraParchmentSheet
import com.kingkharnivore.skillz.ui.screen.shell.ux.isActiveChestCreature
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import com.kingkharnivore.skillz.ui.screen.shell.NavigationConsumptionResult
import com.kingkharnivore.skillz.ui.screen.shell.NavigationFailureReason
import com.kingkharnivore.skillz.domain.achievement.Level99AchievementPreview
import com.kingkharnivore.skillz.domain.achievement.BadgeDefinitionResolver
import com.kingkharnivore.skillz.domain.achievement.BadgeRequirement
import java.text.Collator
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShellChestScreen(
    uiState: ShellUiState,
    onReleaseCreaturesByLevel: (String, Map<Int, Int>) -> Unit,
    onLevelUpCreatureByLevel: (String, Int) -> Unit,
    onOpenBlue: () -> Unit,
    onSortOptionSelected: (ChestSortOption) -> Unit,
    onFilterSelected: (ChestFilterOption) -> Unit,
    focusSpeciesId: String? = null,
    focusRequestId: String? = null,
    onFocusResult: (String, NavigationConsumptionResult) -> Unit = { _, _ -> },
    onEnvironmentSelected: (ChestFilterOption) -> Unit = onFilterSelected,
    onClearFilters: () -> Unit = { onEnvironmentSelected(ChestFilterOption.All); onFilterSelected(ChestFilterOption.All) },
    onOpenRed: () -> Unit = {}

) {
    var selectedStack by remember { mutableStateOf<ChestInventoryStackUiModel?>(null) }
    val masteryCounts = uiState.badgeDashboard?.badges?.mapNotNull { badge ->
        BadgeDefinitionResolver.resolve(badge.badgeId).speciesId?.let { it to badge.count }
    }?.toMap().orEmpty()
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val locale = configuration.locales[0]
    val creatureNames = remember(configuration) {
        CreatureCatalog.all.associate { it.creatureId to resources.getString(it.titleRes) }
    }
    val allStacks = remember(uiState.finds, uiState.chestSortOption, masteryCounts, creatureNames, locale) {
        buildChestInventoryStacks(uiState.finds, uiState.chestSortOption, masteryCounts, creatureNames, locale)
    }
    val neededForTrackedBadges = speciesNeededForTrackedBadges(uiState.badgeDashboard?.badges.orEmpty())
    val filters = ChestFilters.fromKeys(
        uiState.chestEnvironment.takeUnless { it == ChestFilterOption.All }?.key, uiState.chestFilter.key
    )
    val stacks = remember(allStacks, filters, neededForTrackedBadges) {
        filterChestInventoryStacks(allStacks, filters, neededForTrackedBadges)
    }
    val totalCreatureCount = stacks.sumOf { it.count }
    LaunchedEffect(focusSpeciesId, allStacks) {
        focusSpeciesId?.let { id ->
            allStacks.filter { it.creatureId == id }.maxWithOrNull(
                compareBy<ChestInventoryStackUiModel> { if (it.level < 99) 1 else 0 }.thenBy { it.level }
            )?.let { selectedStack = it; focusRequestId?.let { requestId -> onFocusResult(requestId, NavigationConsumptionResult.Consumed) } }
                ?: focusRequestId?.let { requestId -> onFocusResult(requestId, NavigationConsumptionResult.Failed(NavigationFailureReason.SPECIES_NOT_FOUND)) }
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
                                        modifier = Modifier.fillMaxWidth()
                )
                FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChestEnvironmentControl(filters.environment, onEnvironmentSelected)
                    ChestFilterControl(filters.progress, onFilterSelected)
                    ChestSortControl(selected = uiState.chestSortOption, onSelected = onSortOptionSelected)
                }
            }
        }

        if (stacks.isEmpty()) {
            if (allStacks.isEmpty()) EmptyChestState(onOpenBlue = onOpenBlue, onOpenRed = onOpenRed, modifier = Modifier.weight(1f))
            else FilteredChestEmptyState(onClearFilters, Modifier.weight(1f),
                filters.progress == ChestFilterOption.NeededForTrackedBadges)
        } else {
            LazyVerticalGrid(
                modifier = Modifier.weight(1f),
                columns = GridCells.Adaptive(112.dp * androidx.compose.ui.platform.LocalDensity.current.fontScale),
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
        val stack = allStacks.firstOrNull { it.creatureId == selected.creatureId && it.level == selected.level }
        if (stack == null) {
            LaunchedEffect(selected) { selectedStack = null }
            return@let
        }
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

internal fun filterChestInventoryStacks(
    stacks: List<ChestInventoryStackUiModel>, filters: ChestFilters, neededSpecies: Set<String> = emptySet()
): List<ChestInventoryStackUiModel> = stacks.filter { stack ->
    val creature = CreatureCatalog.get(stack.creatureId) ?: return@filter false
    filters.environment.matchesEnvironment(creature) && when (filters.progress) {
        ChestFilterOption.ClosestToMastery -> stack.level in 90..98
        ChestFilterOption.Mastered -> stack.level >= CreatureEconomy.MAX_CREATURE_LEVEL
        ChestFilterOption.NotMastered -> stack.level < CreatureEconomy.MAX_CREATURE_LEVEL
        ChestFilterOption.NeededForTrackedBadges -> stack.level < CreatureEconomy.MAX_CREATURE_LEVEL && stack.creatureId in neededSpecies
        else -> true
    }
}

@Composable private fun ChestFilterControl(selected: ChestFilterOption, onSelected: (ChestFilterOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(selected = selected != ChestFilterOption.All, onClick = { expanded = true },
            modifier = Modifier.testTag("chest-progress"),
            label = { Text(stringResource(R.string.chest_progress_selected, stringResource(if (selected == ChestFilterOption.All) R.string.chest_progress_any else selected.labelRes))) })
        DropdownMenu(expanded, { expanded = false }) {
            ChestFilterOption.entries.filter { it == ChestFilterOption.All || it.isProgress }.forEach { option ->
                DropdownMenuItem(text = { Text(stringResource(if (option == ChestFilterOption.All) R.string.chest_progress_any else option.labelRes)) },
                    leadingIcon = { if (option == selected) Icon(Icons.Default.Check, null) },
                    onClick = { expanded = false; onSelected(option) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable private fun ChestEnvironmentControl(selected: ChestFilterOption, onSelected: (ChestFilterOption) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FilterChip(selected = selected != ChestFilterOption.All, onClick = { expanded = true },
        modifier = Modifier.testTag("chest-environment"),
        label = { Text(stringResource(R.string.chest_environment_selected, stringResource(if (selected == ChestFilterOption.All) R.string.chest_environment_all else selected.labelRes))) })
    if (expanded) ScyraParchmentSheet(onDismissRequest = { expanded = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.chest_environment_title), style = MaterialTheme.typography.titleLarge)
            EnvironmentOption(ChestFilterOption.All, selected) { expanded = false; onSelected(it) }
            listOf(
                ChestFilterOption.Sea to listOf(ChestFilterOption.SunlitReef, ChestFilterOption.DeeperReef, ChestFilterOption.OpenBlue, ChestFilterOption.GreatBlue, ChestFilterOption.Fishbowl, ChestFilterOption.Aquarium, ChestFilterOption.Pond, ChestFilterOption.Lake),
                ChestFilterOption.Land to listOf(ChestFilterOption.GoldenFields, ChestFilterOption.AncientWoods, ChestFilterOption.OpenSands, ChestFilterOption.HighPeaks, ChestFilterOption.GreatWild, ChestFilterOption.Pasture, ChestFilterOption.Glade, ChestFilterOption.Oasis, ChestFilterOption.Ravine, ChestFilterOption.Sanctuary),
                ChestFilterOption.Red to listOf(ChestFilterOption.Triassic, ChestFilterOption.Jurassic, ChestFilterOption.Cretaceous)
            ).forEach { (realm, environments) ->
                Text(stringResource(realm.labelRes), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (listOf(realm) + environments).forEach { option -> EnvironmentOption(option, selected) { expanded = false; onSelected(it) } }
                }
            }
        }
    }
}

@Composable private fun EnvironmentOption(option: ChestFilterOption, selected: ChestFilterOption, onSelected: (ChestFilterOption) -> Unit) {
    FilterChip(selected = selected == option, onClick = { onSelected(option) },
        modifier = Modifier.testTag("chest-environment-${option.key}"),
        leadingIcon = if (option == selected) ({ Icon(Icons.Default.Check, null, Modifier.size(18.dp)) }) else null,
        label = { Text(stringResource(if (option == ChestFilterOption.All) R.string.chest_environment_all else option.labelRes)) })
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
    ChestFilterOption.Sea -> R.string.land_realm_sea
    ChestFilterOption.Land -> R.string.land_realm_land
    ChestFilterOption.Red -> R.string.red_title
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
    ChestFilterOption.GoldenFields -> R.string.land_zone_golden_fields
    ChestFilterOption.AncientWoods -> R.string.land_zone_ancient_woods
    ChestFilterOption.OpenSands -> R.string.land_zone_open_sands
    ChestFilterOption.HighPeaks -> R.string.land_zone_high_peaks
    ChestFilterOption.GreatWild -> R.string.land_zone_great_wild
    ChestFilterOption.Pasture -> R.string.land_zone_pasture
    ChestFilterOption.Glade -> R.string.land_zone_glade
    ChestFilterOption.Oasis -> R.string.land_zone_oasis
    ChestFilterOption.Ravine -> R.string.land_zone_ravine
    ChestFilterOption.Sanctuary -> R.string.land_zone_sanctuary
    ChestFilterOption.Triassic -> R.string.red_triassic
    ChestFilterOption.Jurassic -> R.string.red_jurassic
    ChestFilterOption.Cretaceous -> R.string.red_cretaceous
}

internal fun buildChestInventoryStacks(
    finds: List<UserShellFindInstanceEntity>,
    sortOption: ChestSortOption = ChestSortOption.Level,
    masteryCounts: Map<String, Int> = emptyMap(),
    creatureNames: Map<String, String> = emptyMap(),
    locale: java.util.Locale = java.util.Locale.getDefault()
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
                creatureName = creatureNames[key.first] ?: creature?.displayName ?: definitionTitleFallback(definition),
                level = key.second,
                count = creaturesAtLevel.size,
                iconKey = definition.iconKey,
                isStillwaterExclusive = creature?.sourceType in setOf(CreatureSourceType.STILLWATER, CreatureSourceType.RESTORATIVE_LAND),
                totalValuePearls = CreatureEconomy.releaseValuePearls(key.first, key.second) * creaturesAtLevel.size,
                newestAcquiredAtMs = creaturesAtLevel.maxOfOrNull { it.acquiredAt } ?: 0L,
                oldestAcquiredAtMs = creaturesAtLevel.minOfOrNull { it.acquiredAt } ?: 0L,
                recentActivityAtMs = creaturesAtLevel.maxOfOrNull { instance ->
                    maxOf(instance.acquiredAt, instance.lastActivityAt, instance.viewedAt?.takeIf { it > 0 } ?: 0L)
                } ?: 0L,
                speciesMasteryCount = masteryCounts[key.first] ?: 0
            )
        }
        .let { stacks -> sortChestInventoryStacks(stacks, sortOption, locale) }

internal fun sortChestInventoryStacks(
    stacks: List<ChestInventoryStackUiModel>,
    sortOption: ChestSortOption,
    locale: java.util.Locale = java.util.Locale.getDefault()
): List<ChestInventoryStackUiModel> = stacks.sortedWith(chestStackComparator(sortOption, locale))

private fun chestStackComparator(sortOption: ChestSortOption, locale: java.util.Locale): Comparator<ChestInventoryStackUiModel> {
    val collator = Collator.getInstance(locale)
    val localizedName = Comparator<ChestInventoryStackUiModel> { left, right ->
        collator.compare(left.creatureName, right.creatureName)
    }
    val levelNameTieBreakers = compareByDescending<ChestInventoryStackUiModel> { it.level }
        .then(localizedName)
        .thenBy { it.stableStackKey }
    val nameLevelTieBreakers = localizedName
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
private fun EmptyChestState(onOpenBlue: () -> Unit, onOpenRed: () -> Unit, modifier: Modifier = Modifier) {
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
        OutlinedButton(onClick = onOpenRed) { Text(stringResource(R.string.chest_open_red)) }
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
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
    ) {
        Column(Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 60.dp)) {
                ShellObjectIcon(stack.iconKey, Modifier.size(54.dp).align(Alignment.Center))
                if (shouldShowChestCountBadge(stack.count)) ChestBadge(
                    stringResource(R.string.shell_chest_count_badge, stack.count), Modifier.align(Alignment.TopEnd))
            }
            Text(stack.creatureName, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            ChestBadge(stringResource(R.string.shell_creature_level_short, stack.level))
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
    ScyraParchmentSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
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
            CreatureCatalog.get(stack.creatureId)?.let { creature ->
                val realm = when (creature.realm) {
                    com.kingkharnivore.skillz.utils.shell.CreatureRealm.SEA -> R.string.land_realm_sea
                    com.kingkharnivore.skillz.utils.shell.CreatureRealm.LAND -> R.string.land_realm_land
                    com.kingkharnivore.skillz.utils.shell.CreatureRealm.RED -> R.string.red_title
                }
                Text(stringResource(R.string.chest_creature_environment, stringResource(realm), stringResource(environmentOf(creature).labelRes)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    if (preview.completesBlue) Text(creatureCompletionText(stack.creatureId))
                    if (preview.completesAllWaters) Text(creatureCompletionText(stack.creatureId, includesHabitats = true))
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
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(body)
                Text(stringResource(R.string.shell_creature_release_confirm_reward, rewardPearls), fontWeight = FontWeight.SemiBold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, modifier = Modifier.testTag("chest-confirm-release")) { Text(stringResource(R.string.shell_creature_release_action)) }
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

/** Shared Chest filter includes the exact outstanding rosters of named Red mastery badges. */
internal fun speciesNeededForTrackedBadges(badges: List<com.kingkharnivore.skillz.domain.achievement.BadgeProgressModel>): Set<String> {
    val mastered = badges.filter { it.count > 0 }.mapNotNull { BadgeDefinitionResolver.resolve(it.badgeId).speciesId }.toSet()
    val trackedMembers = badges.filter { it.tracked && !it.terminal }.flatMap {
        com.kingkharnivore.skillz.domain.achievement.BadgeBookCollections.byAward[it.badgeId]?.memberIds.orEmpty()
    }.toSet()
    return badges.filter { (it.tracked || it.badgeId in trackedMembers) && !it.terminal }.flatMap { badge ->
        val definition = BadgeDefinitionResolver.resolve(badge.badgeId)
        val red = com.kingkharnivore.skillz.domain.achievement.RedBadgeCatalog.byId[badge.badgeId]
        val land = com.kingkharnivore.skillz.domain.achievement.LandBadgeCatalog.byId[badge.badgeId]
        val landSpecies = com.kingkharnivore.skillz.utils.shell.LandCreatureCatalog.all.filter { it.isAvailable }.map { it.creatureId }.toSet()
        if (land != null) when (land.metric) {
            com.kingkharnivore.skillz.domain.achievement.LandBadgeMetric.LEVEL,
            com.kingkharnivore.skillz.domain.achievement.LandBadgeMetric.MASTERY -> landSpecies.toList()
            com.kingkharnivore.skillz.domain.achievement.LandBadgeMetric.MASTERED_SPECIES -> (landSpecies - mastered).toList()
            com.kingkharnivore.skillz.domain.achievement.LandBadgeMetric.MASTERED_COLLECTIONS ->
                com.kingkharnivore.skillz.domain.achievement.CollectionCatalog.collections
                    .filter { it.collectionId.startsWith("blue_") || it.collectionId.startsWith("stillwater_") }
                    .filter { it.species.any { species -> species.creatureId in landSpecies } && it.species.none { species -> species.creatureId in mastered } }
                    .flatMap { it.species.map { species -> species.creatureId } }
            else -> emptyList()
        }
        else if (red?.mastery == true) (red.species - mastered).toList()
        else if (badge.badgeId in setOf("mastery_first", "mastery_circle", "mastery_variety", "stillwater_mastery"))
            CreatureCatalog.all.filter { it.isAvailable && (badge.badgeId != "stillwater_mastery" || it.sourceType in setOf(CreatureSourceType.STILLWATER, CreatureSourceType.RESTORATIVE_LAND)) }
                .filter { badge.badgeId != "mastery_variety" || it.creatureId !in mastered }.map { it.creatureId }
        else definition.speciesId?.let { listOf(it) } ?: when (definition.requirement) {
            BadgeRequirement.COMPLETIONIST -> badge.collectionProgress?.missingMasteredSpeciesIds.orEmpty().toList()
            BadgeRequirement.CURATOR -> badge.collectionProgress?.speciesStates.orEmpty().filter { it.ownedCount == 0 }.map { it.speciesId }
            BadgeRequirement.COLLECTOR, BadgeRequirement.EXACT_COUNT -> emptyList()
        }
    }.toSet()
}
