package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.screen.story.PulseEditState
import com.kingkharnivore.skillz.viewmodel.TagUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseEditSheet(
    editState: PulseEditState,
    tags: List<TagUiModel>,
    onSave: (pulseId: Long, title: String, description: String, tagName: String) -> Unit,
) {
    val pulse = editState.editingPulse.value ?: return

    ModalBottomSheet(
        onDismissRequest = { editState.stopEditing() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Edit Pulse",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Refine the moment, tag, or meaning.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                    )
                }
            }

            PulseEditTitleField(
                value = editState.editTitle.value,
                onValueChange = { editState.editTitle.value = it }
            )

            PulseEditJourneyField(
                tags = tags,
                tagName = editState.editTagName.value,
                onTagClicked = { tag -> editState.editTagName.value = tag.name },
                onTagNameChange = { editState.editTagName.value = it }
            )

            OutlinedTextField(
                value = editState.editDescription.value,
                onValueChange = { editState.editDescription.value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 5,
                maxLines = 8,
                label = { Text("Description") },
                placeholder = { Text("Refine the insight, feeling, or learning…") },
                shape = RoundedCornerShape(20.dp)
            )

            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = { editState.stopEditing() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(
                            pulse.pulseId,
                            editState.editTitle.value,
                            editState.editDescription.value,
                            editState.editTagName.value
                        )
                        editState.stopEditing()
                    },
                    enabled = editState.editTitle.value.isNotBlank() ||
                            editState.editDescription.value.isNotBlank(),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun PulseEditTitleField(
    value: String,
    onValueChange: (String) -> Unit
) {
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.primary
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Enter your title",
            style = MaterialTheme.typography.labelLarge.copy(
                letterSpacing = 0.6.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        )

        TextField(
            value = value,
            onValueChange = { onValueChange(it.take(60)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "What was the moment?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                letterSpacing = 0.2.sp
            ),
            colors = colors
        )
    }
}

@Composable
private fun PulseEditJourneyField(
    tags: List<TagUiModel>,
    tagName: String,
    onTagClicked: (TagUiModel) -> Unit,
    onTagNameChange: (String) -> Unit
) {
    Text(
        text = if (tags.size > 1) "Journeys" else "Journey",
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )

    val cleanTags = remember(tags) { tags.filter { it.name.isNotBlank() } }

    if (cleanTags.isNotEmpty()) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 6.dp)
        ) {
            items(items = cleanTags, key = { it.id }) { tag ->
                AssistChip(
                    onClick = { onTagClicked(tag) },
                    label = { Text(tag.name) }
                )
            }
        }
    }

    OutlinedTextField(
        value = tagName,
        onValueChange = onTagNameChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Journey") },
        placeholder = { Text("Leave blank for untagged") },
        singleLine = true,
        shape = RoundedCornerShape(20.dp)
    )
}