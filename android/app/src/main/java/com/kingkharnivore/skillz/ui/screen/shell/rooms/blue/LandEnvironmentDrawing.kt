package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.kingkharnivore.skillz.utils.shell.CreatureZone
import kotlin.math.sin

/** Bounded, deterministic scenery. The shared scene clock is read only during drawing. */
fun DrawScope.drawLandEnvironment(zone: CreatureZone, restorative: Boolean = false, time: Float = 0f) {
    val w = size.width
    val h = size.height
    fun hill(color: Color, y: Float, bend: Float, end: Float) {
        drawPath(Path().apply {
            moveTo(0f, h*y)
            cubicTo(w*.25f,h*(y-bend),w*.66f,h*(end+bend),w,h*end)
            lineTo(w,h); lineTo(0f,h); close()
        }, color)
    }
    fun haze(color: Color, y: Float) {
        val drift = sin(time*.045f)*w*.07f
        drawOval(color,Offset(-w*.2f+drift,h*y),Size(w*1.4f,h*.08f))
    }
    fun grass(x: Float, y: Float, scale: Float, color: Color, seed: Int) {
        val sway = sin(time*.45f+seed)*scale*.16f
        repeat(3) { blade ->
            drawPath(Path().apply {
                moveTo(x,y)
                quadraticBezierTo(x+(blade-1)*scale*.25f+sway,y-scale*.5f,
                    x+(blade-1)*scale*.44f+sway,y-scale*(.68f+blade*.15f))
            },color,style=Stroke(w*.0018f,cap=StrokeCap.Round))
        }
    }
    fun stone(x: Float, y: Float, radius: Float, color: Color) {
        drawPath(Path().apply {
            moveTo(x-radius,y);lineTo(x-radius*.72f,y-radius*.58f)
            lineTo(x+radius*.1f,y-radius*.8f);lineTo(x+radius*.75f,y-radius*.42f)
            lineTo(x+radius,y);close()
        },color)
    }
    fun pine(x: Float, y: Float, height: Float, color: Color) {
        drawLine(color,Offset(x,y),Offset(x,y-height),w*.005f)
        repeat(3) { tier ->
            val top=y-height+height*tier*.21f
            val spread=height*(.15f+tier*.055f)
            drawPath(Path().apply { moveTo(x,top);lineTo(x+spread,top+height*.48f);lineTo(x-spread,top+height*.48f);close() },color)
        }
    }
    when(zone) {
        CreatureZone.GOLDEN_FIELDS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFF0DFC0),Color(0xFFD9DAB3),Color(0xFFADBA83))))
            drawCircle(Color(0xFFF7E9BB).copy(alpha=.6f),w*.14f,Offset(w*.75f,h*.24f))
            haze(Color(0xFFF9EFDB).copy(alpha=.22f),.32f)
            hill(Color(0xFFC9CEA0),.42f,.08f,.38f)
            hill(Color(0xFFB6C18C),.54f,.065f,.46f)
            hill(Color(0xFFC4BC80),.64f,.075f,.56f)
            // Cultivated strips follow the slope, leaving an open meadow for the creatures.
            repeat(7) { row ->
                val y=h*(.49f+row*.018f)
                drawPath(Path().apply {moveTo(0f,y);cubicTo(w*.23f,y-h*.045f,w*.40f,y+h*.006f,w*.53f,y+h*.065f)},
                    Color(0xFFE7D695).copy(alpha=.38f),style=Stroke(w*.005f))
            }
            hill(Color(0xFFA9B580),.74f,.05f,.70f)
            repeat(8) { post ->
                val x=w*(.03f+post*.075f);val y=h*(.76f-post*.008f)
                drawLine(Color(0xFF8D936D).copy(alpha=.48f),Offset(x,y),Offset(x,y-h*.025f),w*.004f)
                if(post<7) drawLine(Color(0xFF8D936D).copy(alpha=.35f),Offset(x,y-h*.018f),Offset(x+w*.075f,y-h*.026f),w*.002f)
            }
            repeat(24) { i ->
                val x=w*((i*37%101)/100f);val y=h*(.65f+(i*19%29)/100f)
                grass(x,y,h*.014f,Color(0xFF72865D).copy(alpha=.45f),i)
                if(i%3==0) drawCircle(if(i%2==0) Color(0xFFDDCF9E) else Color(0xFFD6BBA5),w*.003f,Offset(x,y-h*.014f))
            }
        }
        CreatureZone.ANCIENT_WOODS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFB9C7B1),Color(0xFF7E9A80),Color(0xFF526F5D))))
            // Three trunk planes and a lit clearing create depth without strong contrast behind cards.
            repeat(11) { i ->
                val x=w*(i/10f);val width=w*(.018f+(i%3)*.007f)
                drawRect(Color(0xFF5E7A65).copy(alpha=.23f),Offset(x,h*.12f),Size(width,h*.62f))
            }
            drawPath(Path().apply {moveTo(w*.42f,h*.18f);lineTo(w*.68f,h*.78f);lineTo(w*.98f,h*.78f);lineTo(w*.57f,h*.18f);close()},Color(0xFFE4E6BD).copy(alpha=.12f))
            hill(Color(0xFF71916E),.61f,.07f,.57f)
            hill(Color(0xFF638264),.77f,.06f,.72f)
            repeat(5) { i ->
                val x=w*(i*.26f-.05f);val y=h*(.69f+(i%2)*.07f)
                drawPath(Path().apply {moveTo(x-w*.025f,y);quadraticBezierTo(x-w*.01f,h*.36f,x-w*.035f,h*.14f);lineTo(x+w*.03f,h*.14f);quadraticBezierTo(x+w*.015f,h*.42f,x+w*.05f,y);close()},Color(0xFF455F4D).copy(alpha=.52f))
                drawLine(Color(0xFF455F4D).copy(alpha=.45f),Offset(x,h*.36f),Offset(x+w*.14f,h*.28f),w*.02f)
                repeat(3) { crown -> drawOval(Color(0xFF476D51).copy(alpha=.3f),Offset(x-w*.16f+crown*w*.06f,h*(.13f+crown*.04f)),Size(w*.35f,h*.2f)) }
                drawLine(Color(0xFF455F4D).copy(alpha=.3f),Offset(x,y),Offset(x-w*.08f,y+h*.01f),w*.012f)
            }
            haze(Color(0xFFD5DFBD).copy(alpha=.09f),.63f)
            repeat(17) { i ->
                val x=w*((i*43%101)/100f);val y=h*(.69f+(i*17%25)/100f)
                grass(x,y,h*.025f,Color(0xFF91A781).copy(alpha=.6f),i)
                if(i%4==0) stone(x,y,w*.022f,Color(0xFF82977B))
            }
            repeat(9) { i ->
                val glow=(sin(time*.55f+i*1.7f)+1f)*.12f
                drawCircle(Color(0xFFE1DCA3).copy(alpha=glow),w*.0025f,Offset(w*(.1f+(i*29%80)/100f),h*(.42f+(i*13%30)/100f)+sin(time*.22f+i)*h*.003f))
            }
        }
        CreatureZone.OPEN_SANDS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFE5CFB0),Color(0xFFE1CDA7),Color(0xFFC2A276))))
            drawCircle(Color(0xFFF5DEAF).copy(alpha=.62f),w*.115f,Offset(w*.72f,h*.26f))
            // Distant weathered mesas, long dune crests, and sparse hardy plants.
            repeat(3) { i ->
                val x=w*(.12f+i*.4f);val y=h*(.43f+(i%2)*.025f)
                drawPath(Path().apply {moveTo(x-w*.13f,y);lineTo(x-w*.07f,y-h*.085f);lineTo(x+w*.06f,y-h*.085f);lineTo(x+w*.12f,y);close()},Color(0xFFAC947A).copy(alpha=.27f))
            }
            hill(Color(0xFFD5B78B),.50f,.12f,.43f)
            hill(Color(0xFFE1C599),.59f,-.09f,.55f)
            hill(Color(0xFFC9A97D),.72f,.10f,.61f)
            hill(Color(0xFFD4B68B),.80f,-.045f,.78f)
            repeat(8) { i ->
                val y=h*(.69f+i*.019f)
                drawPath(Path().apply {moveTo(w*.02f,y);quadraticBezierTo(w*.24f,y-h*.018f,w*.40f,y+h*.025f)},Color(0xFFE8CCA0).copy(alpha=.36f),style=Stroke(w*.002f))
            }
            repeat(3) { i ->
                val x=w*(.12f+i*.36f);val y=h*(.73f+(i%2)*.08f)
                val green=Color(0xFF7F8B6C).copy(alpha=.75f)
                drawLine(green,Offset(x,y),Offset(x,y-h*.055f),w*.012f,StrokeCap.Round)
                drawLine(green,Offset(x,y-h*.023f),Offset(x-w*.025f,y-h*.023f),w*.009f,StrokeCap.Round)
                drawLine(green,Offset(x-w*.025f,y-h*.023f),Offset(x-w*.025f,y-h*.043f),w*.009f,StrokeCap.Round)
                grass(x+w*.045f,y,h*.015f,Color(0xFF9F946D),i)
            }
            repeat(12) { i ->
                val x=((i*.083f+time*.002f)%1f)*w
                val y=h*(.53f+(i*17%35)/100f)
                drawLine(Color(0xFFF2DDB8).copy(alpha=.23f),Offset(x,y),Offset(x+w*.025f,y-h*.003f),w*.0015f)
            }
        }
        CreatureZone.HIGH_PEAKS -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFD6E1E3),Color(0xFFB7CBCB),Color(0xFF8EABA8))))
            repeat(5) { i ->
                val x=w*(i*.29f-.08f);val top=h*(.31f+(i%3)*.045f);val base=h*(.64f+(i%2)*.04f)
                val spread=w*.32f
                drawPath(Path().apply {moveTo(x-spread,base);lineTo(x-w*.065f,top+h*.075f);lineTo(x,top);lineTo(x+w*.08f,top+h*.09f);lineTo(x+spread,base);close()},Color(0xFF7D969E).copy(alpha=.52f))
                drawPath(Path().apply {moveTo(x-w*.11f,top+h*.12f);lineTo(x,top);lineTo(x+w*.13f,top+h*.145f);lineTo(x+w*.045f,top+h*.1f);lineTo(x,top+h*.115f);lineTo(x-w*.05f,top+h*.075f);close()},Color(0xFFE8EEEA).copy(alpha=.8f))
                drawPath(Path().apply {moveTo(x,top);lineTo(x+spread,base);lineTo(x+w*.04f,base);close()},Color(0xFF5D7D89).copy(alpha=.17f))
            }
            haze(Color(0xFFE5EDE8).copy(alpha=.25f),.54f)
            hill(Color(0xFF99B2AC),.66f,.025f,.65f)
            hill(Color(0xFF849F98),.80f,.07f,.74f)
            repeat(8) { i -> pine(w*(i*.15f-.03f),h*(.77f+(i%2)*.06f),h*(.07f+(i%3)*.014f),Color(0xFF526F69).copy(alpha=.6f)) }
            repeat(8) { i ->
                val x=w*((i*41%100)/100f);val y=h*(.72f+(i%3)*.07f)
                stone(x,y,w*(.023f+(i%2)*.012f),Color(0xFF718B86))
                drawOval(Color(0xFFE0E6DA).copy(alpha=.6f),Offset(x-w*.025f,y-h*.013f),Size(w*.05f,h*.007f))
            }
            repeat(15) { i ->
                val x=w*((i*.073f+sin(time*.12f+i)*.012f)%1f)
                val y=h*((i*.061f+time*.0025f)%1f)
                drawCircle(Color(0xFFF4F4E9).copy(alpha=.32f),w*.0018f,Offset(x,y))
            }
        }
        else -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFFBFC0AF),Color(0xFF8A9E8D),Color(0xFF4E6C5D))))
            drawCircle(Color(0xFFE2D3A9).copy(alpha=.42f),w*.13f,Offset(w*.72f,h*.28f))
            hill(Color(0xFF7E9380),.45f,.09f,.39f)
            hill(Color(0xFF698571),.59f,.035f,.53f)
            hill(Color(0xFF5B7964),.75f,.065f,.68f)
            // Broad acacia silhouettes and an exposed rock ridge distinguish the remote wilderness.
            repeat(3) { i ->
                val x=w*(.08f+i*.43f);val y=h*(.61f+(i%2)*.12f)
                val height=h*(.12f+(i%2)*.045f)
                val tree=Color(0xFF365644).copy(alpha=.56f)
                drawLine(tree,Offset(x,y),Offset(x+w*.014f,y-height*.74f),w*.014f)
                drawLine(tree,Offset(x+w*.012f,y-height*.6f),Offset(x-w*.075f,y-height*.85f),w*.012f)
                drawLine(tree,Offset(x+w*.012f,y-height*.66f),Offset(x+w*.10f,y-height*.92f),w*.012f)
                drawOval(tree,Offset(x-w*.18f,y-height),Size(w*.38f,height*.3f))
                drawOval(tree,Offset(x-w*.09f,y-height*1.10f),Size(w*.26f,height*.3f))
            }
            stone(w*.73f,h*.79f,w*.19f,Color(0xFF647B6C))
            stone(w*.86f,h*.81f,w*.13f,Color(0xFF526C60))
            haze(Color(0xFFBAC5AB).copy(alpha=.13f),.56f)
            repeat(32) { i ->
                val x=w*((i*37%103)/102f);val y=h*(.68f+(i*17%26)/100f)
                grass(x,y,h*(.018f+(i%3)*.005f),Color(0xFFA5AC7D).copy(alpha=.47f),i)
            }
        }
    }
    if (restorative) {
        drawOval(Color(0xFFE1E5C9).copy(alpha=.2f),Offset(w*.08f,h*.35f),Size(w*.84f,h*.48f))
        if(zone==CreatureZone.OPEN_SANDS || zone==CreatureZone.HIGH_PEAKS) {
            drawOval(Color(0xFF88B3AF).copy(alpha=.65f),Offset(w*.20f,h*.67f),Size(w*.6f,h*.06f))
            drawOval(Color(0xFFD5E4CE).copy(alpha=.35f),Offset(w*.28f,h*.68f),Size(w*.42f,h*.015f))
        }
    }
}
