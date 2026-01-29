package com.kingkharnivore.skillz.ui.atlas.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HorizonControlsRow(
    title: String,
    selectedHours: Int,
    onZoomHours: (Int) -> Unit,
    onEarlier: () -> Unit,
    onNow: () -> Unit,
    onLater: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(onClick = onEarlier, label = { Text("Earlier") })
            AssistChip(onClick = onNow, label = { Text("Now") })
            AssistChip(onClick = onLater, label = { Text("Later") })
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(2, 4, 6, 8, 12).forEach { h ->
                FilterChip(
                    selected = h == selectedHours,
                    onClick = { onZoomHours(h) },
                    label = { Text("${h}h") }
                )
            }
        }
    }
}
