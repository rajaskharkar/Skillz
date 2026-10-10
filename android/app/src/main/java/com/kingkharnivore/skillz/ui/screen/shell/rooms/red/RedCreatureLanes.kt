package com.kingkharnivore.skillz.ui.screen.shell.rooms.red

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land.landAnimalPose
import com.kingkharnivore.skillz.utils.shell.RedSceneCreature
import kotlin.math.ceil
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.roundToInt
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.land.LandAnimalPose

/** Only the capped visible population owns artwork caches, at its actual on-screen size.
 * Static anatomy is rasterized on layout/ownership changes, never on clock ticks. The two
 * motion layers retain native Land poses, limb articulation and independent tail sway;
 * subpixel anatomy changes reuse the last raster while world movement remains continuous.
 * The caches belong to the page's draw node and are released when that page is disposed. */
@Composable
internal fun RedCreatureLanes(
    population: List<RedSceneCreature>,
    highestLevels: Map<String, Int>,
    time: () -> Float,
    modifier: Modifier
) {
    Box(modifier.drawWithCache {
        val columns = if (population.size > 6) 3 else 2
        val predators = population.filter { it.predator }
        val herbivores = population.filterNot { it.predator }
        val predatorRows = ceil(predators.size.toFloat() / columns).toInt()
        val rows = (predatorRows + ceil(herbivores.size.toFloat() / columns).toInt()).coerceAtLeast(2)
        val laneWidth = size.width / columns
        val sceneryGap = size.height * .40f
        val laneHeight = (size.height - sceneryGap) / rows
        val actors = (predators + herbivores).mapIndexed { index, animal ->
            val isPredator = index < predators.size
            val local = if (isPredator) index else index - predators.size
            val row = local / columns + if (isPredator) 0 else predatorRows
            val visual = RedDinosaurVisuals.get(animal.speciesId)
            val growth = 1f + ((highestLevels[animal.speciesId] ?: 1) - 1) / 98f * .12f
            val edge = minOf(visual.edgeDp.dp.toPx() * growth, laneWidth * .85f, laneHeight * .9f).coerceAtLeast(1f)
            val pixels = ceil(edge).toInt()
            fun raster(layer: DinosaurLayer): ImageBitmap {
                val bitmap = ImageBitmap(pixels,pixels)
                CanvasDrawScope().draw(this,layoutDirection,Canvas(bitmap),Size(pixels.toFloat(),pixels.toFloat())) {
                    drawDinosaurLayer(animal.speciesId,0f,0f,0f,layer)
                }
                bitmap.prepareToDraw()
                return bitmap
            }
            CachedDinosaur(animal.speciesId,visual.edgeDp,edge,
                local % columns * laneWidth,sceneryGap + row * laneHeight + laneHeight - edge,
                raster(DinosaurLayer.BODY),raster(DinosaurLayer.DETAILS),ImageBitmap(pixels,pixels))
        }
        onDrawBehind {
            clipRect {
                val seconds = time()
                actors.forEach { actor ->
                    val pose = landAnimalPose(seconds,actor.id.hashCode() and 1023,actor.edgeDp)
                    val left = actor.laneLeft + (laneWidth - actor.edge) * pose.progress
                    drawOval(Color(0xFF343D30).copy(alpha=.15f),Offset(left+actor.edge*.15f,actor.top+actor.edge*.86f),Size(actor.edge*.72f,actor.edge*.04f))
                    // Rasterize the small moving parts into a reused, on-screen-size buffer.
                    // Upload one image instead of retessellating hundreds of paths on the GPU.
                    // No bitmap is allocated on a clock tick; geometry and motion are unchanged.
                    if (actor.motionChanged(seconds,pose)) {
                        actor.frame.asAndroidBitmap().eraseColor(android.graphics.Color.TRANSPARENT)
                        actor.drawScope.draw(this,layoutDirection,actor.canvas,actor.frameSize) {
                            drawDinosaurLayer(actor.id,seconds,pose.walkPhase,pose.activity,DinosaurLayer.BACK_MOTION)
                            drawImage(actor.body)
                            drawDinosaurLayer(actor.id,seconds,pose.walkPhase,pose.activity,DinosaurLayer.FRONT_MOTION)
                            drawImage(actor.details)
                        }
                    }
                    withTransform({
                        translate(left,actor.top)
                        scale(pose.facing,1f,Offset(actor.edge*.5f,actor.edge*.5f))
                    }) {
                        drawImage(actor.frame,dstSize=IntSize(ceil(actor.edge).toInt(),ceil(actor.edge).toInt()))
                    }
                }
            }
        }
    })
}

private data class CachedDinosaur(
    val id: String, val edgeDp: Float, val edge: Float, val laneLeft: Float, val top: Float,
    val body: ImageBitmap, val details: ImageBitmap, val frame: ImageBitmap
) {
    val canvas = Canvas(frame)
    val drawScope = CanvasDrawScope()
    val frameSize = Size(frame.width.toFloat(),frame.height.toFloat())
    private var lastSwing = Int.MIN_VALUE
    private var lastFrontLift = Int.MIN_VALUE
    private var lastBackLift = Int.MIN_VALUE
    private var lastTail = Int.MIN_VALUE

    // Reuse the frame until anatomy moves half a physical pixel. Position and turning
    // remain continuous on every frame; a resting dinosaur does not repaint fixed legs.
    fun motionChanged(seconds: Float, pose: LandAnimalPose): Boolean {
        val scale = edge / 100f * 2f
        val swing = (sin(pose.walkPhase) * 6f * pose.activity * scale).roundToInt()
        val frontLift = (cos(pose.walkPhase).coerceAtLeast(0f) * 3f * pose.activity * scale).roundToInt()
        val backLift = ((-cos(pose.walkPhase)).coerceAtLeast(0f) * 3f * pose.activity * scale).roundToInt()
        val tail = (sin(seconds * .9f) * 2f * scale).roundToInt()
        if (swing == lastSwing && frontLift == lastFrontLift && backLift == lastBackLift && tail == lastTail) return false
        lastSwing = swing; lastFrontLift = frontLift; lastBackLift = backLift; lastTail = tail
        return true
    }
}
