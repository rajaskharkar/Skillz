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
internal fun FocusRoomScreen(
    uiState: ShellUiState,
    onPlace: (String, String) -> Unit,
    onReturn: (String) -> Unit,
    onInvite: (String, String) -> Unit,
    onUpgrade: (String) -> Unit
) {
    var selectedSlot by remember { mutableStateOf<String?>(null) }
    var selectedInstance by remember { mutableStateOf<UserShellFindInstanceEntity?>(null) }
    val placementsBySlot = uiState.focusPlacements.associateBy { it.slotId }
    val findsById = uiState.finds.associateBy { it.instanceId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RoomHeader(
            title = R.string.shell_room_focus_title,
            body = R.string.shell_focus_body
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(32.dp))
                .background(shellChamberBrush())
        ) {
            TurtleShellInteriorBackground(
                modifier = Modifier.matchParentSize(),
                centerGlow = false
            )

            FocusRoomCarvedShelves(
                modifier = Modifier.matchParentSize()
            )

            ShellContentCatalog.focusSlots.sortedBy { it.zIndex }.forEach { slot ->
                val placement = placementsBySlot[slot.slotId]
                val find = placement?.let { findsById[it.instanceId] }

                SlotChip(
                    slot = slot,
                    find = find,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (maxWidth * slot.anchorX) - ((maxWidth * slot.widthFraction) / 2),
                            y = (maxHeight * slot.anchorY) - ((maxHeight * slot.heightFraction) / 2)
                        )
                        .size(
                            width = maxWidth * slot.widthFraction,
                            height = maxHeight * slot.heightFraction
                        ),
                    onEmpty = { selectedSlot = slot.slotId },
                    onFind = {
                        if (find != null) {
                            selectedInstance = find
                        }
                    }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.24f))
        ) {
            val restingCount = restingFinds(uiState).size
            val allNooksHolding = uiState.focusPlacements.size >= ShellContentCatalog.focusSlots.size
            Text(
                text = if (allNooksHolding) {
                    stringResource(R.string.shell_all_nooks_holding, restingCount)
                } else {
                    stringResource(R.string.shell_focus_summary, uiState.focusPlacements.size, restingCount)
                },
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }

    if (selectedSlot != null) {
        EmptySlotSheet(
            slotId = selectedSlot!!,
            uiState = uiState,
            onDismiss = { selectedSlot = null },
            onPlace = { id ->
                onPlace(id, selectedSlot!!)
                selectedSlot = null
            },
            onInvite = { findId ->
                onInvite(findId, selectedSlot!!)
                selectedSlot = null
            }
        )
    }

    if (selectedInstance != null) {
        ObjectCopySheet(
            item = selectedInstance!!,
            pearlBalance = uiState.pearlBalance,
            displayed = true,
            onDismiss = { selectedInstance = null },
            onReturn = {
                onReturn(it)
                selectedInstance = null
            },
            onUpgrade = {
                onUpgrade(it)
                selectedInstance = null
            },
            onPlaceInFocus = null
        )
    }
}

@Composable
internal fun TurtleShellInteriorBackground(
    modifier: Modifier = Modifier,
    centerGlow: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.primary.copy(alpha = 0.24f),
            topLeft = Offset(-w * 0.12f, h * 0.02f),
            size = Size(w * 1.24f, h * 1.10f)
        )

        drawOval(
            color = scheme.surface.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.04f, h * 0.07f),
            size = Size(w * 0.92f, h * 0.86f)
        )

        drawOval(
            color = scheme.secondary.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.13f, h * 0.12f),
            size = Size(w * 0.74f, h * 0.68f)
        )

        if (centerGlow) {
            drawCircle(
                color = scheme.primary.copy(alpha = 0.18f),
                radius = w * 0.25f,
                center = Offset(w * 0.50f, h * 0.35f)
            )
        }

        val spine = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(
                w * 0.46f,
                h * 0.28f,
                w * 0.54f,
                h * 0.48f,
                w * 0.50f,
                h * 0.80f
            )
        }

        drawPath(
            path = spine,
            color = scheme.secondary.copy(alpha = 0.22f),
            style = Stroke(width = 4.5f)
        )

        val bandYs = listOf(0.20f, 0.34f, 0.49f, 0.64f, 0.78f)
        bandYs.forEachIndexed { index, yFraction ->
            val y = h * yFraction
            val leftInset = w * (0.13f + index * 0.018f)
            val rightInset = w - leftInset

            val band = Path().apply {
                moveTo(leftInset, y)
                cubicTo(
                    w * 0.30f,
                    y - h * 0.060f,
                    w * 0.42f,
                    y + h * 0.035f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.58f,
                    y + h * 0.035f,
                    w * 0.70f,
                    y - h * 0.060f,
                    rightInset,
                    y
                )
            }

            drawPath(
                path = band,
                color = scheme.secondary.copy(alpha = 0.12f),
                style = Stroke(width = 3f)
            )
        }

        drawOval(
            color = Color.Black.copy(alpha = 0.12f),
            topLeft = Offset(-w * 0.08f, h * 0.02f),
            size = Size(w * 1.16f, h * 1.04f),
            style = Stroke(width = w * 0.08f)
        )
    }
}

