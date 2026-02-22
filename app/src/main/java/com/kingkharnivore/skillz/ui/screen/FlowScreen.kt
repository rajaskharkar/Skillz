@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.viewmodel.ArcSummaryUiModel
import com.kingkharnivore.skillz.viewmodel.FlowEndAction
import com.kingkharnivore.skillz.viewmodel.FlowRewardUiModel
import com.kingkharnivore.skillz.viewmodel.FlowViewModel
import com.kingkharnivore.skillz.viewmodel.StopwatchState

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

    // Advanced (dialog) minutes input (kept)
    var surgeMinutesInput by remember { mutableStateOf("") }

    // Inline Surge editor (UI-only)
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

                val isArcActuallyCompletable = uiState.isInArc && !uiState.arcIsPending
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

@Composable
private fun RitualFrame(
    rotation: Float,
    corner: Dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    showBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val stroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = rotation },
        shape = RoundedCornerShape(corner),
        tonalElevation = 0.dp,      // ✅ no “card” elevation feel
        shadowElevation = 0.dp,
        color = Color.Transparent   // ✅ transparent background
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (showBorder) Modifier.border(1.dp, stroke, RoundedCornerShape(corner))
                    else Modifier
                )
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

/* ---------------------------------------------------------
   DESIGN BUILDING BLOCKS
   --------------------------------------------------------- */

@Composable
private fun RitualCard(
    rotation: Float,
    corner: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val stroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = rotation },
        shape = RoundedCornerShape(corner),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, stroke, RoundedCornerShape(corner))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}

@Composable
private fun GrandTitleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent,
        disabledIndicatorColor = Color.Transparent,
        cursorColor = MaterialTheme.colorScheme.tertiary
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Set the tone…",
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
                    text = "It begins here.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },
            shape = RoundedCornerShape(28.dp),
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
private fun JourneyLean(
    tags: List<TagEntity>,
    tagName: String,
    onTagClicked: (TagEntity) -> Unit,
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

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    TextField(
        value = tagName,
        onValueChange = onTagNameChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(
            text = "Start a new journey…",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.SemiBold
            ),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)) },
        shape = RoundedCornerShape(999.dp),
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

@Composable
private fun ChronicleField(
    value: String,
    onValueChange: (String) -> Unit
) {
    Text(
        text = "Write your story",
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = 0.6.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Start
        ),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    )

    val colors = TextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        focusedIndicatorColor = Color.Transparent,
        unfocusedIndicatorColor = Color.Transparent
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 1.dp,
        color = Color.Transparent
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(28.dp)
                ),
            placeholder = {
                Text(
                    "Wins, friction, lessons, next moves…",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.40f)
                )
            },

            // ✅ THIS makes typed text cursive
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Thin,
                letterSpacing = 0.2.sp,
                lineHeight = 30.sp
            ),

            minLines = 6,
            shape = RoundedCornerShape(28.dp),
            colors = colors
        )
    }
}

