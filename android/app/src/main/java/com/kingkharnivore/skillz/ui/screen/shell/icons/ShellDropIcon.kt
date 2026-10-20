package com.kingkharnivore.skillz.ui.screen.shell.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/** A rounded water drop with a glint, matching the painted Pearl and Pebble icons. */
@Composable
fun ShellDropIcon(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val drop = Path().apply {
            moveTo(w * .5f, h * .06f)
            cubicTo(w * .43f, h * .25f, w * .15f, h * .46f, w * .15f, h * .63f)
            cubicTo(w * .15f, h * 1.04f, w * .85f, h * 1.04f, w * .85f, h * .63f)
            cubicTo(w * .85f, h * .46f, w * .57f, h * .25f, w * .5f, h * .06f)
            close()
        }
        drawPath(drop, Brush.linearGradient(listOf(Color(0xFF77CFE1), Color(0xFF287B9C))))
        drawCircle(Color.White.copy(alpha = .72f), w * .075f, Offset(w * .36f, h * .62f))
    }
}
