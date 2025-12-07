package com.kingkharnivore.skillz.ui.skills

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val tags by viewModel.tags.collectAsState()

    val stopwatchState = uiState.stopwatch
    val isInFocusMode = uiState.isInFocusMode

    var showEndDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = stopwatchState.isRunning) {
        showEndDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log Session") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (stopwatchState.isRunning) {
                                showEndDialog = true
                            } else {
                                onCancel()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("What's going on?") },
                modifier = Modifier.fillMaxWidth()
            )
            if (tags.isNotEmpty()) {
                TagSuggestionRow(
                    tags = tags,
                    onTagClicked = { tag ->
                        viewModel.onTagNameChange(tag.name)
                    }
                )
            }

            OutlinedTextField(
                value = uiState.tagName,
                onValueChange = viewModel::onTagNameChange,
                label = { Text("Skill (tag)") },
                modifier = Modifier.fillMaxWidth()
            )

            StopwatchSection(
                state = stopwatchState,
                onStartOrResume = { viewModel.startOrResumeStopwatch() },
                onPause = { viewModel.pauseStopwatch() },
                onReset = { viewModel.resetStopwatch() }
            )

            Button(
                onClick = {
                    if (isInFocusMode) {
                        viewModel.exitFocusMode()
                    } else {
                        viewModel.enterFocusMode()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Text(
                    text = if (isInFocusMode) "Exit Focus Mode" else "Enter Focus Mode",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isInFocusMode) {
                Text(
                    text = "Focus Mode active. Your session is being recorded.\n" +
                            "You may turn off the screen — the timer continues.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Notes / Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    enabled = uiState.title.isNotBlank() && uiState.tagName.isNotBlank() && !isSaving,
                    onClick = { viewModel.saveSession(onDone) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isSaving) "Saving..." else "Save")
                }

                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving && !stopwatchState.isRunning, // optional: block cancel while running
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
            }
        }
    }

    // 🔴 "Are you sure you want to end?" popup
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Focus Session?") },
            text = { Text("The stopwatch is still running. Are you sure you want to end and leave this screen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Stop timer and leave
                        viewModel.pauseStopwatch()
                        viewModel.resetStopwatch()
                        viewModel.exitFocusMode()
                        showEndDialog = false
                        onCancel() // navigate back to list
                    }
                ) {
                    Text("End")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text("Continue")
                }
            }
        )
    }
}

@Composable
private fun TagSuggestionRow(
    tags: List<TagEntity>,
    onTagClicked: (TagEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tap to select skill, or enter a new one:",
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

@Composable
private fun StopwatchSection(
    state: StopwatchState,
    onStartOrResume: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Focus mode",
            style = MaterialTheme.typography.titleSmall
        )

        Text(
            text = formatElapsed(state.elapsedMs),
            style = MaterialTheme.typography.headlineMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!state.isRunning && state.elapsedMs == 0L) {
                Button(onClick = onStartOrResume) {
                    Text("Start")
                }
            } else if (state.isRunning) {
                Button(onClick = onPause) {
                    Text("Pause")
                }
            } else {
                Button(onClick = onStartOrResume) {
                    Text("Resume")
                }
            }

            OutlinedButton(
                onClick = onReset,
                enabled = state.elapsedMs > 0L && !state.isRunning
            ) {
                Text("Reset")
            }
        }
    }
}

private fun formatElapsed(elapsedMs: Long): String {
    val totalSeconds = elapsedMs / 1000L
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}