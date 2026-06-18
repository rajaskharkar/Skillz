package com.kingkharnivore.skillz.ui.screen.shell.icons.draw

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun TurtleShellInteriorBackground(
    modifier: Modifier = Modifier,
    centerGlow: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        drawOval(
            color = scheme.primary.copy(alpha = 0.24f),
            topLeft = Offset(-w * 0.12f, h * 0.02f),
            size = Size(w * 1.24f, h * 1.10f)
        )

        drawOval(
            color = scheme.surface.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.04f, h * 0.07f),
            size = Size(w * 0.92f, h * 0.86f)
        )

        drawOval(
            color = scheme.secondary.copy(alpha = 0.16f),
            topLeft = Offset(w * 0.13f, h * 0.12f),
            size = Size(w * 0.74f, h * 0.68f)
        )

        if (centerGlow) {
            drawCircle(
                color = scheme.primary.copy(alpha = 0.18f),
                radius = w * 0.25f,
                center = Offset(w * 0.50f, h * 0.35f)
            )
        }

        val spine = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            cubicTo(
                w * 0.46f,
                h * 0.28f,
                w * 0.54f,
                h * 0.48f,
                w * 0.50f,
                h * 0.80f
            )
        }

        drawPath(
            path = spine,
            color = scheme.secondary.copy(alpha = 0.22f),
            style = Stroke(width = 4.5f)
        )

        val bandYs = listOf(0.20f, 0.34f, 0.49f, 0.64f, 0.78f)
        bandYs.forEachIndexed { index, yFraction ->
            val y = h * yFraction
            val leftInset = w * (0.13f + index * 0.018f)
            val rightInset = w - leftInset

            val band = Path().apply {
                moveTo(leftInset, y)
                cubicTo(
                    w * 0.30f,
                    y - h * 0.060f,
                    w * 0.42f,
                    y + h * 0.035f,
                    w * 0.50f,
                    y
                )
                cubicTo(
                    w * 0.58f,
                    y + h * 0.035f,
                    w * 0.70f,
                    y - h * 0.060f,
                    rightInset,
                    y
                )
            }

            drawPath(
                path = band,
                color = scheme.secondary.copy(alpha = 0.12f),
                style = Stroke(width = 3f)
            )
        }

        drawOval(
            color = Color.Black.copy(alpha = 0.12f),
            topLeft = Offset(-w * 0.08f, h * 0.02f),
            size = Size(w * 1.16f, h * 1.04f),
            style = Stroke(width = w * 0.08f)
        )
    }
}