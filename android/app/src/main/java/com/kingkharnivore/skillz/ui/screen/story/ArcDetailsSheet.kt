@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.ArcMetadata
import com.kingkharnivore.skillz.viewmodel.ArcEditorUiState

@Composable
fun ArcDetailsSheet(
    state: ArcEditorUiState,
    update: ((ArcEditorUiState) -> ArcEditorUiState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onRetryLoad: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit
) {
    if (state.arcId == null) return
    val surface = MaterialTheme.colorScheme.surface
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { target -> target != SheetValue.Hidden || (!state.isSaving && !state.isDirty) }
    )
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val moveNext = { focusManager.moveFocus(FocusDirection.Down); Unit }
    val hideKeyboard = { keyboard?.hide(); Unit }

    ModalBottomSheet(
        onDismissRequest = { if (!state.isSaving) onDismiss() },
        sheetState = sheetState,
        containerColor = surface
    ) {
        Column(
            Modifier.fillMaxWidth().fillMaxHeight(0.92f).background(surface).imePadding()
        ) {
            Row(
                Modifier.fillMaxWidth().background(surface).padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.arc_details_title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.arc_details_supporting_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onDismiss, enabled = !state.isSaving) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.arc_details_close))
                }
            }

            Column(
                Modifier.weight(1f).fillMaxWidth().background(surface).verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.loadErrorResId != null -> {
                        Text(stringResource(state.loadErrorResId), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetryLoad) { Text(stringResource(R.string.common_retry)) }
                    }
                    else -> {
                        ArcField(stringResource(R.string.arc_details_field_title), stringResource(R.string.arc_details_title_placeholder), state.title, ArcMetadata.TITLE_LIMIT, 1, ImeAction.Next, moveNext) { update { s -> s.copy(title = it) } }
                        ArcField(stringResource(R.string.arc_details_field_summary), stringResource(R.string.arc_details_summary_placeholder), state.summary, ArcMetadata.SUMMARY_LIMIT, 3, if (state.reflectionExpanded) ImeAction.Next else ImeAction.Done, if (state.reflectionExpanded) moveNext else hideKeyboard) { update { s -> s.copy(summary = it) } }
                        val expansionState = stringResource(if (state.reflectionExpanded) R.string.a11y_expanded else R.string.a11y_collapsed)
                        TextButton(onClick = { update { it.copy(reflectionExpanded = !it.reflectionExpanded) } }, modifier = Modifier.semantics { stateDescription = expansionState }) {
                            Text(stringResource(if (state.reflectionExpanded) R.string.arc_details_hide_reflection else R.string.arc_details_add_reflection))
                        }
                        AnimatedVisibility(state.reflectionExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                ArcField(stringResource(R.string.arc_details_field_outcome), stringResource(R.string.arc_details_outcome_placeholder), state.outcome, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Next, moveNext) { update { s -> s.copy(outcome = it) } }
                                ArcField(stringResource(R.string.arc_details_field_highlight), stringResource(R.string.arc_details_highlight_placeholder), state.highlight, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Next, moveNext) { update { s -> s.copy(highlight = it) } }
                                ArcField(stringResource(R.string.arc_details_field_next_step), stringResource(R.string.arc_details_next_step_placeholder), state.nextStep, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Done, hideKeyboard) { update { s -> s.copy(nextStep = it) } }
                            }
                        }
                        state.errorResId?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Box(Modifier.fillMaxWidth().background(surface).navigationBarsPadding().padding(20.dp)) {
                Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth()) {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text(stringResource(R.string.arc_details_save_changes))
                }
            }
        }
    }
    if (state.showDiscardConfirmation && !state.isSaving) {
        AlertDialog(onDismissRequest = onKeepEditing, title = { Text(stringResource(R.string.arc_details_discard_title)) },
            text = { Text(stringResource(R.string.arc_details_discard_message)) },
            confirmButton = { TextButton(onClick = onDiscard) { Text(stringResource(R.string.arc_details_discard)) } },
            dismissButton = { TextButton(onClick = onKeepEditing) { Text(stringResource(R.string.arc_details_keep_editing)) } })
    }
}

@Composable
private fun ArcField(label: String, placeholder: String, value: String, limit: Int, minLines: Int, imeAction: ImeAction, onImeAction: () -> Unit, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= limit) onChange(it) }, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = minLines == 1, minLines = minLines,
        supportingText = { if (value.length >= limit - 10) Text(stringResource(R.string.arc_details_character_count, value.length, limit)) },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }))
}
