@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.kingkharnivore.skillz.ui.screen.story

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.ArcMetadata
import com.kingkharnivore.skillz.viewmodel.ArcEditorUiState

@Composable
fun ArcDetailsSheet(
    state: ArcEditorUiState,
    update: ((ArcEditorUiState) -> ArcEditorUiState) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onKeepEditing: () -> Unit,
    onDiscard: () -> Unit
) {
    if (state.arcId == null) return
    val keyboard = LocalSoftwareKeyboardController.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding()) {
            Column(
                Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text("Arc details", style = MaterialTheme.typography.titleLarge)
                        Text("Add context or capture what this Arc meant to you.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, "Close Arc details") }
                }
                ArcField("Title", "Give this Arc a title", state.title, ArcMetadata.TITLE_LIMIT, 1, ImeAction.Next) { update { s -> s.copy(title = it) } }
                ArcField("Summary", "What was this Arc about?", state.summary, ArcMetadata.SUMMARY_LIMIT, 3, if (state.reflectionExpanded) ImeAction.Next else ImeAction.Done, onDone = { keyboard?.hide() }) { update { s -> s.copy(summary = it) } }
                TextButton(
                    onClick = { update { it.copy(reflectionExpanded = !it.reflectionExpanded) } },
                    modifier = Modifier.semantics { stateDescription = if (state.reflectionExpanded) "Expanded" else "Collapsed" }
                ) { Text(if (state.reflectionExpanded) "Hide Arc reflection" else "Add Arc reflection") }
                AnimatedVisibility(state.reflectionExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        ArcField("Outcome", "What did you complete or move forward?", state.outcome, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Next) { update { s -> s.copy(outcome = it) } }
                        ArcField("Highlight", "What stood out during this Arc?", state.highlight, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Next) { update { s -> s.copy(highlight = it) } }
                        ArcField("Next step", "What would you like to continue later?", state.nextStep, ArcMetadata.REFLECTION_LIMIT, 3, ImeAction.Done, onDone = { keyboard?.hide() }) { update { s -> s.copy(nextStep = it) } }
                    }
                }
                state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            Button(onClick = onSave, enabled = state.canSave, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                if (state.isSaving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Text("Save changes")
            }
        }
    }
    if (state.showDiscardConfirmation) {
        AlertDialog(onDismissRequest = onKeepEditing, title = { Text("Discard Arc changes?") },
            text = { Text("Your unsaved Arc details will be lost.") },
            confirmButton = { TextButton(onClick = onDiscard) { Text("Discard") } },
            dismissButton = { TextButton(onClick = onKeepEditing) { Text("Keep editing") } })
    }
}

@Composable
private fun ArcField(label: String, placeholder: String, value: String, limit: Int, minLines: Int, imeAction: ImeAction, onDone: () -> Unit = {}, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = { if (it.length <= limit) onChange(it) }, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, placeholder = { Text(placeholder) }, singleLine = minLines == 1, minLines = minLines,
        supportingText = { if (value.length >= limit - 10) Text("${value.length}/$limit") },
        keyboardOptions = KeyboardOptions(imeAction = imeAction), keyboardActions = KeyboardActions(onDone = { onDone() }))
}
