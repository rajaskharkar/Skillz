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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
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
    val latestEditorState = rememberUpdatedState(state)
    val confirmSheetValueChange: (SheetValue) -> Boolean = remember {
        { target ->
            val current = latestEditorState.value
            target != SheetValue.Hidden || (!current.isSaving && !current.isDirty)
        }
    }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = confirmSheetValueChange
    )
    val keyboard = LocalSoftwareKeyboardController.current
    val hideKeyboard = { keyboard?.hide(); Unit }
    val titleFocus = remember { FocusRequester() }
    val summaryFocus = remember { FocusRequester() }
    val outcomeFocus = remember { FocusRequester() }
    val highlightFocus = remember { FocusRequester() }
    val nextStepFocus = remember { FocusRequester() }
    val focusSummary = { summaryFocus.requestFocus(); Unit }
    val focusOutcome = { outcomeFocus.requestFocus(); Unit }
    val focusHighlight = { highlightFocus.requestFocus(); Unit }
    val focusNextStep = { nextStepFocus.requestFocus(); Unit }
    var focusedField by remember { mutableStateOf<ArcEditorField?>(null) }
    val containFormScroll = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset = Offset(x = 0f, y = available.y)

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
                Velocity(x = 0f, y = available.y)
        }
    }

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
                Modifier.weight(1f).fillMaxWidth().background(surface).nestedScroll(containFormScroll)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.loadErrorResId != null -> {
                        Text(stringResource(state.loadErrorResId), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRetryLoad, enabled = !state.isSaving) { Text(stringResource(R.string.common_retry)) }
                    }
                    else -> {
                        ArcField(stringResource(R.string.arc_details_field_title), stringResource(R.string.arc_details_title_placeholder), state.title, ArcMetadata.TITLE_LIMIT, 1, ImeAction.Next, !state.isSaving, titleFocus, { focusedField = if (it) ArcEditorField.Title else focusedField }, focusSummary) { update { s -> s.copy(title = it) } }
                        ArcField(stringResource(R.string.arc_details_field_summary), stringResource(R.string.arc_details_summary_placeholder), state.summary, ArcMetadata.SUMMARY_LIMIT, 3, if (state.reflectionExpanded) ImeAction.Next else ImeAction.Done, !state.isSaving, summaryFocus, { focusedField = if (it) ArcEditorField.Summary else focusedField }, if (state.reflectionExpanded) focusOutcome else hideKeyboard) { update { s -> s.copy(summary = it) } }
                        val expansionState = stringResource(if (state.reflectionExpanded) R.string.a11y_expanded else R.string.a11y_collapsed)
                        TextButton(onClick = {
                            if (state.reflectionExpanded && focusedField in ArcEditorField.reflectionFields) {
                                summaryFocus.requestFocus()
                            }
                            update { it.copy(reflectionExpanded = !it.reflectionExpanded) }
                        }, enabled = !state.isSaving, modifier = Modifier.semantics { stateDescription = expansionState }) {
                            Text(stringResource(if (state.reflectionExpanded) R.string.arc_details_hide_reflection else R.string.arc_details_add_reflection))
                        }
                        AnimatedVisibility(state.reflectionExpanded) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                ArcField(stringResource(R.string.arc_details_field_outcome), stringResource(R.string.arc_details_outcome_placeholder), state.outcome, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Next, !state.isSaving, outcomeFocus, { focusedField = if (it) ArcEditorField.Outcome else focusedField }, focusHighlight) { update { s -> s.copy(outcome = it) } }
                                ArcField(stringResource(R.string.arc_details_field_highlight), stringResource(R.string.arc_details_highlight_placeholder), state.highlight, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Next, !state.isSaving, highlightFocus, { focusedField = if (it) ArcEditorField.Highlight else focusedField }, focusNextStep) { update { s -> s.copy(highlight = it) } }
                                ArcField(stringResource(R.string.arc_details_field_next_step), stringResource(R.string.arc_details_next_step_placeholder), state.nextStep, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Done, !state.isSaving, nextStepFocus, { focusedField = if (it) ArcEditorField.NextStep else focusedField }, hideKeyboard) { update { s -> s.copy(nextStep = it) } }
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
private fun ArcField(label: String, placeholder: String, value: String, limit: Int, minLines: Int, imeAction: ImeAction, enabled: Boolean, focusRequester: FocusRequester, onFocusChanged: (Boolean) -> Unit, onImeAction: () -> Unit, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= limit) onChange(it) }, enabled = enabled,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onFocusChanged { onFocusChanged(it.isFocused) },
        label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = minLines == 1, minLines = minLines,
        supportingText = { if (value.length >= limit - 10) Text(stringResource(R.string.arc_details_character_count, value.length, limit)) },
        keyboardOptions = KeyboardOptions(imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }))
}

private enum class ArcEditorField {
    Title, Summary, Outcome, Highlight, NextStep;

    companion object {
        val reflectionFields = setOf(Outcome, Highlight, NextStep)
    }
}
