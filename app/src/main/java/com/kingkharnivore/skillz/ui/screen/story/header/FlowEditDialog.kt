package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.story.SessionEditState

@Composable
fun FlowEditDialog(
    editState: SessionEditState,
    onSave: (sessionId: Long, newText: String) -> Unit
) {
    val session = editState.editingSession.value ?: return

    AlertDialog(
        onDismissRequest = { editState.stopEditing() },
        title = { Text("Edit description") },
        text = {
            Column {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editState.editText.value,
                    onValueChange = { editState.editText.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("Add notes about this session") }
                )
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