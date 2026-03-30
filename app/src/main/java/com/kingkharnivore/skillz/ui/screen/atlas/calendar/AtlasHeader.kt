package com.kingkharnivore.skillz.ui.screen.atlas.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.model.AtlasViewMode

@Composable
fun AtlasHeader(
    mode: AtlasViewMode,
    titleText: String,
    subtitleText: String,
    canGoPrev: Boolean,
    showTodayButton: Boolean,
    onSelectMode: (AtlasViewMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val prevLabel = when (mode) {
        AtlasViewMode.DAY -> "Prev day"
        AtlasViewMode.WEEK -> "Prev week"
        AtlasViewMode.MONTH -> "Prev month"
    }

    val nextLabel = when (mode) {
        AtlasViewMode.DAY -> "Next day"
        AtlasViewMode.WEEK -> "Next week"
        AtlasViewMode.MONTH -> "Next month"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AtlasModeSwitcher(
            mode = mode,
            onSelectMode = onSelectMode
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurface
            )

            AssistChip(
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = subtitleText,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    disabledContainerColor = cs.surfaceVariant,
                    disabledLabelColor = cs.onSurfaceVariant
                ),
                border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f))
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onPrev,
                enabled = canGoPrev,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (canGoPrev) cs.onSurface else cs.onSurface.copy(alpha = 0.35f)
                )
            ) {
                Text(
                    text = "‹ $prevLabel",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (showTodayButton) {
                OutlinedButton(
                    onClick = onToday,
                    modifier = Modifier.heightIn(min = 40.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.45f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = cs.primary
                    )
                ) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1
                    )
                }
            } else {
                Spacer(Modifier.width(72.dp))
            }

            TextButton(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = cs.onSurface)
            ) {
                Text(
                    text = "$nextLabel ›",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun AtlasModeSwitcher(
    mode: AtlasViewMode,
    onSelectMode: (AtlasViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cs.surfaceVariant.copy(alpha = 0.55f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AtlasModeTab(
                label = "Day",
                selected = mode == AtlasViewMode.DAY,
                onClick = { onSelectMode(AtlasViewMode.DAY) },
                modifier = Modifier.weight(1f)
            )
            AtlasModeTab(
                label = "Week",
                selected = mode == AtlasViewMode.WEEK,
                onClick = { onSelectMode(AtlasViewMode.WEEK) },
                modifier = Modifier.weight(1f)
            )
            AtlasModeTab(
                label = "Month",
                selected = mode == AtlasViewMode.MONTH,
                onClick = { onSelectMode(AtlasViewMode.MONTH) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun AtlasModeTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    val containerColor by animateColorAsState(
        targetValue = if (selected) cs.primary else Color.Transparent,
        label = "atlas_tab_container"
    )

    val contentColor by animateColorAsState(
        targetValue = if (selected) cs.onPrimary else cs.onSurfaceVariant,
        label = "atlas_tab_content"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = contentColor
        )
    }
}