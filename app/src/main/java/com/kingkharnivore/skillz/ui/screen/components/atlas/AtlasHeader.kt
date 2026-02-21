package com.kingkharnivore.skillz.ui.screen.components.atlas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.atlas.model.AtlasViewMode
import com.kingkharnivore.skillz.utils.time.floorToDay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DAY_FMT = DateTimeFormatter.ofPattern("EEE, MMM d")

@Composable
fun AtlasHeader(
    mode: AtlasViewMode,
    dayStartMs: Long,
    beamsCountLabel: String,
    canGoPrev: Boolean,
    onSelectMode: (AtlasViewMode) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val zone = ZoneId.systemDefault()
    val dayLabel =
        if (dayStartMs > 0L)
            Instant.ofEpochMilli(dayStartMs).atZone(zone).format(DAY_FMT)
        else
            "Atlas"
    // ✅ Determine if we're already on today's dayStart
    val nowDayStart = floorToDay(System.currentTimeMillis())
    val isOnToday = dayStartMs == nowDayStart

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp) // ✅ tighter
    ) {
        // Mode selector
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == AtlasViewMode.DAY,
                onClick = { onSelectMode(AtlasViewMode.DAY) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
            ) { Text("Day") }

            SegmentedButton(
                selected = mode == AtlasViewMode.WEEK,
                onClick = { onSelectMode(AtlasViewMode.WEEK) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
            ) { Text("Week") }

            SegmentedButton(
                selected = mode == AtlasViewMode.MONTH,
                onClick = { onSelectMode(AtlasViewMode.MONTH) },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
            ) { Text("Month") }
        }

        when (mode) {
            AtlasViewMode.DAY -> {
                // Row 1: Prev / Center / Next
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Prev day
                    TextButton(
                        onClick = onPrev,
                        enabled = canGoPrev,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (canGoPrev) cs.onSurface else cs.onSurface.copy(alpha = 0.35f)
                        )
                    ) {
                        Text("‹", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Prev day",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (canGoPrev) cs.onSurface.copy(alpha = 0.85f)
                            else cs.onSurface.copy(alpha = 0.35f)
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Center: Day + Date + beams count (chip is informational)
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.titleMedium
                        )

                        AssistChip(
                            onClick = {},
                            enabled = false, // ✅ no ripple / no click
                            label = {
                                Text(
                                    text = beamsCountLabel,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = cs.surfaceVariant,
                                labelColor = cs.onSurfaceVariant,
                                disabledContainerColor = cs.surfaceVariant,
                                disabledLabelColor = cs.onSurfaceVariant
                            ),
                            border = BorderStroke(1.dp, cs.onSurface.copy(alpha = 0.08f))
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    // Right: Next day
                    TextButton(
                        onClick = onNext,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = cs.onSurface)
                    ) {
                        Text(
                            text = "Next day",
                            style = MaterialTheme.typography.labelLarge,
                            color = cs.onSurface.copy(alpha = 0.85f)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("›", style = MaterialTheme.typography.titleMedium)
                    }
                }

                // Row 2: "Back to Today" (only when NOT on today) — tighter spacing + pill look
                if (!isOnToday) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp), // ✅ tight
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onToday,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            border = BorderStroke(1.dp, cs.primary.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = cs.primary
                            )
                        ) {
                            Text(
                                text = "Back to Today",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            AtlasViewMode.WEEK, AtlasViewMode.MONTH -> {
                // Simple until you implement week/month navigation semantics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (mode == AtlasViewMode.WEEK) "Week" else "Month",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.weight(1f))

                    TextButton(onClick = onToday) { Text("Today") }
                }
            }
        }
    }
}