@Composable
internal fun FocusRoomCarvedShelves(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val shelfColor = scheme.secondary.copy(alpha = 0.22f)
        val shelfShadow = Color.Black.copy(alpha = 0.20f)

        listOf(0.30f, 0.52f, 0.74f).forEach { yFraction ->
            val y = h * yFraction

            val shadow = Path().apply {
                moveTo(w * 0.16f, y + 6f)
                cubicTo(
                    w * 0.32f,
                    y + h * 0.03f,
                    w * 0.68f,
                    y + h * 0.03f,
                    w * 0.84f,
                    y + 6f
                )
            }

            drawPath(
                path = shadow,
                color = shelfShadow,
                style = Stroke(width = 8f)
            )

            val shelf = Path().apply {
                moveTo(w * 0.16f, y)
                cubicTo(
                    w * 0.32f,
                    y - h * 0.025f,
                    w * 0.68f,
                    y - h * 0.025f,
                    w * 0.84f,
                    y
                )
            }

            drawPath(
                path = shelf,
                color = shelfColor,
                style = Stroke(width = 4f)
            )
        }
    }
}

@Composable
internal fun SlotChip(
    slot: ShellSlotDefinition,
    find: UserShellFindInstanceEntity?,
    modifier: Modifier,
    onEmpty: () -> Unit,
    onFind: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val def = find?.let { ShellContentCatalog.find(it.findId) }

    val slotTitle = stringResource(slot.titleRes)
    val label = if (def != null) {
        stringResource(def.titleRes)
    } else {
        slotTitle
    }
    val placedDescription = if (def != null) {
        stringResource(R.string.shell_placed_object_a11y, label)
    } else {
        stringResource(R.string.shell_empty_slot_a11y, slotTitle)
    }

    val isFilled = def != null

    Surface(
        modifier = modifier
            .semantics { contentDescription = placedDescription }
            .clickable {
                if (find == null) {
                    onEmpty()
                } else {
                    onFind()
                }
            },
        shape = RoundedCornerShape(22.dp),
        color = if (isFilled) {
            scheme.surface.copy(alpha = 0.96f)
        } else {
            scheme.primary.copy(alpha = 0.40f)
        },
        border = BorderStroke(
            width = 1.dp,
            color = if (isFilled) {
                scheme.secondary.copy(alpha = 0.68f)
            } else {
                scheme.secondary.copy(alpha = 0.30f)
            }
        )
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(6.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (def != null) {
                    ShellObjectIcon(def.iconKey, Modifier.size(28.dp))
                }
                Text(
                    text = label,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isFilled) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isFilled) {
                        scheme.onSurface
                    } else {
                        scheme.onPrimary.copy(alpha = 0.82f)
                    }
                )
            }
        }
    }
}

@Composable
internal fun EmptySlotSheet(
    slotId: String,
    uiState: ShellUiState,
    onDismiss: () -> Unit,
    onPlace: (String) -> Unit,
    onInvite: (String) -> Unit
) {
    val slot = ShellContentCatalog.focusSlots.first { it.slotId == slotId }
    val placedIds = uiState.focusPlacements.map { it.instanceId }.toSet()

    val compatible = uiState.finds.filter { instance ->
        val def = ShellContentCatalog.find(instance.findId)

        def != null &&
                instance.instanceId !in placedIds &&
                canDisplayInstance(instance, def) &&
                def.placeable &&
                ShellContentCatalog.isCompatibleWithSlot(slot, def)
    }

    val invitable = ShellContentCatalog.focusPearlObjects.filter { def ->
        ShellContentCatalog.isCompatibleWithSlot(slot, def)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.shell_empty_slot_title),
                style = MaterialTheme.typography.titleLarge
            )

            Text(stringResource(R.string.shell_slot_named_title, stringResource(slot.titleRes)))
            if (slot.slotType == ShellSlotType.SURGE_CURRENT) {
                Text(stringResource(R.string.shell_surge_current_reserved_body))
            } else {
                Text(stringResource(R.string.shell_empty_nook_choices))
            }

            Text(
                text = stringResource(R.string.shell_place_from_chest),
                fontWeight = FontWeight.SemiBold
            )

            compatible.forEach { item ->
                val def = ShellContentCatalog.find(item.findId) ?: return@forEach

                ListItem(
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = { Text(stringResource(R.string.shell_place_free)) },
                    modifier = Modifier.clickable { onPlace(item.instanceId) }
                )
            }

            if (compatible.isEmpty()) {
                Text(stringResource(R.string.shell_no_resting_finds_fit))
            }

            if (invitable.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.shell_shape_space_with_pearls),
                    fontWeight = FontWeight.SemiBold
                )
            }

            invitable.forEach { def ->
                val cost = def.pearlCost ?: return@forEach
                val canAfford = uiState.pearlBalance >= cost
                ListItem(
                    leadingContent = { ShellObjectIcon(def.iconKey, Modifier.size(30.dp)) },
                    headlineContent = { Text(stringResource(def.titleRes)) },
                    supportingContent = {
                        Text(
                            if (canAfford) stringResource(R.string.shell_invite_with_pearls, cost)
                            else stringResource(R.string.shell_need_more_pearls, cost - uiState.pearlBalance)
                        )
                    },
                    modifier = Modifier
                        .clickable(enabled = canAfford) { onInvite(def.findId) }
                        .semantics { role = Role.Button }
                )
            }

            OutlinedButton(onClick = onDismiss) {
                Text(stringResource(R.string.shell_leave_empty))
            }
        }
    }
}

