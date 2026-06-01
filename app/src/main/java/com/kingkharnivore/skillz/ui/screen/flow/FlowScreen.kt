@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.model.state.flow.FlowRewardUiModel
import com.kingkharnivore.skillz.ui.screen.flow.reward.ArcSummaryContent
import com.kingkharnivore.skillz.ui.screen.flow.reward.SessionRewardContent
import com.kingkharnivore.skillz.ui.screen.flow.reward.SoftSessionRewardContent
import com.kingkharnivore.skillz.viewmodel.FlowEndAction
import com.kingkharnivore.skillz.viewmodel.FlowViewModel

@Composable
fun FlowScreen(
    viewModel: FlowViewModel,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onOpenShell: () -> Unit = {}
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

    LaunchedEffect(uiState.recentlyResumedArcMessage) {
        if (uiState.recentlyResumedArcMessage != null) {
            kotlinx.coroutines.delay(3_000L)
            viewModel.consumeRecentlyResumedArcMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.flow_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.discardDraftIfIdle()
                        onCancel()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.common_back)
                        )
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

                if (uiState.originPulseId != null && !uiState.originPulseTitle.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.idea_grove_from_pulse),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = uiState.originPulseTitle.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (
                    !uiState.plannedArcTitle.isNullOrBlank() &&
                    uiState.plannedArcStepIndex != null &&
                    uiState.plannedArcTotalSteps != null
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = uiState.plannedArcTitle!!,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(
                                    R.string.flow_screen_planned_arc_step,
                                    uiState.plannedArcStepIndex!! + 1,
                                    uiState.plannedArcTotalSteps!!
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                if (!uiState.recentlyResumedArcMessage.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.recentlyResumedArcMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }

                    Spacer(Modifier.height(10.dp))
                }

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
                        text = stringResource(R.string.help_pref_calm_mode_title),
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
                            isInFlowState && uiState.isSoftMode -> stringResource(R.string.flow_screen_exit_soft_flow)
                            isInFlowState -> stringResource(R.string.flow_screen_exit_flow)
                            uiState.isSoftMode -> stringResource(R.string.flow_screen_begin_soft_flow)
                            else -> stringResource(R.string.flow_screen_enter_flow)
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
                                text = stringResource(R.string.flow_screen_soft_flow_label),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.flow_screen_soft_flow_body),
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
                        Text(stringResource(R.string.story_fab_record_pulse))
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.flow_screen_in_flow_helper),
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
                    Text(
                        if (isSaving) stringResource(R.string.flow_screen_saving)
                        else stringResource(R.string.flow_screen_save_soft_flow)
                    )
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
                        Text(stringResource(R.string.flow_screen_continue_arc))
                    }

                    val isPlannedArc = !uiState.plannedArcTitle.isNullOrBlank()
                    val isArcActuallyCompletable = uiState.isInArc || isPlannedArc
                    val completeLabel = if (isArcActuallyCompletable) {
                        stringResource(R.string.flow_screen_complete_arc)
                    } else {
                        stringResource(R.string.flow_screen_complete_flow)
                    }
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
                        Text(if (isSaving) stringResource(R.string.flow_screen_saving) else completeLabel)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }

    if (showSoftArcConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSoftArcConfirmDialog = false },
            title = { Text(stringResource(R.string.flow_screen_soft_confirm_title)) },
            text = {
                Text(stringResource(R.string.flow_screen_soft_confirm_body))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSoftArcConfirmDialog = false
                        viewModel.setSoftModeAndConcludeArc()
                    }
                ) { Text(stringResource(R.string.flow_screen_enter_soft)) }
            },
            dismissButton = {
                TextButton(onClick = { showSoftArcConfirmDialog = false }) {
                    Text(stringResource(R.string.flow_screen_stay_in_flow))
                }
            }
        )
    }

    if (showPulseDialog) {
        AlertDialog(
            onDismissRequest = { showPulseDialog = false },
            title = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.story_fab_record_pulse))
                    if (isInFlowState && uiState.title.isNotBlank()) {
                        Text(
                            text = stringResource(R.string.flow_screen_current_flow_value, uiState.title),
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
                        label = { Text(stringResource(R.string.flow_screen_pulse_title_label)) },
                        placeholder = { Text(stringResource(R.string.flow_screen_pulse_title_placeholder)) },
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
                        label = { Text(stringResource(R.string.flow_screen_pulse_description_label)) },
                        placeholder = { Text(stringResource(R.string.flow_screen_pulse_description_placeholder)) }
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
                                    text = stringResource(R.string.pulse_screen_attach_to_current_flow),
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = if (attachPulseToCurrentFlow) {
                                        stringResource(R.string.pulse_screen_attach_enabled)
                                    } else {
                                        stringResource(R.string.pulse_screen_attach_disabled)
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
                    enabled = pulseTitle.isNotBlank() || pulseDescription.isNotBlank(),
                    onClick = {
                        viewModel.recordPulse(
                            title = pulseTitle,
                            description = pulseDescription,
                            tagName = pulseTagName,
                            attachToCurrentFlow = attachPulseToCurrentFlow
                        )
                        showPulseDialog = false
                    }
                ) { Text(stringResource(R.string.common_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showPulseDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text(stringResource(R.string.flow_screen_end_session_title)) },
            text = { Text(stringResource(R.string.flow_screen_end_session_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.pauseStopwatch()
                        viewModel.resetStopwatch()
                        viewModel.exitFocusMode()
                        showEndDialog = false
                        onCancel()
                    }
                ) { Text(stringResource(R.string.flow_screen_end)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDialog = false }) {
                    Text(stringResource(R.string.flow_screen_continue))
                }
            }
        )
    }

    if (showPointsDialog && reward != null) {
        val r = reward!!
        var showArcSummary by remember(r) { mutableStateOf(r.isArcOnlySummary) }

        AlertDialog(
            onDismissRequest = {},
            title = {
                Text(
                    when {
                        showArcSummary && r.arcSummary != null -> stringResource(R.string.flow_screen_arc_reward)
                        uiState.isSoftMode -> stringResource(R.string.flow_screen_soft_flow_recorded)
                        else -> stringResource(R.string.flow_screen_you_did_it)
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
                        r.isArcOnlySummary -> stringResource(R.string.common_done)
                        r.arcSummary != null && !showArcSummary -> stringResource(R.string.common_next)
                        else -> stringResource(R.string.common_done)
                    }
                    Text(label)
                }
            },
            dismissButton = {
                if (r.hasShellReward()) {
                    TextButton(
                        onClick = {
                            showPointsDialog = false
                            viewModel.clearLastReward()
                            onOpenShell()
                        }
                    ) { Text(stringResource(R.string.session_reward_enter_shell)) }
                }
            }
        )
    }

    if (showSurgeDialog) {
        val locked = viewModel.isSurgeLocked()
        val currentMinutes = (uiState.surgePlannedMs ?: 0L) / 60_000L

        AlertDialog(
            onDismissRequest = { showSurgeDialog = false },
            title = { Text(stringResource(R.string.help_page_surge_kicker)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (locked) {
                        Text(stringResource(R.string.flow_screen_surge_locked_body))
                        Text(stringResource(R.string.flow_screen_surge_planned_value, currentMinutes))
                    } else {
                        Text(stringResource(R.string.flow_screen_surge_dialog_body))

                        OutlinedTextField(
                            value = surgeMinutesInput,
                            onValueChange = { surgeMinutesInput = it.filter(Char::isDigit) },
                            label = { Text(stringResource(R.string.flow_screen_surge_time_label)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (uiState.isSurgeOn) {
                            Text(stringResource(R.string.flow_screen_surge_current_value, currentMinutes))
                        }
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
                ) {
                    Text(
                        if (locked) stringResource(R.string.common_ok)
                        else stringResource(R.string.common_set)
                    )
                }
            },
            dismissButton = {
                if (!locked && uiState.isSurgeOn) {
                    TextButton(
                        onClick = {
                            viewModel.clearSurgeIfAllowed()
                            showSurgeDialog = false
                        }
                    ) { Text(stringResource(R.string.flow_screen_turn_off)) }
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
    val selectedTitle = if (isSoftMode) {
        stringResource(R.string.flow_screen_soft_short)
    } else {
        stringResource(R.string.flow_card_type_flow)
    }

    val selectedSubtitle = if (isSoftMode) {
        stringResource(R.string.flow_screen_mode_soft_subtitle)
    } else {
        stringResource(R.string.flow_screen_mode_flow_subtitle)
    }

    val selectedIcon = if (isSoftMode) {
        Icons.Outlined.Spa
    } else {
        Icons.Outlined.AutoAwesome
    }

    val selectedContainer = if (isSoftMode) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.primary
    }

    val selectedContent = if (isSoftMode) {
        MaterialTheme.colorScheme.onSecondary
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.animateContentSize()
    ) {
        Text(
            text = stringResource(R.string.flow_screen_mode_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
        )

        AnimatedContent(
            targetState = isLocked,
            label = "session_mode_selector_lock_transition",
            transitionSpec = {
                fadeIn() + expandVertically() togetherWith fadeOut() + shrinkVertically()
            }
        ) { locked ->
            if (locked) {
                ModeOptionCard(
                    modifier = Modifier.fillMaxWidth(),
                    selected = true,
                    enabled = true,
                    title = selectedTitle,
                    subtitle = selectedSubtitle,
                    icon = selectedIcon,
                    selectedContainer = selectedContainer,
                    selectedContent = selectedContent,
                    onClick = {}
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ModeOptionCard(
                        modifier = Modifier.weight(1f),
                        selected = !isSoftMode,
                        enabled = true,
                        title = stringResource(R.string.flow_card_type_flow),
                        subtitle = stringResource(R.string.flow_screen_mode_flow_subtitle),
                        icon = Icons.Outlined.AutoAwesome,
                        selectedContainer = MaterialTheme.colorScheme.primary,
                        selectedContent = MaterialTheme.colorScheme.onPrimary,
                        onClick = onFlowSelected
                    )

                    ModeOptionCard(
                        modifier = Modifier.weight(1f),
                        selected = isSoftMode,
                        enabled = true,
                        title = stringResource(R.string.flow_screen_soft_short),
                        subtitle = stringResource(R.string.flow_screen_mode_soft_subtitle),
                        icon = Icons.Outlined.Spa,
                        selectedContainer = MaterialTheme.colorScheme.secondary,
                        selectedContent = MaterialTheme.colorScheme.onSecondary,
                        onClick = onSoftSelected
                    )
                }
            }
        }

        if (isLocked) {
            Text(
                text = stringResource(R.string.flow_screen_mode_locked_body),
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

private fun FlowRewardUiModel.hasShellReward(): Boolean =
    shellPearlsEarned > 0 || shellStillwaterUnits > 0L ||
        shellGrantedFindIds.isNotEmpty() || shellDiscoveryIds.isNotEmpty() || shellBadgeIds.isNotEmpty()

fun formatMsAsMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}