package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.sourceReasonFor
import com.kingkharnivore.skillz.ui.screen.shell.ux.ObjectCopySheet
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.canDisplayInstance
import com.kingkharnivore.skillz.ui.screen.shell.ux.displayedInstanceIds
import com.kingkharnivore.skillz.ui.screen.shell.ux.isUserVisibleShellFind
import com.kingkharnivore.skillz.ui.screen.shell.ux.kindLabel
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

private enum class ShellChestTab { ALL, ANIMALS, ROOM_OBJECTS }

@Composable
fun ShellChestScreen(
    uiState: ShellUiState,
    onPlace: (String, String) -> Unit,
    onReturn: (String) -> Unit,
    onUpgrade: (String) -> Unit,
    onOpenFocus: () -> Unit
) {
    var category by remember { mutableStateOf(ShellChestTab.ALL) }
    var selectedGroupFindId by remember { mutableStateOf<String?>(null) }
    var selectedInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }
    var placingInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }

    fun isVisibleInTab(def: ShellFindDefinition?): Boolean {
        if (def == null || def.kind == ShellRewardKind.TRINKET) return false
        return when (category) {
            ShellChestTab.ALL -> true
            ShellChestTab.ANIMALS -> def.kind == ShellRewardKind.ANIMAL
            ShellChestTab.ROOM_OBJECTS -> def.kind == ShellRewardKind.OBJECT
        }
    }
    val displayedIds = displayedInstanceIds(uiState)
    val groupedItems = uiState.finds
        .filter { isVisibleInTab(ShellContentCatalog.find(it.findId)) }
        .groupBy { it.findId }
        .toList()
        .sortedBy { (findId, _) -> ShellContentCatalog.find(findId)?.titleRes ?: 0 }
    val stackItems = uiState.stacks.filter { isVisibleInTab(ShellContentCatalog.find(it.findId)) }

    Column(
        Modifier.Companion.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoomHeader(title = R.string.shell_chest_title, body = R.string.shell_chest_body)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.Companion.horizontalScroll(rememberScrollState())
        ) {
            FilterChip(
                selected = category == ShellChestTab.ALL,
                onClick = { category = ShellChestTab.ALL },
                label = { Text(stringResource(R.string.shell_filter_all)) })
            FilterChip(
                selected = category == ShellChestTab.ANIMALS,
                onClick = { category = ShellChestTab.ANIMALS },
                label = { Text(stringResource(R.string.shell_filter_animals)) })
            FilterChip(
                selected = category == ShellChestTab.ROOM_OBJECTS,
                onClick = { category = ShellChestTab.ROOM_OBJECTS },
                label = { Text(stringResource(R.string.shell_filter_room_objects)) })
        }
        LazyVerticalGrid(
            modifier = Modifier.Companion.weight(1f),
            columns = GridCells.Adaptive(168.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(groupedItems) { (findId, copies) ->
                val def = ShellContentCatalog.find(findId) ?: return@items
                val title = stringResource(def.titleRes)
                val categoryLabel = kindLabel(def.kind)
                val activeCopies =
                    if (def.kind == ShellRewardKind.ANIMAL) copies.filter { it.creatureStatus == CreatureStatus.ACTIVE } else copies
                val displayedCount = activeCopies.count { it.instanceId in displayedIds }
                val restingCount = activeCopies.size - displayedCount
                val releasedCount = copies.count { it.creatureStatus == CreatureStatus.RELEASED }
                val usedBeyondBlueCount =
                    copies.count { it.creatureStatus == CreatureStatus.USED_BEYOND_BLUE }
                val bestCopy = copies.maxByOrNull { currentFormOrder(it) }
                val bestFormTitle = if (def.kind == ShellRewardKind.ANIMAL) {
                    stringResource(
                        R.string.the_blue_highest_level_chip,
                        copies.maxOfOrNull { it.animalLevel.coerceAtLeast(1) } ?: 1)
                } else {
                    bestCopy?.let { copy ->
                        ShellContentCatalog.upgradesFor(copy.findId)
                            .firstOrNull { it.upgradeStageId == copy.currentUpgradeStageId }
                            ?.let { stringResource(it.titleRes) }
                    } ?: title
                }
                val rowDescription = stringResource(
                    R.string.shell_chest_group_a11y,
                    title,
                    copies.size,
                    displayedCount,
                    restingCount
                )

                ElevatedCard(
                    onClick = { selectedGroupFindId = findId },
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.Companion.semantics {
                        contentDescription = rowDescription
                        role = Role.Companion.Button
                    }
                ) {
                    Column(
                        Modifier.Companion.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShellObjectIcon(def.iconKey, Modifier.Companion.size(36.dp))
                        Text(title, fontWeight = FontWeight.Companion.Bold)
                        Text(stringResource(R.string.shell_chest_lifetime_count, copies.size))
                        Text(
                            stringResource(
                                R.string.the_blue_swimming_chip,
                                activeCopies.count { it.creatureStatus == CreatureStatus.ACTIVE })
                        )
                        Text(bestFormTitle)
                        if (def.kind == ShellRewardKind.ANIMAL) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.Companion.horizontalScroll(rememberScrollState())
                            ) {
                                copies.groupBy { it.animalLevel.coerceAtLeast(1) }.toSortedMap()
                                    .forEach { (lv, lvCopies) ->
                                        ShellMetricPill(
                                            Icons.Outlined.EmojiEvents,
                                            stringResource(
                                                R.string.shell_creature_level_count_chip,
                                                lv,
                                                lvCopies.size
                                            )
                                        )
                                    }
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.Companion.horizontalScroll(rememberScrollState())
                            ) {
                                if (releasedCount > 0) {
                                    ShellMetricPill(
                                        Icons.Outlined.Route,
                                        stringResource(
                                            R.string.the_blue_released_chip,
                                            releasedCount
                                        )
                                    )
                                }
                                if (usedBeyondBlueCount > 0) {
                                    ShellMetricPill(
                                        Icons.Outlined.Waves,
                                        stringResource(
                                            R.string.the_blue_beyond_blue_chip,
                                            usedBeyondBlueCount
                                        )
                                    )
                                }
                            }
                        } else {
                            Text(
                                stringResource(
                                    R.string.shell_chest_displayed_count,
                                    displayedCount
                                )
                            )
                            Text(stringResource(R.string.shell_chest_resting_count, restingCount))
                        }
                        Text(categoryLabel, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(stackItems) { stack ->
                val def = ShellContentCatalog.find(stack.findId) ?: return@items
                val title = stringResource(def.titleRes)

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        Modifier.Companion.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShellObjectIcon(def.iconKey, Modifier.Companion.size(36.dp))
                        Text(title, fontWeight = FontWeight.Companion.Bold)
                        Text(stringResource(R.string.shell_stack_quantity, stack.quantity))
                        ShellMetricPill(Icons.Outlined.Inventory2, kindLabel(def.kind))
                        Text(sourceReasonFor(def), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    selectedGroupFindId?.let { findId ->
        CopyGroupSheet(
            findId = findId,
            uiState = uiState,
            onDismiss = { selectedGroupFindId = null },
            onSelectCopy = { instance ->
                selectedGroupFindId = null
                selectedInstance = instance
            }
        )
    }

    selectedInstance?.let { instance ->
        val isDisplayed = uiState.focusPlacements.any { it.instanceId == instance.instanceId }
        ObjectCopySheet(
            item = instance,
            pearlBalance = uiState.pearlBalance,
            displayed = isDisplayed,
            onDismiss = { selectedInstance = null },
            onReturn = {
                onReturn(it)
                selectedInstance = null
            },
            onUpgrade = {
                onUpgrade(it)
                selectedInstance = null
            },
            onPlaceInFocus = if (isDisplayed) {
                null
            } else {
                {
                    placingInstance = instance
                    selectedInstance = null
                }
            }
        )
    }

    placingInstance?.let { instance ->
        ChestPlacementSheet(
            instance = instance,
            uiState = uiState,
            onDismiss = { placingInstance = null },
            onPlace = { slotId ->
                onPlace(instance.instanceId, slotId)
                placingInstance = null
                onOpenFocus()
            },
            onOpenFocus = {
                placingInstance = null
                onOpenFocus()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyGroupSheet(
    findId: String,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onSelectCopy: (UserShellFindInstanceEntity) -> Unit
) {
    val def = ShellContentCatalog.find(findId)
    val copies = uiState.finds.filter { it.findId == findId && isUserVisibleShellFind(
        ShellContentCatalog.find(it.findId)
    )
    }.sortedWith(
        compareByDescending<UserShellFindInstanceEntity> { currentFormOrder(it) }.thenByDescending { it.acquiredAt }
    )
    val displayedIds = displayedInstanceIds(uiState)
    val title = def?.let { stringResource(it.titleRes) } ?: findId
    val activeCopies = if (def?.kind == ShellRewardKind.ANIMAL) copies.filter { it.creatureStatus == CreatureStatus.ACTIVE } else copies
    val displayedCount = activeCopies.count { it.instanceId in displayedIds }
    val restingCount = activeCopies.size - displayedCount
    val releasedCount = copies.count { it.creatureStatus == CreatureStatus.RELEASED }
    val usedBeyondBlueCount = copies.count { it.creatureStatus == CreatureStatus.USED_BEYOND_BLUE }
    val highestLevel = copies.maxOfOrNull { it.animalLevel.coerceAtLeast(1) } ?: 1
    val bestFormTitle = copies.maxByOrNull { currentFormOrder(it) }?.let { copy ->
        ShellContentCatalog.upgradesFor(copy.findId)
            .firstOrNull { it.upgradeStageId == copy.currentUpgradeStageId }
            ?.let { stringResource(it.titleRes) }
    } ?: title

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.Companion.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShellObjectIcon(def?.iconKey ?: "shell", Modifier.Companion.size(56.dp))
            Text(
                text = stringResource(R.string.shell_collection_title, title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Companion.Bold
            )
            Text(stringResource(R.string.shell_chest_lifetime_count, copies.size))
            Text(
                stringResource(
                    R.string.the_blue_swimming_chip,
                    activeCopies.count { it.creatureStatus == CreatureStatus.ACTIVE })
            )
            Text(stringResource(R.string.shell_chest_displayed_count, displayedCount))
            Text(stringResource(R.string.shell_chest_resting_count, restingCount))
            if (def?.kind == ShellRewardKind.ANIMAL) {
                Text(stringResource(R.string.the_blue_highest_level_chip, highestLevel))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.Companion.horizontalScroll(rememberScrollState())
                ) {
                    copies.groupBy { it.animalLevel.coerceAtLeast(1) }.toSortedMap()
                        .forEach { (lv, lvCopies) ->
                            ShellMetricPill(
                                Icons.Outlined.EmojiEvents,
                                stringResource(
                                    R.string.shell_creature_level_count_chip,
                                    lv,
                                    lvCopies.size
                                )
                            )
                        }
                }
                if (releasedCount > 0) Text(
                    stringResource(
                        R.string.the_blue_released_chip,
                        releasedCount
                    )
                )
                if (usedBeyondBlueCount > 0) Text(
                    stringResource(
                        R.string.the_blue_beyond_blue_chip,
                        usedBeyondBlueCount
                    )
                )
            } else {
                Text(stringResource(R.string.shell_chest_best_form, bestFormTitle))
            }
            Text(
                stringResource(R.string.shell_copies_heading),
                fontWeight = FontWeight.Companion.SemiBold
            )

            copies.forEach { copy ->
                val rowTitle = if (def?.kind == ShellRewardKind.ANIMAL) {
                    stringResource(
                        R.string.shell_creature_level_short,
                        copy.animalLevel.coerceAtLeast(1)
                    )
                } else {
                    ShellContentCatalog.upgradesFor(copy.findId)
                        .firstOrNull { it.upgradeStageId == copy.currentUpgradeStageId }
                        ?.let { stringResource(it.titleRes) } ?: title
                }
                val status = when {
                    copy.instanceId in displayedIds -> stringResource(R.string.shell_status_displayed_focus)
                    def?.kind == ShellRewardKind.ANIMAL && copy.creatureStatus == CreatureStatus.RELEASED ->
                        stringResource(R.string.shell_lifetime_record_status_released)

                    def?.kind == ShellRewardKind.ANIMAL && copy.creatureStatus == CreatureStatus.USED_BEYOND_BLUE ->
                        stringResource(R.string.shell_lifetime_record_status_used_beyond_blue)

                    else -> stringResource(R.string.shell_status_resting)
                }

                ListItem(
                    leadingContent = {
                        ShellObjectIcon(
                            def?.iconKey ?: "shell",
                            Modifier.Companion.size(30.dp)
                        )
                    },
                    headlineContent = { Text(rowTitle) },
                    supportingContent = { Text(status) },
                    modifier = Modifier.Companion
                        .clickable { onSelectCopy(copy) }
                        .semantics { role = Role.Companion.Button }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChestPlacementSheet(
    instance: UserShellFindInstanceEntity,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onPlace: (String) -> Unit,
    onOpenFocus: () -> Unit
) {
    val def = ShellContentCatalog.find(instance.findId)
    val placementsBySlot = uiState.focusPlacements.associateBy { it.slotId }
    val findsById = uiState.finds.associateBy { it.instanceId }

    val slots = if (def == null || !canDisplayInstance(instance, def)) {
        emptyList()
    } else {
        ShellContentCatalog.focusSlots.filter { slot ->
            ShellContentCatalog.isCompatibleWithSlot(slot, def)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.Companion.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = def?.let { stringResource(it.titleRes) } ?: instance.findId,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = if (slots.any { placementsBySlot[it.slotId] != null }) {
                    stringResource(R.string.shell_choose_something_to_swap)
                } else {
                    stringResource(R.string.shell_compatible_slots)
                }
            )

            slots.forEach { slot ->
                val slotTitle = stringResource(slot.titleRes)
                val displayedCopy =
                    placementsBySlot[slot.slotId]?.let { placement -> findsById[placement.instanceId] }
                val displayedDef = displayedCopy?.let { ShellContentCatalog.find(it.findId) }
                ListItem(
                    headlineContent = { Text(slotTitle) },
                    supportingContent = {
                        Text(
                            displayedDef?.let {
                                stringResource(
                                    R.string.shell_swap_with,
                                    stringResource(it.titleRes)
                                )
                            }
                                ?: stringResource(R.string.shell_place_free)
                        )
                    },
                    modifier = Modifier.Companion
                        .clickable { onPlace(slot.slotId) }
                        .semantics {
                            contentDescription = slotTitle
                            role = Role.Companion.Button
                        }
                )
            }

            if (slots.isEmpty()) {
                Text(stringResource(R.string.shell_no_compatible_nooks))
            }

            OutlinedButton(onClick = onOpenFocus) {
                Text(stringResource(R.string.shell_room_focus_title))
            }
        }
    }
}

@Composable
fun ShellMetricPill(icon: ImageVector, text: String, modifier: Modifier = Modifier.Companion) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            Modifier.Companion.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.Companion.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.Companion.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun currentFormOrder(instance: UserShellFindInstanceEntity): Int =
    ShellContentCatalog.upgradesFor(instance.findId)
        .firstOrNull { it.upgradeStageId == instance.currentUpgradeStageId }
        ?.orderIndex ?: 0