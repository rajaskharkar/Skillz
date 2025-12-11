package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.ui.viewmodel.FocusOnViewModel
import com.kingkharnivore.skillz.ui.viewmodel.StopwatchState
import com.kingkharnivore.skillz.utils.score.ScoreBreakdown
import com.kingkharnivore.skillz.utils.score.ScoreCalculator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusOnScreen(
    viewModel: FocusOnViewModel,
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

    var showPointsDialog by remember { mutableStateOf(false) }
    var lastBreakdown by remember { mutableStateOf<ScoreBreakdown?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Focus On") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            onCancel()
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                onReset = { viewModel.resetStopwatch() },
                viewModel
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
                    text = "Focus Mode active. You may use other parts oft this app.\n" +
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
                    enabled = uiState.title.isNotBlank() && uiState.tagName.isNotBlank() && !isSaving && !isInFocusMode,
                    onClick = {
                        val durationMs = stopwatchState.elapsedMs.coerceAtLeast(0L)
                        val tenMinutesMs = 10 * 60_000L

                        if (durationMs >= tenMinutesMs) {
                            // Long session → compute and show points summary
                            val breakdown = ScoreCalculator.breakdownFromDuration(durationMs)
                            lastBreakdown = breakdown
                            showPointsDialog = true
                        } else {
                            // Short session → behave as before
                            viewModel.saveSession(onDone)
                        }
                    },
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

    // 🟢 "Points earned" popup for sessions >= 10 minutes
    if (showPointsDialog && lastBreakdown != null) {
        val breakdown = lastBreakdown!!

        AlertDialog(
            // 🔒 No way to dismiss: back + outside taps are intercepted,
            // onDismissRequest does NOT change state
            onDismissRequest = {
                // Do nothing: keep dialog shown until user taps "Nice!"
            },
            title = { Text("Session complete!") },
            text = {
                Column {
                    Text("Here’s what you earned this session:")
                    Spacer(Modifier.height(8.dp))

                    Text("Time: ${breakdown.minutes} min")
                    Text("10-min bonuses: ${breakdown.tenMinuteBonuses}")
                    Text("30-min bonuses: ${breakdown.thirtyMinuteBonuses}")
                    Text("60-min bonuses: ${breakdown.sixtyMinuteBonuses}")

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Total points: ${breakdown.totalPoints}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // user acknowledges → save and exit
                        showPointsDialog = false
                        viewModel.saveSession(onDone)
                    }
                ) {
                    Text("End")
                }
            },
            // 🔒 No dismiss button = no "cancel" path
            dismissButton = {}
        )
    }
}

@Composable
fun TagSuggestionRow(
    tags: List<TagEntity>,
    onTagClicked: (TagEntity) -> Unit
) {
    if (tags.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Tap a skill:",
            style = MaterialTheme.typography.labelSmall
        )

        LazyRow(
            modifier = Modifier
                .fillMaxWidth(),                  // 👈 important
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(
                items = tags,
                key = { it.id }                  // 👈 good practice
            ) { tag ->
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
    onReset: () -> Unit,
    viewModel: FocusOnViewModel
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
            OutlinedButton(
                onClick = {
                    val threshold = 2 * 60_000L // 2 minutes in ms
                    if (state.elapsedMs >= threshold) {
                        // More than 2 minutes → ask "Are you sure?"
                        showResetConfirm = true
                    } else {
                        // Under 2 minutes → reset directly
                        viewModel.resetStopwatch()
                    }
                },
                enabled = state.elapsedMs > 0L && !state.isRunning
            ) {
                Text("Reset")
            }
        }

        if (showResetConfirm) {
            val minutes = (state.elapsedMs / 60_000L).toInt()
            AlertDialog(
                onDismissRequest = { showResetConfirm = false },
                title = { Text("Reset session?") },
                text = {
                    Text("You've already focused for $minutes minute${if (minutes != 1) "s" else ""}. Are you sure you want to reset and lose this progress?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showResetConfirm = false
                            viewModel.resetStopwatch()
                        }
                    ) {
                        Text("Yes, reset")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showResetConfirm = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
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