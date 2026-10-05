package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale

/** Geological reliefs share the existing medallion, progress ring and earned treatment. */
@Composable
internal fun RedBadgeArtwork(id: String, modifier: Modifier) {
    val red = MaterialTheme.colorScheme.tertiary
    val flow = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        scale(size.width / 100f, size.height / 100f, Offset.Zero) {
            val stroke = Stroke(4f, cap = StrokeCap.Round)
            fun line(x: Float, y: Float, xx: Float, yy: Float, color: Color = red) =
                drawLine(color, Offset(x,y), Offset(xx,yy), 4f, StrokeCap.Round)
            fun relief(vararg points: Float) = drawPath(Path().apply {
                moveTo(points[0], points[1])
                for (i in 2 until points.size step 2) lineTo(points[i], points[i+1])
            }, red, style = stroke)
            fun strata(count: Int = 3) { repeat(count) { i ->
                val y = 35f + i * 14f
                relief(12f,y,35f,y-5, 60f,y+3,88f,y-4)
            } }
            fun footprint(x: Float = 50f, y: Float = 58f, color: Color = red) {
                drawOval(color, Offset(x-9,y-3), Size(18f,24f))
                line(x,y,x-17,y-23,color); line(x,y,x,y-30,color); line(x,y,x+17,y-23,color)
            }
            fun fern(x: Float) { line(x,85f,x,47f); repeat(3) { i ->
                val y=56f+i*10; line(x,y,x-10,y-7); line(x,y,x+10,y-7)
            } }
            fun skull() {
                relief(20f,56f,30f,35f,61f,31f,84f,43f,86f,59f,67f,63f,60f,77f,32f,73f,20f,56f)
                drawCircle(red,7f,Offset(42f,49f)); drawCircle(red,4f,Offset(72f,48f))
                repeat(4) { i -> line(36f+i*9,66f,39f+i*9,75f) }
            }
            fun skeleton() {
                relief(10f,70f,32f,55f,59f,55f,73f,29f,89f,29f)
                repeat(4) { i -> val x=32f+i*8; relief(x,49f,x+2,59f,x,68f) }
                relief(33f,57f,29f,83f,20f,83f); relief(58f,58f,68f,82f,78f,82f)
            }
            when(id) {
                "red_first_footprint" -> {
                    relief(13f,73f,18f,27f,49f,12f,82f,26f,90f,74f,70f,89f,30f,87f,13f,73f)
                    footprint()
                }
                "power_unyielding" -> {
                    drawOval(red.copy(alpha=.18f),Offset(15f,10f),Size(70f,82f))
                    drawOval(red,Offset(15f,10f),Size(70f,82f),style=Stroke(3f))
                    drawOval(red.copy(alpha=.45f),Offset(22f,17f),Size(56f,68f),style=Stroke(2f))
                    footprint()
                }
                "red_dawn" -> { drawCircle(red.copy(alpha=.3f),24f,Offset(49f,35f)); skeleton(); line(14f,87f,88f,87f) }
                "red_ascendant", "red_the_red" -> {
                    skeleton()
                    if(id == "red_the_red") drawCircle(red,46f,Offset(50f,50f),style=Stroke(2f))
                }
                "red_giants" -> {
                    fern(19f); relief(48f,18f,48f,82f)
                    repeat(4) { i -> val y=26f+i*17; drawOval(red,Offset(35f,y-5),Size(29f,11f),style=stroke); line(63f,y,79f,y-5) }
                }
                "red_colossus" -> {
                    // A long-necked sauropod relief, distinct from Giants' single vertebra.
                    relief(9f,69f,32f,55f,62f,55f,71f,15f,87f,12f,92f,19f,79f,24f,75f,60f)
                    repeat(4) { i -> val x=33f+i*9; relief(x,51f,x+3,62f,x,70f) }
                    relief(34f,61f,31f,85f,20f,85f); relief(64f,61f,72f,85f,83f,85f)
                    line(71f,37f,80f,38f)
                }
                "red_titans" -> {
                    // Three massive limb-bone pillars for the three named titans.
                    repeat(3) { i ->
                        val x=23f+i*27f; val top=if(i==1) 13f else 27f
                        drawOval(red,Offset(x-9,top),Size(18f,12f),style=stroke)
                        line(x-4,top+12,x-4,77f); line(x+4,top+12,x+4,77f)
                        drawOval(red,Offset(x-10,76f),Size(20f,12f),style=stroke)
                    }
                }
                "red_dominion" -> { fern(15f); fern(86f); skull() }
                "red_last_age" -> { skeleton(); drawCircle(red,8f,Offset(70f,15f)); line(77f,9f,89f,0f); line(66f,8f,77f,0f) }
                "red_extinction" -> { drawOval(red,Offset(5f,8f),Size(90f,29f),style=stroke); skull() }
                "red_deep_time", "power_strata", "power_pressure" -> {
                    strata(if(id == "power_strata") 4 else 3)
                    if(id == "power_pressure") { relief(39f,8f,50f,20f,61f,8f); relief(39f,93f,50f,81f,61f,93f) }
                }
                "power_spark" -> {
                    relief(10f,74f,17f,30f,54f,12f,89f,40f,84f,83f,10f,74f)
                    drawPath(Path().apply { moveTo(49f,27f); cubicTo(73f,55f,65f,74f,49f,72f); cubicTo(28f,70f,30f,55f,49f,27f) }, red)
                }
                "power_resolve", "power_bedrock" -> {
                    relief(8f,80f,20f,30f,46f,17f,80f,27f,93f,82f,8f,80f)
                    relief(46f,18f,54f,41f, 40f,55f,57f,81f)
                    if(id == "power_resolve") { line(22f,10f,30f,23f); line(75f,7f,68f,20f); line(88f,49f,99f,49f) }
                }
                "power_adaptation" -> { footprint(26f, 60f); footprint(75f, 40f,flow) }
                "red_armored" -> { relief(12f,70f,20f,48f,30f,30f,42f,48f,53f,27f,65f,47f,77f,36f,89f,70f,12f,70f); line(22f,77f,80f,77f) }
                "red_horned" -> { skull(); relief(30f,38f,21f,14f,42f,35f); relief(58f,31f,69f,10f,68f,37f) }
                "red_raptor" -> {
                    // One hooked sickle claw with the smaller supporting toe bones.
                    relief(24f,20f,40f,16f,66f,31f,79f,56f,73f,80f,54f,90f,63f,65f,57f,49f,40f,39f,24f,20f)
                    line(20f,55f,40f,70f); line(16f,70f,36f,82f)
                }
                "red_clawed" -> repeat(3) { i -> val x=22f+i*24; relief(x,17f,x-6,57f,x+3,82f,x+12, 60f) }
                "red_crested" -> { skull(); relief(31f,35f,40f,9f,65f,15f,62f,31f) }
                "red_feathered" -> { relief(20f,86f, 70f,15f); repeat(5) { i -> val x=28f+i*8; val y=72f-i*11; relief(x-12,y-12,x,y,x+21,y+2) } }
                "red_kings" -> { skull(); relief(28f,25f,23f,8f,39f,18f, 50f,3f,61f,18f,78f,8f,73f,25f) }
                "red_apex" -> skull()
                else -> Unit
            }
        }
    }
}
