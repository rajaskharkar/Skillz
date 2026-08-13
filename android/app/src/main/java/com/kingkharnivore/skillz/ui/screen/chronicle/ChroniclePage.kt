package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.ChronicleMomentEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChroniclePage(holder: ChronicleStateHolder, modifier: Modifier = Modifier) {
    val state by holder.state.collectAsStateWithLifecycle()
    var deleteTarget by remember { mutableStateOf<ChronicleMomentEntity?>(null) }
    val moveUpLabel = stringResource(R.string.chronicle_move_up)
    val moveDownLabel = stringResource(R.string.chronicle_move_down)
    val removeLabel = stringResource(R.string.chronicle_remove_action)
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()
    val displayedMoments = remember(state.moments, state.stagedOrder) {
        if (state.stagedOrder.isEmpty()) state.moments else {
            val byId = state.moments.associateBy { it.id }
            state.stagedOrder.mapNotNull(byId::get)
        }
    }
    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 28.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (displayedMoments.isEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.chronicle_empty_title), style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.chronicle_empty_body), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary)
            }
        }
        itemsIndexed(displayedMoments, key = { _, item -> item.id }) { index, moment ->
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                if (state.editingId == moment.id) {
                    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextField(value = state.editingText, onValueChange = holder::editText,
                                modifier = Modifier.fillMaxWidth(), colors = chronicleTextFieldColors())
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { deleteTarget = moment }) { Text(stringResource(R.string.chronicle_remove)) }
                                TextButton(onClick = holder::cancelEdit) { Text(stringResource(R.string.common_cancel)) }
                                Button(onClick = holder::finishEdit, enabled = state.editingText.isNotBlank()) {
                                    Text(stringResource(R.string.common_done))
                                }
                            }
                        }
                    }
                } else {
                    val isDragging = state.draggingId == moment.id
                    Surface(
                        color = if (isDragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = if (isDragging) 6.dp else 0.dp
                    ) { Text(
                        text = moment.text.orEmpty(), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().semantics {
                            customActions = listOf(
                                CustomAccessibilityAction(moveUpLabel) { holder.move(moment, -1); true },
                                CustomAccessibilityAction(moveDownLabel) { holder.move(moment, 1); true },
                                CustomAccessibilityAction(removeLabel) { deleteTarget = moment; true }
                            )
                        }.combinedClickable(onClick = { holder.beginEdit(moment) }, onLongClick = null)
                            .pointerInput(moment.id, state.editingId) {
                                if (state.editingId == null) detectDragGesturesAfterLongPress(
                                    onDragStart = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); holder.startDrag(moment) },
                                    onDragEnd = holder::finishDrag,
                                    onDragCancel = holder::cancelDrag,
                                    onDrag = { change, amount ->
                                        change.consume()
                                        val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == moment.id }
                                        holder.dragByPixels(amount.y, visible?.size?.toFloat()?.coerceAtLeast(1f) ?: 96f)
                                        if (visible != null) {
                                            val viewport = listState.layoutInfo
                                            val nearBottom = visible.offset + visible.size > viewport.viewportEndOffset - 48
                                            val nearTop = visible.offset < viewport.viewportStartOffset + 48
                                            if ((nearBottom && amount.y > 0) || (nearTop && amount.y < 0)) {
                                                launch { listState.scrollBy(amount.y) }
                                            }
                                        }
                                    }
                                )
                            }
                            .padding(vertical = 8.dp)
                    ) }
                }
                if (index < displayedMoments.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.secondary.copy(alpha = if (state.draggingId != null) .55f else .22f))
            }
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextField(value = state.draft, onValueChange = holder::setDraft,
                        enabled = state.editingId == null && !state.isCommitting,
                        placeholder = { Text(stringResource(R.string.chronicle_write_placeholder)) },
                        modifier = Modifier.fillMaxWidth(), colors = chronicleTextFieldColors())
                    Button(onClick = { holder.add() }, enabled = state.draft.isNotBlank() && !state.isCommitting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = .25f),
                            disabledContentColor = MaterialTheme.colorScheme.onSecondary), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.chronicle_add))
                    }
                    if (state.hasError) Text(stringResource(R.string.chronicle_save_error), color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
    deleteTarget?.let { moment ->
        AlertDialog(onDismissRequest = { deleteTarget = null }, containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(R.string.chronicle_remove_title), color = MaterialTheme.colorScheme.primary) },
            confirmButton = { TextButton(onClick = { holder.delete(moment); holder.cancelEdit(); deleteTarget = null }) { Text(stringResource(R.string.chronicle_remove)) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.common_cancel)) } })
    }
}

@Composable private fun chronicleTextFieldColors() = TextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.primary, unfocusedTextColor = MaterialTheme.colorScheme.primary,
    disabledTextColor = MaterialTheme.colorScheme.secondary,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    disabledContainerColor = MaterialTheme.colorScheme.surface,
    focusedIndicatorColor = MaterialTheme.colorScheme.secondary, unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = .35f),
    disabledIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = .20f),
    focusedPlaceholderColor = MaterialTheme.colorScheme.secondary, unfocusedPlaceholderColor = MaterialTheme.colorScheme.secondary,
    disabledPlaceholderColor = MaterialTheme.colorScheme.secondary.copy(alpha = .55f)
)
