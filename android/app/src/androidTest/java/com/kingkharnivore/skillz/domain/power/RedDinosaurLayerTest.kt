package com.kingkharnivore.skillz.domain.power

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.kingkharnivore.skillz.ui.screen.shell.rooms.red.*
import com.kingkharnivore.skillz.utils.shell.RedCreatureCatalog
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class RedDinosaurLayerTest {
    private fun render(block: DrawScope.() -> Unit): ImageBitmap = ImageBitmap(128,128).also {
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(it),Size(128f,128f),block)
    }

    @Test fun cachedAnatomyPreservesEverySpeciesAcrossWalkingAndRestingPoses() {
        RedCreatureCatalog.entries.forEach { species ->
            val body=render { drawDinosaurLayer(species.id,0f,0f,0f,DinosaurLayer.BODY) }
            val details=render { drawDinosaurLayer(species.id,0f,0f,0f,DinosaurLayer.DETAILS) }
            listOf(0f to 0f, 1.2f to .7f, 4f to 1f).forEach { (phase,activity) ->
                val expected=render { drawRect(Color.White); drawRedDinosaur(species.id,12f,phase,activity) }.toPixelMap()
                val actual=render {
                    drawRect(Color.White)
                    drawDinosaurLayer(species.id,12f,phase,activity,DinosaurLayer.BACK_MOTION)
                    drawImage(body)
                    drawDinosaurLayer(species.id,12f,phase,activity,DinosaurLayer.FRONT_MOTION)
                    drawImage(details)
                }.toPixelMap()
                var error=0f
                for(y in 0 until 128) for(x in 0 until 128) {
                    val a=actual[x,y]; val b=expected[x,y]
                    error+=abs(a.red-b.red)+abs(a.green-b.green)+abs(a.blue-b.blue)
                }
                // Cached alpha edges may round by a channel value; misplaced anatomy may not.
                assertTrue("${species.id} phase=$phase error=$error", error/(128*128*3) < .002f)
            }
        }
    }
}
