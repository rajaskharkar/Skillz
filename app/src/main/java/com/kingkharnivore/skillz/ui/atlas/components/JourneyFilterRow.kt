package com.kingkharnivore.skillz.ui.atlas.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.atlas.model.JourneyChipUi
import com.kingkharnivore.skillz.ui.atlas.model.JourneyFilter

@Composable
fun JourneyFilterRow(
    journeys: List<JourneyChipUi>,
    selected: JourneyFilter,
    onSelectAll: () -> Unit,
    onSelect: (Long) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 6.dp)
    ) {
        FilterChip(
            selected = selected is JourneyFilter.All,
            onClick = onSelectAll,
            label = { Text("All", style = MaterialTheme.typography.labelLarge) }
        )
        journeys.forEach { j ->
            val isSel = selected is JourneyFilter.Only && selected.tagId == j.tagId
            FilterChip(
                selected = isSel,
                onClick = { onSelect(j.tagId) },
                label = { Text(j.name, maxLines = 1) }
            )
        }
    }
}
