package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RitualCard(
    rotation: Float,
    corner: Dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val stroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = rotation },
        shape = RoundedCornerShape(corner),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, stroke, RoundedCornerShape(corner))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}