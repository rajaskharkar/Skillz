package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.BuildConfig
import com.kingkharnivore.skillz.data.model.entity.SessionListItemUiModel
import com.kingkharnivore.skillz.ui.components.SkillzTopAppBar
import com.kingkharnivore.skillz.ui.viewmodel.SkillListViewModel
import com.kingkharnivore.skillz.ui.viewmodel.TagUiModel
import com.kingkharnivore.skillz.utils.formatDuration
import com.kingkharnivore.skillz.utils.score.ScoreFilter

@OptIn(ExperimentalMaterial3Api::class)
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

    val title = when (BuildConfig.FLAVOR) {
        "aera" -> "Aera"
        "scyra" -> "Scyra"
        else -> "Skillz"
    }

    Scaffold(
        topBar = { SkillzTopAppBar() },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSessionClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text("+") // you can swap to an Icon if you prefer
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = uiState.errorMessage ?: "Error",
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }

                uiState.sessions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No sessions yet.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tap + to log your first session.")
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {

                        TagFilterRow(
                            tags = uiState.tags,
                            selectedTagId = uiState.selectedTagId,
                            onTagSelected = { tagId ->
                                viewModel.selectTag(tagId)
                            }
                        )

                        if (isFocusModeOn) {
                            Spacer(modifier = Modifier.height(16.dp))

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

                                    // 🔹 Icon
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

                                    // 🔹 Text content
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

                                    // 🔹 BIG CENTERED BUTTON
                                    // 🔥 ELEVATED, POPPING CTA BUTTON
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
                                            containerColor = MaterialTheme.colorScheme.onPrimary,     // bright contrasting
                                            contentColor = MaterialTheme.colorScheme.primary          // Scyra color text
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

                        if (uiState.selectedTagId != null && uiState.sessions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total time: ${formatDuration(uiState.totalDurationMs)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        if (BuildConfig.SHOW_SCORE) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // 🔹 Score filter chips (24h / 7d / 30d / all)
                            ScoreFilterChips(
                                selectedFilter = uiState.scoreFilter,
                                onFilterSelected = viewModel::onScoreFilterSelected
                            )


                            Spacer(Modifier.height(16.dp))

                            // 🔹 Score in the middle of the screen
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                ScoreDisplay(
                                    score = uiState.currentScore,
                                    scoreFilter = uiState.scoreFilter,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                )
                            }

                            HorizontalDivider()
                        }

                        SessionList(
                            sessions = uiState.sessions,
                            listState = listState,
                            onSessionClick = { item ->
                                onSessionClick(item.sessionId)
                            },
                            onDeleteSession = { item ->
                                viewModel.deleteSession(item.sessionId)   // use the id from the UiModel
                            },
                            onUpdateSessionDescription = { item, description ->
                                viewModel.updateSessionDescription(item.sessionId, description)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreFilterChips(
    selectedFilter: ScoreFilter,
    onFilterSelected: (ScoreFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        ScoreFilter.values().forEach { filter ->
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
            // "All" chip
            item {
                FilterChip(
                    selected = selectedTagId == null,
                    onClick = { onTagSelected(null) },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            // One chip per tag
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
        modifier = modifier
            .padding(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Big number
            Text(
                text = "$score",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 70.sp,
                    fontWeight = FontWeight.Normal
                ),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(4.dp))

            // Label for selected window
            Text(
                text = when (scoreFilter) {
                    ScoreFilter.LAST_24_HOURS -> "Last 24 hours"
                    ScoreFilter.LAST_7_DAYS   -> "Last 7 days"
                    ScoreFilter.LAST_30_DAYS  -> "Last 30 days"
                    ScoreFilter.ALL_TIME      -> "All time"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SessionList(
    sessions: List<SessionListItemUiModel>,
    listState: LazyListState,
    onSessionClick: (SessionListItemUiModel) -> Unit,
    onDeleteSession: (SessionListItemUiModel) -> Unit,
    onUpdateSessionDescription: (SessionListItemUiModel, String) -> Unit
)
{
    var expandedSessionIds by remember { mutableStateOf(setOf<Long>()) }

    var editingSession by remember { mutableStateOf<SessionListItemUiModel?>(null) }
    var editText by remember { mutableStateOf("") }

    // ─── Edit Description Dialog ─────────────────────
    if (editingSession != null) {
        AlertDialog(
            onDismissRequest = { editingSession = null },
            title = { Text("Edit description") },
            text = {
                Column {
                    Text(
                        text = editingSession!!.title,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
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
                        val session = editingSession
                        if (session != null) {
                            onUpdateSessionDescription(session, editText)
                        }
                        editingSession = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingSession = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = sessions,
            key = { it.sessionId }
        ) { session ->
            val isExpanded = expandedSessionIds.contains(session.sessionId)

            SessionRowCard(
                session = session,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedSessionIds = if (isExpanded) {
                        expandedSessionIds - session.sessionId
                    } else {
                        expandedSessionIds + session.sessionId
                    }
                },
                onClick = { onSessionClick(session) },
                onDeleteSession = { onDeleteSession(session) },
                onLongPress = {
                    editingSession = session
                    editText = session.description
                }
            )
        }
    }
}

@Composable
private fun SessionRowCard(
    session: SessionListItemUiModel,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit,
    onDeleteSession: () -> Unit,
    onLongPress: () -> Unit
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
            onClick = { onToggleExpand() },
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
                    // Skill / Tag name
                    Text(
                        text = session.tagName,
                        style = MaterialTheme.typography.labelMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // 🗑 Show delete icon ONLY when expanded
                if (isExpanded) {
                    IconButton(
                        onClick = { showDeleteDialog = true }
                    ) {
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
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
