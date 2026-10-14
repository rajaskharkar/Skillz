package com.kingkharnivore.skillz.ui.screen.shell.rooms.red

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kingkharnivore.skillz.utils.shell.RedEra
import kotlin.math.cos
import kotlin.math.sin

/** Stylized regional landscapes, not a literal global map. Shared horizon joins the era pages.
 * Geological references are recorded in artifacts/power-red/ENVIRONMENT_REFERENCES.md. */
internal fun DrawScope.drawRedEarth(era: RedEra,time: Float) {
    val w=size.width;val h=size.height
    val sky=when(era) { RedEra.TRIASSIC->Color(0xFFE8D3B1);RedEra.JURASSIC->Color(0xFFB9CEC5);RedEra.CRETACEOUS->Color(0xFFCADDD1) }
    val ground=when(era) { RedEra.TRIASSIC->Color(0xFFB5815B);RedEra.JURASSIC->Color(0xFF8A9D72);RedEra.CRETACEOUS->Color(0xFF9CAF80) }
    fun shape(color: Color,block: Path.()->Unit)=drawPath(Path().apply(block),color)
    fun hill(color: Color,y: Float,rise: Float,end: Float) = shape(color) {
        moveTo(0f,h*y);cubicTo(w*.28f,h*(y-rise),w*.68f,h*(end+rise),w,h*end);lineTo(w,h);lineTo(0f,h);close()
    }
    fun river(color: Color,x: Float,y: Float,width: Float) = shape(color) {
        moveTo(w*x,h*y);cubicTo(w*(x-.18f),h*.60f,w*(x+.18f),h*.70f,w*(x-.15f),h)
        lineTo(w*(x-.15f+width),h);cubicTo(w*(x+.18f+width),h*.70f,w*(x-.18f+width),h*.60f,w*(x+.016f),h*y);close()
    }
    fun conifer(x: Float,y: Float,edge: Float,color: Color) {
        drawLine(Color(0xFF655B43),Offset(x,y),Offset(x,y-edge),w*.005f)
        repeat(4) { i -> val top=y-edge+i*edge*.16f;val spread=edge*(.16f+i*.025f)
            shape(color) { moveTo(x,top);quadraticTo(x+spread*.45f,top+edge*.2f,x+spread,top+edge*.43f);lineTo(x-spread,top+edge*.43f);quadraticTo(x-spread*.45f,top+edge*.2f,x,top);close() }
        }
    }
    fun cycad(x: Float,y: Float,edge: Float) {
        drawLine(Color(0xFF76664D),Offset(x,y),Offset(x,y-edge*.6f),edge*.1f)
        repeat(7) { i -> val a=(i*25f+15f)*.017453f;val dx=cos(a)*edge*.7f;val dy=-sin(a)*edge*.5f
            drawPath(Path().apply {moveTo(x,y-edge*.6f);quadraticTo(x+dx*.6f,y-edge*.9f+dy,x+dx,y-edge*.55f+dy)},Color(0xFF5F7854),style=Stroke(edge*.065f,cap=StrokeCap.Round))
        }
    }
    fun fern(x: Float,y: Float,edge: Float) {
        val sway=sin(time*.35f+x)*edge*.10f
        repeat(5) { i -> val dx=(i-2)*edge*.25f
            drawPath(Path().apply {moveTo(x,y);quadraticTo(x+dx*.4f+sway,y-edge*.8f,x+dx+sway,y-edge*(.65f-kotlin.math.abs(i-2)*.12f))},Color(0xFF586F4C).copy(alpha=.7f),style=Stroke(edge*.10f,cap=StrokeCap.Round))
        }
    }
    drawRect(Brush.verticalGradient(listOf(sky,ground)))
    drawCircle(Color(0xFFF6E5B4).copy(alpha=.5f),w*.15f,Offset(w*.78f,h*.23f))
    when(era) {
        RedEra.TRIASSIC -> {
            // Continental red beds: flat-topped mesas, exposed strata, a seasonal riverbed.
            hill(Color(0xFFC4A18A),.43f,.08f,.43f)
            shape(Color(0xFFAA775D)) {
                moveTo(0f,h*.56f);lineTo(w*.12f,h*.47f);lineTo(w*.18f,h*.32f);lineTo(w*.43f,h*.32f)
                lineTo(w*.49f,h*.47f);lineTo(w*.64f,h*.54f);lineTo(w*.76f,h*.36f);lineTo(w*.94f,h*.36f)
                lineTo(w,h*.50f);lineTo(w,h*.68f);lineTo(0f,h*.68f);close()
            }
            repeat(5) { i ->
                val y=h*(.37f+i*.033f)
                drawLine(Color(0xFFE2B18A).copy(alpha=.45f),Offset(w*.17f,y),Offset(w*.45f,y),h*.007f)
                drawLine(Color(0xFF815B46).copy(alpha=.3f),Offset(w*.77f,y+h*.03f),Offset(w*.96f,y+h*.03f),h*.004f)
            }
            hill(Color(0xFFC49B70),.50f,.02f,.50f)
            river(Color(0xFFE0C498),.58f,.56f,.14f)
            river(Color(0xFF9EA99A).copy(alpha=.55f),.58f,.56f,.035f)
            conifer(w*.06f,h*.57f,h*.13f,Color(0xFF64735B))
            cycad(w*.88f,h*.74f,h*.06f);cycad(w*.09f,h*.86f,h*.045f)
            repeat(10) { i -> val x=w*((i*31%97)/100f);val y=h*(.72f+(i*13%23)/100f)
                drawPath(Path().apply {moveTo(x,y);lineTo(x+w*.04f,y+h*.005f);lineTo(x+w*.055f,y-h*.009f)},Color(0xFF8E6F50).copy(alpha=.3f),style=Stroke(w*.002f))
            }
        }
        RedEra.JURASSIC -> {
            // Rift escarpments and a widening water corridor interrupt the old plateau.
            hill(Color(0xFF91AAA0),.43f,.08f,.43f)
            shape(Color(0xFF697F70)) {moveTo(0f,h*.54f);lineTo(w*.13f,h*.40f);lineTo(w*.37f,h*.36f);lineTo(w*.45f,h*.58f);lineTo(0f,h*.66f);close()}
            shape(Color(0xFF7D8F70)) {moveTo(w*.60f,h*.58f);lineTo(w*.72f,h*.39f);lineTo(w*.96f,h*.42f);lineTo(w,h*.51f);lineTo(w,h*.68f);close()}
            hill(Color(0xFF99A97D),.50f,.03f,.50f)
            river(Color(0xFF8BB3AE),.54f,.40f,.17f)
            repeat(10) { i ->
                val x=if(i<5) w*(.025f+i*.075f) else w*(.72f+(i-5)*.065f)
                conifer(x,h*(.55f+(i%3)*.045f),h*(.13f+(i%3)*.035f),if(i%2==0) Color(0xFF526F58) else Color(0xFF637D60))
            }
            cycad(w*.10f,h*.88f,h*.07f);cycad(w*.92f,h*.79f,h*.065f)
            repeat(12) { i -> fern(w*((i*37%101)/100f),h*(.73f+(i*11%23)/100f),h*.026f) }
        }
        RedEra.CRETACEOUS -> {
            // A shallow inland sea, pale coastal deposits, branching deltas and flowering plants.
            drawRect(Color(0xFF8CB8B6),Offset(0f,h*.43f),Size(w,h*.28f))
            repeat(6) { i -> val y=h*(.45f+i*.025f);val drift=sin(time*.22f+i)*w*.02f
                drawLine(Color(0xFFDCE4CF).copy(alpha=.3f),Offset(w*.08f+drift,y),Offset(w*.93f+drift,y),h*.002f)
            }
            shape(Color(0xFFE0D6B7)) {moveTo(w*.60f,h*.43f);lineTo(w*.73f,h*.30f);lineTo(w*.96f,h*.33f);lineTo(w,h*.46f);lineTo(w,h*.61f);lineTo(w*.50f,h*.57f);close()}
            repeat(7) { i -> drawLine(Color(0xFFB7B296).copy(alpha=.6f),Offset(w*(.66f+i*.05f),h*.40f),Offset(w*(.63f+i*.05f),h*.50f),w*.005f) }
            hill(Color(0xFFABB88C),.50f,.05f,.50f)
            river(Color(0xFF91B4A3),.36f,.50f,.09f)
            river(Color(0xFF91B4A3),.36f,.50f,.025f)
            repeat(4) { i ->
                val x=w*(.06f+i*.27f);val y=h*(.69f+(i%2)*.13f);val edge=h*.08f
                drawLine(Color(0xFF756A50),Offset(x,y),Offset(x,y-edge),w*.009f)
                repeat(3) { j -> drawOval(if(j%2==0) Color(0xFF698563) else Color(0xFF7F986C),Offset(x-edge*.65f+j*edge*.28f,y-edge*1.4f+(j%2)*edge*.2f),Size(edge*.85f,edge*.75f)) }
            }
            repeat(18) { i -> val x=w*((i*37%101)/100f);val y=h*(.75f+(i*11%21)/100f)
                fern(x,y,h*.023f)
                if(i%2==0) {
                    val edge=w*.006f
                    repeat(5) { petal -> val angle=petal*1.2566f;drawCircle(Color(0xFFE4CCAE),edge,Offset(x+cos(angle)*edge,y-h*.018f+sin(angle)*edge)) }
                    drawCircle(Color(0xFFAD8460),edge*.5f,Offset(x,y-h*.018f))
                }
            }
        }
    }
    // A shared atmospheric edge joins the regional palettes and silhouettes while swiping.
    // Both sides of each boundary meet at identical colors, with no hard vertical seam.
    val blendWidth=w*.22f
    fun blendEdge(left: Boolean) {
        val origin=Offset(if(left) 0f else w-blendWidth,0f)
        val edgeSize=Size(blendWidth,h)
        drawIntoCanvas { it.saveLayer(Rect(origin,edgeSize),Paint()) }
        drawRect(Brush.verticalGradient(listOf(Color(0xFFCED2B9),Color(0xFFABA982))),origin,edgeSize)
        drawRect(Brush.horizontalGradient(
            if(left) listOf(Color.Black,Color.Transparent) else listOf(Color.Transparent,Color.Black),
            startX=origin.x,endX=origin.x+blendWidth),origin,edgeSize,blendMode=BlendMode.DstIn)
        drawIntoCanvas { it.restore() }
    }
    if(era!=RedEra.TRIASSIC) blendEdge(true)
    if(era!=RedEra.CRETACEOUS) blendEdge(false)
}