@Composable
internal fun ObjectCopySheet(
    item: UserShellFindInstanceEntity,
    pearlBalance: Int,
    displayed: Boolean,
    onDismiss: () -> Unit,
    onReturn: (String) -> Unit,
    onUpgrade: (String) -> Unit,
    onPlaceInFocus: (() -> Unit)?
) {
    val def = ShellContentCatalog.find(item.findId)
    val isAnimal = def?.kind == ShellRewardKind.ANIMAL

    val current = if (!isAnimal) {
        def?.let {
            ShellContentCatalog.upgradesFor(it.findId)
                .firstOrNull { stage -> stage.upgradeStageId == item.currentUpgradeStageId }
        }
    } else {
        null
    }

    val next = if (!isAnimal) {
        def?.let { ShellContentCatalog.nextUpgrade(it.findId, item.currentUpgradeStageId) }
    } else {
        null
    }

    val findTitle = if (def != null) {
        stringResource(def.titleRes)
    } else {
        item.findId
    }

    val currentTitle = if (current != null) {
        stringResource(current.titleRes)
    } else {
        findTitle
    }

    val kindText = if (def != null) {
        kindLabel(def.kind)
    } else {
        null
    }

    val sourceText = if (def != null) {
        stringResource(R.string.shell_source_label, sourceReasonFor(def))
    } else {
        null
    }

    val statusText = when {
        displayed -> stringResource(R.string.shell_status_displayed_focus)
        isAnimal && item.creatureStatus != CreatureStatus.ACTIVE ->
            "Lifetime record · ${item.creatureStatus.lowercase().replace('_', ' ')}"
        else -> stringResource(R.string.shell_status_resting)
    }

    val animalUpgradeA11y = stringResource(R.string.shell_upgrade_animal_a11y)
    val returnToChestText = returnToChestLabel(def)
    val placeInFocusText = placeInFocusLabel(def)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = findTitle,
                style = MaterialTheme.typography.titleLarge
            )

            if (kindText != null) {
                Text(kindText)
            }

            Text(statusText)

            if (isAnimal) {
                Text(stringResource(R.string.shell_creature_level_value, item.animalLevel.coerceAtLeast(1)))

                if (sourceText != null) {
                    Text(sourceText)
                }

                if (item.creatureStatus == CreatureStatus.ACTIVE) {
                    val cost = CreatureEconomy.growthCostPearls(
                        item.findId,
                        item.animalLevel.coerceAtLeast(1)
                    )
                    val canAfford = pearlBalance >= cost

                    if (!canAfford) {
                        Text(stringResource(R.string.shell_need_more_pearls, cost - pearlBalance))
                    }

                    Button(
                        onClick = { onUpgrade(item.instanceId) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = animalUpgradeA11y
                        }
                    ) {
                        Text(stringResource(R.string.shell_creature_grow_with_pearls_cost, cost))
                    }
                } else {
                    Text(stringResource(R.string.shell_creature_not_swimming_lifetime_remains))
                }
            } else {
                Text(stringResource(R.string.shell_form_label, currentTitle))

                if (sourceText != null) {
                    Text(sourceText)
                }

                if (next != null && def != null) {
                    val nextTitle = stringResource(next.titleRes)
                    val upgradeVerb = stringResource(next.upgradeVerbRes)
                    val upgradeDescription = upgradeA11yLabel(def)
                    val canAfford = pearlBalance >= next.pearlCost

                    Text(stringResource(R.string.shell_next_form, nextTitle))

                    if (!canAfford) {
                        Text(stringResource(R.string.shell_need_more_pearls, next.pearlCost - pearlBalance))
                    }

                    Button(
                        onClick = { onUpgrade(item.instanceId) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = upgradeDescription
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.shell_upgrade_with_pearls,
                                upgradeVerb,
                                next.pearlCost
                            )
                        )
                    }
                } else {
                    Text(restingCurrentFormLabel(def))
                }
            }

            if (displayed) {
                OutlinedButton(onClick = { onReturn(item.instanceId) }) {
                    Text(returnToChestText)
                }
            } else if (onPlaceInFocus != null && canDisplayInstance(item, def)) {
                OutlinedButton(onClick = onPlaceInFocus) {
                    Text(placeInFocusText)
                }
            }
        }
    }
}

private enum class ShellChestTab { ALL, ANIMALS, ROOM_OBJECTS }

@Composable