@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.FlowListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.FlowListUiState
import com.kingkharnivore.skillz.data.model.entity.Journey7dStatUiModel
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils
import com.kingkharnivore.skillz.utils.time.formatDuration
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun StoryScreen(
    viewModel: StoryViewModel,
    onAddSessionClick: () -> Unit,
    onScheduleBeamClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onGoToActiveSession: () -> Unit,
    isFocusModeOn: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(uiState.sessions.size) {
        if (uiState.sessions.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = onScheduleBeamClick,
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) { Text("⏰") }

                SkillListFab(onClick = onAddSessionClick)
            }
        }
    )  { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            StoryBody(
                uiState = uiState,
                listState = listState,
                isFlowStateActive = isFocusModeOn,
                onTagSelected = viewModel::selectTag,
                onPeriodSelected = viewModel::onPeriodSelected,
                onPrev = viewModel::goPrev,
                onNext = viewModel::goNext,
                onToday = viewModel::goToday,
                onGoToActiveSession = onGoToActiveSession,
                onAddSessionClick = onAddSessionClick,
                onSessionClick = onSessionClick,
                onDeleteSession = viewModel::deleteSession,
                onUpdateSessionDescription = viewModel::updateSessionDescription,
                onOpenViewJourneys = viewModel::openViewJourneys
            )

            ViewJourneysBottomSheet(
                uiState = uiState,
                onClose = viewModel::closeViewJourneys,
                onSessionClick = onSessionClick
            )
        }
    }
}

