package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueDisplayDisabledReason
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellMetricPill
import com.kingkharnivore.skillz.ui.screen.shell.theBlueDisplayDisabledReason

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TheBlueAnimalDetailSheet(
    animal: TheBlueAnimalGroupUiModel,
    focusSlotId: String?,
    firstRestingInstanceId: String?,
    pearlBalance: Int,
    onDismiss: () -> Unit,
    onGrow: (String) -> Unit,
    onRelease: () -> Unit,
    onBeyondBlue: () -> Unit,
    onDisplayInFocus: (String, String) -> Unit,
    onOpenChest: () -> Unit
) {
    val name = findName(animal.findId)
    val zone = zoneTitle(animal.zoneId)
    val title = stringResource(R.string.the_blue_animal_count, name, animal.totalCount)
    val source = theBlueEncounteredReason(animal.findId)
    val detailDescription = stringResource(R.string.the_blue_detail_a11y, title, zone, source)
    val growthInstanceId = animal.highestLevelActiveInstanceId ?: animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val releaseInstanceId = animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val highestLevel = animal.highestLevel.coerceAtLeast(1)
    val growthCost = CreatureEconomy.growthCostPearls(animal.findId, highestLevel)
    val isMastered = highestLevel >= CreatureEconomy.MAX_CREATURE_LEVEL
    val canGrow = growthInstanceId != null && !isMastered && pearlBalance >= growthCost
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .semantics { contentDescription = detailDescription },
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ElevatedCard {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        ShellObjectIcon(CreatureCatalog.get(animal.findId)?.staticIconKey ?: "animal", Modifier.size(56.dp))
                        Column {
                            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(text = zone, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(source)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                ShellMetricPill(Icons.Outlined.Waves, stringResource(R.string.the_blue_swimming_chip, animal.totalCount))
                ShellMetricPill(Icons.Outlined.AutoStories, stringResource(R.string.the_blue_lifetime_chip, animal.lifetimeEncounteredCount))
                ShellMetricPill(Icons.Outlined.EmojiEvents, stringResource(R.string.the_blue_highest_level_chip, animal.highestLevel))
                if (animal.releasedCount > 0) ShellMetricPill(Icons.Outlined.Route, stringResource(R.string.the_blue_released_chip, animal.releasedCount))
                if (animal.usedBeyondBlueCount > 0) ShellMetricPill(Icons.Outlined.WaterDrop, stringResource(R.string.the_blue_beyond_blue_chip, animal.usedBeyondBlueCount))
            }
            animal.flowTimeValueMinutes?.let {
                ElevatedCard {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Route, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.the_blue_created_by_flow_title)) },
                        supportingContent = {
                            Text(stringResource(R.string.the_blue_created_by_flow_value, formatMinutesCompact(it)))
                        }
                    )
                }
            }
            animal.releaseValuePearls?.let {
                ElevatedCard {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Diamond, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.the_blue_pearl_value_title)) },
                        supportingContent = { Text(stringResource(R.string.shell_creature_pearl_value_each, it)) }
                    )
                }
                ElevatedCard {
                    ListItem(
                        leadingContent = { Icon(Icons.Outlined.Diamond, contentDescription = null) },
                        headlineContent = { Text(stringResource(R.string.the_blue_release_return_title)) },
                        supportingContent = { Text(stringResource(R.string.shell_creature_release_value_each, it)) }
                    )
                }
            }

            Text(stringResource(R.string.shell_creature_levels_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (animal.levelCounts.isEmpty()) {
                Text(stringResource(R.string.the_blue_forms_unavailable))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    animal.levelCounts.sortedBy { it.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 0 }
                        .forEach { level ->
                            val lv = level.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1
                            ShellMetricPill(Icons.Outlined.EmojiEvents, stringResource(R.string.shell_creature_level_count_chip, lv, level.count))
                        }
                }
            }
            Text(stringResource(R.string.the_blue_growth_support_copy))
            Text(
                text = if (isMastered) {
                    stringResource(R.string.shell_creature_level_up_unavailable_max)
                } else {
                    stringResource(R.string.shell_creature_level_up_cost, growthCost)
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
            )

            Text(stringResource(R.string.the_blue_displayed_in_focus_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(animal.displayedInFocusCount.toString())
            Text(stringResource(R.string.the_blue_resting_in_chest_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(animal.restingCount.toString())

            Text(stringResource(R.string.the_blue_actions), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { growthInstanceId?.let(onGrow) },
                enabled = canGrow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.shell_creature_level_up))
            }
            if (!canGrow) {
                val missing = (growthCost - pearlBalance).coerceAtLeast(0)
                Text(
                    text = when {
                        isMastered -> stringResource(R.string.shell_creature_level_up_unavailable_max)
                        growthInstanceId == null -> stringResource(R.string.shell_creature_no_active_to_grow)
                        else -> stringResource(R.string.shell_creature_need_more_pearls_to_grow, missing)
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            OutlinedButton(onClick = onBeyondBlue, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Waves, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.beyond_blue_encounter_cta))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (firstRestingInstanceId != null && focusSlotId != null) {
                            onDisplayInFocus(firstRestingInstanceId, focusSlotId)
                        }
                    },
                    enabled = firstRestingInstanceId != null && focusSlotId != null,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.the_blue_display_one_in_focus)) }
                OutlinedButton(onClick = onOpenChest, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.the_blue_view_in_chest))
                }
            }
            OutlinedButton(
                onClick = onRelease,
                enabled = releaseInstanceId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.shell_creature_release_for_pearls))
            }
            when (theBlueDisplayDisabledReason(focusSlotId, firstRestingInstanceId)) {
                TheBlueDisplayDisabledReason.NO_FOCUS_SLOT -> Text(
                    stringResource(R.string.the_blue_no_focus_slot, name),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                TheBlueDisplayDisabledReason.NO_RESTING_COPY -> Text(
                    stringResource(R.string.the_blue_no_resting_copy),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                null -> Unit
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun theBlueEncounteredReason(findId: String): String = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.the_blue_encountered_minnow)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.the_blue_encountered_seahorse)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.the_blue_encountered_manta)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.the_blue_encountered_whale)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.the_blue_source_octopus)
    else -> theBlueSourceReason(findId)
}

@Composable
private fun theBlueSourceReason(findId: String): String = when (findId) {
    ShellContentCatalog.FOCUS_MINNOW -> stringResource(R.string.the_blue_source_minnow)
    ShellContentCatalog.FOCUS_SEAHORSE -> stringResource(R.string.the_blue_source_seahorse)
    ShellContentCatalog.FOCUS_MANTA -> stringResource(R.string.the_blue_source_manta)
    ShellContentCatalog.FOCUS_WHALE -> stringResource(R.string.the_blue_source_whale)
    ShellContentCatalog.FOCUS_OCTOPUS -> stringResource(R.string.the_blue_source_octopus)
    else -> ShellContentCatalog.find(findId)?.let { stringResource(it.descriptionRes) } ?: stringResource(R.string.reward_card_shell_recorded_body)
}