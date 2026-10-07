package com.kingkharnivore.skillz.ui.screen.chronicle

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val LocalChronicleReaderFactory = staticCompositionLocalOf<((String, String) -> ChronicleReadState)?> { null }

/** Subscribes only while the card is expanded; reads every moment, including media. */
@Composable
fun ExpandedChronicle(ownerType: String, ownerId: Long, fallback: String) {
    val factory = LocalChronicleReaderFactory.current
    val holder = remember(ownerType, ownerId, factory) { factory?.invoke(ownerType, ownerId.toString()) }
    DisposableEffect(holder) { onDispose { holder?.close() } }
    val moments = holder?.moments?.collectAsState()?.value.orEmpty()
    if (moments.isNotEmpty()) Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp)
    ) {
        ChronicleReader(moments, Modifier.padding(16.dp))
    } else if (fallback.isNotBlank()) Text(fallback, style = MaterialTheme.typography.bodyMedium)
}
