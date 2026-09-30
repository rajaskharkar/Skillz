package com.kingkharnivore.skillz.ui.screen.shell.icons.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.kingkharnivore.skillz.utils.shell.CreatureDefinition
import com.kingkharnivore.skillz.utils.shell.CreatureRenderFamily as Family
import kotlin.math.cos
import kotlin.math.sin

/** Shared species artwork for icons and scenes. Gait is supplied by the scene's single clock;
 * static icons use the exact same anatomy in a neutral stance. The art fits a 100 × 100 frame. */
fun DrawScope.drawLandCreature(
    creature: CreatureDefinition, time: Float = 0f, walkPhase: Float = 0f, activity: Float = 0f
) {
    val painter = LandPainter(this, creature, time, walkPhase, activity)
    withTransform({ scale(size.width / 100f, size.height / 100f, Offset.Zero) }) {
        painter.draw()
    }
}

private class LandPainter(
    val scope: DrawScope, val creature: CreatureDefinition, val time: Float, val phase: Float, val activity: Float
) {
    val id = creature.creatureId.removePrefix("creature_")
    val family = creature.renderFamily
    val ink = Color(0xFF35413D)
    val cream = Color(0xFFE7DDC5)
    val body = when (id) {
        "polar_bear", "arctic_fox", "snowy_owl", "mountain_goat", "sheep", "goose" -> Color(0xFFDDDCD0)
        "tiger", "orangutan", "fox", "red_panda" -> Color(0xFFB57543)
        "black_bear", "gorilla", "chimpanzee", "panda", "gibbon" -> Color(0xFF424C47)
        "elephant", "rhinoceros", "hippopotamus", "gray_wolf", "donkey" -> Color(0xFF858880)
        "crocodile", "komodo_dragon", "monitor_lizard", "iguana", "chameleon" -> Color(0xFF738064)
        "peacock" -> Color(0xFF447A7C)
        "pig" -> Color(0xFFC18F80)
        "snow_leopard" -> Color(0xFFB8BBB0)
        "horse", "wild_boar", "bison", "grizzly_bear" -> Color(0xFF80644F)
        "zebra", "cow", "okapi" -> Color(0xFFC9C1AD)
        "cheetah", "lion", "giraffe", "camel", "leopard" -> Color(0xFFBA9C66)
        "mandrill", "baboon", "macaque" -> Color(0xFF857764)
        "sloth", "koala", "wombat" -> Color(0xFF92958A)
        "tapir", "wildebeest", "buffalo", "musk_ox" -> Color(0xFF5E6256)
        else -> Color(0xFFA38A69)
    }
    val shade = Color(body.red * .76f, body.green * .78f, body.blue * .78f)
    fun oval(c: Color, x: Float, y: Float, w: Float, h: Float) = scope.drawOval(c, Offset(x,y), Size(w,h))
    fun line(c: Color, x: Float, y: Float, xx: Float, yy: Float, w: Float=3f) =
        scope.drawLine(c, Offset(x,y), Offset(xx,yy), w, StrokeCap.Round)
    fun dot(c: Color, r: Float, x: Float, y: Float) = scope.drawCircle(c,r,Offset(x,y))
    fun shape(c: Color, block: Path.()->Unit) = scope.drawPath(Path().apply(block),c)
    fun curve(c: Color, w: Float, block: Path.()->Unit) = scope.drawPath(Path().apply(block),c,style=Stroke(w,cap=StrokeCap.Round))
    fun tri(c:Color,x:Float,y:Float,xx:Float,yy:Float,xxx:Float,yyy:Float) = shape(c) { moveTo(x,y);lineTo(xx,yy);lineTo(xxx,yyy);close() }
    fun eye(x:Float,y:Float) { dot(ink,1.25f,x,y);dot(cream,.35f,x+.3f,y-.3f) }
    fun leg(x:Float,top:Float,width:Float,back:Boolean=false,hoof:Boolean=false) {
        val swing = sin(phase + if(back) 3.141593f else 0f) * 6f * activity
        val lift = (cos(phase + if(back) 3.141593f else 0f)).coerceAtLeast(0f)*3f*activity
        val c = if(back) shade else body
        val foot = 86f-lift
        line(c,x,top,x-swing*.45f,(top+foot)*.53f,width)
        line(c,x-swing*.45f,(top+foot)*.53f,x+swing,foot,width*.8f)
        line(if(hoof) ink else c,x+swing-1f,foot,x+swing+3f,foot,width*.8f)
    }
    fun draw() {
        when(family) {
            Family.PRIMATE -> primate()
            Family.GROUND_BIRD, Family.RAPTOR -> bird()
            Family.LAND_SNAKE, Family.SCORPION, Family.REPTILE, Family.TORTOISE -> reptile()
            else -> quadruped()
        }
    }
    fun primate() {
        val gorilla=id=="gorilla"
        val chimp=id=="chimpanzee"
        val orang=id=="orangutan"
        val gibbon=id=="gibbon"
        val ape=gorilla||chimp||orang||gibbon
        val arm=if(gorilla) 12f else if(orang) 7f else 5.5f
        val breath=sin(time*.85f)*.45f
        if(!ape) curve(body,3f) { moveTo(29f,59f);cubicTo(6f,60f,7f,25f,20f,30f) }
        leg(35f,60f,if(gorilla) 10f else 6f,true)
        leg(44f,64f,if(gorilla) 11f else 6f)
        if(gibbon||orang) {
            oval(body,36f,34f+breath,29f,35f)
            if(orang) { // Long russet hair, high shoulders and hanging arms.
                repeat(5) { tri(shade,35f+it*6,53f,38f+it*6,72f,42f+it*6,51f) }
            }
            line(shade,43f,39f,22f,62f,arm);line(shade,22f,62f,18f,82f,arm*.75f)
            line(body,60f,39f,76f,60f,arm);line(body,76f,60f,82f,84f,arm*.75f)
        } else {
            oval(body,25f,if(gorilla) 32f+breath else 43f+breath,if(gorilla) 47f else 39f,if(gorilla) 36f else 24f)
            if(gorilla) oval(Color(0xFF858E83),28f,37f,24f,20f) // Silver saddle.
            val swing=sin(phase)*4f*activity
            line(shade,59f,44f,70f-swing,68f,arm);line(shade,70f-swing,68f,72f+swing,85f,arm*.72f)
            line(body,66f,46f,77f+swing,68f,arm);line(body,77f+swing,68f,82f-swing,85f,arm*.72f)
            oval(shade,76f-swing,81f,11f,6f)
        }
        val hx=if(orang||gibbon) 53f else 68f
        val hy=if(gorilla) 30f else if(orang||gibbon) 26f else 41f
        oval(body,hx-12f,hy-9f,if(gorilla) 26f else 21f,if(gorilla) 28f else 23f)
        if(chimp) { dot(shade,5f,hx-12f,hy+3f);dot(cream,2.8f,hx-12f,hy+3f) }
        if(orang) { oval(shade,hx-13f,hy,11f,18f);oval(shade,hx+6f,hy,10f,18f) }
        val face=if(gorilla) Color(0xFF69736A) else if(chimp) Color(0xFFBAA68E) else cream.copy(alpha=.65f)
        oval(face,hx-5f,hy,16f,17f)
        if(id=="baboon"||id=="mandrill") {
            oval(shade,hx+2f,hy+7f,22f,10f)
            if(id=="mandrill") { line(Color(0xFF839CA0),hx+4f,hy+5f,hx+16f,hy+12f,5f);line(Color(0xFFA96454),hx+8f,hy+5f,hx+19f,hy+14f,2f) }
        } else oval(face,hx+3f,hy+10f,12f,7f)
        if(gorilla) { line(ink,hx-3f,hy+4f,hx+11f,hy+4f,3f);tri(body,hx-10f,hy-7f,hx-3f,hy-14f,hx+5f,hy-8f) }
        if(gibbon) oval(body,hx-2f,hy+2f,12f,11f)
        eye(hx+7f,hy+6f)
        line(ink,hx+7f,hy+14f,hx+12f,hy+14f,1f)
    }
    fun quadruped() {
        val equid=id in landAnatomyGroup1
        val camelid=id in landAnatomyGroup2
        val deer=id in landAnatomyGroup3
        val goat=id in landAnatomyGroup4
        val wool=id in landAnatomyGroup5
        val cat=family==Family.BIG_CAT
        val bear=family==Family.BEAR
        val mega=family==Family.MEGAFAUNA
        val giraffe=id=="giraffe"
        val hopping=id in landAnatomyGroup6
        val low=id in landAnatomyGroup7
        val longNeck=camelid||giraffe||id=="gerenuk"||id in landAnatomyGroup6
        val small=family==Family.SMALL_MAMMAL
        val top=when { giraffe->43f;equid||deer->42f;camelid->48f;bear||mega->45f;low->62f;hopping->49f;else->54f }
        val height=when { id=="cheetah"->15f;id=="jaguar"->24f;giraffe->19f;bear||mega->27f;wool->25f;low->17f;equid->23f;else->20f }
        val neckY=when { giraffe->15f;hopping->33f;longNeck->28f;equid->29f;deer->34f;goat->43f;else->top+1f }
        val hx=if(giraffe) 75f else if(equid) 76f else 78f
        val legWidth=when { mega->9f;bear->7f;equid->4.5f;deer->3f;cat->4.5f;else->4f }
        leg(33f,top+height-5f,legWidth,true,equid||deer||goat)
        leg(63f,top+height-5f,legWidth,true,equid||deer||goat)
        // Tail silhouettes are as important as faces at scene scale.
        when(id) {
            "squirrel" -> { curve(shade,13f) { moveTo(30f,69f);cubicTo(3f,66f,7f,25f,25f,35f);cubicTo(37f,41f,18f,44f,20f,56f) } }
            "red_panda","raccoon","lemur" -> {
                curve(body,9f) { moveTo(31f,65f);cubicTo(16f,72f,3f,67f,9f,47f) }
                repeat(4) { line(ink,8f,49f+it*5,14f,51f+it*5,2.8f) }
            }
            "fox","arctic_fox","fennec_fox" -> { shape(body) { moveTo(30f,59f);cubicTo(16f,51f,14f,69f,3f,70f);cubicTo(14f,81f,30f,78f,34f,64f);close() };tri(cream,3f,70f,12f,65f,14f,77f) }
            "rabbit","pika" -> dot(cream,5f,27f,67f)
            "pig" -> curve(body,2.5f) { moveTo(28f,59f);cubicTo(10f,47f,13f,68f,23f,57f) }
            "platypus" -> oval(shade,5f,67f,30f,11f)
            "anteater" -> { shape(shade) { moveTo(34f,53f);cubicTo(14f,49f,12f,66f,3f,82f);lineTo(34f,72f);close() } }
            else -> if(hopping) tri(body,36f,57f,5f,86f,43f,72f)
                else if(equid) curve(shade,6f) { moveTo(29f,49f);cubicTo(15f,53f,24f,71f,14f,77f) }
                else if(!bear && !low) curve(body,if(cat) 3f else 2.5f) { moveTo(29f,top+7f);cubicTo(12f,top+5f,7f,top+30f+sin(time)*2f,8f,top+18f) }
        }
        val breath=sin(time*.9f)*.35f
        oval(body,25f,top+breath,46f,height)
        if(wool) repeat(7) { dot(body,7f,30f+it*5f,top+5f+(it%2)*2f) }
        if(bear) oval(body,49f,top-6f,25f,29f)
        if(id in landAnatomyGroup8) { oval(shade,48f,top-9f,27f,32f);tri(shade,50f,top+20f,60f,79f,71f,top+15f) }
        if(id=="camel") { shape(body) { moveTo(28f,top+7f);cubicTo(30f,top-26f,47f,top-27f,54f,top+4f);close() } }
        if(longNeck||equid||deer||goat) {
            shape(body) { moveTo(57f,top+height-3f);lineTo(hx-13f,neckY+2f);lineTo(hx-3f,neckY);lineTo(75f,top+13f);close() }
        }
        if(equid) curve(shade,4f) { moveTo(hx-13f,neckY+1f);quadraticTo(61f,32f,60f,52f) }
        if(hopping) { oval(body,34f,55f,24f,24f);line(body,56f,55f,68f,64f,4f);line(body,68f,64f,70f,71f,3f) }
        else { leg(29f,top+height-4f,legWidth,false,equid||deer||goat);leg(64f,top+height-4f,legWidth,false,equid||deer||goat) }
        if(hopping) { line(body,43f,68f,35f,81f,8f);line(body,35f,81f,55f,86f,5f) }
        markings(top,height)
        val nod=(1f-activity)*sin(time*.6f)*1.5f
        scope.withTransform({ rotate(nod,Offset(66f,top+8f)) }) {
            if(id=="lion") oval(shade,62f,top-11f,31f,34f)
            if(equid) {
                // Long, sloped face and narrower muzzle, rather than a round generic head.
                shape(body) { moveTo(69f,neckY-1f);quadraticTo(81f,neckY-5f,83f,neckY+4f);lineTo(94f,neckY+18f);quadraticTo(97f,neckY+27f,86f,neckY+24f);lineTo(71f,neckY+12f);close() }
                oval(shade,86f,neckY+18f,9f,7f)
                if(id=="horse") shape(cream) { moveTo(80f,neckY+3f);lineTo(89f,neckY+19f);lineTo(86f,neckY+18f);lineTo(77f,neckY+5f);close() }
            } else if(mega && !giraffe) {
                oval(body,65f,top-2f,27f,25f)
                if(id=="hippopotamus") { oval(body,75f,top+7f,23f,18f);dot(shade,2f,94f,top+13f);dot(body,3f,76f,top-2f) }
                if(id=="elephant") { oval(shade,60f,top-3f,19f,25f);curve(body,8f) { moveTo(87f,top+12f);cubicTo(89f,77f,98f,85f,94f,69f) };curve(cream,2.5f) { moveTo(82f,top+19f);quadraticTo(89f,top+27f,91f,top+18f) } }
                if(id=="rhinoceros") { tri(cream,85f,top+12f,90f,top-6f,94f,top+14f);tri(cream,79f,top+9f,81f,top+1f,86f,top+11f);line(shade,52f,top+1f,56f,top+22f,1.5f) }
            } else {
                oval(body,66f,neckY-2f,if(cat||bear||id=="moose") 24f else 19f,if(cat||bear) 21f else 17f)
                val muzzle=when { id=="moose"->18f;id=="anteater"->21f;id=="platypus"->21f;cat->12f;else->15f }
                oval(if(cat) cream else if(id=="platypus") shade else body,78f,neckY+8f,muzzle,if(id=="moose") 13f else 8f)
                if(id=="tapir") curve(body,5f) { moveTo(87f,neckY+10f);quadraticTo(97f,neckY+12f,91f,neckY+20f) }
            }
            ears(neckY,cat,bear,equid)
            horns(neckY)
            if(goat) tri(shade,81f,neckY+13f,78f,neckY+26f,86f,neckY+13f)
            if(id=="moose") tri(shade,70f,neckY+15f,73f,neckY+31f,78f,neckY+16f)
            if(id in landAnatomyGroup9) curve(cream,3f) { moveTo(87f,neckY+15f);quadraticTo(96f,neckY+21f,93f,neckY+7f) }
            if(id in landAnatomyGroup10) { oval(if(id=="red_panda") cream else ink,69f,neckY+1f,16f,8f); if(id=="badger") line(cream,69f,neckY,84f,neckY+11f,3f) }
            if(id=="koala") oval(ink,80f,neckY+4f,7f,11f)
            if(id=="sloth") { oval(cream,68f,neckY,21f,15f);line(shade,71f,neckY+4f,82f,neckY+6f,3f);repeat(3) { line(cream,65f+it*2f,82f,66f+it*2f,87f,1f) } }
            if(id=="cheetah") line(ink,82f,neckY+5f,86f,neckY+13f,1.3f)
            if(cat&&id=="tiger") repeat(3) { line(ink,69f+it*4f,neckY+1f,72f+it*4f,neckY+5f,1.8f) }
            eye(if(equid) 80f else 83f,neckY+5f)
            if(!mega||giraffe) dot(ink,1.4f,if(equid) 93f else 92f,neckY+12f)
        }
    }
    fun ears(y:Float,cat:Boolean,bear:Boolean,equid:Boolean) {
        if(id=="elephant"||id=="hippopotamus") return
        val long=id in landAnatomyGroup11
        if(cat||bear||id in landAnatomyGroup12) {
            val r=if(id=="koala") 7f else 4f
            dot(shade,r,71f,y);dot(body,r,82f,y);dot(cream,r*.48f,82f,y)
            if(id in landAnatomyGroup13) { line(ink,71f,y-3f,69f,y-10f,1.5f);line(ink,82f,y-3f,84f,y-10f,1.5f) }
        } else {
            val length=if(long) 21f else if(equid) 12f else 8f
            tri(shade,69f,y+3f,66f,y-length,76f,y+1f)
            tri(body,77f,y+2f,82f,y-length,85f,y+5f)
            if(long) line(cream,80f,y-2f,81f,y-length+5f,2f)
        }
    }
    fun horns(y:Float) {
        when(id) {
            "moose" -> repeat(2) { n -> val x=67f+n*16f;line(shade,x,y,x-3f,y-15f,3f);oval(shade,x-12f,y-23f,18f,11f);repeat(4) { i->line(shade,x-11f+i*5,y-17f,x-13f+i*5,y-28f,2f) } }
            "deer","reindeer" -> repeat(2) { n -> val x=68f+n*12;curve(shade,2f) { moveTo(x,y);quadraticTo(x-12f,y-17f,x-6f,y-29f) };line(shade,x-7f,y-13f,x-16f,y-20f,1.7f);line(shade,x-9f,y-19f,x,y-26f,1.7f) }
            "sheep","bighorn_sheep" -> { scope.drawArc(shade,0f,310f,false,Offset(65f,y-10f),Size(17f,19f),style=Stroke(if(id=="bighorn_sheep") 5f else 3.5f));dot(body,3f,73f,y) }
            "ibex","goat","mountain_goat","tahr","chamois" -> repeat(2) { n -> val x=72f+n*8f;curve(shade,if(id=="ibex") 3.5f else 2.5f) { moveTo(x,y);quadraticTo(x-2f,y-26f,if(id=="ibex") x-21f else x-12f,y-23f) } }
            "oryx","gazelle","gerenuk","addax" -> repeat(2) { n -> val x=72f+n*8f;curve(shade,2f) { moveTo(x,y);cubicTo(x-7f,y-12f,x+3f,y-18f,x-7f,y-31f) } }
            "cow","buffalo","bison","wildebeest","yak","musk_ox","takin" -> { curve(cream,3.5f) { moveTo(73f,y);quadraticTo(58f,y+1f,62f,y-13f) };curve(cream,3f) { moveTo(81f,y);quadraticTo(94f,y+1f,91f,y-12f) } }
            "giraffe" -> { line(shade,73f,y,70f,y-8f,2.5f);dot(shade,2f,70f,y-9f);line(shade,80f,y,80f,y-8f,2.5f);dot(shade,2f,80f,y-9f) }
        }
    }
    fun markings(top:Float,height:Float) {
        when(id) {
            "tiger","zebra" -> repeat(6) { i -> tri(ink,29f+i*6f,top+2f,34f+i*6f,top+2f,30f+i*6f,top+height-3f) }
            "okapi" -> repeat(4) { i -> line(cream,25f,68f+i*4f,34f,68f+i*4f,1.7f);line(cream,60f,68f+i*4f,67f,68f+i*4f,1.7f) }
            "leopard","jaguar","snow_leopard" -> repeat(12) { i -> scope.drawCircle(shade,if(id=="jaguar") 2.6f else 2f,Offset(30f+(i%6)*6.5f,top+6f+(i/6)*9f),style=Stroke(1.1f)) }
            "cheetah","spotted_hyena" -> repeat(15) { i -> dot(shade,1.5f,29f+(i%7)*6f,top+5f+(i/7)*6f) }
            "giraffe" -> { repeat(10) { i -> oval(shade,30f+(i%5)*8f,top+3f+(i/5)*8f,5f,6f) };repeat(4) { i -> oval(shade,65f,24f+i*7f,5f,4f) } }
            "cow" -> { oval(shade,31f,top+2f,16f,18f);oval(shade,53f,top+9f,14f,12f) }
            "panda" -> { oval(cream,29f,top,30f,height);oval(cream,62f,top-4f,25f,23f) }
            "anteater" -> { tri(ink,48f,top,68f,top+height,38f,top+height);line(cream,48f,top,68f,top+height,2f) }
            "porcupine","hedgehog","echidna" -> repeat(15) { i -> line(shade,26f+i*2.7f,top+8f,22f+i*3.1f,top-(if(id=="porcupine") 13f else 6f)+(i%3)*3f,1.6f) }
            "pangolin","armadillo" -> repeat(6) { i -> curve(shade,1.5f) { moveTo(29f+i*6f,top+1f);quadraticTo(36f+i*6f,top+10f,29f+i*6f,top+height) } }
        }
    }
    fun bird() {
        val tall=id in landAnatomyGroup14
        val long=id in landAnatomyGroup15||tall
        val owl=id=="snowy_owl"
        val raptor=family==Family.RAPTOR
        val y=if(tall) 25f else if(long) 36f else 45f
        leg(43f,65f,2.5f,true);leg(57f,65f,2.5f)
        if(id=="peacock") {
            repeat(7) { i -> val x=8f+i*5f;oval(Color(0xFF557B62),x,23f+(i-3)*(i-3),12f,45f);dot(Color(0xFFB1A565),3.4f,x+6f,31f+(i-3)*(i-3));dot(body,1.8f,x+6f,31f+(i-3)*(i-3)) }
        } else if(id=="turkey") repeat(6) { i -> oval(shade,8f+i*5f,31f+(i-3)*(i-3),11f,34f) }
        else if(id in landAnatomyGroup16) tri(shade,34f,58f,2f,43f,30f,69f)
        else tri(shade,32f,52f,12f,43f,23f,68f)
        oval(body,25f,if(owl) 37f else 48f,45f,if(owl) 39f else 24f)
        oval(shade,30f,51f+sin(time)*.5f,29f,17f)
        repeat(3) { i -> line(body.copy(alpha=.5f),34f+i*6f,55f,39f+i*6f,65f,1.2f) }
        curve(body,if(tall) 6f else 10f) { moveTo(62f,57f);quadraticTo(74f,52f,70f,y) }
        oval(if(id=="eagle") cream else body,63f,y-7f,20f,19f)
        val beak=Color(0xFFB79962)
        if(id=="duck"||id=="goose") oval(beak,79f,y+2f,16f,6f)
        else if(raptor) { tri(beak,80f,y,92f,y+5f,86f,y+13f) }
        else tri(beak,81f,y,94f,y+5f,81f,y+7f)
        if(id=="chicken") { repeat(3) { dot(Color(0xFFAD6053),3f,67f+it*5f,y-8f) };oval(Color(0xFFAD6053),78f,y+7f,5f,8f) }
        if(id=="duck") { oval(Color(0xFF4C7366),63f,y-7f,20f,19f);line(cream,65f,y+11f,77f,y+11f,2f) }
        if(id=="vulture") { oval(Color(0xFFB69A87),64f,y-7f,18f,18f);oval(cream,59f,y+10f,17f,7f) }
        if(id=="peacock") repeat(3) { line(body,71f,y-6f,66f+it*5f,y-16f,1.5f);dot(body,2f,66f+it*5f,y-16f) }
        if(owl) { oval(cream,49f,33f,33f,27f);oval(body,52f,37f,13f,15f);oval(body,66f,37f,13f,15f);eye(59f,43f);eye(72f,43f);tri(beak,63f,47f,66f,54f,69f,47f) }
        else eye(76f,y)
    }
    fun reptile() {
        val wave=sin(time*.8f)*activity
        if(family==Family.LAND_SNAKE) {
            curve(body,9f) { moveTo(9f,77f);cubicTo(3f,45f+wave*4,58f,91f,51f,61f);cubicTo(44f,39f,72f,40f,82f,52f) }
            repeat(6) { i -> line(shade,13f+i*5,69f,15f+i*5,74f,2f) }
            oval(body,77f,46f,18f,12f);eye(89f,49f);repeat(3) { i -> dot(cream,2.5f,8f+i*2f,78f+i) }
            return
        }
        if(family==Family.SCORPION) {
            repeat(4) { i -> val x=32f+i*9f;line(shade,x,63f,x-8f,72f+wave,2f);line(shade,x-8f,72f+wave,x-5f,81f,2f) }
            oval(body,27f,53f,43f,16f)
            curve(body,5f) { moveTo(30f,57f);cubicTo(5f,59f,10f,20f,33f,30f) };tri(shade,30f,27f,36f,37f,38f,29f)
            line(body,64f,58f,78f,45f,3f);line(body,65f,64f,82f,74f,3f)
            scope.drawArc(body,30f,290f,false,Offset(75f,35f),Size(13f,15f),style=Stroke(4f))
            scope.drawArc(body,30f,290f,false,Offset(79f,67f),Size(13f,15f),style=Stroke(4f));return
        }
        val tortoise=family==Family.TORTOISE
        val croc=id=="crocodile"
        val cham=id=="chameleon"
        repeat(2) { i -> val x=34f+i*28f;val step=sin(phase+i*3.14f)*4f*activity;line(shade,x,61f,x-6f+step,73f,5f);line(shade,x-6f+step,73f,x+2f+step,79f,3f) }
        if(cham) curve(body,5f) { moveTo(30f,62f);cubicTo(0f,48f,8f,87f,22f,72f);cubicTo(28f,64f,15f,59f,16f,69f) }
        else shape(body) { moveTo(33f,55f);quadraticTo(20f,70f+wave*3,3f,77f);quadraticTo(24f,77f,41f,64f);close() }
        oval(body,24f,52f,49f,20f)
        if(tortoise) { oval(Color(0xFF8B8964),23f,37f,48f,34f);repeat(4) { i->curve(shade,1.7f) { moveTo(29f+i*10f,43f);quadraticTo(40f+i*8f,51f,29f+i*10f,65f) } };line(shade,28f,55f,66f,55f,1.5f) }
        else if(id=="iguana"||cham||croc) repeat(7) { i->tri(shade,26f+i*6f,54f,29f+i*6f,if(cham) 43f else 47f,33f+i*6f,54f) }
        oval(body,65f,if(cham) 44f else 53f,if(croc) 33f else 25f,if(cham) 22f else 13f)
        if(croc) { line(shade,75f,62f,96f,62f,1.2f);repeat(5) { i->dot(cream,.9f,77f+i*4f,61f) } }
        if(cham) { dot(cream,4f,82f,50f);dot(body,2.5f,82f,50f) }
        eye(if(croc) 75f else 82f,if(cham) 50f else 57f)
    }
}

