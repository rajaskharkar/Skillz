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
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellDepthTier
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.isActiveChestCreature
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import kotlin.math.roundToInt

internal data class ChestInventoryStackUiModel(
    val creatureId: String,
    val creatureName: String,
    val level: Int,
    val count: Int,
    val iconKey: String,
    val rarityLabel: String,
    val sortOrder: Int
)

@Composable
fun ShellChestScreen(
    uiState: ShellUiState,
    onReleaseCreaturesByLevel: (String, Map<Int, Int>) -> Unit,
    onOpenBlue: () -> Unit
) {
    var selectedStack by remember { mutableStateOf<ChestInventoryStackUiModel?>(null) }
    val stacks = remember(uiState.finds) { buildChestInventoryStacks(uiState.finds) }
    val totalCreatureCount = stacks.sumOf { it.count }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoomHeader(title = R.string.shell_chest_title, body = R.string.shell_chest_body)
        if (stacks.isNotEmpty()) {
            Text(
                text = stringResource(R.string.shell_chest_inventory_stats, totalCreatureCount, stacks.size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (stacks.isEmpty()) {
            EmptyChestState(onOpenBlue = onOpenBlue, modifier = Modifier.weight(1f))
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
            onRelease = { releaseCount ->
                val safeCount = releaseCount.coerceIn(1, stack.count.coerceAtLeast(1))
                onReleaseCreaturesByLevel(stack.creatureId, chestReleaseSelection(stack, safeCount))
                selectedStack = null
            }
        )
    }
}

internal fun buildChestInventoryStacks(finds: List<UserShellFindInstanceEntity>): List<ChestInventoryStackUiModel> =
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
                rarityLabel = rarityLabel(definition.depthTier),
                sortOrder = raritySortOrder(definition.depthTier)
            )
        }
        .sortedWith(
            compareByDescending<ChestInventoryStackUiModel> { it.sortOrder }
                .thenBy { it.creatureName }
                .thenByDescending { it.level }
        )

private fun definitionTitleFallback(definition: ShellFindDefinition): String = definition.findId
    .removePrefix("creature_")
    .removePrefix("focus_")
    .split('_')
    .joinToString(" ") { it.replaceFirstChar { char -> char.titlecase() } }

private fun rarityLabel(depthTier: ShellDepthTier?): String = when (depthTier) {
    ShellDepthTier.DEEP_OCEAN -> "Legendary"
    ShellDepthTier.OPEN_BLUE -> "Epic"
    ShellDepthTier.DEEPER_REEF -> "Rare"
    ShellDepthTier.REEF -> "Common"
    null -> "Common"
}

private fun raritySortOrder(depthTier: ShellDepthTier?): Int = when (depthTier) {
    ShellDepthTier.DEEP_OCEAN -> 4
    ShellDepthTier.OPEN_BLUE -> 3
    ShellDepthTier.DEEPER_REEF -> 2
    ShellDepthTier.REEF -> 1
    null -> 0
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
                text = stringResource(R.string.shell_creature_level_short, stack.level),
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
    onDismiss: () -> Unit,
    onRelease: (Int) -> Unit
) {
    var releaseCount by remember(stack.creatureId, stack.level, stack.count) { mutableIntStateOf(1) }
    val safeReleaseCount = releaseCount.coerceIn(1, stack.count.coerceAtLeast(1))
    ModalBottomSheet(onDismissRequest = onDismiss) {
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
            Text(stringResource(R.string.shell_chest_detail_owned, stack.count))
            Text(stringResource(R.string.shell_chest_detail_rarity, stack.rarityLabel))
            Text(stringResource(R.string.shell_chest_detail_source), color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (stack.count > 1) {
                Text(
                    text = stringResource(R.string.shell_chest_release_selected_count, safeReleaseCount, stack.count),
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
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.common_close))
                }
                Button(onClick = { onRelease(safeReleaseCount) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shell_chest_release_count, safeReleaseCount))
                }
            }
        }
    }
}


internal fun chestReleaseSelection(stack: ChestInventoryStackUiModel, requestedCount: Int): Map<Int, Int> =
    mapOf(stack.level to requestedCount.coerceIn(1, stack.count.coerceAtLeast(1)))

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
