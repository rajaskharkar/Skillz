package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import kotlin.math.roundToInt

private data class ReleaseLevelGroup(
    val level: Int,
    val ownedQuantity: Int,
    val releaseValuePearls: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseCreatureConfirmationSheet(
    animal: TheBlueAnimalGroupUiModel,
    onDismiss: () -> Unit,
    onConfirm: (findId: String, selectionsByLevel: Map<Int, Int>) -> Unit
) {
    val name = findName(animal.findId)
    val levelGroups = remember(animal.findId, animal.levelCounts) {
        animal.levelCounts
            .mapNotNull { count ->
                val level = count.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: return@mapNotNull null
                ReleaseLevelGroup(
                    level = level,
                    ownedQuantity = count.count.coerceAtLeast(0),
                    releaseValuePearls = CreatureEconomy.releaseValuePearls(animal.findId, level)
                )
            }
            .filter { it.ownedQuantity > 0 }
            .sortedByDescending { it.level }
    }
    val ownedQuantity = levelGroups.sumOf { it.ownedQuantity }.coerceAtLeast(0)
    val singleReleaseLevel = levelGroups.firstOrNull()?.level ?: animal.highestLevel.coerceAtLeast(1)
    val singleReleaseValue = CreatureEconomy.releaseValuePearls(animal.findId, singleReleaseLevel)
    val levelSignature = levelGroups.joinToString(separator = "|") { "${it.level}:${it.ownedQuantity}" }
    val defaultSelections = remember(levelSignature) {
        if (levelGroups.size == 1 && ownedQuantity > 0) listOf(1) else List(levelGroups.size) { 0 }
    }
    var selectedQuantities by rememberSaveable(animal.findId, levelSignature) { mutableStateOf(defaultSelections) }
    var confirming by remember { mutableStateOf(false) }

    val safeSelectedQuantities = levelGroups.mapIndexed { index, group ->
        (selectedQuantities.getOrNull(index) ?: 0).coerceIn(0, group.ownedQuantity)
    }
    val totalSelected = safeSelectedQuantities.sum()
    val totalReward = levelGroups.mapIndexed { index, group ->
        group.releaseValuePearls * safeSelectedQuantities[index]
    }.sum()
    val selectionsByLevel = levelGroups.mapIndexedNotNull { index, group ->
        val selected = safeSelectedQuantities[index]
        if (selected > 0) group.level to selected else null
    }.toMap()
    val canRelease = totalSelected > 0 && !confirming
    val releaseButtonDescription = stringResource(R.string.shell_chest_release_button_a11y)

    fun updateSelected(index: Int, quantity: Int) {
        selectedQuantities = safeSelectedQuantities.mapIndexed { currentIndex, currentQuantity ->
            if (currentIndex == index) quantity.coerceIn(0, levelGroups[index].ownedQuantity) else currentQuantity
        }
    }

    fun selectLowestOne() {
        val lowestIndex = levelGroups.indexOfLast { it.ownedQuantity > 0 }
        if (lowestIndex >= 0) {
            selectedQuantities = List(levelGroups.size) { index -> if (index == lowestIndex) 1 else 0 }
        }
    }

    fun selectAllLowest() {
        val lowestIndex = levelGroups.indexOfLast { it.ownedQuantity > 0 }
        if (lowestIndex >= 0) {
            selectedQuantities = List(levelGroups.size) { index ->
                if (index == lowestIndex) levelGroups[index].ownedQuantity else 0
            }
        }
    }

    fun keepHighest() {
        val highestLevel = levelGroups.maxOfOrNull { it.level }
        selectedQuantities = levelGroups.map { group ->
            if (group.level == highestLevel) 0 else group.ownedQuantity
        }
    }

    fun clearSelection() {
        selectedQuantities = List(levelGroups.size) { 0 }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(R.string.shell_creature_release_confirm_title, name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (ownedQuantity > 1) {
                Text(stringResource(R.string.shell_creature_release_level_aware_body, ownedQuantity, name))
                Text(
                    text = stringResource(R.string.shell_creature_release_selected_total, totalSelected, ownedQuantity),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics {
                        contentDescription = "Selected $totalSelected of $ownedQuantity $name"
                    }
                )
                Text(
                    text = stringResource(R.string.shell_creature_release_reward_preview, totalReward),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics {
                        contentDescription = "You will receive $totalReward Pearls"
                    }
                )
                Text(stringResource(R.string.shell_creature_release_quick_select), fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    OutlinedButton(onClick = { selectLowestOne() }) { Text(stringResource(R.string.shell_creature_release_quick_lowest)) }
                    OutlinedButton(onClick = { selectAllLowest() }) { Text(stringResource(R.string.shell_creature_release_quick_all_lowest)) }
                    OutlinedButton(onClick = { keepHighest() }, enabled = levelGroups.size > 1) { Text(stringResource(R.string.shell_creature_release_quick_keep_highest)) }
                    OutlinedButton(onClick = { clearSelection() }) { Text(stringResource(R.string.shell_creature_release_quick_clear)) }
                }
                levelGroups.forEachIndexed { index, group ->
                    CreatureReleaseLevelSelector(
                        creatureName = name,
                        level = group.level,
                        ownedQuantity = group.ownedQuantity,
                        selectedQuantity = safeSelectedQuantities[index],
                        releaseValuePearls = group.releaseValuePearls,
                        onQuantityChange = { updateSelected(index, it) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            } else {
                Text(stringResource(R.string.shell_creature_release_single_body, name))
                Text(
                    text = stringResource(R.string.shell_creature_release_reward_preview, singleReleaseValue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics {
                        contentDescription = "You will receive $singleReleaseValue Pearls"
                    }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shell_creature_keep_swimming))
                }
                Button(
                    onClick = {
                        if (canRelease) {
                            confirming = true
                            onConfirm(animal.findId, selectionsByLevel)
                        } else if (ownedQuantity == 1 && !confirming) {
                            confirming = true
                            onConfirm(animal.findId, mapOf(singleReleaseLevel to 1))
                        }
                    },
                    enabled = (ownedQuantity == 1 || canRelease) && !confirming,
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            contentDescription = releaseButtonDescription
                        }
                ) {
                    Text(
                        when {
                            ownedQuantity == 1 || totalSelected > 0 -> stringResource(R.string.shell_creature_release_action)
                            else -> stringResource(R.string.shell_creature_release_confirm_disabled)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatureReleaseLevelSelector(
    creatureName: String,
    level: Int,
    ownedQuantity: Int,
    selectedQuantity: Int,
    releaseValuePearls: Int,
    onQuantityChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeOwnedQuantity = ownedQuantity.coerceAtLeast(1)
    val decreaseDescription = stringResource(R.string.shell_creature_release_quantity_decrease_level, level, creatureName)
    val increaseDescription = stringResource(R.string.shell_creature_release_quantity_increase_level, level, creatureName)

    ElevatedCard(
        modifier = modifier.semantics {
            contentDescription = "Level $level $creatureName. $ownedQuantity owned. $selectedQuantity selected."
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(stringResource(R.string.shell_creature_release_level_label, level), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.shell_creature_release_level_owned, ownedQuantity), modifier = Modifier.weight(1f))
                Text(stringResource(R.string.shell_creature_release_level_selected, selectedQuantity), modifier = Modifier.weight(1f))
            }
            Text(stringResource(R.string.shell_creature_release_level_reward_each, releaseValuePearls))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { onQuantityChange((selectedQuantity - 1).coerceAtLeast(0)) },
                    enabled = selectedQuantity > 0
                ) {
                    Icon(Icons.Outlined.Remove, contentDescription = decreaseDescription)
                }
                Text(
                    text = selectedQuantity.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { onQuantityChange((selectedQuantity + 1).coerceAtMost(safeOwnedQuantity)) },
                    enabled = selectedQuantity < safeOwnedQuantity
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = increaseDescription)
                }
            }
            Slider(
                value = selectedQuantity.toFloat(),
                onValueChange = { value -> onQuantityChange(value.roundToInt().coerceIn(0, safeOwnedQuantity)) },
                valueRange = 0f..safeOwnedQuantity.toFloat(),
                steps = (safeOwnedQuantity - 1).coerceAtLeast(0)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0")
                Text(safeOwnedQuantity.toString())
            }
        }
    }
}
