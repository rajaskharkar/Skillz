package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R

/**
 * Shared, state-hoisted Chronicle Story Canvas. [value] is the durable composer draft owned by
 * the parent state holder; committed Moment persistence is exposed separately by [onAddText].
 * The compatibility callback keeps existing Flow/Pulse restoration intact during the migration.
 */
@Composable
fun ChronicleField(
    value: String,
    onValueChange: (String) -> Unit,
    moments: List<ChronicleTextMoment> = emptyList(),
    onAddText: (String) -> Unit = {},
    onEditText: (String, String) -> Unit = { _, _ -> },
    onMove: (Int, Int) -> Unit = { _, _ -> },
    onGallery: () -> Unit = {},
    onCamera: () -> Unit = {},
    onVideo: () -> Unit = {},
    onAudio: () -> Unit = {},
    onDictate: () -> Unit = {},
    onVoiceNote: () -> Unit = {}
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var editText by rememberSaveable { mutableStateOf("") }
    val previewMoments = remember { mutableStateListOf<ChronicleTextMoment>() }
    val displayed = if (moments.isEmpty()) previewMoments else moments
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(
            text = "Chronicle",
            style = MaterialTheme.typography.headlineSmall,
            color = primary,
            modifier = Modifier.fillMaxWidth()
        )

        if (displayed.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                itemsIndexed(displayed, key = { _, item -> item.id }) { index, moment ->
                    if (editingId == moment.id) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                ChronicleTextField(editText, { editText = it }, "Edit moment")
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    TextButton(onClick = { editingId = null }) { Text("Cancel", color = secondary) }
                                    TextButton(
                                        enabled = editText.isNotBlank(),
                                        onClick = {
                                            onEditText(moment.id, editText.trim())
                                            previewMoments.indexOfFirst { it.id == moment.id }
                                                .takeIf { it >= 0 }?.let { previewMoments[it] = moment.copy(text = editText.trim()) }
                                            editingId = null
                                        }
                                    ) { Text("Done", color = primary) }
                                }
                            }
                        }
                    } else {
                        TextButton(
                            onClick = { editingId = moment.id; editText = moment.text },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(moment.text, color = primary, style = MaterialTheme.typography.bodyLarge) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(
                                enabled = index > 0,
                                onClick = { onMove(index, index - 1) },
                                modifier = Modifier.semantics {
                                    customActions = listOf(CustomAccessibilityAction("Move moment up") { onMove(index, index - 1); true })
                                }
                            ) { Icon(Icons.Outlined.KeyboardArrowUp, "Move up", tint = secondary) }
                            IconButton(
                                enabled = index < displayed.lastIndex,
                                onClick = { onMove(index, index + 1) }
                            ) { Icon(Icons.Outlined.KeyboardArrowDown, "Move down", tint = secondary) }
                        }
                    }
                    if (index < displayed.lastIndex) {
                        HorizontalDivider(color = secondary.copy(alpha = .18f))
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ChronicleTextField(value, onValueChange, "Write a moment…", enabled = editingId == null)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Row {
                        ChronicleAction(Icons.Outlined.Collections, "Choose photos and videos", editingId == null, onGallery)
                        ChronicleAction(Icons.Outlined.CameraAlt, "Take photo", editingId == null, onCamera)
                        ChronicleAction(Icons.Outlined.Videocam, "Record video", editingId == null, onVideo)
                        ChronicleAction(Icons.Outlined.AudioFile, "Import audio", editingId == null, onAudio)
                        ChronicleAction(Icons.Outlined.Mic, "Dictate; long press for voice note", editingId == null, onDictate,
                            listOf(CustomAccessibilityAction("Record voice note") { onVoiceNote(); true }))
                    }
                    Button(
                        enabled = value.isNotBlank() && editingId == null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = secondary.copy(alpha = .18f),
                            disabledContentColor = secondary
                        ),
                        onClick = {
                            val text = value.trim()
                            onAddText(text)
                            if (moments.isEmpty()) previewMoments += ChronicleTextMoment("local-${System.nanoTime()}", text)
                            onValueChange("")
                        }
                    ) { Text("Add") }
                }
            }
        }
    }
}

data class ChronicleTextMoment(val id: String, val text: String)

@Composable
private fun ChronicleTextField(value: String, onChange: (String) -> Unit, placeholder: String, enabled: Boolean = true) {
    TextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.secondary.copy(alpha = .7f)) },
        colors = TextFieldDefaults.colors(
            focusedTextColor = MaterialTheme.colorScheme.primary,
            unfocusedTextColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = .35f),
            unfocusedIndicatorColor = MaterialTheme.colorScheme.secondary.copy(alpha = .18f)
        )
    )
}

@Composable
private fun ChronicleAction(
    image: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    actions: List<CustomAccessibilityAction> = emptyList()
) = IconButton(enabled = enabled, onClick = onClick, modifier = Modifier.semantics { customActions = actions }) {
    Icon(image, label, tint = MaterialTheme.colorScheme.secondary)
}
