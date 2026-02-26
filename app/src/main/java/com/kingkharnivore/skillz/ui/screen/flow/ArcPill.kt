package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ArcPill(
    arcMultiplier: Double,
    arcNextIndex: Int?
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83C\uDFD4\uFE0F")
            Spacer(Modifier.width(8.dp))
            Text("Arc Active", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            Text(
                "×${"%.1f".format(arcMultiplier)}",
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            arcNextIndex?.let {
                Spacer(Modifier.width(10.dp))
                Text("Now: Flow $it", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}