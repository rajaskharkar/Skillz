package com.kingkharnivore.skillz.utils.shell

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush

@Composable
fun shellBackground(): Brush {
    val scheme = MaterialTheme.colorScheme

    return Brush.verticalGradient(
        colors = listOf(
            scheme.primary,
            scheme.background
        )
    )
}