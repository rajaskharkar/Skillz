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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
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

    val backText = stringResource(R.string.arc_detail_back)
    val restartText = stringResource(R.string.arc_detail_restart)
    val resumeArcText = stringResource(R.string.arc_detail_resume_arc)
    val beginArcText = stringResource(R.string.arc_detail_begin_arc)
    val loadingText = stringResource(R.string.arc_detail_loading)
    val genericErrorText = stringResource(R.string.arc_detail_error_generic)

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
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = backText
                                }
                        ) {
                            Text(backText)
                        }

                        if (uiState.hasActiveRun) {
                            TextButton(
                                onClick = {
                                    viewModel.restartArc(onReady = onBeginArc)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .semantics {
                                        contentDescription = restartText
                                    }
                            ) {
                                Text(restartText)
                            }
                        }

                        val primaryText = if (uiState.hasActiveRun) resumeArcText else beginArcText

                        Button(
                            onClick = {
                                viewModel.beginArc(onReady = onBeginArc)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics {
                                    contentDescription = primaryText
                                },
                            shape = RoundedCornerShape(999.dp),
                            enabled = uiState.steps.isNotEmpty()
                        ) {
                            Text(primaryText)
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
                    CircularProgressIndicator(
                        modifier = Modifier.semantics {
                            contentDescription = loadingText
                        }
                    )
                }
            }

            uiState.errorMessage != null -> {
                val errorText = uiState.errorMessage ?: genericErrorText

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
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.semantics {
                            contentDescription = errorText
                        }
                    ) {
                        Text(
                            text = errorText,
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
    val routeTitle = stringResource(R.string.arc_detail_route_title)
    val routeSubtitle = stringResource(R.string.arc_detail_route_subtitle)

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
                    text = routeTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = routeSubtitle,
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
    val titleText = stringResource(R.string.arc_detail_active_run_title)
    val bodyText = stringResource(R.string.arc_detail_active_run_body, stepNumber, totalSteps)
    val a11yText = stringResource(R.string.arc_detail_active_run_a11y, titleText, bodyText)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11yText
            },
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
                text = titleText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = bodyText,
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
    val repeatTitle = stringResource(R.string.arc_detail_repeat_title)
    val oneTime = stringResource(R.string.arc_detail_repeat_one_time)
    val daily = stringResource(R.string.arc_detail_repeat_daily)
    val weekdays = stringResource(R.string.arc_detail_repeat_weekdays)
    val weekly = stringResource(R.string.arc_detail_repeat_weekly)
    val custom = stringResource(R.string.arc_detail_repeat_custom)

    val mon = stringResource(R.string.arc_detail_day_mon)
    val tue = stringResource(R.string.arc_detail_day_tue)
    val wed = stringResource(R.string.arc_detail_day_wed)
    val thu = stringResource(R.string.arc_detail_day_thu)
    val fri = stringResource(R.string.arc_detail_day_fri)
    val sat = stringResource(R.string.arc_detail_day_sat)
    val sun = stringResource(R.string.arc_detail_day_sun)

    val label = when (recurrenceType) {
        "daily" -> daily
        "weekdays" -> weekdays
        "weekly" -> weekly
        "custom" -> {
            val days = recurrenceDaysCsv
                .split(",")
                .mapNotNull { it.toIntOrNull() }
                .sorted()
                .mapNotNull {
                    when (it) {
                        1 -> mon
                        2 -> tue
                        3 -> wed
                        4 -> thu
                        5 -> fri
                        6 -> sat
                        7 -> sun
                        else -> null
                    }
                }
            if (days.isEmpty()) custom else days.joinToString(" • ")
        }
        else -> oneTime
    }

    val a11yText = stringResource(R.string.arc_detail_repeat_a11y, repeatTitle, label)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11yText
            },
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
                text = repeatTitle,
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
    val arcType = stringResource(R.string.arc_detail_type_arc)
    val studioArcType = stringResource(R.string.arc_detail_type_studio_arc)
    val editText = stringResource(R.string.common_edit)
    val addToStudioText = stringResource(R.string.arc_detail_add_to_studio)
    val removeFromStudioText = stringResource(R.string.arc_detail_remove_from_studio)
    val launchText = if (uiState.launchCount > 0) {
        pluralStringResource(R.plurals.arc_detail_launch_count, uiState.launchCount, uiState.launchCount)
    } else {
        stringResource(R.string.arc_detail_not_launched_yet)
    }

    val typeText = if (uiState.isInStudio) studioArcType else arcType
    val a11yText = stringResource(R.string.arc_detail_header_a11y, typeText, uiState.title, launchText)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11yText
            },
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
                        text = typeText,
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
                text = launchText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )

            HorizontalDivider()

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(
                    onClick = onEditArc,
                    modifier = Modifier.semantics {
                        contentDescription = editText
                    }
                ) {
                    Text(editText)
                }

                if (uiState.isInStudio) {
                    TextButton(
                        onClick = onRemoveFromStudio,
                        modifier = Modifier.semantics {
                            contentDescription = removeFromStudioText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = null
                        )
                        Text(removeFromStudioText)
                    }
                } else {
                    TextButton(
                        onClick = onAddToStudio,
                        modifier = Modifier.semantics {
                            contentDescription = addToStudioText
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.StarBorder,
                            contentDescription = null
                        )
                        Text(addToStudioText)
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
    val stepCountText = pluralStringResource(
        R.plurals.arc_detail_step_count,
        uiState.steps.size,
        uiState.steps.size
    )
    val minutesText = if (uiState.untimedCount > 0) {
        stringResource(R.string.arc_detail_minutes_plus, uiState.totalMinutes)
    } else {
        stringResource(R.string.arc_detail_minutes, uiState.totalMinutes)
    }

    val summaryText = listOf(stepCountText, minutesText).joinToString(" • ")

    val detailText = buildList {
        if (uiState.untimedCount > 0) add(stringResource(R.string.arc_detail_untimed_count, uiState.untimedCount))
        if (uiState.surgeCount > 0) add(stringResource(R.string.arc_detail_surge_count, uiState.surgeCount))
        if (uiState.softCount > 0) add(stringResource(R.string.arc_detail_soft_count, uiState.softCount))
    }.joinToString(" • ")

    val a11yText = stringResource(
        R.string.arc_detail_summary_a11y,
        summaryText,
        if (detailText.isBlank()) summaryText else detailText
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11yText
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = summaryText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (detailText.isNotBlank()) {
                Text(
                    text = detailText,
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
    val currentText = stringResource(R.string.arc_detail_current)
    val surgeText = stringResource(R.string.arc_detail_surge)
    val softText = stringResource(R.string.arc_detail_soft)
    val stepNumberText = stringResource(R.string.arc_detail_step_number_a11y, step.orderIndex + 1)

    val badgeSummary = buildList {
        if (isActiveStep) add(currentText)
        step.targetMinutes?.let { add(stringResource(R.string.arc_detail_minutes, it)) }
        if (step.launchWithSurge) add(surgeText)
        if (step.isSoftMode) add(softText)
    }.joinToString(" • ")

    val a11yText = if (step.tagName.isNotBlank()) {
        stringResource(
            R.string.arc_detail_step_card_with_tag_a11y,
            stepNumberText,
            step.title,
            step.tagName,
            badgeSummary.ifBlank { stepNumberText }
        )
    } else {
        stringResource(
            R.string.arc_detail_step_card_a11y,
            stepNumberText,
            step.title,
            badgeSummary.ifBlank { stepNumberText }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = a11yText
            },
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
                    MiniBadge(text = currentText)
                }
                step.targetMinutes?.let {
                    MiniBadge(text = stringResource(R.string.arc_detail_minutes, it))
                }
                if (step.launchWithSurge) {
                    MiniBadge(
                        text = surgeText,
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
                    MiniBadge(text = softText)
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