package com.kingkharnivore.skillz.ui.atlas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

enum class HorizonAnchorUi { EARLIER, NOW, LATER }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizonControlsRow(
    title: String,
    selectedHours: Int,
    selectedAnchor: HorizonAnchorUi,          // ✅ NEW
    onZoomHours: (Int) -> Unit,
    onEarlier: () -> Unit,
    onNow: () -> Unit,
    onLater: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface.copy(alpha = 0.8f)
        )

        // ✅ Replace AssistChip with FilterChip so selection can be highlighted
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedAnchor == HorizonAnchorUi.EARLIER,
                onClick = onEarlier,
                label = {
                    Text(
                        text = "Earlier"
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = cs.secondaryContainer,
                    selectedLabelColor = cs.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedAnchor == HorizonAnchorUi.EARLIER
                )
            )

            FilterChip(
                selected = selectedAnchor == HorizonAnchorUi.NOW,
                onClick = onNow,
                label = {
                    Text(
                        text = "Now"
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = cs.secondaryContainer,
                    selectedLabelColor = cs.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedAnchor == HorizonAnchorUi.NOW
                )
            )

            FilterChip(
                selected = selectedAnchor == HorizonAnchorUi.LATER,
                onClick = onLater,
                label = {
                    Text(
                        text = "Later"
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = cs.secondaryContainer,
                    selectedLabelColor = cs.onSecondaryContainer
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedAnchor == HorizonAnchorUi.LATER
                )
            )
        }

        // ✅ Zoom chips: make selected state punchier
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4, 6, 8, 12).forEach { h ->
                val selected = h == selectedHours

                FilterChip(
                    selected = selected,
                    onClick = { onZoomHours(h) },
                    label = { Text(text = "${h}h") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = cs.primaryContainer,
                        selectedLabelColor = cs.onPrimaryContainer
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected
                    ),
                    modifier = Modifier.padding(end = 0.dp)
                )
            }
        }
    }
}
