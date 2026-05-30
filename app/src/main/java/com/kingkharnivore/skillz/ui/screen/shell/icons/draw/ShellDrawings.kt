package com.kingkharnivore.skillz.ui.screen.shell.icons.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

fun DrawScope.drawStaticCreatureIcon(key: String, scheme: androidx.compose.material3.ColorScheme) {
    val w = size.width
    val h = size.height
    val primary = scheme.primary
    val secondary = scheme.secondary
    val surface = scheme.surface
    fun fishBody(body: Color, accent: Color, tall: Float = 0.30f, long: Float = 0.54f, stripes: Int = 0, beak: Boolean = false) {
        drawOval(body, topLeft = Offset(w * 0.20f, h * (0.50f - tall / 2f)), size = Size(w * long, h * tall))
        drawPath(Path().apply { moveTo(w*0.19f,h*0.50f); lineTo(w*0.03f,h*0.35f); lineTo(w*0.03f,h*0.65f); close() }, accent)
        drawPath(Path().apply { moveTo(w*0.50f,h*(0.50f-tall/2f)); lineTo(w*0.60f,h*0.17f); lineTo(w*0.66f,h*(0.50f-tall/2f+0.04f)); close() }, accent.copy(alpha = 0.72f))
        if (beak) drawPath(Path().apply { moveTo(w*0.74f,h*0.47f); lineTo(w*0.92f,h*0.42f); lineTo(w*0.75f,h*0.55f); close() }, accent)
        repeat(stripes) { i ->
            val x = w * (0.34f + i * 0.11f)
            drawLine(surface.copy(alpha = 0.72f), Offset(x, h*(0.50f-tall/2f+0.02f)), Offset(x + w*0.03f, h*(0.50f+tall/2f-0.02f)), strokeWidth = w*0.035f)
        }
        drawCircle(surface, radius = w * 0.035f, center = Offset(w * 0.64f, h * 0.44f))
    }
    fun starFish() {
        val path = Path()
        repeat(10) { i ->
            val r = if (i % 2 == 0) 0.43f else 0.19f
            val a = (-90 + i * 36) * Math.PI / 180.0
            val x = w * 0.5f + kotlin.math.cos(a).toFloat() * w * r
            val y = h * 0.5f + kotlin.math.sin(a).toFloat() * h * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, secondary.copy(alpha = 0.82f))
        drawCircle(primary.copy(alpha = 0.38f), w*0.055f, Offset(w*0.5f,h*0.5f))
        repeat(5) { i ->
            val a=(-90+i*72)*Math.PI/180.0
            drawCircle(surface.copy(alpha=0.65f), w*0.025f, Offset(w*0.5f+kotlin.math.cos(a).toFloat()*w*0.24f, h*0.5f+kotlin.math.sin(a).toFloat()*h*0.24f))
        }
    }
    fun urchin() {
        repeat(18) { i -> val a=i*6.28318f/18f; drawLine(primary, Offset(w*0.5f,h*0.5f), Offset(w*(0.5f+kotlin.math.cos(a)*0.45f), h*(0.5f+kotlin.math.sin(a)*0.45f)), strokeWidth=w*0.028f) }
        drawCircle(secondary.copy(alpha=0.72f), w*0.25f, Offset(w*0.5f,h*0.5f))
        drawCircle(surface.copy(alpha=0.5f), w*0.05f, Offset(w*0.44f,h*0.44f))
    }
    when {
        "starfish" in key -> starFish()
        "urchin" in key -> urchin()
        "minnow" in key -> fishBody(primary.copy(alpha = 0.68f), secondary.copy(alpha = 0.48f), tall = 0.22f, long = 0.48f)
        "octopus" in key -> { val c = secondary.copy(alpha = 0.74f); drawOval(c, Offset(w * 0.32f, h * 0.12f), Size(w * 0.36f, h * 0.36f)); repeat(8) { i -> drawLine(c, Offset(w * (0.34f + i * 0.045f), h * 0.46f), Offset(w * (0.12f + i * 0.09f), h * 0.88f), strokeWidth = w * 0.035f) }; drawCircle(surface.copy(alpha = 0.56f), w * 0.024f, Offset(w * 0.44f, h * 0.28f)); drawCircle(surface.copy(alpha = 0.56f), w * 0.024f, Offset(w * 0.56f, h * 0.28f)) }
        "clownfish" in key -> fishBody(Color(0xFFE9782E), surface, tall=0.34f, long=0.55f, stripes=3)
        "blue_tang" in key || "tang" in key -> fishBody(Color(0xFF2D77C8), Color(0xFFF2D14C), tall=0.32f, long=0.58f, stripes=1)
        "butterflyfish" in key -> fishBody(Color(0xFFF4D35E), Color(0xFF1A1B2E), tall=0.46f, long=0.48f, stripes=4)
        "angelfish" in key -> fishBody(Color(0xFF6C63C7), Color(0xFFEFB8C8), tall=0.50f, long=0.42f, stripes=2)
        "parrotfish" in key -> fishBody(Color(0xFF13A999), Color(0xFFFF8F3D), tall=0.36f, long=0.62f, stripes=2, beak=true)
        "lionfish" in key -> { fishBody(Color(0xFFB45A3C), Color(0xFFF3D6A2), tall=0.38f, long=0.50f, stripes=4); repeat(6){i->drawLine(secondary.copy(alpha=0.55f), Offset(w*(0.34f+i*0.06f),h*0.33f), Offset(w*(0.24f+i*0.10f),h*0.05f), strokeWidth=w*0.02f)} }
        "pufferfish" in key -> { drawCircle(primary.copy(alpha=0.72f), w*0.28f, Offset(w*0.52f,h*0.50f)); repeat(10){i->val a=i*6.28318f/10f; drawLine(secondary, Offset(w*0.52f,h*0.50f), Offset(w*(0.52f+kotlin.math.cos(a)*0.40f), h*(0.5f+kotlin.math.sin(a)*0.40f)), strokeWidth=w*0.018f)}; drawCircle(surface,w*0.03f,Offset(w*0.62f,h*0.42f)) }
        "seahorse" in key -> { drawCircle(secondary.copy(alpha=0.76f), w*0.15f, Offset(w*0.58f,h*0.25f)); drawLine(secondary.copy(alpha=0.76f), Offset(w*0.66f,h*0.25f), Offset(w*0.86f,h*0.20f), strokeWidth=w*0.08f); drawPath(Path().apply { moveTo(w*0.54f,h*0.36f); cubicTo(w*0.28f,h*0.43f,w*0.36f,h*0.80f,w*0.58f,h*0.70f); cubicTo(w*0.78f,h*0.62f,w*0.68f,h*0.48f,w*0.54f,h*0.56f) }, secondary.copy(alpha=0.76f), style=Stroke(width=w*0.13f)); drawCircle(surface,w*0.03f,Offset(w*0.62f,h*0.21f)) }
        "stingray" in key -> { drawPath(Path().apply { moveTo(w*0.50f,h*0.26f); cubicTo(w*0.20f,h*0.32f,w*0.08f,h*0.58f,w*0.08f,h*0.70f); cubicTo(w*0.34f,h*0.62f,w*0.42f,h*0.62f,w*0.50f,h*0.76f); cubicTo(w*0.58f,h*0.62f,w*0.66f,h*0.62f,w*0.92f,h*0.70f); cubicTo(w*0.92f,h*0.58f,w*0.80f,h*0.32f,w*0.50f,h*0.26f); close() }, primary.copy(alpha=0.68f)); drawLine(secondary.copy(alpha=0.52f), Offset(w*0.50f,h*0.72f), Offset(w*0.50f,h*0.96f), strokeWidth=w*0.025f) }
        "manta" in key -> { drawPath(Path().apply { moveTo(w*0.50f,h*0.18f); cubicTo(w*0.12f,h*0.30f,w*0.03f,h*0.65f,w*0.02f,h*0.80f); cubicTo(w*0.29f,h*0.64f,w*0.40f,h*0.64f,w*0.50f,h*0.78f); cubicTo(w*0.60f,h*0.64f,w*0.71f,h*0.64f,w*0.98f,h*0.80f); cubicTo(w*0.97f,h*0.65f,w*0.88f,h*0.30f,w*0.50f,h*0.18f); close() }, primary.copy(alpha=0.74f)); drawLine(secondary.copy(alpha=0.55f), Offset(w*0.50f,h*0.74f), Offset(w*0.50f,h*0.98f), strokeWidth=w*0.035f) }
        "jellyfish" in key -> { drawArc(secondary.copy(alpha=0.72f),180f,180f,true,Offset(w*0.18f,h*0.16f),Size(w*0.64f,h*0.48f)); repeat(5){i->drawLine(primary.copy(alpha=0.5f),Offset(w*(0.25f+i*0.12f),h*0.50f),Offset(w*(0.20f+i*0.13f),h*0.90f),strokeWidth=w*0.035f)} }
        "turtle" in key -> { drawOval(primary.copy(alpha=0.72f), Offset(w*0.24f,h*0.24f), Size(w*0.50f,h*0.42f)); drawOval(secondary.copy(alpha=0.55f), Offset(w*0.42f,h*0.08f), Size(w*0.16f,h*0.16f)); repeat(4){i->val x=if(i%2==0)0.15f else 0.74f; val y=if(i<2)0.30f else 0.62f; drawOval(secondary.copy(alpha=0.55f),Offset(w*x,h*y),Size(w*0.18f,h*0.12f))} }
        "sea_otter" in key || "otter" in key -> { drawOval(primary.copy(alpha=0.62f), Offset(w*0.16f,h*0.42f), Size(w*0.58f,h*0.20f)); drawCircle(primary.copy(alpha=0.72f), w*0.14f, Offset(w*0.70f,h*0.38f)); drawCircle(surface.copy(alpha=0.55f), w*0.035f, Offset(w*0.75f,h*0.35f)); drawCircle(secondary.copy(alpha=0.52f), w*0.045f, Offset(w*0.46f,h*0.48f)); drawCircle(secondary.copy(alpha=0.52f), w*0.04f, Offset(w*0.56f,h*0.47f)) }
        "sea_lion" in key -> { drawOval(primary.copy(alpha=0.68f), Offset(w*0.14f,h*0.36f), Size(w*0.60f,h*0.28f)); drawOval(primary.copy(alpha=0.76f), Offset(w*0.62f,h*0.28f), Size(w*0.22f,h*0.20f)); drawPath(Path().apply { moveTo(w*0.42f,h*0.58f); lineTo(w*0.18f,h*0.82f); lineTo(w*0.54f,h*0.63f); close() }, secondary.copy(alpha=0.58f)); drawPath(Path().apply { moveTo(w*0.56f,h*0.58f); lineTo(w*0.82f,h*0.80f); lineTo(w*0.66f,h*0.58f); close() }, secondary.copy(alpha=0.45f)); drawCircle(surface,w*0.025f,Offset(w*0.78f,h*0.34f)) }
        "penguin" in key -> { drawOval(Color(0xFF263238).copy(alpha=0.80f), Offset(w*0.32f,h*0.10f), Size(w*0.36f,h*0.72f)); drawOval(surface.copy(alpha=0.78f), Offset(w*0.40f,h*0.25f), Size(w*0.20f,h*0.42f)); drawPath(Path().apply { moveTo(w*0.32f,h*0.38f); lineTo(w*0.10f,h*0.52f); lineTo(w*0.34f,h*0.55f); close() }, primary.copy(alpha=0.48f)); drawPath(Path().apply { moveTo(w*0.68f,h*0.38f); lineTo(w*0.90f,h*0.52f); lineTo(w*0.66f,h*0.55f); close() }, primary.copy(alpha=0.48f)); drawPath(Path().apply { moveTo(w*0.55f,h*0.18f); lineTo(w*0.78f,h*0.23f); lineTo(w*0.56f,h*0.28f); close() }, secondary) }
        "seal" in key -> { drawOval(primary.copy(alpha=0.68f), Offset(w*0.16f,h*0.36f), Size(w*0.62f,h*0.26f)); drawCircle(primary.copy(alpha=0.72f), w*0.12f, Offset(w*0.75f,h*0.42f)); drawPath(Path().apply { moveTo(w*0.42f,h*0.58f); lineTo(w*0.26f,h*0.76f); lineTo(w*0.52f,h*0.62f); close() }, secondary.copy(alpha=0.50f)); drawCircle(surface,w*0.025f,Offset(w*0.79f,h*0.38f)) }
        "dolphin" in key -> { drawArc(primary.copy(alpha=0.72f),200f,200f,false,Offset(w*0.12f,h*0.18f),Size(w*0.70f,h*0.50f),style=Stroke(width=w*0.15f)); drawPath(Path().apply{moveTo(w*0.75f,h*0.41f);lineTo(w*0.98f,h*0.32f);lineTo(w*0.76f,h*0.50f);close()}, primary.copy(alpha=0.72f)); drawPath(Path().apply{moveTo(w*0.18f,h*0.50f);lineTo(w*0.02f,h*0.32f);lineTo(w*0.14f,h*0.50f);lineTo(w*0.02f,h*0.68f);close()}, primary.copy(alpha=0.68f)); drawPath(Path().apply{moveTo(w*0.47f,h*0.36f);lineTo(w*0.55f,h*0.12f);lineTo(w*0.61f,h*0.39f);close()}, secondary.copy(alpha=0.55f)); drawCircle(surface,w*0.024f,Offset(w*0.68f,h*0.38f)) }
        "orca" in key -> { drawOval(Color(0xFF263238).copy(alpha=0.72f),Offset(w*0.14f,h*0.35f),Size(w*0.66f,h*0.30f)); drawOval(surface.copy(alpha=0.74f),Offset(w*0.42f,h*0.46f),Size(w*0.22f,h*0.10f)); drawPath(Path().apply{moveTo(w*0.76f,h*0.50f);lineTo(w*0.98f,h*0.30f);lineTo(w*0.90f,h*0.50f);lineTo(w*0.98f,h*0.70f);close()}, Color(0xFF263238).copy(alpha=0.72f)); drawPath(Path().apply{moveTo(w*0.42f,h*0.36f);lineTo(w*0.50f,h*0.08f);lineTo(w*0.58f,h*0.38f);close()}, Color(0xFF263238).copy(alpha=0.72f)) }
        "anglerfish" in key -> { fishBody(Color(0xFF30313B), secondary, tall=0.38f, long=0.56f); drawLine(secondary,Offset(w*0.60f,h*0.32f),Offset(w*0.72f,h*0.12f),strokeWidth=w*0.025f); drawCircle(secondary,w*0.045f,Offset(w*0.74f,h*0.10f)) }
        "megalodon" in key -> { fishBody(Color(0xFF27323A), Color(0xFF6B7A85), tall=0.34f, long=0.70f); drawPath(Path().apply{moveTo(w*0.45f,h*0.36f);lineTo(w*0.55f,h*0.04f);lineTo(w*0.64f,h*0.38f);close()}, Color(0xFF6B7A85)) }
        "shark" in key || "great_white" in key -> { fishBody(Color(0xFF6B7A85), Color(0xFFB0BEC5), tall=0.30f, long=0.66f); drawPath(Path().apply{moveTo(w*0.42f,h*0.38f);lineTo(w*0.50f,h*0.12f);lineTo(w*0.58f,h*0.40f);close()}, Color(0xFFB0BEC5)) }
        "blue_whale" in key -> { drawOval(primary.copy(alpha=0.56f), Offset(w*0.05f,h*0.38f), Size(w*0.82f,h*0.24f)); drawPath(Path().apply{moveTo(w*0.84f,h*0.50f);lineTo(w*0.99f,h*0.35f);lineTo(w*0.94f,h*0.50f);lineTo(w*0.99f,h*0.65f);close()}, primary.copy(alpha=0.52f)); drawOval(secondary.copy(alpha=0.26f),Offset(w*0.34f,h*0.52f),Size(w*0.30f,h*0.06f)); drawPath(Path().apply{moveTo(w*0.55f,h*0.38f);lineTo(w*0.60f,h*0.25f);lineTo(w*0.64f,h*0.39f);close()}, secondary.copy(alpha=0.32f)) }
        "humpback_whale" in key -> { drawPath(Path().apply{moveTo(w*0.12f,h*0.54f); cubicTo(w*0.24f,h*0.22f,w*0.58f,h*0.24f,w*0.80f,h*0.46f); cubicTo(w*0.62f,h*0.68f,w*0.28f,h*0.70f,w*0.12f,h*0.54f); close()}, primary.copy(alpha=0.58f)); drawPath(Path().apply{moveTo(w*0.78f,h*0.48f);lineTo(w*0.98f,h*0.30f);lineTo(w*0.90f,h*0.50f);lineTo(w*0.98f,h*0.70f);close()}, primary.copy(alpha=0.52f)); drawLine(secondary.copy(alpha=0.42f), Offset(w*0.40f,h*0.58f), Offset(w*0.22f,h*0.92f), strokeWidth=w*0.06f); repeat(3){i->drawCircle(secondary.copy(alpha=0.35f), w*0.018f, Offset(w*(0.20f+i*0.05f), h*0.42f))} }
        "whale" in key -> { drawOval(primary.copy(alpha=0.58f), Offset(w*0.12f,h*0.34f), Size(w*0.68f,h*0.34f)); drawPath(Path().apply{moveTo(w*0.78f,h*0.50f);lineTo(w*0.98f,h*0.30f);lineTo(w*0.92f,h*0.50f);lineTo(w*0.98f,h*0.70f);close()}, primary.copy(alpha=0.58f)); drawOval(secondary.copy(alpha=0.32f),Offset(w*0.33f,h*0.50f),Size(w*0.26f,h*0.08f)) }
        "leviathan" in key -> { val c=Color(0xFF203A5F).copy(alpha=0.78f); drawArc(c,180f,220f,false,Offset(w*0.06f,h*0.20f),Size(w*0.86f,h*0.56f),style=Stroke(width=w*0.09f)); drawCircle(secondary.copy(alpha=0.60f),w*0.10f,Offset(w*0.78f,h*0.32f)); drawPath(Path().apply{moveTo(w*0.44f,h*0.26f);lineTo(w*0.54f,h*0.02f);lineTo(w*0.58f,h*0.30f);close()},secondary.copy(alpha=0.32f)) }
        "kraken" in key -> { val c=secondary.copy(alpha=0.75f); drawOval(c,Offset(w*0.28f,h*0.08f),Size(w*0.44f,h*0.44f)); repeat(10){i->drawLine(c,Offset(w*(0.30f+i*0.045f),h*0.50f),Offset(w*(0.04f+i*0.10f),h*0.94f),strokeWidth=w*0.045f)} }
        "giant_squid" in key -> { val c=secondary.copy(alpha=0.68f); drawPath(Path().apply{moveTo(w*0.50f,h*0.04f); cubicTo(w*0.22f,h*0.30f,w*0.34f,h*0.58f,w*0.50f,h*0.58f); cubicTo(w*0.66f,h*0.58f,w*0.78f,h*0.30f,w*0.50f,h*0.04f); close()},c); repeat(8){i->drawLine(c,Offset(w*(0.34f+i*0.045f),h*0.56f),Offset(w*(0.10f+i*0.09f),h*0.94f),strokeWidth=w*0.035f)} }
        "squid" in key -> { val c=secondary.copy(alpha=0.70f); drawOval(c,Offset(w*0.34f,h*0.10f),Size(w*0.32f,h*0.42f)); repeat(5){i->drawLine(c,Offset(w*(0.36f+i*0.07f),h*0.50f),Offset(w*(0.20f+i*0.14f),h*0.90f),strokeWidth=w*0.04f)} }
        "moray_eel" in key || "eel" in key -> { drawArc(primary.copy(alpha=0.72f),165f,250f,false,Offset(w*0.08f,h*0.26f),Size(w*0.78f,h*0.54f),style=Stroke(width=w*0.16f)); drawOval(primary.copy(alpha=0.78f),Offset(w*0.66f,h*0.30f),Size(w*0.22f,h*0.18f)); drawLine(surface.copy(alpha=0.60f),Offset(w*0.76f,h*0.42f),Offset(w*0.88f,h*0.46f),strokeWidth=w*0.018f); drawCircle(surface,w*0.025f,Offset(w*0.78f,h*0.36f)) }
        "sea_snake" in key || "snake" in key -> { drawPath(Path().apply{moveTo(w*0.10f,h*0.58f); cubicTo(w*0.28f,h*0.20f,w*0.42f,h*0.82f,w*0.58f,h*0.44f); cubicTo(w*0.70f,h*0.18f,w*0.84f,h*0.36f,w*0.92f,h*0.30f)}, primary.copy(alpha=0.68f), style=Stroke(width=w*0.08f)); repeat(5){i->drawLine(secondary.copy(alpha=0.55f),Offset(w*(0.22f+i*0.12f),h*(0.48f+(i%2)*0.06f)),Offset(w*(0.26f+i*0.12f),h*(0.38f+(i%2)*0.06f)),strokeWidth=w*0.02f)}; drawCircle(primary.copy(alpha=0.72f),w*0.07f,Offset(w*0.91f,h*0.30f)) }
        "sunfish" in key -> { drawOval(primary.copy(alpha=0.68f),Offset(w*0.28f,h*0.20f),Size(w*0.36f,h*0.54f)); drawPath(Path().apply{moveTo(w*0.46f,h*0.20f);lineTo(w*0.54f,h*0.02f);lineTo(w*0.56f,h*0.22f);close()}, secondary.copy(alpha=0.5f)); drawPath(Path().apply{moveTo(w*0.46f,h*0.72f);lineTo(w*0.54f,h*0.96f);lineTo(w*0.56f,h*0.70f);close()}, secondary.copy(alpha=0.5f)) }
        "swordfish" in key -> { fishBody(primary.copy(alpha=0.68f), secondary.copy(alpha=0.55f), tall=0.22f, long=0.66f); drawLine(secondary,Offset(w*0.78f,h*0.49f),Offset(w*0.99f,h*0.43f),strokeWidth=w*0.025f) }
        "flying_fish" in key -> { fishBody(primary.copy(alpha=0.68f), secondary.copy(alpha=0.55f), tall=0.22f, long=0.58f); drawPath(Path().apply{moveTo(w*0.42f,h*0.40f);lineTo(w*0.22f,h*0.08f);lineTo(w*0.62f,h*0.38f);close()},secondary.copy(alpha=0.42f)) }
        "barracuda" in key -> fishBody(primary.copy(alpha=0.62f), secondary.copy(alpha=0.42f), tall=0.20f, long=0.72f)
        else -> {
            fishBody(primary.copy(alpha = 0.54f), secondary.copy(alpha = 0.42f), tall = 0.28f, long = 0.50f, stripes = 1)
        }
    }
}