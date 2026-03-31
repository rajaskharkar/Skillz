@file:OptIn(ExperimentalMaterial3Api::class)

package com.kingkharnivore.skillz.ui.screen.paths.arc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoGraph
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.viewmodel.ArcDetailLaunchPayload
import com.kingkharnivore.skillz.viewmodel.ArcDetailStepUiModel
import com.kingkharnivore.skillz.viewmodel.ArcDetailUiState
import com.kingkharnivore.skillz.viewmodel.ArcDetailViewModel

@Composable
fun ArcDetailScreen(
    viewModel: ArcDetailViewModel,
    onBack: () -> Unit,
    onEditArc: (Long) -> Unit,
    onBeginArc: (ArcDetailLaunchPayload) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing
            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
        bottomBar = {
            if (!uiState.isLoading && uiState.errorMessage == null) {
                Surface(shadowElevation = 4.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Back")
                        }

                        if (uiState.hasActiveRun) {
                            TextButton(
                                onClick = {
                                    viewModel.restartArc(onReady = onBeginArc)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Restart")
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.beginArc(onReady = onBeginArc)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            enabled = uiState.steps.isNotEmpty()
                        ) {
                            Text(if (uiState.hasActiveRun) "Resume Arc" else "Begin Arc")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            uiState.errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text(
                            text = uiState.errorMessage ?: "Something went wrong",
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            else -> {
                ArcDetailContent(
                    uiState = uiState,
                    onEditArc = { onEditArc(uiState.arcId) },
                    onAddToStudio = viewModel::addToStudio,
                    onRemoveFromStudio = viewModel::removeFromStudio,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun ArcDetailContent(
    uiState: ArcDetailUiState,
    onEditArc: () -> Unit,
    onAddToStudio: () -> Unit,
    onRemoveFromStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ArcHeaderCard(
                uiState = uiState,
                onEditArc = onEditArc,
                onAddToStudio = onAddToStudio,
                onRemoveFromStudio = onRemoveFromStudio
            )
        }

        item {
            ArcSummaryCard(uiState = uiState)
        }

        if (uiState.hasActiveRun && uiState.activeStepNumber != null) {
            item {
                ActiveRunCard(
                    stepNumber = uiState.activeStepNumber!!,
                    totalSteps = uiState.activeRunTotalSteps ?: uiState.steps.size
                )
            }
        }

        item {
            ArcRecurrenceCard(
                recurrenceType = uiState.recurrenceType,
                recurrenceDaysCsv = uiState.recurrenceDaysCsv
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Route",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "This is the sequence you’ll move through when you begin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
        }

        items(uiState.steps, key = { it.id }) { step ->
            ArcStepCard(
                step = step,
                isActiveStep = uiState.activeRunStepIndex == step.orderIndex
            )
        }
    }
}

@Composable
private fun ActiveRunCard(
    stepNumber: Int,
    totalSteps: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Arc in progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Resume from step $stepNumber of $totalSteps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
            )
        }
    }
}

@Composable
private fun ArcRecurrenceCard(
    recurrenceType: String,
    recurrenceDaysCsv: String
) {
    val label = when (recurrenceType) {
        "daily" -> "Daily"
        "weekdays" -> "Weekdays"
        "weekly" -> "Weekly"
        "custom" -> {
            val days = recurrenceDaysCsv
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .sorted()
                .mapNotNull {
                    when (it) {
                        1 -> "Mon"
                        2 -> "Tue"
                        3 -> "Wed"
                        4 -> "Thu"
                        5 -> "Fri"
                        6 -> "Sat"
                        7 -> "Sun"
                        else -> null
                    }
                }
            if (days.isEmpty()) "Custom" else days.joinToString(" • ")
        }
        else -> "One time"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Repeat",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
            )
        }
    }
}

@Composable
private fun ArcHeaderCard(
    uiState: ArcDetailUiState,
    onEditArc: () -> Unit,
    onAddToStudio: () -> Unit,
    onRemoveFromStudio: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoGraph,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.isInStudio) "Studio Arc" else "Arc",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = uiState.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (uiState.launchCount > 0) {
                    "Launched ${uiState.launchCount} ${if (uiState.launchCount == 1) "time" else "times"}"
                } else {
                    "Not launched yet"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onEditArc) {
                    Text("Edit")
                }

                if (uiState.isInStudio) {
                    TextButton(onClick = onRemoveFromStudio) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null
                        )
                        Text("Remove from Studio")
                    }
                } else {
                    TextButton(onClick = onAddToStudio) {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = null
                        )
                        Text("Add to Studio")
                    }
                }
            }
        }
    }
}

@Composable
private fun ArcSummaryCard(
    uiState: ArcDetailUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val summary = buildList {
                add("${uiState.steps.size} ${if (uiState.steps.size == 1) "step" else "steps"}")
                if (uiState.untimedCount > 0) {
                    add("${uiState.totalMinutes}+ min")
                } else {
                    add("${uiState.totalMinutes} min")
                }
            }.joinToString(" • ")

            Text(
                text = summary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            val detail = buildList {
                if (uiState.untimedCount > 0) add("${uiState.untimedCount} untimed")
                if (uiState.surgeCount > 0) add("${uiState.surgeCount} Surge")
                if (uiState.softCount > 0) add("${uiState.softCount} Soft")
            }.joinToString(" • ")

            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
        }
    }
}

@Composable
private fun ArcStepCard(
    step: ArcDetailStepUiModel,
    isActiveStep: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActiveStep) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = (step.orderIndex + 1).toString(),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Icon(
                    imageVector = if (step.isSoftMode) Icons.Outlined.Spa else Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = if (step.isSoftMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 10.dp)
                )

                Column(
                    modifier = Modifier.padding(start = 10.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (step.tagName.isNotBlank()) {
                        Text(
                            text = step.tagName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isActiveStep) {
                    MiniBadge(text = "Current")
                }
                step.targetMinutes?.let { MiniBadge(text = "${it} min") }
                if (step.launchWithSurge) {
                    MiniBadge(
                        text = "Surge",
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.Speed,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
                if (step.isSoftMode) {
                    MiniBadge(text = "Soft")
                }
            }
        }
    }
}

@Composable
private fun MiniBadge(
    text: String,
    icon: @Composable (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon?.invoke()
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}