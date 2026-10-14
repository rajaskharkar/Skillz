package com.kingkharnivore.skillz.ui.screen.shell.rooms.red

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.IntSize
import com.kingkharnivore.skillz.utils.shell.RedEra
import kotlin.math.roundToInt

/** One immutable background texture per region, scoped to the screen and viewport size.
 * A 1080 × 2400 ceiling bounds each cache below 10 MiB even on large displays. */
@Composable
internal fun RedTerrain(era: RedEra, modifier: Modifier) {
    Box(modifier.drawWithCache {
        val scale = minOf(1f,1080f / size.width.coerceAtLeast(1f),2400f / size.height.coerceAtLeast(1f))
        val width = (size.width * scale).roundToInt().coerceAtLeast(1)
        val height = (size.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = ImageBitmap(width,height)
        CanvasDrawScope().draw(this,layoutDirection,Canvas(bitmap),Size(width.toFloat(),height.toFloat())) {
            drawRedEarth(era,0f)
        }
        bitmap.prepareToDraw()
        onDrawBehind {
            drawImage(bitmap,dstSize=IntSize(size.width.roundToInt(),size.height.roundToInt()),filterQuality=FilterQuality.Low)
        }
    })
}

/** Cache only the small translucent mist strip, never a full-screen transparent layer. */
@Composable
internal fun RedAtmosphere(time: () -> Float, modifier: Modifier) {
    Box(modifier.drawWithCache {
        val width = (size.width * 1.25f).roundToInt().coerceAtLeast(1)
        val height = (size.height * .07f).roundToInt().coerceAtLeast(1)
        val mist = ImageBitmap(width,height)
        CanvasDrawScope().draw(this,layoutDirection,Canvas(mist),Size(width.toFloat(),height.toFloat())) {
            drawOval(androidx.compose.ui.graphics.Color.White.copy(alpha=.12f))
        }
        mist.prepareToDraw()
        onDrawBehind {
            val drift = kotlin.math.sin(time() * .03f) * size.width * .04f
            drawImage(mist,androidx.compose.ui.geometry.Offset(-size.width*.15f+drift,size.height*.32f))
        }
    })
}
