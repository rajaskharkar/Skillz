@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.skills

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.SessionListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.SessionListUiState
import com.kingkharnivore.skillz.ui.components.SkillzTopAppBar
import com.kingkharnivore.skillz.ui.viewmodel.SkillListViewModel
import com.kingkharnivore.skillz.ui.viewmodel.TagUiModel
import com.kingkharnivore.skillz.utils.formatDuration
import com.kingkharnivore.skillz.utils.score.ScoreFilter

@Composable
fun SkillListScreen(
    viewModel: SkillListViewModel,
    onAddSessionClick: () -> Unit,
    onSessionClick: (Long) -> Unit,
    onGoToActiveSession: () -> Unit,
    isFocusModeOn: Boolean
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.sessions.size) {
        if (uiState.sessions.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Scaffold(
        topBar = { SkillzTopAppBar() },
        floatingActionButton = { SkillListFab(onClick = onAddSessionClick) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SkillListBody(
                uiState = uiState,
                listState = listState,
                isFocusModeOn = isFocusModeOn,
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

/* ──────────────────────────────────────────────────────────────────────────────
 * Body: loading / error / empty / content
 * ────────────────────────────────────────────────────────────────────────────── */

@Composable
private fun SkillListBody(
    uiState: SessionListUiState,
    listState: LazyListState,
    isFocusModeOn: Boolean,
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
                EmptyState(modifier = Modifier.align(Alignment.Center))
            }

            else -> {
                if (isFocusModeOn) {
                    FocusModeContent(
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
                    NormalModeContent(
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

/* ──────────────────────────────────────────────────────────────────────────────
 * Focus Mode: Entire screen scrolls + floating mini bar
 * ────────────────────────────────────────────────────────────────────────────── */

@Composable
private fun FocusModeContent(
    uiState: SessionListUiState,
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

        SessionEditDialog(
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
                SkillListHeader(
                    uiState = uiState,
                    onTagSelected = onTagSelected,
                    onScoreFilterSelected = onScoreFilterSelected,
                    extraTopContent = {
                        FocusModeHeroCard(onGoToActiveSession = onGoToActiveSession)
                    }
                )
            }

            items(
                items = uiState.sessions,
                key = { it.sessionId }
            ) { session ->
                SessionRowCard(
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
private fun FocusModeHeroCard(
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
                    text = "FOCUS MODE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Active session in progress",
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
                    text = "Resume Session",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
 * Normal Mode: Header static, sessions scroll
 * ────────────────────────────────────────────────────────────────────────────── */

@Composable
private fun NormalModeContent(
    uiState: SessionListUiState,
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
        SkillListHeader(
            uiState = uiState,
            onTagSelected = onTagSelected,
            onScoreFilterSelected = onScoreFilterSelected
        )

        SessionEditDialog(
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
                SessionRowCard(
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

/* ──────────────────────────────────────────────────────────────────────────────
 * Header: tags + total time + score + optional focus card
 * ────────────────────────────────────────────────────────────────────────────── */

@Composable
private fun SkillListHeader(
    uiState: SessionListUiState,
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
                scoreFilter = uiState.scoreFilter,
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()
    }
}

/* ──────────────────────────────────────────────────────────────────────────────
 * Small UI states
 * ────────────────────────────────────────────────────────────────────────────── */

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

/* ──────────────────────────────────────────────────────────────────────────────
 * Edit dialog state (shared by both modes)
 * ────────────────────────────────────────────────────────────────────────────── */

private class SessionEditState(
    val editingSession: MutableState<SessionListItemUiModel?>,
    val editText: MutableState<String>
) {
    fun startEditing(session: SessionListItemUiModel) {
        editingSession.value = session
        editText.value = session.description
    }

    fun stopEditing() {
        editingSession.value = null
    }
}

@Composable
private fun rememberSessionEditState(): SessionEditState {
    val editingSession = remember { mutableStateOf<SessionListItemUiModel?>(null) }
    val editText = remember { mutableStateOf("") }
    return remember { SessionEditState(editingSession, editText) }
}

@Composable
private fun SessionEditDialog(
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

/* ──────────────────────────────────────────────────────────────────────────────
 * Expand/collapse state
 * ────────────────────────────────────────────────────────────────────────────── */

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

/* ──────────────────────────────────────────────────────────────────────────────
 * Mini bar alpha computation (extracted)
 * ────────────────────────────────────────────────────────────────────────────── */

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

/* ──────────────────────────────────────────────────────────────────────────────
 * Existing reusable components (kept)
 * ────────────────────────────────────────────────────────────────────────────── */

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
            text = "Filter by skill:",
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
    scoreFilter: ScoreFilter,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 70.sp,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

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
private fun SessionRowCard(
    session: SessionListItemUiModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onDeleteSession: () -> Unit,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    onToggleExpand()
                    onClick()
                },
                onLongClick = onLongPress
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
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

                if (isExpanded) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete session"
                        )
                    }
                }
            }

            if (session.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = session.description,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isExpanded) "Tap to collapse" else "Tap to view more",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Duration: ${formatDuration(session.durationMs)}",
                style = MaterialTheme.typography.bodySmall
            )

            if (BuildConfig.SHOW_SCORE) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Points earned: ${session.score}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete session?") },
            text = { Text("Are you sure you want to delete this session? This action cannot be undone.") },
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
                contentDescription = "Focus Mode",
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
