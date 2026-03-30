package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.model.ui.PulseListItemUiModel
import com.kingkharnivore.skillz.ui.screen.story.SessionEditState
import com.kingkharnivore.skillz.ui.screen.story.chronicle.PulseCard
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowDetailsSheet(
    editState: SessionEditState,
    tags: List<TagUiModel>,
    childPulses: List<PulseListItemUiModel>,
    onSaveNotes: (sessionId: Long, newText: String) -> Unit,
    onCreatePulse: (sessionId: Long, title: String, description: String, tagName: String) -> Unit,
    onDeletePulse: (Long) -> Unit,
    onEditPulse: (PulseListItemUiModel) -> Unit
) {
    val session = editState.editingSession.value ?: return

    var pulseTitle by rememberSaveable(session.sessionId) { mutableStateOf("") }
    var pulseDescription by rememberSaveable(session.sessionId) { mutableStateOf("") }
    var pulseTagName by rememberSaveable(session.sessionId) { mutableStateOf("") }
    var showPulseComposer by rememberSaveable(session.sessionId) { mutableStateOf(false) }

    LaunchedEffect(session.sessionId) {
        editState.editText.value = session.description
    }

    ModalBottomSheet(
        onDismissRequest = { editState.stopEditing() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color = if (session.isSoftMode) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (session.isSoftMode) {
                    MaterialTheme.colorScheme.onSecondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (session.isSoftMode) "Soft Flow" else "Flow",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (session.tagName.isNotBlank()) {
                        Text(
                            text = session.tagName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = editState.editText.value,
                    onValueChange = { editState.editText.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8,
                    placeholder = {
                        Text("Refine what happened in this Flow.")
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { editState.stopEditing() }
                    ) { Text("Close") }

                    Button(
                        onClick = {
                            onSaveNotes(session.sessionId, editState.editText.value)
                        }
                    ) { Text("Save Notes") }
                }
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Pulses",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Moments captured in or added to this Flow",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                        )
                    }

                    Text(
                        text = childPulses.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (childPulses.isEmpty()) {
                    Text(
                        text = "No pulses yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        childPulses.forEach { pulse ->
                            PulseCard(
                                pulse = pulse,
                                isExpanded = false,
                                onToggleExpand = {},
                                onLongPress = { onEditPulse(pulse) },
                                onDeletePulse = { onDeletePulse(pulse.pulseId) },
                                nested = true
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showPulseComposer = !showPulseComposer },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showPulseComposer) "Hide Pulse Composer" else "Add Pulse")
                }

                if (showPulseComposer) {
                    OutlinedTextField(
                        value = pulseTitle,
                        onValueChange = { pulseTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Pulse title") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = pulseTagName,
                        onValueChange = { pulseTagName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Journey (optional)") },
                        singleLine = true,
                        supportingText = {
                            val suggestions = tags.take(6).joinToString(" • ") { it.name }
                            if (suggestions.isNotBlank()) {
                                Text("Suggestions: $suggestions")
                            }
                        }
                    )

                    OutlinedTextField(
                        value = pulseDescription,
                        onValueChange = { pulseDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        label = { Text("Pulse description") }
                    )

                    Button(
                        enabled = pulseTitle.isNotBlank() && pulseDescription.isNotBlank(),
                        onClick = {
                            onCreatePulse(
                                session.sessionId,
                                pulseTitle,
                                pulseDescription,
                                pulseTagName
                            )
                            pulseTitle = ""
                            pulseDescription = ""
                            pulseTagName = ""
                            showPulseComposer = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Pulse")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}