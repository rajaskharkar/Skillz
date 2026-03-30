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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
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
    var showPulseDialog by remember { mutableStateOf(false) }
    var showSoftArcConfirmDialog by remember { mutableStateOf(false) }

    var surgeMinutesInput by remember { mutableStateOf("") }
    var surgeMinutesInline by rememberSaveable { mutableStateOf("") }

    var pulseTitle by rememberSaveable { mutableStateOf("") }
    var pulseDescription by rememberSaveable { mutableStateOf("") }
    var pulseTagName by rememberSaveable { mutableStateOf("") }
    var attachPulseToCurrentFlow by rememberSaveable { mutableStateOf(true) }

    val stopwatchState = uiState.stopwatch
    val isInFlowState = uiState.isInFlowMode
    val hasTime = stopwatchState.elapsedMs > 0L
    val modeLocked = viewModel.isModeLocked()

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

            RitualCard(rotation = -0.20f, corner = 30.dp) {
                GrandTitleField(
                    value = uiState.title,
                    onValueChange = viewModel::onTitleChange
                )
            }

            RitualCard(rotation = 0.08f, corner = 26.dp) {
                SessionModeSelector(
                    isSoftMode = uiState.isSoftMode,
                    isLocked = modeLocked,
                    onFlowSelected = { viewModel.setSoftMode(false) },
                    onSoftSelected = {
                        if (!uiState.isSoftMode && uiState.isInArc && !modeLocked) {
                            showSoftArcConfirmDialog = true
                        } else {
                            viewModel.setSoftMode(true)
                        }
                    }
                )
            }

            RitualCard(rotation = 0.12f, corner = 26.dp) {
                JourneyLean(
                    tags = tags,
                    tagName = uiState.tagName,
                    onTagClicked = { tag -> viewModel.onTagNameChange(tag.name) },
                    onTagNameChange = viewModel::onTagNameChange
                )
            }

            RitualFrame(rotation = -0.08f, corner = 32.dp, showBorder = false) {

                if (uiState.isInArc && uiState.arcMultiplier != null) {
                    ArcPill(
                        arcMultiplier = uiState.arcMultiplier!!,
                        arcNextIndex = uiState.arcNextIndex,
                        isPending = uiState.arcIsPending,
                        graceRemainingMs = uiState.arcGraceRemainingMs,
                        pauseRemainingMs = uiState.arcPauseRemainingMs,
                        isInFlow = uiState.stopwatch.isRunning,
                        calmMode = uiState.calmMode
                    )
                    Spacer(Modifier.height(10.dp))
                }

                val shouldShowStopwatch = !uiState.calmMode || !stopwatchState.isRunning

                if (shouldShowStopwatch) {
                    StopwatchSection(
                        state = stopwatchState,
                        viewModel = viewModel,
                        showScoreUi = uiState.showScoreUi,
                        calmMode = uiState.calmMode
                    )
                } else {
                    Text(
                        text = "Calm Mode",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        if (isInFlowState) viewModel.exitFocusMode() else viewModel.enterFocusMode()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = when {
                            isInFlowState && uiState.isSoftMode -> "Exit Soft Flow"
                            isInFlowState -> "Exit Flow"
                            uiState.isSoftMode -> "Begin Soft Flow"
                            else -> "Enter Flow"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                if (BuildConfig.FLAVOR != "aera" && !uiState.isSoftMode) {
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
                            onMinutesChange = { raw ->
                                surgeMinutesInline = raw.filter(Char::isDigit).take(3)
                            },
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
                            onLongPress = { showSurgeDialog = true },
                            calmMode = uiState.calmMode
                        )
                    }
                }

                if (uiState.isSoftMode) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Soft Flow",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "This session will be recorded without score, Surge, Beam, or Arc progression.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            )
                        }
                    }
                }

                if (isInFlowState) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            pulseTitle = ""
                            pulseDescription = ""
                            pulseTagName = ""
                            attachPulseToCurrentFlow = true
                            showPulseDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text("Record Pulse")
                    }

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

            if (uiState.isSoftMode) {
                Button(
                    enabled = uiState.title.isNotBlank() &&
                            uiState.tagName.isNotBlank() &&
                            hasTime &&
                            !isSaving &&
                            !isInFlowState,
                    onClick = { viewModel.onEndFlowClicked(FlowEndAction.SAVE_FLOW) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Text(if (isSaving) "Saving..." else "Save Soft Flow")
                }
            } else {
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
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showSoftArcConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSoftArcConfirmDialog = false },
            title = { Text("Enter Soft Mode?") },
            text = {
                Text("This will complete your current Arc. Soft sessions are recorded without score or Arc progression.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSoftArcConfirmDialog = false
                        viewModel.setSoftModeAndConcludeArc()
                    }
                ) { Text("Enter Soft") }
            },
            dismissButton = {
                TextButton(onClick = { showSoftArcConfirmDialog = false }) {
                    Text("Stay in Flow")
                }
            }
        )
    }

    if (showPulseDialog) {
        AlertDialog(
            onDismissRequest = { showPulseDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Record Pulse")
                    if (isInFlowState && uiState.title.isNotBlank()) {
                        Text(
                            text = "Current Flow: ${uiState.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = pulseTitle,
                        onValueChange = { pulseTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Title") },
                        placeholder = { Text("What was the moment?") },
                        singleLine = true
                    )

                    JourneyLean(
                        tags = tags,
                        tagName = pulseTagName,
                        onTagClicked = { tag -> pulseTagName = tag.name },
                        onTagNameChange = { pulseTagName = it }
                    )

                    OutlinedTextField(
                        value = pulseDescription,
                        onValueChange = { pulseDescription = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 8,
                        label = { Text("Description") },
                        placeholder = { Text("Capture the thought, learning, or feeling…") }
                    )

                    if (isInFlowState) {
                        HorizontalDivider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = attachPulseToCurrentFlow,
                                    onClick = { attachPulseToCurrentFlow = !attachPulseToCurrentFlow }
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Attach to current Flow",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (attachPulseToCurrentFlow) {
                                        "This Pulse will appear under the active Flow in Chronicles."
                                    } else {
                                        "This Pulse will be saved as its own moment."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = attachPulseToCurrentFlow,
                                onCheckedChange = { attachPulseToCurrentFlow = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = pulseTitle.isNotBlank() && pulseDescription.isNotBlank(),
                    onClick = {
                        viewModel.recordPulse(
                            title = pulseTitle,
                            description = pulseDescription,
                            tagName = pulseTagName,
                            attachToCurrentFlow = attachPulseToCurrentFlow
                        )
                        showPulseDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPulseDialog = false }) { Text("Cancel") }
            }
        )
    }

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

    if (showPointsDialog && reward != null) {
        val r = reward!!
        var showArcSummary by remember(r) { mutableStateOf(r.isArcOnlySummary) }

        AlertDialog(
            onDismissRequest = { },
            title = {
                Text(
                    when {
                        showArcSummary && r.arcSummary != null -> "Arc Reward"
                        uiState.isSoftMode -> "Soft Flow recorded"
                        else -> "You did it!"
                    }
                )
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 0.75f * LocalConfiguration.current.screenHeightDp.dp)
                ) {
                    when {
                        showArcSummary && r.arcSummary != null -> {
                            ArcSummaryContent(
                                arc = r.arcSummary!!,
                                isAera = BuildConfig.FLAVOR == "aera",
                                calmMode = uiState.calmMode
                            )
                        }

                        uiState.isSoftMode -> {
                            SoftSessionRewardContent(r = r)
                        }

                        else -> {
                            SessionRewardContent(
                                r = r,
                                isAera = BuildConfig.FLAVOR == "aera",
                                calmMode = uiState.calmMode
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (r.arcSummary != null && !showArcSummary && !r.isArcOnlySummary) {
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
                ) {
                    val label = when {
                        r.isArcOnlySummary -> "Done"
                        r.arcSummary != null && !showArcSummary -> "Next"
                        else -> "Done"
                    }
                    Text(label)
                }
            },
            dismissButton = {}
        )
    }

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

@Composable
private fun SessionModeSelector(
    isSoftMode: Boolean,
    isLocked: Boolean,
    onFlowSelected: () -> Unit,
    onSoftSelected: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Mode",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ModeOptionCard(
                modifier = Modifier.weight(1f),
                selected = !isSoftMode,
                enabled = !isLocked,
                title = "Flow",
                subtitle = "Scored",
                icon = Icons.Outlined.AutoAwesome,
                selectedContainer = MaterialTheme.colorScheme.primary,
                selectedContent = MaterialTheme.colorScheme.onPrimary,
                onClick = onFlowSelected
            )

            ModeOptionCard(
                modifier = Modifier.weight(1f),
                selected = isSoftMode,
                enabled = !isLocked,
                title = "Soft",
                subtitle = "Gentle",
                icon = Icons.Outlined.Spa,
                selectedContainer = MaterialTheme.colorScheme.secondary,
                selectedContent = MaterialTheme.colorScheme.onSecondary,
                onClick = onSoftSelected
            )
        }

        if (isLocked) {
            Text(
                text = "Mode locks once time starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }
    }
}

@Composable
private fun ModeOptionCard(
    modifier: Modifier = Modifier,
    selected: Boolean,
    enabled: Boolean,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedContainer: androidx.compose.ui.graphics.Color,
    selectedContent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    val container = if (selected) selectedContainer else MaterialTheme.colorScheme.surfaceVariant
    val content = if (selected) selectedContent else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = content
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.78f)
                )
            }
        }
    }
}

@Composable
private fun SoftSessionRewardContent(
    r: FlowRewardUiModel
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Soft Flow",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = "You showed up without turning this session into a score chase.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "${r.minutes} min",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Text(
            text = "This session is part of your Story, but it does not affect score, Surge, Beam, or Arc progression.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
        )
    }
}

fun formatMsAsMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}