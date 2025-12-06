package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.kingkharnivore.skillz.data.model.SkillEntity
import com.kingkharnivore.skillz.ui.theme.GryffindorOffWhite
import com.kingkharnivore.skillz.ui.theme.GryffindorRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkillListScreen(
    viewModel: SkillListViewModel,
    onAddSkillClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Skillz") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = GryffindorRed,
                    titleContentColor = GryffindorOffWhite
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSkillClick,
                containerColor = GryffindorRed,
                contentColor = GryffindorOffWhite
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Skill")
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

                uiState.skills.isEmpty() -> {
                    Text(
                        text = "No skills yet. Tap + to add one.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    SkillList(
                        skills = uiState.skills,
                        onSkillClick = { /* TODO: open detail later */ }
                    )
                }
            }
        }
    }
}

@Composable
private fun SkillList(
    skills: List<SkillEntity>,
    onSkillClick: (SkillEntity) -> Unit
) {
    var expandedSkillIds by remember { mutableStateOf(setOf<Long>()) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(skills) { skill ->
            val isExpanded = expandedSkillIds.contains(skill.id)
            SkillRow(
                skill = skill,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedSkillIds = if (isExpanded) {
                        expandedSkillIds - skill.id
                    } else {
                        expandedSkillIds + skill.id
                    }
                },
                onClick = { onSkillClick(skill) }
            )
        }
    }
}

@Composable
private fun SkillRow(
    skill: SkillEntity,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface

        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // toggle expanded state when card is clicked
                onToggleExpand()
            }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = skill.name,
                style = MaterialTheme.typography.titleMedium
            )
            if (skill.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = skill.description,
                    style = MaterialTheme.typography.bodyMedium,
                    // if expanded: show everything, else: 2 lines with ellipsis
                    maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                    overflow = if (isExpanded) TextOverflow.Visible else TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isExpanded) "Tap to collapse" else "Tap to view more",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (skill.totalTimeMs > 0L) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total: ${formatDurationMinutes(skill.totalTimeMs)} min",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun formatDurationMinutes(durationMs: Long): Long {
    return durationMs / 1000L / 60L
}
