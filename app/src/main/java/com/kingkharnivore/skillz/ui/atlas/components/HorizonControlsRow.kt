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
    selectedAnchor: HorizonAnchorUi,
    onZoomHours: (Int) -> Unit,
    onEarlier: () -> Unit,
    onNow: () -> Unit,
    onLater: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val selectedContainer = cs.primary
    val selectedLabel = cs.onPrimary

    val unselectedContainer = cs.surfaceVariant
    val unselectedLabel = cs.onSurfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurface.copy(alpha = 0.8f)
        )

        // Anchor chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HorizonChip(
                text = "Earlier",
                selected = selectedAnchor == HorizonAnchorUi.EARLIER,
                onClick = onEarlier,
                selectedContainer = selectedContainer,
                selectedLabel = selectedLabel,
                unselectedContainer = unselectedContainer,
                unselectedLabel = unselectedLabel
            )
            HorizonChip(
                text = "Now",
                selected = selectedAnchor == HorizonAnchorUi.NOW,
                onClick = onNow,
                selectedContainer = selectedContainer,
                selectedLabel = selectedLabel,
                unselectedContainer = unselectedContainer,
                unselectedLabel = unselectedLabel
            )
            HorizonChip(
                text = "Later",
                selected = selectedAnchor == HorizonAnchorUi.LATER,
                onClick = onLater,
                selectedContainer = selectedContainer,
                selectedLabel = selectedLabel,
                unselectedContainer = unselectedContainer,
                unselectedLabel = unselectedLabel
            )
        }

        // Zoom chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4, 6, 8, 12).forEach { h ->
                HorizonChip(
                    text = "${h}h",
                    selected = (h == selectedHours),
                    onClick = { onZoomHours(h) },
                    selectedContainer = selectedContainer,
                    selectedLabel = selectedLabel,
                    unselectedContainer = unselectedContainer,
                    unselectedLabel = unselectedLabel,
                    modifier = Modifier.padding(end = 0.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HorizonChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    selectedContainer: androidx.compose.ui.graphics.Color,
    selectedLabel: androidx.compose.ui.graphics.Color,
    unselectedContainer: androidx.compose.ui.graphics.Color,
    unselectedLabel: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = text) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = unselectedContainer,
            labelColor = unselectedLabel,
            selectedContainerColor = selectedContainer,
            selectedLabelColor = selectedLabel
        ),
        border = null, // ✅ lighter/cleaner
        modifier = modifier
    )
}
