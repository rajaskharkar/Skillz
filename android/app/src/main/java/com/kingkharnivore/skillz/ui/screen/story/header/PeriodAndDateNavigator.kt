package com.kingkharnivore.skillz.ui.screen.story.header

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.helpers.formatPeriodSubtitle
import com.kingkharnivore.skillz.ui.screen.helpers.formatPeriodTitle
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils

@Composable
fun PeriodAndDateNavigator(
    period: StoryPeriod,
    anchorDayStartMs: Long,
    firstSessionStartMs: Long?,
    onPeriodSelected: (StoryPeriod) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val nowMs = System.currentTimeMillis()

    val previousLabel = stringResource(R.string.period_navigator_previous)
    val nextLabel = stringResource(R.string.period_navigator_next)
    val filtersA11yLabel = stringResource(R.string.period_navigator_period_filters)
    val dateNavA11yLabel = stringResource(R.string.period_navigator_date_navigation)
    val selectedLabel = stringResource(R.string.period_navigator_selected)
    val notSelectedLabel = stringResource(R.string.period_navigator_not_selected)
    val jumpToCurrentPeriodLabel = stringResource(R.string.period_navigator_jump_to_current_period)

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

    val periodTitle = formatPeriodTitle(period, normalizedAnchor)
    val periodSubtitle = formatPeriodSubtitle(period, normalizedAnchor)
    val currentPeriodState = stringResource(
        R.string.period_navigator_current_period,
        periodTitle,
        periodSubtitle
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .semantics {
                    contentDescription = filtersA11yLabel
                }
        ) {
            listOf(StoryPeriod.DAY, StoryPeriod.WEEK, StoryPeriod.MONTH).forEach { p ->
                val chipState = stringResource(
                    R.string.period_navigator_filter_state,
                    p.label,
                    if (p == period) selectedLabel else notSelectedLabel
                )

                FilterChip(
                    selected = p == period,
                    onClick = { onPeriodSelected(p) },
                    label = {
                        Text(
                            text = p.label,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f),
                        selectedLabelColor = MaterialTheme.colorScheme.secondary,
                        containerColor = MaterialTheme.colorScheme.surface,
                        labelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    ),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .semantics {
                            contentDescription = p.label
                            stateDescription = chipState
                        }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = dateNavA11yLabel
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPrev,
                enabled = canGoPrev
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = previousLabel,
                    tint = if (canGoPrev) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        heading()
                        contentDescription = currentPeriodState
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = periodTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = periodSubtitle,
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
                    contentDescription = nextLabel,
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
                val label = when (period) {
                    StoryPeriod.DAY -> stringResource(R.string.period_navigator_back_to_today)
                    StoryPeriod.WEEK -> stringResource(R.string.period_navigator_back_to_this_week)
                    StoryPeriod.MONTH -> stringResource(R.string.period_navigator_back_to_this_month)
                }

                Card(
                    onClick = onToday,
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = jumpToCurrentPeriodLabel
                        stateDescription = label
                    },
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