@Composable
fun StoryBody(
    uiState: FlowListUiState,
    listState: LazyListState,
    isFlowStateActive: Boolean,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onGoToActiveSession: () -> Unit,
    onAddSessionClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onUpdateSessionDescription: (Long, String) -> Unit,
    onOpenViewJourneys: (Long) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> {
                LoadingState(modifier = Modifier.align(Alignment.Center))
            }

            uiState.errorMessage != null -> {
                ErrorState(
                    message = uiState.errorMessage,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            uiState.sessions.isEmpty() -> {
                if (isFlowStateActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        StoryHeader(
                            uiState = uiState,
                            onTagSelected = onTagSelected,
                            onPeriodSelected = onPeriodSelected,
                            onPrev = onPrev,
                            onNext = onNext,
                            onToday = onToday,
                            onOpenViewJourneys = onOpenViewJourneys,
                            extraTopContent = { FlowModeHeroCard(onGoToActiveSession = onGoToActiveSession) }
                        )
                    }
                } else {
                    // ✅ Keep the SAME scroll + header structure as non-empty days
                    LazyColumn(
                        state = listState, // pass listState into StoryBody for this
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            StoryHeader(
                                uiState = uiState,
                                onTagSelected = onTagSelected,
                                onPeriodSelected = onPeriodSelected,
                                onPrev = onPrev,
                                onNext = onNext,
                                onToday = onToday,
                                onOpenViewJourneys = onOpenViewJourneys
                            )
                        }

                        item {
                            FirstTimeUser(
                                onAddSessionClick = onAddSessionClick
                            )
                        }
                    }
                }
            }

            else -> {
                if (isFlowStateActive) {
                    FlowStateActiveContent(
                        uiState = uiState,
                        listState = listState,
                        onTagSelected = onTagSelected,
                        onPeriodSelected = onPeriodSelected,
                        onPrev = onPrev,
                        onNext = onNext,
                        onToday = onToday,
                        onGoToActiveSession = onGoToActiveSession,
                        onSessionClick = onSessionClick,
                        onDeleteSession = onDeleteSession,
                        onOpenViewJourneys = onOpenViewJourneys,
                        onUpdateSessionDescription = onUpdateSessionDescription
                    )
                } else {
                    FlowStateInactiveContent(
                        uiState = uiState,
                        listState = listState,
                        onTagSelected = onTagSelected,
                        onPeriodSelected = onPeriodSelected,
                        onPrev = onPrev,
                        onNext = onNext,
                        onToday = onToday,
                        onSessionClick = onSessionClick,
                        onDeleteSession = onDeleteSession,
                        onOpenViewJourneys = onOpenViewJourneys,
                        onUpdateSessionDescription = onUpdateSessionDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun FirstTimeUser(
    onAddSessionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = onAddSessionClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                text = "Start your Story!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SagasCard(
    period: StoryPeriod,
    anchorDayStartMs: Long,
    stats: List<Journey7dStatUiModel>,
    onOpenViewJourneys: (tagId: Long) -> Unit
) {
    val totalFlows = remember(stats) { stats.sumOf { it.sessionsCount } }
    val totalDuration = remember(stats) { stats.sumOf { it.totalDurationMs } }
    val totalScore = remember(stats) { stats.sumOf { it.totalScore } }

    var isSagasExpanded by rememberSaveable(period, anchorDayStartMs) {
        mutableStateOf(false)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // ── Header (not a button anymore) ──────────────────────────
            SagaHeader(
                title = "Your Saga",
                subtitle = sagaSubtitle(period, anchorDayStartMs),
                periodLabel = period.label,
                totalFlows = totalFlows,
                totalDurationMs = totalDuration,
                totalScore = totalScore
            )

            // ── Delicate action BELOW-RIGHT ───────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { isSagasExpanded = !isSagasExpanded },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isSagasExpanded) "Hide" else "View",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                    )
                }
            }

            // ── Expanded content ──────────────────────────────────────
            AnimatedVisibility(visible = isSagasExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (stats.isEmpty()) {
                        Text(
                            text = "No flows in this view.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    } else {
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                                .verticalScroll(scrollState),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            stats.forEachIndexed { index, stat ->
                                SagaJourneyRow(
                                    rank = index + 1,
                                    stat = stat,
                                    onClick = { onOpenViewJourneys(stat.tagId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SagaHeader(
    title: String,
    subtitle: String,
    periodLabel: String,
    totalFlows: Int,
    totalDurationMs: Long,
    totalScore: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Title row + period pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: icon + title/subtitle
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📜", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            // Right: small period chip
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 1.dp
            ) {
                Text(
                    text = periodLabel,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
        }

        // Stats row (flows / duration / score)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SagaHeaderStat(label = "Flows", value = totalFlows.toString())
                SagaHeaderStat(label = "Duration", value = formatDuration(totalDurationMs))
                SagaHeaderStat(label = "Score", value = "🔥 $totalScore", alignEnd = true)
            }
        }
    }
}

@Composable
private fun SagaHeaderStat(
    label: String,
    value: String,
    alignEnd: Boolean = false
) {
    Column(
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SagaJourneyRow(
    rank: Int,
    stat: Journey7dStatUiModel,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            ) {
                Text(
                    text = "#$rank",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }

            Spacer(Modifier.width(10.dp))

            // Left: journey info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = stat.tagName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${stat.sessionsCount} flow${if (stat.sessionsCount == 1) "" else "s"} • ${formatDuration(stat.totalDurationMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            // Right: score + hint
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stat.totalScore.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Scry journeys",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForwardIos,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                    )
                }
            }
        }
    }
}

/**
 * Subtitle under “Sagas”
 * You can keep this minimal; it reuses your existing date formatting utilities if you want,
 * but this is drop-in and safe.
 */
private fun sagaSubtitle(
    period: StoryPeriod,
    anchorDayStartMs: Long
): String {

    val nowMs = System.currentTimeMillis()

    // Normalize anchor and current period start
    val normalizedAnchor = TimeWindowUtils.normalizeAnchor(anchorDayStartMs, period)
    val currentPeriodStart = TimeWindowUtils.startOfPeriodMs(nowMs, period)

    val isCurrent = normalizedAnchor == currentPeriodStart

    return when (period) {
        StoryPeriod.DAY ->
            if (isCurrent) "Record for today"
            else "Record for this day"

        StoryPeriod.WEEK ->
            if (isCurrent) "Record for this week"
            else "Record for the week"

        StoryPeriod.MONTH ->
            if (isCurrent) "Record for this month"
            else "Record for the month"
    }
}

@Composable
private fun FlowModeHeroCard(
    onGoToActiveSession: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Focus mode active",
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "IN FLOW",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Your story is unfolding now",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Jump back in!",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onGoToActiveSession,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp)
                    .padding(top = 4.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 8.dp,
                    pressedElevation = 12.dp,
                    focusedElevation = 10.dp
                ),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "View Active Flow",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

class ExpandedSessionIdsState(
    private val ids: MutableState<Set<Long>>
) {
    fun isExpanded(id: Long): Boolean = ids.value.contains(id)

    fun toggle(id: Long) {
        ids.value = if (ids.value.contains(id)) ids.value - id else ids.value + id
    }
}

@Composable
fun rememberExpandedSessionIdsState(): ExpandedSessionIdsState {
    val ids = remember { mutableStateOf(setOf<Long>()) }
    return remember { ExpandedSessionIdsState(ids) }
}

// ─────────────────────────────────────────────────────────────────────────────
// Edit dialog helpers
// ─────────────────────────────────────────────────────────────────────────────

class SessionEditState(
    val editingSession: MutableState<FlowListItemUiModel?>,
    val editText: MutableState<String>
) {
    fun startEditing(session: FlowListItemUiModel) {
        editingSession.value = session
        editText.value = session.description
    }

    fun stopEditing() {
        editingSession.value = null
    }
}

@Composable
fun rememberSessionEditState(): SessionEditState {
    val editingSession = remember { mutableStateOf<FlowListItemUiModel?>(null) }
    val editText = remember { mutableStateOf("") }
    return remember { SessionEditState(editingSession, editText) }
}

@Composable
fun FlowEditDialog(
    editState: SessionEditState,
    onSave: (sessionId: Long, newText: String) -> Unit
) {
    val session = editState.editingSession.value ?: return

    AlertDialog(
        onDismissRequest = { editState.stopEditing() },
        title = { Text("Edit description") },
        text = {
            Column {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editState.editText.value,
                    onValueChange = { editState.editText.value = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    placeholder = { Text("Add notes about this session") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(session.sessionId, editState.editText.value)
                    editState.stopEditing()
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { editState.stopEditing() }) { Text("Cancel") }
        }
    )
}

@Composable
fun rememberMiniBarAlpha(
    listState: androidx.compose.foundation.lazy.LazyListState
): State<Float> {

    val density = androidx.compose.ui.platform.LocalDensity.current

    // Fade starts after 180dp scroll, fully visible at 300dp
    val thresholdStartPx = remember(density) {
        with(density) { 180.dp.toPx() }
    }

    val thresholdEndPx = remember(density) {
        with(density) { 300.dp.toPx() }
    }

    val rawScrollOffset = if (listState.firstVisibleItemIndex > 0) {
        thresholdEndPx
    } else {
        listState.firstVisibleItemScrollOffset.toFloat()
    }

    return androidx.compose.animation.core.animateFloatAsState(
        targetValue = when {
            rawScrollOffset < thresholdStartPx -> 0f
            rawScrollOffset >= thresholdEndPx -> 1f
            else -> (rawScrollOffset - thresholdStartPx) /
                    (thresholdEndPx - thresholdStartPx)
        },
        label = "miniBarAlpha"
    )
}


@Composable
private fun FlowStateActiveContent(
    uiState: FlowListUiState,
    listState: LazyListState,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onGoToActiveSession: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    onUpdateSessionDescription: (Long, String) -> Unit
) {
    val expandedState = rememberExpandedSessionIdsState()
    val editState = rememberSessionEditState()

    val miniBarAlpha by rememberMiniBarAlpha(listState)

    Box(modifier = Modifier.fillMaxSize()) {

        FlowEditDialog(
            editState = editState,
            onSave = { sessionId, newText -> onUpdateSessionDescription(sessionId, newText) }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StoryHeader(
                    uiState = uiState,
                    onTagSelected = onTagSelected,
                    onPeriodSelected = onPeriodSelected,
                    onPrev = onPrev,
                    onNext = onNext,
                    onToday = onToday,
                    onOpenViewJourneys = onOpenViewJourneys,
                    extraTopContent = { FlowModeHeroCard(onGoToActiveSession = onGoToActiveSession) }
                )
            }

            items(
                items = uiState.sessions,
                key = { it.sessionId }
            ) { session ->
                FlowCard(
                    session = session,
                    isExpanded = expandedState.isExpanded(session.sessionId),
                    onToggleExpand = { expandedState.toggle(session.sessionId) },
                    onDeleteSession = { onDeleteSession(session.sessionId) },
                    onLongPress = { editState.startEditing(session) },
                    onClick = { onSessionClick(session.sessionId) }
                )
            }
        }

        if (miniBarAlpha > 0f) {
            FocusModeFloatingMiniBar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 20.dp)
                    .graphicsLayer { alpha = miniBarAlpha }
                    .zIndex(10f),
                onClick = onGoToActiveSession
            )
        }
    }
}

@Composable
private fun BeamBonusChip(
    bonusPoints: Int
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "★",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "+$bonusPoints",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FlowCard(
    session: FlowListItemUiModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteSession: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val showSurgeStat = session.isSurge && session.surgePoints > 0
    val isBeamed = session.beamBonusPoints > 0

    val baseContainer = if (session.isSurge) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = {
                onToggleExpand()
                onClick()
            },
            onLongClick = onLongPress
        )

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = baseContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.tagName,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Right rail (stable-ish)
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (isBeamed) {
                        BeamBonusChip(bonusPoints = session.beamBonusPoints)
                        Spacer(Modifier.height(6.dp))
                    }

                    if (showSurgeStat) {
                        Text(
                            text = "+${session.surgePoints}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Surge",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    if (isExpanded) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete session"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
            Spacer(modifier = Modifier.height(10.dp))

            // ── Body ───────────────────────────────────────────────────
            if (session.description.isNotBlank()) {
                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = "Duration: ${formatDuration(session.durationMs)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (BuildConfig.SHOW_SCORE) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Scyra Score: ${session.score}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete flow?") },
            text = { Text("Are you sure you want to delete this flow? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteSession()
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun FocusModeFloatingMiniBar(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Flow State",
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Flow Active",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(end = 12.dp)
            )

            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(38.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
            ) {
                Text("Resume", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun FlowStateInactiveContent(
    uiState: FlowListUiState,
    listState: LazyListState,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    onUpdateSessionDescription: (Long, String) -> Unit
) {
    val expandedState = rememberExpandedSessionIdsState()
    val editState = rememberSessionEditState()

    FlowEditDialog(
        editState = editState,
        onSave = { sessionId, newText -> onUpdateSessionDescription(sessionId, newText) }
    )

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StoryHeader(
                uiState = uiState,
                onTagSelected = onTagSelected,
                onPeriodSelected = onPeriodSelected,
                onPrev = onPrev,
                onNext = onNext,
                onToday = onToday,
                onOpenViewJourneys = onOpenViewJourneys
            )
        }

        items(
            items = uiState.sessions,
            key = { it.sessionId }
        ) { session ->
            FlowCard(
                session = session,
                isExpanded = expandedState.isExpanded(session.sessionId),
                onToggleExpand = { expandedState.toggle(session.sessionId) },
                onDeleteSession = { onDeleteSession(session.sessionId) },
                onLongPress = { editState.startEditing(session) },
                onClick = { onSessionClick(session.sessionId) }
            )
        }
    }
}

@Composable
private fun StoryHeader(
    uiState: FlowListUiState,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onOpenViewJourneys: (Long) -> Unit,
    extraTopContent: (@Composable () -> Unit)? = null
) {
    TagFilterRow(
        tags = uiState.tags,
        selectedTagId = uiState.selectedTagId,
        onTagSelected = onTagSelected
    )

    Spacer(modifier = Modifier.height(12.dp))

    PeriodAndDateNavigator(
        period = uiState.period,
        anchorDayStartMs = uiState.anchorDayStartMs,
        firstSessionStartMs = uiState.firstSessionStartMs,
        onPeriodSelected = onPeriodSelected,
        onPrev = onPrev,
        onNext = onNext,
        onToday = onToday
    )

    Spacer(modifier = Modifier.height(12.dp))

    extraTopContent?.invoke()

    if (uiState.selectedTagId != null) {
        TotalTimeHighlight(
            totalDurationMs = uiState.totalDurationMs,
            subtitle = "Time in view"
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        ScoreDisplay(
            score = uiState.currentScore,
            surgeScore = uiState.currentSurgeScore,
            period = uiState.period,
            modifier = Modifier.fillMaxWidth()
        )
    }

    HorizontalDivider()

    HorizontalDivider()

    Spacer(Modifier.height(12.dp))

    StorySummaryCardSection(
        uiState = uiState,
        onOpenViewJourneys = onOpenViewJourneys
    )
}

@Composable
private fun StorySummaryCardSection(
    uiState: FlowListUiState,
    onOpenViewJourneys: (Long) -> Unit
) {
    if (uiState.sagasInView.isEmpty()) return

    SagasCard(
        period = uiState.period,
        anchorDayStartMs = uiState.anchorDayStartMs,
        stats = uiState.sagasInView,
        onOpenViewJourneys = onOpenViewJourneys
    )
}

@Composable
private fun PeriodAndDateNavigator(
    period: StoryPeriod,
    anchorDayStartMs: Long,
    firstSessionStartMs: Long?,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val nowMs = System.currentTimeMillis()

    // Normalize anchors for correct comparisons (esp when switching period)
    val normalizedAnchor = remember(period, anchorDayStartMs) {
        TimeWindowUtils.normalizeAnchor(anchorDayStartMs, period)
    }

    val minAnchor = remember(period, firstSessionStartMs, nowMs) {
        TimeWindowUtils.startOfPeriodMs(firstSessionStartMs ?: nowMs, period)
    }

    val maxAnchor = remember(period, nowMs) {
        TimeWindowUtils.startOfPeriodMs(nowMs, period)
    }

    val prevAnchor = remember(period, normalizedAnchor) {
        TimeWindowUtils.shiftAnchor(normalizedAnchor, period, -1)
    }

    val nextAnchor = remember(period, normalizedAnchor) {
        TimeWindowUtils.shiftAnchor(normalizedAnchor, period, +1)
    }

    val canGoPrev = prevAnchor >= minAnchor
    val canGoNext = nextAnchor <= maxAnchor

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        // Period chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            listOf(StoryPeriod.DAY, StoryPeriod.WEEK, StoryPeriod.MONTH).forEach { p ->
                FilterChip(
                    selected = p == period,
                    onClick = { onPeriodSelected(p) },
                    label = { Text(p.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // Date nav row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                enabled = canGoPrev
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "Previous",
                    tint = if (canGoPrev) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    }
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatPeriodTitle(period, normalizedAnchor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = formatPeriodSubtitle(period, normalizedAnchor),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }

            IconButton(
                onClick = onNext,
                enabled = canGoNext
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForwardIos,
                    contentDescription = "Next",
                    tint = if (canGoNext) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    }
                )
            }
        }

        val showJumpToNow = normalizedAnchor != maxAnchor

        AnimatedVisibility(visible = showJumpToNow) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Card(
                    onClick = onToday,
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )

                        val label = when (period) {
                            StoryPeriod.DAY -> "Back to Today"
                            StoryPeriod.WEEK -> "Back to This Week"
                            StoryPeriod.MONTH -> "Back to This Month"
                        }

                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatPeriodTitle(period: StoryPeriod, anchorDayStartMs: Long): String {
    val zone = ZoneId.systemDefault()
    val window = TimeWindowUtils.windowFor(anchorDayStartMs, period)
    val start = Instant.ofEpochMilli(window.startMs).atZone(zone).toLocalDate()
    val endExclusive = Instant.ofEpochMilli(window.endMs).atZone(zone).toLocalDate()
    val end = endExclusive.minusDays(1)

    return when (period) {
        StoryPeriod.DAY -> start.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        StoryPeriod.WEEK -> "Week of ${start.format(DateTimeFormatter.ofPattern("MMM d"))}"
        StoryPeriod.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}

private fun formatPeriodSubtitle(period: StoryPeriod, anchorDayStartMs: Long): String {
    val zone = ZoneId.systemDefault()
    val window = TimeWindowUtils.windowFor(anchorDayStartMs, period)
    val start = Instant.ofEpochMilli(window.startMs).atZone(zone).toLocalDate()
    val endExclusive = Instant.ofEpochMilli(window.endMs).atZone(zone).toLocalDate()
    val end = endExclusive.minusDays(1)

    return when (period) {
        StoryPeriod.DAY -> "Sessions in this day"
        StoryPeriod.WEEK -> "${start.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("MMM d"))}"
        StoryPeriod.MONTH -> "Sessions in this month"
    }
}

@Composable
fun TotalTimeHighlight(
    totalDurationMs: Long,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondary,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "TOTAL TIME",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                )
            }

            Text(
                text = formatDuration(totalDurationMs),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun TagFilterRow(
    tags: List<TagUiModel>,
    selectedTagId: Long?,
    onTagSelected: (Long?) -> Unit
) {
    if (tags.isEmpty()) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Observe a journey:",
            style = MaterialTheme.typography.labelSmall
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedTagId == null,
                    onClick = { onTagSelected(null) },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            items(
                items = tags,
                key = { it.id }
            ) { tag ->
                FilterChip(
                    selected = selectedTagId == tag.id,
                    onClick = { onTagSelected(tag.id) },
                    label = { Text(tag.name) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@Composable
fun ScoreDisplay(
    score: Int,
    surgeScore: Int,
    period: StoryPeriod,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 70.sp)
            )

            if (surgeScore > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "+$surgeScore Surge",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = when (period) {
                    StoryPeriod.DAY -> "Today"
                    StoryPeriod.WEEK -> "This view"
                    StoryPeriod.MONTH -> "This view"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    CircularProgressIndicator(modifier = modifier)
}

@Composable
private fun ErrorState(message: String?, modifier: Modifier = Modifier) {
    Text(
        text = message ?: "Error",
        modifier = modifier,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("No sessions yet.")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap + to log your first session.")
    }
}

// FAB, edit dialog, expanded state, mini bar, hero card, flow card etc.
// ✅ Keep your existing implementations below this point unchanged.
// (You can paste your existing implementations as-is.)

@Composable
private fun SkillListFab(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text("+")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewJourneysBottomSheet(
    uiState: FlowListUiState,
    onClose: () -> Unit,
    onSessionClick: (Long) -> Unit
) {
    if (!uiState.isViewJourneysOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Internal "navigation" inside sheet
    var selectedSessionId by remember(uiState.isViewJourneysOpen, uiState.viewJourneysTitle) {
        mutableStateOf<Long?>(null)
    }

    val sessions = uiState.viewJourneysSessions
    val selected = remember(selectedSessionId, sessions) {
        selectedSessionId?.let { id -> sessions.firstOrNull { it.sessionId == id } }
    }

    val windowTitle = remember(uiState.anchorDayStartMs, uiState.period) {
        formatPeriodTitle(uiState.period, uiState.anchorDayStartMs)
    }
    val windowSubtitle = remember(uiState.anchorDayStartMs, uiState.period) {
        formatPeriodSubtitle(uiState.period, uiState.anchorDayStartMs)
    }

    val totalDuration = remember(sessions) { sessions.sumOf { it.durationMs } }
    val totalScyraScore = remember(sessions) { sessions.sumOf { it.score } }
    val totalBeamBonus = remember(sessions) { sessions.sumOf { it.beamBonusPoints } }
    val totalBaseScore = remember(totalScyraScore, totalBeamBonus) { totalScyraScore - totalBeamBonus }
    val totalSurge = remember(sessions) { sessions.sumOf { it.surgePoints } } // separate

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Top bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selected != null) {
                    TextButton(onClick = { selectedSessionId = null }) { Text("Back") }
                } else {
                    Spacer(Modifier.width(8.dp))
                }

                Spacer(Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.viewJourneysTitle.ifBlank { "Journey" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$windowTitle • $windowSubtitle",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                TextButton(onClick = onClose) { Text("Close") }
            }

            if (selected == null) {
                // ── LIST MODE ──────────────────────────────────────────
                JourneyViewSummary(
                    flowsCount = sessions.size,
                    totalDurationMs = totalDuration,
                    totalBaseScore = totalBaseScore,
                    totalBeamBonus = totalBeamBonus,
                    totalScyraScore = totalScyraScore,
                    totalSurge = totalSurge
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))

                if (sessions.isEmpty()) {
                    Text(
                        text = "No flows for this journey in this view.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 6.dp)
                    ) {
                        items(
                            items = sessions,
                            key = { it.sessionId }
                        ) { s ->
                            JourneySessionRow(
                                session = s,
                                onExpand = { selectedSessionId = s.sessionId },
                                onScry = { selectedSessionId = s.sessionId } // ✅ always works
                            )
                        }
                    }
                }
            } else {
                // ── DETAIL MODE ───────────────────────────────────────
                JourneySessionDetail(
                    session = selected,
                    onOpenFull = null // until you actually have a full screen
                )
            }
        }
    }
}

@Composable
private fun JourneyViewSummary(
    flowsCount: Int,
    totalDurationMs: Long,
    totalBaseScore: Int,
    totalBeamBonus: Int,
    totalScyraScore: Int,
    totalSurge: Int
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Row 1: counts + time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$flowsCount flow${if (flowsCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "⏱ ${formatDuration(totalDurationMs)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            // Row 2: Scoring breakdown (no background pills)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScoreBreakdownRow(label = "Base", value = totalBaseScore.toString())
                if (totalBeamBonus > 0) ScoreBreakdownRow(label = "Beam bonus", value = "+$totalBeamBonus")
                ScoreBreakdownRow(label = "Scyra Score", value = "🔥 $totalScyraScore", strong = true)

                if (totalSurge > 0) {
                    ScoreBreakdownRow(label = "Surge", value = "+$totalSurge", strong = false)
                }
            }
        }
    }
}

@Composable
private fun ScoreBreakdownRow(
    label: String,
    value: String,
    strong: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun JourneySessionDetail(
    session: FlowListItemUiModel,
    onOpenFull: (() -> Unit)? = null // optional
) {
    val baseScore = remember(session.score, session.beamBonusPoints) {
        (session.score - session.beamBonusPoints).coerceAtLeast(0)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            // If you want: show tag name too (optional)
            if (session.tagName.isNotBlank()) {
                Text(
                    text = session.tagName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }

            if (session.description.isNotBlank()) {
                Text(text = session.description, style = MaterialTheme.typography.bodyMedium)
            } else {
                Text(
                    text = "No description yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))

            // Core stats
            DetailStatRow(label = "Duration", value = formatDuration(session.durationMs))

            // Score breakdown
            DetailStatRow(label = "Base score", value = baseScore.toString())
            if (session.beamBonusPoints > 0) DetailStatRow(label = "Beam bonus", value = "+${session.beamBonusPoints}")
            DetailStatRow(label = "Scyra Score", value = "🔥 ${session.score}", strong = true)

            if (session.isSurge && session.surgePoints > 0) {
                DetailStatRow(label = "Surge", value = "+${session.surgePoints}")
            }

            if (onOpenFull != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onOpenFull,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Open full flow")
                }
            }
        }
    }
}

@Composable
private fun DetailStatRow(
    label: String,
    value: String,
    strong: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = if (strong) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelLarge,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun JourneySessionRow(
    session: FlowListItemUiModel,
    onExpand: () -> Unit,
    onScry: () -> Unit
) {
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = journeySessionMeta(session),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🔥", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = session.score.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(
                    onClick = onScry,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("Scry", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

private fun journeySessionMeta(session: FlowListItemUiModel): String {
    return buildString {
        append("⏱ ")
        append(formatDuration(session.durationMs))

        if (session.beamBonusPoints > 0) {
            append("  •  ★ +")
            append(session.beamBonusPoints)
        }
        if (session.isSurge && session.surgePoints > 0) {
            append("  •  Surge +")
            append(session.surgePoints)
        }
    }
}
