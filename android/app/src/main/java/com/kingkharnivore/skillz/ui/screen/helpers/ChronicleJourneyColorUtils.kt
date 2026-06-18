package com.kingkharnivore.skillz.ui.screen.helpers

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private val ATLAS_JOURNEY_PALETTE: List<Color> = listOf(
    Color(0xFF8B1E1E),
    Color(0xFF3A5F8C),
    Color(0xFF2F8F86),
    Color(0xFF6F9E91),
    Color(0xFFD1B45A),
    Color(0xFFCC8A3E),
    Color(0xFF7A4A32),
    Color(0xFF8C6AA8),
    Color(0xFF3E8F6B)
)

fun assignJourneyColors(tagIdsInPriorityOrder: List<Long>): Map<Long, Color> {
    val palette = buildExpandedPalette()
    return tagIdsInPriorityOrder.mapIndexed { index, tagId ->
        tagId to palette[index % palette.size]
    }.toMap()
}

private fun buildExpandedPalette(): List<Color> {
    val base = ATLAS_JOURNEY_PALETTE
    val lighter = base.map { lerp(it, Color.White, 0.18f) }
    val darker = base.map { lerp(it, Color.Black, 0.12f) }

    return buildList {
        addAll(base)
        addAll(lighter)
        addAll(darker)
    }
}