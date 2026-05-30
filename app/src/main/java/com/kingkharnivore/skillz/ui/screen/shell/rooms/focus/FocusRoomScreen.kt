package com.kingkharnivore.skillz.ui.screen.shell.rooms.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.kingkharnivore.skillz.data.model.shell.ShellSlotDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellSlotType
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.TurtleShellInteriorBackground
import com.kingkharnivore.skillz.ui.screen.shell.ux.ObjectCopySheet
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.canDisplayInstance
import com.kingkharnivore.skillz.ui.screen.shell.ux.restingFinds
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

@Composable
fun FocusRoomScreen(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptySlotSheet(
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
private fun FocusRoomCarvedShelves(
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
private fun SlotChip(
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