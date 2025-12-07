package com.kingkharnivore.skillz.ui.skills

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.data.model.entity.SessionListItemUiModel
import com.kingkharnivore.skillz.data.model.entity.TagEntity
import com.kingkharnivore.skillz.ui.viewmodel.SkillListViewModel
import com.kingkharnivore.skillz.utils.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillListScreen(
    viewModel: SkillListViewModel,
    onAddSessionClick: () -> Unit,
    onSessionClick: (Long) -> Unit // sessionId
) {

    val uiState by viewModel.uiState.collectAsState()

    val listState = rememberLazyListState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skillz") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
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

                        if (uiState.selectedTagId != null && uiState.sessions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Total time: ${formatDuration(uiState.totalDurationMs)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        SessionList(
                            sessions = uiState.sessions,
                            listState = listState,
                            onSessionClick = onSessionClick,
                            onDeleteSession = { sessionId ->
                                viewModel.deleteSession(sessionId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TagFilterRow(
    tags: List<TagEntity>,
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
                    label = { Text("All") }
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
                    label = { Text(tag.name) }
                )
            }
        }
    }
}

@Composable
private fun SessionList(
    sessions: List<SessionListItemUiModel>,
    listState: LazyListState,
    onSessionClick: (Long) -> Unit,
    onDeleteSession: (Long) -> Unit
) {
    var expandedSessionIds by remember { mutableStateOf(setOf<Long>()) }

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
                onClick = { onSessionClick(session.sessionId) },
                onDeleteSession = { onDeleteSession(session.sessionId) }
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
    onDeleteSession: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // 🔮 Animated border color between secondary (gold) and primary (maroon)
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val animatedBorderColor by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.secondary,           // gold
        targetValue = MaterialTheme.colorScheme.primary,             // maroon
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderColor"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onToggleExpand()
            }
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

private fun formatDurationMinutes(durationMs: Long): Long {
    return durationMs / 1000L / 60L
}
