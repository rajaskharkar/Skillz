package com.kingkharnivore.skillz.ui.screen.shell

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.*
import com.kingkharnivore.skillz.data.model.shell.*
import com.kingkharnivore.skillz.domain.shell.*
import com.kingkharnivore.skillz.viewmodel.shell.*
import kotlinx.coroutines.*
import kotlin.math.*


@Composable
internal fun ShellChestScreen(
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

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            RoomHeader(
                title = R.string.shell_chest_title,
                body = R.string.shell_chest_body
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(
                    selected = category == ShellChestTab.ALL,
                    onClick = { category = ShellChestTab.ALL },
                    label = { Text(stringResource(R.string.shell_filter_all)) }
                )
                FilterChip(
                    selected = category == ShellChestTab.ANIMALS,
                    onClick = { category = ShellChestTab.ANIMALS },
                    label = { Text(stringResource(R.string.shell_filter_animals)) }
                )
                FilterChip(
                    selected = category == ShellChestTab.ROOM_OBJECTS,
                    onClick = { category = ShellChestTab.ROOM_OBJECTS },
                    label = { Text(stringResource(R.string.shell_filter_room_objects)) }
                )
            }
        }

        items(groupedItems) { (findId, copies) ->
            val def = ShellContentCatalog.find(findId) ?: return@items
            val title = stringResource(def.titleRes)
            val categoryLabel = kindLabel(def.kind) + (depthLabel(def.depthTier)?.let { " · $it" } ?: "")
            val activeCopies = if (def.kind == ShellRewardKind.ANIMAL) copies.filter { it.creatureStatus == CreatureStatus.ACTIVE } else copies
            val displayedCount = activeCopies.count { it.instanceId in displayedIds }
            val restingCount = activeCopies.size - displayedCount
            val releasedCount = copies.count { it.creatureStatus == CreatureStatus.RELEASED }
            val usedBeyondBlueCount = copies.count { it.creatureStatus == CreatureStatus.USED_BEYOND_BLUE }
            val bestCopy = copies.maxByOrNull { currentFormOrder(it) }
            val bestFormTitle = if (def.kind == ShellRewardKind.ANIMAL) {
                "Highest level: Level ${copies.maxOfOrNull { it.animalLevel.coerceAtLeast(1) } ?: 1}"
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
                modifier = Modifier.semantics {
                    contentDescription = rowDescription
                    role = Role.Button
                }
            ) {
                ListItem(
                    leadingContent = {
                        ShellObjectIcon(def.iconKey, Modifier.size(36.dp))
                    },
                    headlineContent = {
                        Text(stringResource(R.string.shell_chest_group_title, title, copies.size))
                    },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.shell_chest_group_status,
                                bestFormTitle,
                                displayedCount,
                                restingCount,
                                categoryLabel
                            ) + "\n" + sourceReasonFor(def)
                        )
                    }
                )
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
                ListItem(
                    leadingContent = { ShellObjectIcon(def.iconKey, Modifier.size(36.dp)) },
                    headlineContent = { Text(stringResource(R.string.shell_chest_group_title, title, stack.quantity)) },
                    supportingContent = {
                        Text("${kindLabel(def.kind)} · ${stringResource(R.string.shell_stack_quantity, stack.quantity)}\n${sourceReasonFor(def)}")
                    }
                )
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

@Composable
internal fun CopyGroupSheet(
    findId: String,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onSelectCopy: (UserShellFindInstanceEntity) -> Unit
) {
    val def = ShellContentCatalog.find(findId)
    val copies = uiState.finds.filter { it.findId == findId && isUserVisibleShellFind(ShellContentCatalog.find(it.findId)) }.sortedWith(
        compareByDescending<UserShellFindInstanceEntity> { currentFormOrder(it) }.thenByDescending { it.acquiredAt }
    )
    val displayedIds = displayedInstanceIds(uiState)
    val title = def?.let { stringResource(it.titleRes) } ?: findId

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_chest_group_title, title, copies.size),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            copies.forEach { copy ->
                val rowTitle = if (def?.kind == ShellRewardKind.ANIMAL) {
                    "Level ${copy.animalLevel.coerceAtLeast(1)}"
                } else {
                    ShellContentCatalog.upgradesFor(copy.findId)
                        .firstOrNull { it.upgradeStageId == copy.currentUpgradeStageId }
                        ?.let { stringResource(it.titleRes) } ?: title
                }
                val status = when {
                    copy.instanceId in displayedIds -> stringResource(R.string.shell_status_displayed_focus)
                    def?.kind == ShellRewardKind.ANIMAL && copy.creatureStatus != CreatureStatus.ACTIVE -> "Lifetime record · ${copy.creatureStatus.lowercase().replace('_', ' ')}"
                    else -> stringResource(R.string.shell_status_resting)
                }

                ListItem(
                    leadingContent = { ShellObjectIcon(def?.iconKey ?: "shell", Modifier.size(30.dp)) },
                    headlineContent = { Text(rowTitle) },
                    supportingContent = { Text(status) },
                    modifier = Modifier
                        .clickable { onSelectCopy(copy) }
                        .semantics { role = Role.Button }
                )
            }
        }
    }
}

@Composable
internal fun ChestPlacementSheet(
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
            modifier = Modifier.padding(20.dp),
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
                val displayedCopy = placementsBySlot[slot.slotId]?.let { placement -> findsById[placement.instanceId] }
                val displayedDef = displayedCopy?.let { ShellContentCatalog.find(it.findId) }
                ListItem(
                    headlineContent = { Text(slotTitle) },
                    supportingContent = {
                        Text(
                            displayedDef?.let { stringResource(R.string.shell_swap_with, stringResource(it.titleRes)) }
                                ?: stringResource(R.string.shell_place_free)
                        )
                    },
                    modifier = Modifier
                        .clickable { onPlace(slot.slotId) }
                        .semantics {
                            contentDescription = slotTitle
                            role = Role.Button
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