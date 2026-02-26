@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.ui.screen.flow.reward.ArcSummaryContent
import com.kingkharnivore.skillz.ui.screen.flow.reward.SessionRewardContent
import com.kingkharnivore.skillz.viewmodel.FlowEndAction
import com.kingkharnivore.skillz.viewmodel.FlowViewModel

@Composable
fun FlowScreen(
    viewModel: FlowViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val error by viewModel.error.collectAsState()
    val tags by viewModel.suggestedTags.collectAsState()
    val reward by viewModel.lastReward.collectAsState()
    val exitAfterReward by viewModel.exitAfterReward.collectAsState()
    val awaitingNextFlow by viewModel.awaitingNextFlowAfterContinue.collectAsState()

    var showSurgeDialog by remember { mutableStateOf(false) }
    var showEndDialog by remember { mutableStateOf(false) }
    var showPointsDialog by remember { mutableStateOf(false) }

    var surgeMinutesInput by remember { mutableStateOf("") }
    var surgeMinutesInline by rememberSaveable { mutableStateOf("") }

    val stopwatchState = uiState.stopwatch
    val isInFlowState = uiState.isInFlowMode
    val hasTime = stopwatchState.elapsedMs > 0L

    LaunchedEffect(reward) {
        if (reward != null) showPointsDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flow") },
                navigationIcon = {
                    IconButton(onClick = { onCancel() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1) Title — grand, centered
            RitualCard(rotation = -0.20f, corner = 30.dp) {
                GrandTitleField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange
                )
            }

            // 2) Journeys — leaner, centered
            RitualCard(rotation = 0.12f, corner = 26.dp) {
                JourneyLean(
                    tags = tags,
                    tagName = uiState.tagName,
                    onTagClicked = { tag -> viewModel.onTagNameChange(tag.name) },
                    onTagNameChange = viewModel::onTagNameChange
                )
            }

            // 3) Core — timer center + reset near + enter flow + surge (small/right)
            RitualFrame(rotation = -0.08f, corner = 32.dp, showBorder = false) {

                if (uiState.isInArc && uiState.arcMultiplier != null) {
                    ArcPill(
                        arcMultiplier = uiState.arcMultiplier!!,
                        arcNextIndex = uiState.arcNextIndex
                    )
                    Spacer(Modifier.height(10.dp))
                }

                StopwatchSection(
                    state = stopwatchState,
                    viewModel = viewModel
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { if (isInFlowState) viewModel.exitFocusMode() else viewModel.enterFocusMode() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (isInFlowState) "Exit Flow" else "Enter Flow",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Surge: discreet when off + right aligned reliably
                if (BuildConfig.FLAVOR != "aera") {
                    val locked = viewModel.isSurgeLocked()

                    Spacer(Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        SurgeMiniControl(
                            modifier = Modifier.align(Alignment.CenterEnd),
                            isInFlow = isInFlowState,
                            elapsedMs = stopwatchState.elapsedMs,
                            locked = locked,
                            isSurgeOn = uiState.isSurgeOn,
                            plannedMs = uiState.surgePlannedMs,
                            minutesInline = surgeMinutesInline,
                            onMinutesChange = { raw -> surgeMinutesInline = raw.filter(Char::isDigit).take(3) },
                            onCommit = {
                                val mins = surgeMinutesInline.toIntOrNull()
                                if (mins != null && mins > 0 && !locked && !isInFlowState) {
                                    viewModel.setSurgePlannedMinutes(mins)
                                }
                            },
                            onToggleOff = {
                                if (!locked && !isInFlowState) {
                                    viewModel.clearSurgeIfAllowed()
                                    surgeMinutesInline = ""
                                }
                            },
                            onLongPress = { showSurgeDialog = true }
                        )
                    }
                }

                if (isInFlowState) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "You're in Flow. You may use other parts of this app.\nYou may turn off the screen — the timer continues.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 4) Description — inviting UI (same text)
            RitualCard(rotation = 0.16f, corner = 28.dp) {
                ChronicleField(
                    value = uiState.description,
                    onValueChange = viewModel::onDescriptionChange
                )
            }

            if (error != null) {
                Text(
                    text = error ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Bottom actions — unchanged logic
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    enabled = uiState.title.isNotBlank() &&
                            uiState.tagName.isNotBlank() &&
                            hasTime &&
                            !isSaving &&
                            !isInFlowState,
                    onClick = { viewModel.onEndFlowClicked(FlowEndAction.CONTINUE_ARC) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text("Continue Arc")
                }

                val isArcActuallyCompletable = uiState.isInArc
                val completeLabel = if (isArcActuallyCompletable) "Complete Arc" else "Complete Flow"
                val completeAction = if (isArcActuallyCompletable) {
                    FlowEndAction.COMPLETE_ARC
                } else {
                    FlowEndAction.SAVE_FLOW
                }

                Button(
                    enabled = uiState.title.isNotBlank() &&
                            uiState.tagName.isNotBlank() &&
                            hasTime &&
                            !isSaving &&
                            !isInFlowState,
                    onClick = { viewModel.onEndFlowClicked(completeAction) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (isSaving) "Saving..." else completeLabel)
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    // 🔴 End dialog (kept as-is)
    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("End Focus Session?") },
            text = { Text("The stopwatch is still running. Are you sure you want to end and leave this screen?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pauseStopwatch()
                        viewModel.resetStopwatch()
                        viewModel.exitFocusMode()
                        showEndDialog = false
                        onCancel()
                    }
                ) { Text("End") }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) { Text("Continue") }
            }
        )
    }

    // 🟢 Rewards dialog (unchanged)
    if (showPointsDialog && reward != null) {
        val r = reward!!
        var showArcSummary by remember(r) { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { /* locked */ },
            title = { Text(if (showArcSummary && r.arcSummary != null) "Arc Reward" else "You did it!") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 0.75f * LocalConfiguration.current.screenHeightDp.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showArcSummary && r.arcSummary != null) {
                        ArcSummaryContent(
                            arc = r.arcSummary!!,
                            isAera = BuildConfig.FLAVOR == "aera"
                        )
                    } else {
                        SessionRewardContent(
                            r = r,
                            isAera = BuildConfig.FLAVOR == "aera"
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (r.arcSummary != null && !showArcSummary) {
                            showArcSummary = true
                        } else {
                            showPointsDialog = false
                            if (awaitingNextFlow) {
                                viewModel.beginNextFlowAfterContinue()
                            } else {
                                viewModel.clearLastReward()
                                if (exitAfterReward && viewModel.consumeExitAfterReward()) onDone()
                            }
                        }
                    }
                ) { Text(if (r.arcSummary != null && !showArcSummary) "Next" else "Done") }
            },
            dismissButton = {}
        )
    }

    // Surge dialog (kept)
    if (showSurgeDialog) {
        val locked = viewModel.isSurgeLocked()
        val currentMinutes = (uiState.surgePlannedMs ?: 0L) / 60_000L

        AlertDialog(
            onDismissRequest = { showSurgeDialog = false },
            title = { Text("Surge") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (locked) {
                        Text("Surge is locked on once time starts.")
                        Text("Planned: $currentMinutes min")
                    } else {
                        Text("Set a planned time limit. Finish early to earn Surge Points.")

                        OutlinedTextField(
                            value = surgeMinutesInput,
                            onValueChange = { surgeMinutesInput = it.filter(Char::isDigit) },
                            label = { Text("Surge Time (minutes)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.isSurgeOn) Text("Current: $currentMinutes min")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!locked) {
                            val mins = surgeMinutesInput.toIntOrNull()
                            if (mins != null && mins > 0) viewModel.setSurgePlannedMinutes(mins)
                        }
                        showSurgeDialog = false
                    }
                ) { Text(if (locked) "OK" else "Set") }
            },
            dismissButton = {
                if (!locked && uiState.isSurgeOn) {
                    TextButton(
                        onClick = {
                            viewModel.clearSurgeIfAllowed()
                            showSurgeDialog = false
                        }
                    ) { Text("Turn Off") }
                }
            }
        )
    }
}

fun formatMsAsMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
