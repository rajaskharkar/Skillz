package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.TagEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSkillScreen(
    viewModel: AddSessionViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagName by remember { mutableStateOf("") }
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val tags by viewModel.tags.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Session") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Session title") },
                modifier = Modifier.fillMaxWidth()
            )
            if (tags.isNotEmpty()) {
                TagSuggestionRow(
                    tags = tags,
                    onTagClicked = { tag ->
                        tagName = tag.name
                    }
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Notes / Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Skill (tag)") },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // TODO: Stopwatch UI will go here later

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    enabled = title.isNotBlank() && tagName.isNotBlank() && !isSaving,
                    onClick = {
                        viewModel.saveSession(
                            title = title.trim(),
                            description = description.trim(),
                            tagName = tagName.trim(),
                            onDone = onDone
                        )
                    }
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }

                Button(onClick = onCancel, enabled = !isSaving) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun TagSuggestionRow(
    tags: List<TagEntity>,
    onTagClicked: (TagEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tap to insert into title:",
            style = MaterialTheme.typography.labelSmall
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                AssistChip(
                    onClick = { onTagClicked(tag) },
                    label = { Text(tag.name) }
                )
            }
        }
    }
}