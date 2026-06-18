package com.kingkharnivore.skillz.ui.screen.shell.icons

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

@Composable
fun ShellPearlMiniIcon(
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        drawCircle(
            color = scheme.onPrimary,
            radius = size.minDimension * 0.36f,
            center = Offset(size.width / 2f, size.height / 2f)
        )

        drawCircle(
            color = scheme.primary.copy(alpha = 0.42f),
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.60f, size.height * 0.38f)
        )
    }
}