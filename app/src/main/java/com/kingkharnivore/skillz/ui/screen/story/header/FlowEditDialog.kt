package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.story.SessionEditState

@Composable
fun FlowEditDialog(
    editState: SessionEditState,
    onSave: (sessionId: Long, newText: String) -> Unit
) {
    val session = editState.editingSession.value ?: return

    val shape = RoundedCornerShape(24.dp)
    val containerColor = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val subtleBg = MaterialTheme.colorScheme.surfaceVariant
    val subtleText = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = { editState.stopEditing() },
        shape = shape,
        containerColor = containerColor,
        tonalElevation = 2.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Edit notes",
                    style = MaterialTheme.typography.titleLarge,
                    color = onSurface
                )
                Text(
                    text = "Refine what happened in this Flow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = subtleText
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Session title "card"
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = subtleBg,
                    tonalElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Flow",
                            style = MaterialTheme.typography.labelMedium,
                            color = subtleText
                        )
                        Text(
                            text = session.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = onSurface
                        )
                    }
                }

                // Field "frame"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(subtleBg)
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.labelMedium,
                                color = subtleText
                            )
                            Text(
                                text = "${editState.editText.value.length} chars",
                                style = MaterialTheme.typography.labelSmall,
                                color = subtleText
                            )
                        }

                        OutlinedTextField(
                            value = editState.editText.value,
                            onValueChange = { editState.editText.value = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            placeholder = {
                                Text(
                                    "Add what you did, how it felt, what you learned…",
                                    color = subtleText
                                )
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(session.sessionId, editState.editText.value)
                    editState.stopEditing()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { editState.stopEditing() }) { Text("Cancel") }
        }
    )
}