@Composable
private fun ArcPill(
    arcMultiplier: Double,
    arcNextIndex: Int?
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔥")
            Spacer(Modifier.width(8.dp))
            Text("Arc Active", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text(
                "×${"%.1f".format(arcMultiplier)}",
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            arcNextIndex?.let {
                Spacer(Modifier.width(10.dp))
                Text("Now: Flow $it", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

/**
 * Surge (Option A: two-row):
 * - Wrap-content pill so it can truly sit on the right.
 * - Discreet when OFF.
 * - Toggle disabled once Flow is entered OR locked.
 * - Countdown while Flow active.
 * - Long-press opens dialog.
 */
@Composable
private fun SurgeMiniControl(
    modifier: Modifier = Modifier,
    isInFlow: Boolean,
    elapsedMs: Long,
    locked: Boolean,
    isSurgeOn: Boolean,
    plannedMs: Long?,
    minutesInline: String,
    onMinutesChange: (String) -> Unit,
    onCommit: () -> Unit,
    onToggleOff: () -> Unit,
    onLongPress: () -> Unit
) {
    var pendingOn by rememberSaveable { mutableStateOf(false) }
    var isEditing by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isSurgeOn) {
        if (isSurgeOn) pendingOn = false
        if (!isSurgeOn && !pendingOn) isEditing = false
    }

    val effectiveOn = isSurgeOn || pendingOn
    val toggleEnabled = !isInFlow && !locked

    val planned = plannedMs
    val remainingMs = if (planned != null) (planned - elapsedMs).coerceAtLeast(0L) else null
    val completed = isInFlow && planned != null && remainingMs == 0L

    LaunchedEffect(effectiveOn, plannedMs, toggleEnabled) {
        if (effectiveOn && toggleEnabled && plannedMs == null) isEditing = true
    }

    // ✅ Auto-revert if user toggled Surge ON but never set minutes
    LaunchedEffect(isEditing, plannedMs, pendingOn) {
        if (!isEditing && pendingOn && plannedMs == null) {
            pendingOn = false
            onToggleOff()
        }
    }

    val isOff = !effectiveOn

    val stroke = if (isOff)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    val containerColor = when {
        completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        effectiveOn -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
    }

    val outerShape = RoundedCornerShape(if (isOff) 999.dp else 18.dp)
    val contentHPad = if (isOff) 10.dp else 12.dp
    val contentVPad = if (isOff) 6.dp else 10.dp
    val rowSpacing = if (isOff) 6.dp else 10.dp

    val offTrack = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
    val offThumb = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)

    Surface(
        modifier = modifier
            .wrapContentWidth()
            .combinedClickable(
                onClick = {
                    if (effectiveOn && toggleEnabled && !isInFlow) isEditing = true
                },
                onLongClick = { onLongPress() }
            )
            .border(1.dp, stroke, outerShape),
        shape = outerShape,
        tonalElevation = 1.dp,
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = contentHPad, vertical = contentVPad),
            verticalArrangement = Arrangement.spacedBy(rowSpacing)
        ) {
            // Row 1: label + status + toggle (WRAP CONTENT — no fillMaxWidth)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("⚡")
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Surge",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isOff) FontWeight.Medium else FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isOff) 0.70f else 1f)
                )

                Spacer(Modifier.width(10.dp))

                // Status text: remove the awkward branch
                val statusText = when {
                    completed -> "Complete"
                    planned != null && elapsedMs > 0L && remainingMs != null -> formatMsAsMmSs(remainingMs)
                    planned != null -> "${planned / 60_000L} min"
                    else -> ""
                }

                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Switch(
                    checked = effectiveOn,
                    onCheckedChange = { checked ->
                        if (!toggleEnabled) return@Switch

                        if (checked) {
                            pendingOn = true
                            if (plannedMs == null) isEditing = true
                        } else {
                            pendingOn = false
                            isEditing = false
                            onToggleOff()
                        }
                    },
                    enabled = toggleEnabled,
                    colors = SwitchDefaults.colors(
                        uncheckedTrackColor = offTrack,
                        uncheckedThumbColor = offThumb
                    )
                )
            }

            // Row 2: editor (only when editing) — still wrap-content
            AnimatedVisibility(visible = effectiveOn && toggleEnabled && isEditing && !isInFlow) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = minutesInline,
                        onValueChange = { raw ->
                            onMinutesChange(raw.filter(Char::isDigit).take(3))
                        },
                        modifier = Modifier
                            .width(88.dp)
                            .height(48.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.labelLarge.copy(textAlign = TextAlign.Center),
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = MaterialTheme.typography.labelLarge.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        ),
                        shape = RoundedCornerShape(999.dp)
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = "mins",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    )

                    Spacer(Modifier.width(10.dp))

                    val minsValid = minutesInline.toIntOrNull()?.let { it > 0 } == true

                    Surface(
                        onClick = {
                            if (minsValid) {
                                onCommit()
                                isEditing = false
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        tonalElevation = if (minsValid) 2.dp else 0.dp,
                        color = if (minsValid)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Set",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (minsValid)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------
   YOUR EXISTING REWARD UI + HELPERS (UNCHANGED)
   --------------------------------------------------------- */

@Composable
private fun ArcSummaryContent(
    arc: ArcSummaryUiModel,
    isAera: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Arc completed.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Totals across this Arc.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
        )

        RewardCard(title = "Arc totals") {
            MetricLine("Flows", "${arc.totalSessions}", MetricTone.Neutral)
            MetricLine("Total duration", formatDuration(arc.totalDurationMs), MetricTone.Neutral)

            if (!isAera) {
                DividerSoft()
                MetricLine("Peak multiplier", "×${"%.1f".format(arc.peakMultiplier)}", MetricTone.Glow)
                HighlightMetric("Arc bonus points", "+${arc.totalArcBonusPoints}", glow = true)
                HighlightMetric("Total Scyra Score", "+${arc.totalFinalPoints}", glow = true)
            }
        }
    }
}

@Composable
private fun SessionRewardContent(
    r: FlowRewardUiModel,
    isAera: Boolean
) {
    val showBeamUi = r.beamBonusPoints > 0

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        RewardChipRowV2(
            isAera = isAera,
            totalMinutes = r.minutes,
            totalScyra = r.finalScyraPoints,
            beamBonusPoints = r.beamBonusPoints,
            showBeamUi = showBeamUi,
            surgePoints = r.surgePoints
        )

        if (isAera) {
            RewardCard(title = "Session details", subtitle = "Time only") {
                MetricLine("Total time", "${r.minutes} min", MetricTone.Neutral)
                if (showBeamUi) MetricLine("Time in Beam ⭐", formatMsAsMmSs(r.beamEligibleMs), MetricTone.Glow)
            }
            return
        }

        RewardTotalCard(
            title = "Total Scyra Score",
            value = r.finalScyraPoints,
            footnote = "This Flow"
        )

        RewardCard(title = "Breakdown", subtitle = "How your score was built") {
            HighlightMetric("Base Scyra score", "+${r.baseScyraPoints}")

            val hasAnyBonus =
                r.tenMinuteBonuses > 0 || r.thirtyMinuteBonuses > 0 || r.sixtyMinuteBonuses > 0

            if (hasAnyBonus) {
                DividerSoft()
                Text(
                    text = "Time bonuses",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                if (r.tenMinuteBonuses > 0) BonusLine("10-minute bonus", r.tenMinuteBonuses, 5)
                if (r.thirtyMinuteBonuses > 0) BonusLine("30-minute bonus", r.thirtyMinuteBonuses, 15)
                if (r.sixtyMinuteBonuses > 0) BonusLine("60-minute bonus", r.sixtyMinuteBonuses, 50)
            }

            val showArcUi =
                (r.arcIndexInArc ?: 0) >= 2 && (r.arcBonusPoints > 0 || r.arcMultiplierUsed != null)

            if (showArcUi) {
                DividerSoft()
                Text(
                    text = "Arc 🔥",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                MetricLine(
                    label = "Arc multiplier used",
                    value = r.arcMultiplierUsed?.let { "×${"%.1f".format(it)}" } ?: "—",
                    tone = if (r.arcMultiplierUsed != null) MetricTone.Glow else MetricTone.Muted
                )

                HighlightMetric("Arc points gained", "+${r.arcBonusPoints}", glow = true)

                if (r.arcDidLevelUp) {
                    Text(
                        text = "Multiplier grew by +0.1",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (showBeamUi) {
                DividerSoft()
                Text(
                    text = "Beam ⭐",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f)
                )

                MetricLine("Time in Beam", formatMsAsMmSs(r.beamEligibleMs), MetricTone.Glow)

                MetricLine(
                    "Beam multiplier",
                    r.beamMultiplier?.let { "×${"%.2f".format(it)}" } ?: "—",
                    tone = if (r.beamMultiplier != null) MetricTone.Glow else MetricTone.Muted
                )

                HighlightMetric("Beam points gained", "+${r.beamBonusPoints}", glow = true)
            }
        }
    }
}

@Composable
private fun StopwatchSection(
    state: StopwatchState,
    viewModel: FlowViewModel
) {
    var showResetConfirm by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "In Flow", style = MaterialTheme.typography.titleSmall)

        Text(
            text = formatElapsed(state.elapsedMs),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val threshold = 2 * 60_000L
                    if (state.elapsedMs >= threshold) showResetConfirm = true
                    else viewModel.resetStopwatch()
                },
                enabled = state.elapsedMs > 0L && !state.isRunning
            ) { Text("Reset") }
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
                    ) { Text("Yes, reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetConfirm = false }) { Text("Cancel") }
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

    return if (hours > 0) String.format("%d:%02d:%02d", hours, minutes, seconds)
    else String.format("%02d:%02d", minutes, seconds)
}

private fun formatMsAsMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
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
        if (showBeamUi) RewardChip(text = "⭐ +$beamBonusPoints")
        if (surgePoints > 0) RewardChip(text = "⚡ +$surgePoints")
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
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
    val valueColor = if (glow) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

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
private fun BonusLine(label: String, count: Int, pointsEach: Int) {
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
        modifier = Modifier.fillMaxWidth().height(1.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        content = {}
    )
}