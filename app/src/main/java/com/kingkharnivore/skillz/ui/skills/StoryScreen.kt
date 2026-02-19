@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.skills

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.FlowListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.FlowListUiState
import com.kingkharnivore.skillz.data.model.entity.Journey7dStatUiModel
import com.kingkharnivore.skillz.ui.theme.AntiqueGold
import com.kingkharnivore.skillz.ui.theme.RavenclawBlue
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
    ) { innerPadding ->
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
                onUpdateSessionDescription = viewModel::updateSessionDescription
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
    onUpdateSessionDescription: (Long, String) -> Unit
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
                                onToday = onToday
                            )
                        }

                        item {
                            EmptyDayMotivation(
                                uiState = uiState,
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
                        onUpdateSessionDescription = onUpdateSessionDescription
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDayMotivation(
    uiState: FlowListUiState,
    onAddSessionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ AERA CTA
        if (BuildConfig.FLAVOR == "aera") {
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

        // ✅ 7 day motivation summary
        TopJourneysLast7DaysCard(stats = uiState.topJourneysLast7d)
    }
}

@Composable
private fun TopJourneysLast7DaysCard(
    stats: List<Journey7dStatUiModel>
) {
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Last 7 days",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Most active journeys",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            if (stats.isEmpty()) {
                Text(
                    text = "No recent journeys yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
                return@Column
            }

            // ✅ IMPORTANT: NO LazyColumn here (prevents nested vertical scroll crash)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                stats.forEach { s ->
                    JourneyStatRow(stat = s)
                }
            }
        }
    }
}

@Composable
private fun JourneyStatRow(
    stat: Journey7dStatUiModel
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stat.tagName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${stat.sessionsCount} flow${if (stat.sessionsCount == 1) "" else "s"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "🔥 ${stat.totalScore}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "⏱ ${formatDuration(stat.totalDurationMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
                )
            }
        }
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
    onUpdateSessionDescription: (Long, String) -> Unit
) {
    val expandedState = rememberExpandedSessionIdsState()
    val editState = rememberSessionEditState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        StoryHeader(
            uiState = uiState,
            onTagSelected = onTagSelected,
            onPeriodSelected = onPeriodSelected,
            onPrev = onPrev,
            onNext = onNext,
            onToday = onToday
        )

        FlowEditDialog(
            editState = editState,
            onSave = { sessionId, newText -> onUpdateSessionDescription(sessionId, newText) }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
}

@Composable
private fun StoryHeader(
    uiState: FlowListUiState,
    onTagSelected: (Long?) -> Unit,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
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

    if (uiState.selectedTagId != null && uiState.sessions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        TotalTimeHighlight(
            totalDurationMs = uiState.totalDurationMs,
            subtitle = "Time in view"
        )
        Spacer(Modifier.height(12.dp))
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
    val nowMs = remember { System.currentTimeMillis() }

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