// Immutable anatomy groups are shared by every frame and icon.
private val landAnatomyGroup1 = setOf("horse","donkey","zebra","okapi")
private val landAnatomyGroup2 = setOf("camel","alpaca","llama","vicuna")
private val landAnatomyGroup3 = setOf("deer","reindeer","moose","gazelle","gerenuk","oryx","addax","chamois","ibex")
private val landAnatomyGroup4 = setOf("goat","mountain_goat","tahr","takin")
private val landAnatomyGroup5 = setOf("sheep","bighorn_sheep","alpaca","musk_ox","yak")
private val landAnatomyGroup6 = setOf("kangaroo","wallaby","jerboa")
private val landAnatomyGroup7 = setOf("badger","wolverine","hedgehog","echidna","porcupine","armadillo","pangolin","wombat","platypus","capybara")
private val landAnatomyGroup8 = setOf("bison","yak","musk_ox","wildebeest")
private val landAnatomyGroup9 = setOf("wild_boar","warthog")
private val landAnatomyGroup10 = setOf("badger","raccoon","red_panda","panda")
private val landAnatomyGroup11 = setOf("rabbit","donkey","jerboa","kangaroo","wallaby","fennec_fox")
private val landAnatomyGroup12 = setOf("koala","pika","capybara","wombat")
private val landAnatomyGroup13 = setOf("caracal","lynx","bobcat")
private val landAnatomyGroup14 = setOf("ostrich","emu")
private val landAnatomyGroup15 = setOf("goose","roadrunner")
private val landAnatomyGroup16 = setOf("pheasant","roadrunner")
