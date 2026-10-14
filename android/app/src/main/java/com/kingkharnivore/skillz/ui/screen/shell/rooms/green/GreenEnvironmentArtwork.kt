package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import com.kingkharnivore.skillz.domain.green.GreenEnvironment
import kotlin.math.*

/** Layered, deterministic landscape art; ornament is never selectable or counted as a specimen. */
internal fun DrawScope.drawGreenLandscape(environment: GreenEnvironment, dark: Boolean, continuation: Boolean = false) {
    val w=size.width; val h=size.height
    val palette=when(environment) {
        GreenEnvironment.GARDEN -> listOf(Color(0xFFF8DEB7),Color(0xFFC6DCB4),Color(0xFF6EAA85),Color(0xFF365F61))
        GreenEnvironment.WOODLANDS -> listOf(Color(0xFFCFD7B8),Color(0xFF97BC9A),Color(0xFF477C66),Color(0xFF203F48))
        GreenEnvironment.RAINFOREST -> listOf(Color(0xFFB3E5CA),Color(0xFF68B2A0),Color(0xFF246D65),Color(0xFF163F4C))
        GreenEnvironment.WETLANDS -> listOf(Color(0xFFF3C7C8),Color(0xFFACCAD3),Color(0xFF68A6B2),Color(0xFF386B86))
        GreenEnvironment.DRYLANDS -> listOf(Color(0xFFF5C9BC),Color(0xFFEABF89),Color(0xFFC99277),Color(0xFF866B82))
        GreenEnvironment.HIGHLANDS -> listOf(Color(0xFFD8D6F1),Color(0xFFAFCCD3),Color(0xFF82ADA6),Color(0xFF476C87))
    }
    drawRect(Brush.verticalGradient(if(continuation) listOf(palette[2],palette[3]) else palette))
    if(!continuation) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF1BF).copy(alpha=.7f),Color.Transparent),Offset(w*.75f,h*.18f),w*.55f),w*.55f,Offset(w*.75f,h*.18f))
        drawCircle(Color(0xFFFFF0CE).copy(alpha=if(dark) .4f else .8f),w*.06f,Offset(w*.75f,h*.18f))
    }
    fun hill(y:Float,color:Color,phase:Float) {
        val p=Path().apply {moveTo(-w*.1f,h);lineTo(-w*.1f,y);cubicTo(w*.24f,y-h*.13f+phase,w*.6f,y+h*.1f,w*1.1f,y-h*.05f);lineTo(w*1.1f,h);close()}
        drawPath(p,color)
    }
    if(environment==GreenEnvironment.HIGHLANDS || environment==GreenEnvironment.DRYLANDS) {
        repeat(3) { layer ->
            val baseline=h*(.42f+layer*.13f)
            val mountain=Path().apply {moveTo(-20f,baseline);lineTo(w*.16f,baseline-h*.12f);lineTo(w*.3f,baseline-h*.06f);lineTo(w*.52f,baseline-h*.23f);lineTo(w*.66f,baseline-h*.10f);lineTo(w*.82f,baseline-h*.19f);lineTo(w*1.1f,baseline);lineTo(w*1.1f,h);lineTo(-20f,h);close()}
            drawPath(mountain,lerp(palette[1],palette[3],.23f+layer*.2f))
            if(environment==GreenEnvironment.HIGHLANDS) {
                val snow=Path().apply {moveTo(w*.4f,baseline-h*.145f);lineTo(w*.52f,baseline-h*.23f);lineTo(w*.61f,baseline-h*.14f);lineTo(w*.54f,baseline-h*.174f);lineTo(w*.50f,baseline-h*.16f);close()}
                drawPath(snow,Color(0xFFF2F3E9).copy(alpha=.75f-layer*.1f))
            }
        }
    } else {
        hill(h*.34f,palette[2].copy(alpha=.3f),h*.08f)
        hill(h*.47f,palette[2].copy(alpha=.5f),-h*.08f)
    }
    when(environment) {
        GreenEnvironment.GARDEN -> {
            hill(h*.57f,Color(0xFF87AE87),h*.02f)
            hill(h*.77f,Color(0xFF628F77),-h*.04f)
            val path=Path().apply {moveTo(w*.52f,h*.39f);cubicTo(w*.19f,h*.59f,w*.94f,h*.61f,w*.54f,h*.83f);cubicTo(w*.3f,h*.96f,w*.53f,h,w*.48f,h*1.05f)}
            drawPath(path,Color(0xFFECD9AD).copy(alpha=.48f),style=Stroke(w*.08f,cap=StrokeCap.Round))
            repeat(15) { n ->val y=h*(.47f+n*.037f); val x=w*(.5f+.16f*sin(n*.62f))
                drawOval(Color(0xFFE6D7B9).copy(alpha=.55f),Offset(x,y),Size(w*(.025f+n*.002f),h*.008f)) }
            // A distant open arch frames the garden, without occupying plant touch targets.
            val arch=Path().apply {moveTo(w*.06f,h*.52f);lineTo(w*.06f,h*.34f);cubicTo(w*.06f,h*.23f,w*.31f,h*.23f,w*.31f,h*.34f);lineTo(w*.31f,h*.52f)}
            drawPath(arch,Color(0xFF658675).copy(alpha=.45f),style=Stroke(w*.022f))
            repeat(40) { n ->val x=w*(.02f+(n*37%96)/100f);val y=h*(.58f+(n*13%40)/100f)
                drawCircle(listOf(Color(0xFFFFD987),Color(0xFFF5B1C0),Color(0xFFEEDCEE))[n%3].copy(alpha=.75f),1.2f+(n%3),Offset(x,y)) }
        }
        GreenEnvironment.WOODLANDS, GreenEnvironment.RAINFOREST -> {
            val jungle=environment==GreenEnvironment.RAINFOREST
            repeat(14) { n ->
                val x=w*((n*23%110)/100f-.05f);val depth=n%3;val trunk=w*(.014f+depth*.011f)
                val y=h*(.15f+depth*.06f)
                drawLine(palette[3].copy(alpha=.14f+depth*.1f),Offset(x,y),Offset(x-w*.055f,h*.82f),trunk)
                drawOval(palette[2].copy(alpha=.2f+depth*.06f),Offset(x-w*.20f,y-h*.09f),Size(w*.4f,h*.2f))
            }
            repeat(4) { n ->
                val ray=Path().apply {moveTo(w*(.6f+n*.09f),0f);lineTo(w*(.10f+n*.18f),h*.82f);lineTo(w*(.25f+n*.18f),h*.84f);close()}
                drawPath(ray,Brush.verticalGradient(listOf(Color(0xFFF3F9C2).copy(alpha=.12f),Color.Transparent)))
            }
            hill(h*.75f,palette[3].copy(alpha=.3f),h*.04f)
            if(jungle) {
                val waterfall=Path().apply {moveTo(w*.74f,h*.30f);cubicTo(w*.64f,h*.46f,w*.88f,h*.54f,w*.74f,h*.68f)}
                drawPath(waterfall,Color(0xFFB9F0E0).copy(alpha=.42f),style=Stroke(w*.028f))
                repeat(6) { n ->val vine=Path().apply {moveTo(w*n/5,0f);quadraticTo(w*(n/5f+.12f),h*.18f,w*n/5,h*.35f)}
                    drawPath(vine,Color(0xFF216C54).copy(alpha=.6f),style=Stroke(2f)) }
            }
        }
        GreenEnvironment.WETLANDS -> {
            drawRect(Brush.verticalGradient(listOf(Color.Transparent,Color(0xFF598DAD).copy(alpha=.55f))),topLeft=Offset(0f,h*.38f),size=Size(w,h*.62f))
            repeat(33) { n ->val x=w*(n*37%100)/100;val y=h*(.40f+(n*17%59)/100f)
                drawOval(Color(0xFFE8E6D8).copy(alpha=.22f),Offset(x-w*.1f,y),Size(w*(.09f+n%4*.035f),h*.009f),style=Stroke(1.1f)) }
            repeat(5) { n ->drawOval(Color(0xFF70A596).copy(alpha=.72f),Offset(w*(if(n%2==0) -.25f else .69f),h*(.49f+n*.11f)),Size(w*.6f,h*.09f)) }
            drawOval(Brush.radialGradient(listOf(Color(0xFFFFD4B9).copy(alpha=.25f),Color.Transparent)),Offset(w*.41f,h*.4f),Size(w*.62f,h*.45f))
        }
        GreenEnvironment.DRYLANDS -> {
            hill(h*.63f,Color(0xFFD5A177),-h*.07f)
            hill(h*.84f,Color(0xFFBF8B7B),h*.07f)
            val arch=Path().apply {moveTo(w*.75f,h*.65f);cubicTo(w*.67f,h*.42f,w*.87f,h*.30f,w*.93f,h*.61f)}
            drawPath(arch,Color(0xFFB37967),style=Stroke(w*.075f,cap=StrokeCap.Round))
            drawPath(arch,Color(0xFFE7B48C).copy(alpha=.65f),style=Stroke(w*.023f))
            repeat(12) { n ->val y=h*(.61f+n*.032f)
                val dune=Path().apply {moveTo(0f,y);quadraticTo(w*.4f,y-h*.08f,w,y+h*.03f)}
                drawPath(dune,Color(0xFFF4C8A3).copy(alpha=.16f),style=Stroke(1f)) }
        }
        GreenEnvironment.HIGHLANDS -> {
            hill(h*.71f,Color(0xFF81A293),h*.08f)
            hill(h*.9f,Color(0xFF557E79),-h*.04f)
            repeat(8) { n ->drawOval(Color(0xFFF0E9F7).copy(alpha=.12f),Offset(w*(n*.19f-.4f),h*(.36f+n%3*.12f)),Size(w*.95f,h*.055f)) }
            repeat(9) { n ->val x=w*(n*31%100)/100;val y=h*(.71f+n%4*.071f)
                drawOval(Color(0xFF637E8A).copy(alpha=.5f),Offset(x,y),Size(w*.11f,h*.025f)) }
        }
    }
    // Foreground shadows and tiny luminous motes add depth while keeping specimens prominent.
    drawRect(Brush.verticalGradient(listOf(Color.Transparent,palette[3].copy(alpha=.25f)),h*.65f,h))
    repeat(26) { n ->val x=w*(n*47%101)/101;val y=h*(.26f+(n*29%69)/100f)
        drawCircle(Color(0xFFFFF0AD).copy(alpha=if(environment==GreenEnvironment.WOODLANDS || environment==GreenEnvironment.RAINFOREST) .7f else .3f),if(n%4==0) 2.6f else 1.2f,Offset(x,y)) }
    if(dark) drawRect(Color(0xFF10243E).copy(alpha=.30f))
}
