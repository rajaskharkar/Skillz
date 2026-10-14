package com.kingkharnivore.skillz.ui.screen.shell.rooms.red

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import kotlin.math.ceil
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.cos
import kotlin.math.sin

enum class DinosaurBody { EARLY_LONG_NECK, SAUROPOD, THEROPOD, ORNITHOPOD, PLATED, HORNED, ARMORED, FEATHERED, OSTRICH }
data class DinosaurVisual(val body: DinosaurBody, val edgeDp: Float)

/** Presentation only. Like LandAnimalScale, compressed scale keeps small species readable. */
object RedDinosaurVisuals {
    val byName = buildMap {
        fun group(body: DinosaurBody, edge: Float, names: String) = names.split(" ").forEach { put(it,DinosaurVisual(body,edge)) }
        group(DinosaurBody.EARLY_LONG_NECK,82f,"saturnalia thecodontosaurus coloradisaurus mussaurus massospondylus")
        group(DinosaurBody.EARLY_LONG_NECK,112f,"melanorosaurus riojasaurus plateosaurus")
        group(DinosaurBody.THEROPOD,66f,"staurikosaurus chindesaurus eoraptor guaibasaurus coelophysis compsognathus")
        group(DinosaurBody.THEROPOD,102f,"liliensternus herrerasaurus dilophosaurus cryolophosaurus ceratosaurus")
        group(DinosaurBody.THEROPOD,122f,"megalosaurus yangchuanosaurus torvosaurus allosaurus baryonyx suchomimus carnotaurus rajasaurus majungasaurus")
        group(DinosaurBody.THEROPOD,142f,"acrocanthosaurus tarbosaurus carcharodontosaurus giganotosaurus spinosaurus tyrannosaurus")
        group(DinosaurBody.ORNITHOPOD,64f,"heterodontosaurus dryosaurus psittacosaurus")
        group(DinosaurBody.ORNITHOPOD,104f,"camptosaurus iguanodon pachycephalosaurus corythosaurus parasaurolophus edmontosaurus")
        group(DinosaurBody.SAUROPOD,150f,"camarasaurus mamenchisaurus apatosaurus diplodocus giraffatitan brachiosaurus patagotitan argentinosaurus")
        group(DinosaurBody.SAUROPOD,106f,"nigersaurus")
        group(DinosaurBody.PLATED,103f,"kentrosaurus stegosaurus")
        group(DinosaurBody.HORNED,78f,"protoceratops")
        group(DinosaurBody.HORNED,115f,"styracosaurus pachyrhinosaurus triceratops")
        group(DinosaurBody.ARMORED,112f,"ankylosaurus")
        group(DinosaurBody.FEATHERED,54f,"archaeopteryx microraptor")
        group(DinosaurBody.FEATHERED,83f,"oviraptor deinonychus velociraptor")
        group(DinosaurBody.FEATHERED,115f,"utahraptor yutyrannus therizinosaurus")
        group(DinosaurBody.OSTRICH,95f,"gallimimus")
        group(DinosaurBody.OSTRICH,132f,"deinocheirus")
    }
    fun get(id: String): DinosaurVisual = requireNotNull(byName[id.substringAfter('_')]) { "Missing dinosaur art: $id" }
}

@Composable
fun RedDinosaurIcon(id: String, modifier: Modifier = Modifier) {
    Box(modifier.drawWithCache {
        val bitmap = ImageBitmap(ceil(size.width).toInt().coerceAtLeast(1),ceil(size.height).toInt().coerceAtLeast(1))
        CanvasDrawScope().draw(this,layoutDirection,Canvas(bitmap),Size(bitmap.width.toFloat(),bitmap.height.toFloat())) { drawRedDinosaur(id) }
        bitmap.prepareToDraw()
        onDrawBehind { drawImage(bitmap) }
    })
}

/** The same vector anatomy renders in static cards and animated scenes, as in Land. */
internal enum class DinosaurLayer { BACK_MOTION, BODY, FRONT_MOTION, DETAILS }

fun DrawScope.drawRedDinosaur(id: String, seconds: Float = 0f, phase: Float = 0f, activity: Float = 0f) =
    drawDinosaurLayer(id,seconds,phase,activity,null)

