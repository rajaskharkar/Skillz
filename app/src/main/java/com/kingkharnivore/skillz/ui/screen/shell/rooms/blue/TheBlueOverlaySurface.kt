package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TheBlueOverlaySurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = scheme.surface.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.14f)),
        modifier = modifier,
        content = { Box(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) { content() } }
    )
}