@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.skills

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
import com.kingkharnivore.skillz.ui.theme.AntiqueGold
import com.kingkharnivore.skillz.ui.theme.RavenclawBlue
import com.kingkharnivore.skillz.viewmodel.StoryViewModel
import com.kingkharnivore.skillz.viewmodel.TagUiModel
import com.kingkharnivore.skillz.utils.formatDuration
import com.kingkharnivore.skillz.utils.score.ScoreFilter

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

    androidx.compose.runtime.LaunchedEffect(uiState.sessions.size) {
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
                onScoreFilterSelected = viewModel::onScoreFilterSelected,
                onGoToActiveSession = onGoToActiveSession,
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
    onScoreFilterSelected: (ScoreFilter) -> Unit,
    onGoToActiveSession: () -> Unit,
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 24.dp)
                    ) {
                        StoryHeader(
                            uiState = uiState,
                            onTagSelected = onTagSelected,
                            onScoreFilterSelected = onScoreFilterSelected,
                            extraTopContent = {
                                FlowModeHeroCard(onGoToActiveSession = onGoToActiveSession)
                            }
                        )
                    }
                }
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }

            else -> {
                if (isFlowStateActive) {
                    FlowStateActiveContent(
                        uiState = uiState,
                        listState = listState,
                        onTagSelected = onTagSelected,
                        onScoreFilterSelected = onScoreFilterSelected,
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
                        onScoreFilterSelected = onScoreFilterSelected,
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
private fun FlowStateActiveContent(
    uiState: FlowListUiState,
    listState: LazyListState,
    onTagSelected: (Long?) -> Unit,
    onScoreFilterSelected: (ScoreFilter) -> Unit,
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
            onSave = { sessionId, newText ->
                onUpdateSessionDescription(sessionId, newText)
            }
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
                    onScoreFilterSelected = onScoreFilterSelected,
                    extraTopContent = {
                        FlowModeHeroCard(onGoToActiveSession = onGoToActiveSession)
                    }
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

@Composable
private fun FlowStateInactiveContent(
    uiState: FlowListUiState,
    listState: LazyListState,
    onTagSelected: (Long?) -> Unit,
    onScoreFilterSelected: (ScoreFilter) -> Unit,
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
            onScoreFilterSelected = onScoreFilterSelected
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
fun TotalTimeHighlight(
    totalDurationMs: Long,
    scoreFilterLabel: String,
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
                    text = scoreFilterLabel,
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
private fun StoryHeader(
    uiState: FlowListUiState,
    onTagSelected: (Long?) -> Unit,
    onScoreFilterSelected: (ScoreFilter) -> Unit,
    extraTopContent: (@Composable () -> Unit)? = null
) {
    TagFilterRow(
        tags = uiState.tags,
        selectedTagId = uiState.selectedTagId,
        onTagSelected = onTagSelected
    )

    Spacer(modifier = Modifier.height(16.dp))

    extraTopContent?.invoke()

    if (uiState.selectedTagId != null && uiState.sessions.isNotEmpty()) {
        Spacer(modifier = Modifier.height(8.dp))
        TotalTimeHighlight(
            totalDurationMs = uiState.totalDurationMs,
            scoreFilterLabel = uiState.scoreFilter.label
        )

        Spacer(Modifier.height(12.dp))
    }

    if (BuildConfig.SHOW_SCORE) {
        Spacer(modifier = Modifier.height(12.dp))

        ScoreFilterChips(
            selectedFilter = uiState.scoreFilter,
            availableFilters = uiState.availableScoreFilters,
            onFilterSelected = onScoreFilterSelected
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            ScoreDisplay(
                score = uiState.currentScore,
                surgeScore = uiState.currentSurgeScore,
                scoreFilter = uiState.scoreFilter,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()
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

private class SessionEditState(
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
private fun rememberSessionEditState(): SessionEditState {
    val editingSession = remember { mutableStateOf<FlowListItemUiModel?>(null) }
    val editText = remember { mutableStateOf("") }
    return remember { SessionEditState(editingSession, editText) }
}

@Composable
private fun FlowEditDialog(
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

private class ExpandedSessionIdsState(
    private val ids: MutableState<Set<Long>>
) {
    fun isExpanded(id: Long): Boolean = ids.value.contains(id)

    fun toggle(id: Long) {
        ids.value = if (ids.value.contains(id)) ids.value - id else ids.value + id
    }
}

@Composable
private fun rememberExpandedSessionIdsState(): ExpandedSessionIdsState {
    val ids = remember { mutableStateOf(setOf<Long>()) }
    return remember { ExpandedSessionIdsState(ids) }
}

@Composable
private fun rememberMiniBarAlpha(listState: LazyListState): State<Float> {
    val density = LocalDensity.current
    val thresholdStartPx = remember(density) { with(density) { 180.dp.toPx() } }
    val thresholdEndPx = remember(density) { with(density) { 300.dp.toPx() } }

    val rawScrollOffset = if (listState.firstVisibleItemIndex > 0) {
        thresholdEndPx
    } else {
        listState.firstVisibleItemScrollOffset.toFloat()
    }

    return animateFloatAsState(
        targetValue = when {
            rawScrollOffset < thresholdStartPx -> 0f
            rawScrollOffset >= thresholdEndPx -> 1f
            else -> (rawScrollOffset - thresholdStartPx) / (thresholdEndPx - thresholdStartPx)
        },
        label = "miniBarAlpha"
    )
}

@Composable
fun ScoreFilterChips(
    selectedFilter: ScoreFilter,
    availableFilters: Set<ScoreFilter>,
    onFilterSelected: (ScoreFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        ScoreFilter.values()
            .filter { it in availableFilters }
            .forEach { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) },
                    modifier = Modifier.padding(end = 8.dp)
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
    scoreFilter: ScoreFilter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Primary score
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 70.sp
                )
            )

            // Surge score (only if > 0)
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
                text = when (scoreFilter) {
                    ScoreFilter.LAST_24_HOURS -> "Last 24 hours"
                    ScoreFilter.LAST_7_DAYS -> "Last 7 days"
                    ScoreFilter.LAST_30_DAYS -> "Last 30 days"
                    ScoreFilter.ALL_TIME -> "All time"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

    val baseContainer = if (session.isSurge) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.surface
    }

    val isBeamed = session.beamBonusPoints > 0
    // If beamed, tint the base container slightly (doesn't break theme)
    val container = if (isBeamed) {
        baseContainer.copy(alpha = 1f) // keep base solid
    } else baseContainer

    val showSurgeStat = session.isSurge && session.surgePoints > 0


    // Surge “ink” that reads well in both light & dark without relying on primary
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f

    // Ravenclaw tinge (use your constant if you have it globally)
    val ravenclaw = RavenclawBlue

    // Beam visuals (subtle in dark/light)
    val beamOutline = ravenclaw.copy(alpha = if (isLightTheme) 0.35f else 0.55f)
    val beamRail = ravenclaw.copy(alpha = if (isLightTheme) 0.90f else 0.95f)
    val beamTint = ravenclaw.copy(alpha = if (isLightTheme) 0.06f else 0.12f)
    val surgeInk = if (isLightTheme) {
        Color(0xFF7B2D2A) // oxide crimson (light)
    } else {
        Color(0xFFFFC56A) // molten gold (dark)
    }

    // ── Keep layout stable so Surge stat never shifts on expand/collapse ──
    val deleteSlotWidth = 48.dp // IconButton touch target width
    val surgeSlotMinWidth = 92.dp // room for +999 + label
    val rightRailMinWidth = (if (showSurgeStat) surgeSlotMinWidth else 0.dp) + deleteSlotWidth

    // ── Subtle expand/collapse feedback even if description is empty ──
    val extraPad by animateDpAsState(
        targetValue = if (isExpanded) 6.dp else 2.dp,
        label = "flowCardExtraPad"
    )
    val dividerAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        label = "flowCardDividerAlpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.995f,
        label = "flowCardScale"
    )

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // ✅ Beam tint overlay behind the card (subtle)
        if (isBeamed) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(beamTint)
            )
        }

        // ✅ Use OutlinedCard when beamed (cleanest “special” cue)
        val cardModifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .combinedClickable(
                onClick = {
                    onToggleExpand()
                    onClick()
                },
                onLongClick = onLongPress
            )

        if (isBeamed) {
            OutlinedCard(
                modifier = cardModifier,
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = container,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = CardDefaults.outlinedCardBorder(enabled = true)
            ) {
                FlowCardContent(
                    session = session,
                    isExpanded = isExpanded,
                    showSurgeStat = showSurgeStat,
                    surgeInk = surgeInk,
                    deleteSlotWidth = deleteSlotWidth,
                    surgeSlotMinWidth = surgeSlotMinWidth,
                    rightRailMinWidth = rightRailMinWidth,
                    dividerAlpha = dividerAlpha,
                    extraPad = extraPad,
                    isBeamed = isBeamed,
                    beamRail = beamRail,
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = container,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                modifier = cardModifier
            ) {
                FlowCardContent(
                    session = session,
                    isExpanded = isExpanded,
                    showSurgeStat = showSurgeStat,
                    surgeInk = surgeInk,
                    deleteSlotWidth = deleteSlotWidth,
                    surgeSlotMinWidth = surgeSlotMinWidth,
                    rightRailMinWidth = rightRailMinWidth,
                    dividerAlpha = dividerAlpha,
                    extraPad = extraPad,
                    isBeamed = false,
                    beamRail = beamRail,
                    onDeleteClick = { showDeleteDialog = true }
                )
            }
        }

        // ✅ Extra: draw a thin left rail for beamed flows
        if (isBeamed) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 8.dp, top = 10.dp, bottom = 10.dp)
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(beamRail)
            )
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
private fun FlowCardContent(
    session: FlowListItemUiModel,
    isExpanded: Boolean,
    showSurgeStat: Boolean,
    surgeInk: Color,
    deleteSlotWidth: Dp,
    surgeSlotMinWidth: Dp,
    rightRailMinWidth: Dp,
    dividerAlpha: Float,
    extraPad: Dp,
    isBeamed: Boolean,
    beamRail: Color,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {

        // ── Header row ───────────────────────────────────────────────────────
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

            // ── Right rail (your existing stable rail) ────────────────────────
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .widthIn(min = rightRailMinWidth)
                    .heightIn(min = 48.dp)
            ) {

                // ⭐ Beam bonus (top priority)
                if (isBeamed) {
                    val isLightThemeColors: Boolean =
                        MaterialTheme.colorScheme.background.luminance() > 0.5f
                    BeamBonusChip(
                        bonusPoints = session.beamBonusPoints,
                        starColor = if (isLightThemeColors) RavenclawBlue else AntiqueGold
                    )
                    Spacer(Modifier.height(6.dp))
                }

                // ⚡ Surge stat
                if (showSurgeStat) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${session.surgePoints}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = surgeInk
                        )
                        Text(
                            text = "Surge",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.5.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                // 🗑 Delete slot — ALWAYS RESERVED
                Box(
                    modifier = Modifier
                        .width(deleteSlotWidth)
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isExpanded) {
                        IconButton(onClick = onDeleteClick) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete session"
                            )
                        }
                    } else {
                        Spacer(Modifier.size(24.dp)) // invisible placeholder
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        HorizontalDivider(
            modifier = Modifier.graphicsLayer { alpha = dividerAlpha },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        )

        Spacer(modifier = Modifier.height(extraPad))

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

@Composable
private fun BeamBonusChip(
    bonusPoints: Int,
    starColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = starColor.copy(alpha = 0.14f),
        contentColor = starColor,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "★", // gold star
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
                text = "Focus Mode Active",
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