internal fun DrawScope.drawDinosaurLayer(id: String, seconds: Float, phase: Float, activity: Float, layer: DinosaurLayer?) {
    val painter = DinosaurPainter(this,id.substringAfter('_'),RedDinosaurVisuals.get(id),seconds,phase,activity,layer)
    withTransform({ scale(size.width / 100f,size.height / 100f,Offset.Zero) }) { painter.draw() }
}

private class DinosaurPainter(val d: DrawScope,val name: String,val visual: DinosaurVisual,val time: Float,val phase: Float,val activity: Float,val layer: DinosaurLayer?) {
    private var stage = DinosaurLayer.BODY
    private val visible: Boolean get() = layer == null || layer == stage
    private val palette = listOf(Color(0xFF8D674B),Color(0xFF70836A),Color(0xFFAD8053),Color(0xFF6D8384),Color(0xFF967361),Color(0xFF7E8060))
    private val body = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    private val shade = Color(body.red*.70f,body.green*.73f,body.blue*.72f)
    private val belly = Color(0xFFDCCBA6)
    private val ink = Color(0xFF343B32)
    private val red = Color(0xFFB86448)
    private fun oval(c: Color,x: Float,y: Float,w: Float,h: Float) = if (visible) d.drawOval(c,Offset(x,y),Size(w,h)) else Unit
    private fun line(c: Color,x: Float,y: Float,xx: Float,yy: Float,w: Float=3f) = if (visible) d.drawLine(c,Offset(x,y),Offset(xx,yy),w,StrokeCap.Round) else Unit
    private fun path(c: Color,block: Path.()->Unit) = if (visible) d.drawPath(Path().apply(block),c) else Unit
    private fun curve(c: Color,w: Float,block: Path.()->Unit) = if (visible) d.drawPath(Path().apply(block),c,style=Stroke(w,cap=StrokeCap.Round)) else Unit
    private fun dot(c: Color,x: Float,y: Float,r: Float) = if (visible) d.drawCircle(c,r,Offset(x,y)) else Unit
    private fun triangle(c: Color,x: Float,y: Float,xx: Float,yy: Float,xxx: Float,yyy: Float) = path(c) { moveTo(x,y);lineTo(xx,yy);lineTo(xxx,yyy);close() }
    private fun eye(x: Float,y: Float) { dot(ink,x,y,1.15f);dot(belly,x+.25f,y-.3f,.3f) }
    private fun leg(x: Float,y: Float,back: Boolean,width: Float=5f) {
        val previous = stage
        stage = if (back) DinosaurLayer.BACK_MOTION else DinosaurLayer.FRONT_MOTION
        val offset = if(back) 3.14159f else 0f
        val swing = sin(phase+offset)*6f*activity
        val lift = cos(phase+offset).coerceAtLeast(0f)*3f*activity
        val c = if(back) shade else body
        line(c,x,y,x-2f-swing*.45f,72f,width)
        line(c,x-2f-swing*.45f,72f,x+swing,86f-lift,width*.72f)
        line(c,x+swing,86f-lift,x+swing+6f,86f-lift,width*.66f)
        line(belly,x+swing+4f,86f-lift,x+swing+7f,86f-lift,.9f)
        stage = if (back) previous else DinosaurLayer.DETAILS
    }
    private fun tail(x: Float=35f,y: Float=58f,club: Boolean=false) {
        val previous = stage
        stage = DinosaurLayer.BACK_MOTION
        val sway=sin(time*.9f)*2f
        path(body) { moveTo(x,y-7);cubicTo(17f,y-8,13f,y-2+sway,2f,y-12+sway);cubicTo(12f,y+5,29f,y+8,x+4,y+5);close() }
        if(club) oval(shade,1f,y-17+sway,13f,10f)
        stage = previous
    }
    private fun spots(x: Float,y: Float,count: Int=6) { repeat(count) { i ->
        val size=if(name.hashCode()%2==0) 2.2f else 1.5f
        oval(shade.copy(alpha=.7f),x+(i%4)*8f,y+(i/4)*6f,size*1.4f,size)
    } }
    fun draw() {
        when(visual.body) {
            DinosaurBody.SAUROPOD, DinosaurBody.EARLY_LONG_NECK -> longNeck()
            DinosaurBody.PLATED, DinosaurBody.HORNED, DinosaurBody.ARMORED -> quadruped()
            else -> biped()
        }
    }
    private fun longNeck() {
        val early=visual.body==DinosaurBody.EARLY_LONG_NECK
        val high=name in setOf("brachiosaurus","giraffatitan","mamenchisaurus")
        val hx=if(high) 76f else 86f
        val hy=if(high) 11f else if(early) 30f else 24f
        tail(32f,59f)
        leg(36f,60f,true,if(early) 4f else 7f);leg(60f,60f,true,if(early) 4f else 7f)
        oval(body,22f,44f,46f,25f)
        oval(belly.copy(alpha=.52f),34f,61f,27f,6f)
        path(body) { moveTo(53f,48f);cubicTo(68f,40f,hx-10,hy+9,hx-6,hy+2);lineTo(hx+3,hy+6);cubicTo(hx-2,hy+28,76f,61f,61f,65f);close() }
        oval(body,hx-7,hy-3,if(name=="nigersaurus") 18f else 15f,9f)
        line(shade,hx+1,hy+4,hx+8,hy+4,1f);eye(hx+1,hy)
        leg(31f,63f,false,if(early) 5f else 8f)
        if(!early || name in setOf("melanorosaurus","riojasaurus")) leg( 60f,60f,false,7f)
        else { line(body,62f,49f,67f,63f,3f);line(body,67f,63f,73f, 60f,2f) }
        spots(30f,51f)
        if(name=="diplodocus" || name=="apatosaurus") repeat(7) { i -> triangle(shade,25f+i*6,45f,28f+i*6,40f,30f+i*6,45f) }
    }
    private fun quadruped() {
        val horned=visual.body==DinosaurBody.HORNED
        val armored=visual.body==DinosaurBody.ARMORED
        tail(34f,64f,armored)
        leg(38f,64f,true,7f);leg(70f,64f,true,6f)
        oval(body,22f,43f,52f,30f)
        if(visual.body==DinosaurBody.PLATED) {
            repeat(6) { i -> val x=19f+i*8;val h=if(name=="kentrosaurus" && i<3) 24f else 14f+(3-kotlin.math.abs(3-i))*3f
                path(if(i%2==0) shade else red) { moveTo(x,46f);lineTo(x-3,46-h*.7f);lineTo(x+2,46-h);lineTo(x+9,45f);close() }
            }
            repeat(2) { i -> line(belly,10f+i*6,62f,5f+i*6,48f,2f) }
            oval(body,68f,56f,16f,12f);eye(79f,59f)
        } else if(horned) {
            oval(shade,58f,32f,23f,34f);oval(body,62f,35f,16f,27f)
            oval(body,71f,49f,23f,16f);triangle(belly,91f,54f,98f,60f,90f,63f)
            if(name=="styracosaurus") repeat(6) { i -> val a=(i*26+155)*.017453f;line(belly,68f+cos(a)*8f,48f+sin(a)*12f,68f+cos(a)*19f,48f+sin(a)*22f,2.5f) }
            if(name=="triceratops") { triangle(belly,76f,49f,78f,28f,82f,49f);triangle(belly,84f, 50f,92f,33f,88f,53f) }
            if(name=="pachyrhinosaurus") oval(belly,85f,46f,10f,7f)
            else if(name!="protoceratops") triangle(belly,89f,52f,94f,40f,94f,54f)
            eye(83f,54f)
        } else {
            repeat(12) { i -> oval(if(i%2==0) belly else shade,25f+(i%6)*7,45f+(i/6)*9,6f,4f) }
            repeat(6) { i -> triangle(belly,25f+i*8,67f,28f+i*8,74f,31f+i*8,65f) }
            oval(body,69f,54f,22f,15f);eye(85f,57f)
        }
        leg(31f,65f,false,7f);leg(64f,65f,false,6f)
    }
    private fun biped() {
        val feather=visual.body==DinosaurBody.FEATHERED
        val ostrich=visual.body==DinosaurBody.OSTRICH
        val herb=visual.body==DinosaurBody.ORNITHOPOD
        val croc=name in setOf("spinosaurus","baryonyx","suchomimus")
        val big=name in setOf("tyrannosaurus","tarbosaurus","carcharodontosaurus","giganotosaurus")
        val tall=name in setOf("therizinosaurus","deinocheirus")
        val hy=if(tall) 25f else if(ostrich) 31f else 38f
        tail(34f,56f)
        leg(51f,59f,true,if(big) 8f else 5f)
        if(name=="spinosaurus" || name=="acrocanthosaurus" || name=="deinocheirus") {
            path(shade) { moveTo(24f,49f);quadraticTo(42f,if(name=="spinosaurus") 9f else 29f,65f,44f);close() }
            repeat(5) { i -> line(body,30f+i*6,45f,34f+i*5,if(name=="spinosaurus") 23f else 37f,1.2f) }
        }
        oval(body,24f,42f,43f,25f)
        oval(belly.copy(alpha=.65f),35f,58f,25f,7f)
        curve(body,if(big) 15f else 10f) { moveTo(57f,51f);quadraticTo(67f,hy+5,73f,hy+4) }
        if(feather) {
            repeat(9) { i -> curve(shade,2f) { moveTo(24f+i*4,45f);quadraticTo(19f+i*4,42f,21f+i*4,38f) } }
            if(name in setOf("archaeopteryx","microraptor")) {
                path(shade) { moveTo(53f,48f);quadraticTo(34f,63f,22f, 70f);lineTo(51f, 60f);close() }
                repeat(6) { i -> line(body,28f+i*4, 60f+i*.5f,22f+i*5,76f-i,2f) }
                if(name=="microraptor") repeat(4) { i -> line(shade, 40f+i*2, 70f,31f+i*4,81f,2f) }
            }
        }
        val headWidth=if(croc) 28f else if(big) 24f else if(ostrich||feather) 16f else 21f
        oval(body,68f,hy-3,headWidth,if(big) 17f else 12f)
        if(herb||ostrich||name=="oviraptor") triangle(shade,83f,hy+4,98f,hy+8,83f,hy+12)
        line(shade,79f,hy+8,68f+headWidth-1,hy+8,1f);eye(77f,hy+1)
        if(name=="parasaurolophus") curve(red,5f) { moveTo(75f,hy-2);quadraticTo(68f,hy-17,53f,hy-19) }
        if(name in setOf("corythosaurus","oviraptor")) oval(red,69f,hy-15,14f,15f)
        if(name=="pachycephalosaurus") oval(belly,69f,hy-10,15f,10f)
        if(name in setOf("dilophosaurus","cryolophosaurus","liliensternus")) {
            path(red) { moveTo(68f,hy);lineTo(71f,hy-12);quadraticTo(80f,hy-15,85f,hy-1);close() }
        }
        if(name in setOf("ceratosaurus","rajasaurus","majungasaurus")) triangle(belly,82f,hy-2,86f,hy-11,90f,hy-1)
        if(name=="carnotaurus") { triangle(belly, 70f,hy,68f,hy-9,76f,hy);triangle(belly,79f,hy,82f,hy-9,84f,hy) }
        val armLength=if(big||name=="carnotaurus") 7f else if(tall) 22f else 13f
        line(shade,62f, 50f,70f, 50f+armLength*.5f,3f)
        line(body, 70f,50f+armLength*.5f,74f, 50f+armLength,2.5f)
        if(name in setOf("therizinosaurus","deinocheirus","baryonyx")) repeat(3) { i -> curve(belly,1.1f) { moveTo(73f+i*2, 50f+armLength);quadraticTo( 80f+i*2, 60f+armLength,76f+i*2,65f+armLength) } }
        leg(43f, 60f,false,if(big) 9f else if(tall) 7f else 5f)
        if(name in setOf("deinonychus","utahraptor","velociraptor")) curve(ink,1.7f) { moveTo( 50f,83f);quadraticTo( 50f,75f,54f,76f) }
        if(herb && name!="heterodontosaurus" && name!="dryosaurus" && name!="psittacosaurus") leg(67f, 60f,false,4f)
        if(name=="psittacosaurus") repeat(6) { i -> line(shade,5f+i*3, 50f+i,2f+i*3, 40f+i,1f) }
        spots(29f,48f,if(feather) 4 else 8)
    }
}
