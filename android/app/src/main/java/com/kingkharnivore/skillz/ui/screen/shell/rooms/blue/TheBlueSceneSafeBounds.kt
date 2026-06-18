package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.ui.geometry.Offset

data class TheBlueSceneSafeBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = (right - left).coerceAtLeast(1f)
    val height: Float get() = (bottom - top).coerceAtLeast(1f)

    fun clampCenter(center: Offset, halfWidth: Float, halfHeight: Float): Offset {
        val minX = left + halfWidth
        val maxX = right - halfWidth
        val minY = top + halfHeight
        val maxY = bottom - halfHeight
        return Offset(
            x = if (minX <= maxX) center.x.coerceIn(minX, maxX) else (left + right) / 2f,
            y = if (minY <= maxY) center.y.coerceIn(minY, maxY) else (top + bottom) / 2f
        )
    }
}
