package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
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
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.viewmodel.FlowViewModel
import com.kingkharnivore.skillz.viewmodel.StopwatchState
import com.kingkharnivore.skillz.utils.score.ScoreCalculator
import com.kingkharnivore.skillz.viewmodel.BEAM_MIN_ELIGIBLE_MS

@OptIn(ExperimentalMaterial3Api::class)
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

    var showSurgeDialog by remember { mutableStateOf(false) }
    var surgeMinutesInput by remember { mutableStateOf("") }

    val stopwatchState = uiState.stopwatch
    val isInFlowState = uiState.isInFlowMode

    var showEndDialog by remember { mutableStateOf(false) }

    var showPointsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flow") },
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
            if (BuildConfig.FLAVOR != "aera") {
                Button(
                    onClick = { showSurgeDialog = true },
                    enabled = !uiState.isInFlowMode && (!uiState.isSurgeOn || !viewModel.isSurgeLocked()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    val surgePlannedMs = uiState.surgePlannedMs

                    val label = when {
                        uiState.isSurgeOn && surgePlannedMs != null -> {
                            val mins = (surgePlannedMs / 60_000L).toInt()
                            if (viewModel.isSurgeLocked()) "Surge: $mins min (Locked)"
                            else "Surge: $mins min"
                        }

                        else -> "Turn on Surge"
                    }
                    Text(label, style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("Dive in") },
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
                label = { Text("Start a new journey") },
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
                    if (isInFlowState) {
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
                    text = if (isInFlowState) "Exit Flow" else "Enter Flow",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (isInFlowState) {
                Text(
                    text = "You\'re in Flow. You may use other parts of this app.\n" +
                            "You may turn off the screen — the timer continues.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::onDescriptionChange,
                label = { Text("Write your story") },
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
                    enabled = uiState.title.isNotBlank() && uiState.tagName.isNotBlank() && !isSaving && !isInFlowState,
                    onClick = {
                        val durationMs = uiState.stopwatch.elapsedMs.coerceAtLeast(0L)
                        val tenMinutesMs = 1 * 60_000L
                        val surgePoints =
                            ScoreCalculator.surgePoints(uiState.surgePlannedMs, durationMs)
                        val shouldShowDialog = durationMs >= tenMinutesMs || surgePoints > 0

                        if (shouldShowDialog) {
                            viewModel.prepareRewardPreview()
                            showPointsDialog = true
                        } else {
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
    if (showPointsDialog && reward != null) {
        val r = reward ?: return

        AlertDialog(
            onDismissRequest = { /* locked */ },
            title = { Text("You did it!") },
            text = {
                val isAera = BuildConfig.FLAVOR == "aera"
                val hasBeamPoints = r.beamBonusPoints > 0
                val showBeamUi = hasBeamPoints

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ─────────────────────────────────────────────
                    // Header
                    // ─────────────────────────────────────────────
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (r.surgePoints > 0) "Surge completed." else "Flow completed.",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Logged into your story.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                        )
                    }

                    // ─────────────────────────────────────────────
                    // Chips row: Total Scyra, Beam time, Surge
                    // (Aera-safe: it will show time-only chip set)
                    // ─────────────────────────────────────────────
                    RewardChipRowV2(
                        isAera = isAera,
                        totalMinutes = r.minutes,
                        totalScyra = r.finalScyraPoints,
                        beamBonusPoints = r.beamBonusPoints,
                        showBeamUi = showBeamUi,
                        surgePoints = r.surgePoints
                    )

                    if (isAera) {
                        // AERA: no scores, no breakdown math
                        RewardCard(
                            title = "Session details",
                            subtitle = "Time only"
                        ) {
                            MetricLine(
                                label = "Total time",
                                value = "${r.minutes} min",
                                tone = MetricTone.Neutral
                            )

                            if (showBeamUi) {
                                MetricLine(
                                    label = "Time in Beam ⭐",
                                    value = formatMsAsMmSs(r.beamEligibleMs),
                                    tone = MetricTone.Glow
                                )
                            }
                        }
                        return@Column
                    }

                    // ─────────────────────────────────────────────
                    // Total Scyra Score hero card
                    // ─────────────────────────────────────────────
                    RewardTotalCard(
                        title = "Total Scyra Score",
                        value = r.finalScyraPoints,
                        footnote = "This Flow"
                    )

                    // ─────────────────────────────────────────────
                    // Breakdown: how total was built (NO surge here)
                    // ─────────────────────────────────────────────
                    RewardCard(
                        title = "Breakdown",
                        subtitle = "How your score was built"
                    ) {
                        // Base Scyra score (big)
                        HighlightMetric(
                            label = "Base Scyra score",
                            value = "+${r.baseScyraPoints}"
                        )

                        // Time bonuses
                        val hasAnyBonus =
                            r.tenMinuteBonuses > 0 || r.thirtyMinuteBonuses > 0 || r.sixtyMinuteBonuses > 0

                        if (hasAnyBonus) {
                            DividerSoft()
                            Text(
                                text = "Time bonuses",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                            )

                            if (r.tenMinuteBonuses > 0) {
                                BonusLine(
                                    label = "10-minute bonus",
                                    count = r.tenMinuteBonuses,
                                    pointsEach = 5
                                )
                            }
                            if (r.thirtyMinuteBonuses > 0) {
                                BonusLine(
                                    label = "30-minute bonus",
                                    count = r.thirtyMinuteBonuses,
                                    pointsEach = 15
                                )
                            }
                            if (r.sixtyMinuteBonuses > 0) {
                                BonusLine(
                                    label = "60-minute bonus",
                                    count = r.sixtyMinuteBonuses,
                                    pointsEach = 50
                                )
                            }

                            DividerSoft()
                        }

                        if (showBeamUi) {
                            DividerSoft()
                            Text(
                                text = "Beam ⭐",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                            )

                            MetricLine(
                                label = "Time in Beam",
                                value = formatMsAsMmSs(r.beamEligibleMs),
                                tone = MetricTone.Glow
                            )

                            MetricLine(
                                label = "Beam multiplier",
                                value = r.beamMultiplier?.let { "×${"%.2f".format(it)}" } ?: "—",
                                tone = if (r.beamMultiplier != null) MetricTone.Glow else MetricTone.Muted
                            )

                            HighlightMetric(
                                label = "Beam points gained",
                                value = "+${r.beamBonusPoints}",
                                glow = true
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPointsDialog = false
                        viewModel.saveSession(onDone)
                    }
                ) { Text("End") }
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
                        Text("Planned: ${currentMinutes} min")
                    } else {
                        Text("Set a planned time limit. Finish early to earn Surge Points.")

                        OutlinedTextField(
                            value = surgeMinutesInput,
                            onValueChange = {
                                surgeMinutesInput = it.filter(Char::isDigit)
                            },
                            label = { Text("Surge Time (minutes)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.isSurgeOn) {
                            Text("Current: ${currentMinutes} min")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!locked) {
                            val mins = surgeMinutesInput.toIntOrNull()
                            if (mins != null && mins > 0) {
                                viewModel.setSurgePlannedMinutes(mins)
                            }
                        }
                        showSurgeDialog = false
                    }
                ) {
                    Text(if (locked) "OK" else "Set")
                }
            },
            dismissButton = {
                if (!locked && uiState.isSurgeOn) {
                    TextButton(
                        onClick = {
                            viewModel.clearSurgeIfAllowed()
                            showSurgeDialog = false
                        }
                    ) {
                        Text("Turn Off")
                    }
                }
            }
        )
    }
}

@Composable
private fun StatText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
    )
}

@Composable
fun TagSuggestionRow(
    tags: List<TagEntity>,
    onTagClicked: (TagEntity) -> Unit
) {
    // ✅ defensive: ignore broken/empty names just in case
    val cleanTags = remember(tags) { tags.filter { it.name.isNotBlank() } }
    if (cleanTags.isEmpty()) return

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Continue an existing journey",
            style = MaterialTheme.typography.labelSmall
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            items(
                items = cleanTags,
                key = { it.id }
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
    viewModel: FlowViewModel
) {
    var showResetConfirm by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "In Flow",
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

@Composable
fun BonusRow(label: String, value: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
        Text(
            text = "×$value",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
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

private enum class MetricTone { Neutral, Glow, Muted }

@Composable
private fun RewardChipRowV2(
    isAera: Boolean,
    totalMinutes: Int,
    totalScyra: Int,
    beamBonusPoints: Int,
    showBeamUi: Boolean,
    surgePoints: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isAera) {
            RewardChip(text = "⏱ $totalMinutes min")
            return
        }

        RewardChip(text = "🔥 +$totalScyra")

        // ✅ strict: only show Beam chip if points > 0
        if (showBeamUi) {
            RewardChip(text = "⭐ +$beamBonusPoints")
        }

        if (surgePoints > 0) {
            RewardChip(text = "⚡ +$surgePoints")
        }
    }
}


@Composable
private fun RewardChip(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
        )
    }
}

@Composable
private fun RewardTotalCard(
    title: String,
    value: Int,
    footnote: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 4.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                    Text(
                        text = footnote,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }

                Text(
                    text = "+$value",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun RewardCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            content()
        }
    }
}

@Composable
private fun MetricLine(
    label: String,
    value: String,
    tone: MetricTone
) {
    val valueColor = when (tone) {
        MetricTone.Neutral -> MaterialTheme.colorScheme.onSurface
        MetricTone.Glow -> MaterialTheme.colorScheme.primary
        MetricTone.Muted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = valueColor
        )
    }
}

@Composable
private fun HighlightMetric(
    label: String,
    value: String,
    glow: Boolean = true
) {
    val valueColor =
        if (glow) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun BonusLine(
    label: String,
    count: Int,
    pointsEach: Int
) {
    val total = count * pointsEach
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Text(
                text = "×$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
            )
        }

        Text(
            text = "+$total",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DividerSoft() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        content = {}
    )
}

private fun formatMsAsMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
