package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun RitualFrame(
    rotation: Float,
    corner: Dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    showBorder: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val stroke = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { rotationZ = rotation },
        shape = RoundedCornerShape(corner),
        tonalElevation = 0.dp,      // ✅ no “card” elevation feel
        shadowElevation = 0.dp,
        color = Color.Transparent   // ✅ transparent background
    ) {
        Column(
            modifier = Modifier
                .then(
                    if (showBorder) Modifier.border(1.dp, stroke, RoundedCornerShape(corner))
                    else Modifier
                )
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content
        )
    }
}