package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import kotlin.math.*

/** Species art direction is explicit, independent of catalogue order and theme accent colors. */
internal enum class BotanicalForm {
    DAISY, CUP, SPIRE, ROSE, CLUSTER, SUNFLOWER, WISTERIA, TREE, CONIFER, FERN,
    MONSTERA, BROMELIAD, ORCHID, HELICONIA, BANANA, PITCHER, TITAN, RAFFLESIA,
    REED, CATTAIL, PAPYRUS, LILY, LOTUS, ROSETTE, JADE, PRICKLY_PEAR, BARREL,
    SAGUARO, WELWITSCHIA, MOSS
}
internal data class BotanicalAppearance(val form: BotanicalForm, val flower: Color, val foliage: Color = Color(0xFF398552))
internal object BotanicalIllustrations {
    private fun style(form: BotanicalForm, flower: Long, foliage: Long = 0xFF398552) = BotanicalAppearance(form, Color(flower), Color(foliage))
    val species = mapOf(
        "daisy" to style(BotanicalForm.DAISY, 0xFFFFF7DE),
        "tulip" to style(BotanicalForm.CUP, 0xFFF65D79),
        "lavender" to style(BotanicalForm.SPIRE, 0xFFA482E2, 0xFF679078),
        "rose" to style(BotanicalForm.ROSE, 0xFFE6446D),
        "hydrangea" to style(BotanicalForm.CLUSTER, 0xFF899DEC),
        "sunflower" to style(BotanicalForm.SUNFLOWER, 0xFFFFC83D),
        "wisteria" to style(BotanicalForm.WISTERIA, 0xFFB89BEF),
        "peony" to style(BotanicalForm.ROSE, 0xFFFFACC6),
        "cherry_blossom" to style(BotanicalForm.TREE, 0xFFFFB6D1, 0xFF77A571),
        "magnolia" to style(BotanicalForm.TREE, 0xFFFFEFDC, 0xFF467456),
        "fern" to style(BotanicalForm.FERN, 0xFFD2D975),
        "birch" to style(BotanicalForm.TREE, 0xFFCBE88E, 0xFF84B84C),
        "aspen" to style(BotanicalForm.TREE, 0xFFFFD057, 0xFFD5AA37),
        "maple" to style(BotanicalForm.TREE, 0xFFFFB64B, 0xFFD15E36),
        "willow" to style(BotanicalForm.TREE, 0xFFD4DB73, 0xFF82A858),
        "oak" to style(BotanicalForm.TREE, 0xFFC59B56, 0xFF4D8C53),
        "banyan" to style(BotanicalForm.TREE, 0xFFE5A85B, 0xFF387F57),
        "rainbow_eucalyptus" to style(BotanicalForm.TREE, 0xFFDA8CCC, 0xFF7FAE98),
        "redwood" to style(BotanicalForm.CONIFER, 0xFFAD683C, 0xFF326D59),
        "giant_sequoia" to style(BotanicalForm.CONIFER, 0xFFB16A43, 0xFF397658),
        "monstera" to style(BotanicalForm.MONSTERA, 0xFFCEDD8B, 0xFF267C62),
        "bromeliad" to style(BotanicalForm.BROMELIAD, 0xFFFF5C6D, 0xFF549860),
        "orchid" to style(BotanicalForm.ORCHID, 0xFFE69CFA),
        "heliconia" to style(BotanicalForm.HELICONIA, 0xFFFF613A),
        "cacao" to style(BotanicalForm.TREE, 0xFFE7AB43, 0xFF337A4E),
        "banana" to style(BotanicalForm.BANANA, 0xFFFFD454, 0xFF62A64C),
        "pitcher_plant" to style(BotanicalForm.PITCHER, 0xFFB95174, 0xFF779A4A),
        "corpse_flower" to style(BotanicalForm.TITAN, 0xFF9D3059, 0xFF7D9B4D),
        "rafflesia" to style(BotanicalForm.RAFFLESIA, 0xFFCB553E, 0xFF5B783E),
        "kapok" to style(BotanicalForm.TREE, 0xFFF0DD9E, 0xFF458F59),
        "reed" to style(BotanicalForm.REED, 0xFFDBC596, 0xFF65976F),
        "cattail" to style(BotanicalForm.CATTAIL, 0xFF79503C, 0xFF56865B),
        "papyrus" to style(BotanicalForm.PAPYRUS, 0xFFA7C36D, 0xFF589765),
        "waterlily" to style(BotanicalForm.LILY, 0xFFFFEBEF, 0xFF4B9F80),
        "lotus" to style(BotanicalForm.LOTUS, 0xFFFF9FC9, 0xFF6EAE8E),
        "mangrove" to style(BotanicalForm.TREE, 0xFFEDCF8E, 0xFF337F63),
        "bald_cypress" to style(BotanicalForm.CONIFER, 0xFFE7A066, 0xFF9CA25B),
        "pickerelweed" to style(BotanicalForm.SPIRE, 0xFF9893ED, 0xFF3D9278),
        "giant_water_lily" to style(BotanicalForm.LILY, 0xFFFAF3DC, 0xFF67A174),
        "victoria_lily" to style(BotanicalForm.LILY, 0xFFFFABD5, 0xFF69996A),
        "aloe" to style(BotanicalForm.ROSETTE, 0xFFFF9565, 0xFF7AAD8E),
        "jade" to style(BotanicalForm.JADE, 0xFFFFDAE3, 0xFF56A77D),
        "yucca" to style(BotanicalForm.ROSETTE, 0xFFFFF2D5, 0xFF6E9669),
        "agave" to style(BotanicalForm.ROSETTE, 0xFFD0CE98, 0xFF78B3AD),
        "prickly_pear" to style(BotanicalForm.PRICKLY_PEAR, 0xFFF18B73, 0xFF71A580),
        "barrel_cactus" to style(BotanicalForm.BARREL, 0xFFFFD26D, 0xFF85A56A),
        "saguaro" to style(BotanicalForm.SAGUARO, 0xFFFFF2CC, 0xFF679B79),
        "welwitschia" to style(BotanicalForm.WELWITSCHIA, 0xFFB47D4D, 0xFF88A184),
        "dragons_blood_tree" to style(BotanicalForm.TREE, 0xFFD47A50, 0xFF557F62),
        "baobab" to style(BotanicalForm.TREE, 0xFFEAD6A5, 0xFF8AAB65),
        "moss" to style(BotanicalForm.MOSS, 0xFFBACE69, 0xFF76A654),
        "heather" to style(BotanicalForm.SPIRE, 0xFFD48CBC, 0xFF578970),
        "edelweiss" to style(BotanicalForm.DAISY, 0xFFFFFCE9, 0xFF9BAF9D),
        "rhododendron" to style(BotanicalForm.CLUSTER, 0xFFE987B0, 0xFF417B69),
        "juniper" to style(BotanicalForm.CONIFER, 0xFF82BDE0, 0xFF527E75),
        "larch" to style(BotanicalForm.CONIFER, 0xFFEBC75F, 0xFFB6B05B),
        "fir" to style(BotanicalForm.CONIFER, 0xFFACA0B8, 0xFF2D6E65),
        "snow_lotus" to style(BotanicalForm.CUP, 0xFFF0F2CD, 0xFFA0B2A4),
        "himalayan_cedar" to style(BotanicalForm.CONIFER, 0xFFAD9065, 0xFF528778),
        "bristlecone_pine" to style(BotanicalForm.CONIFER, 0xFFC3956E, 0xFF718F65)
    )
}

private val bark = Color(0xFF855F48)
private val gold = Color(0xFFF2C44B)
private fun Color.shade(amount: Float) = lerp(this, if (amount > 0) Color.White else Color(0xFF092D27), abs(amount))
private fun DrawScope.strokePath(path: Path, color: Color, width: Float) = drawPath(path, color, style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round))
private fun DrawScope.stem(x: Float, y: Float, endX: Float, endY: Float, width: Float, color: Color) {
    val p = Path().apply { moveTo(x,y); cubicTo(x-8,y-35,endX+8,endY+30,endX,endY) }
    strokePath(p,color.shade(-.25f),width+1); strokePath(p,color,width)
}
private fun DrawScope.leaf(x: Float,y: Float, length: Float, width: Float, angle: Float, color: Color, lobed: Boolean=false) {
    rotate(angle,Offset(x,y)) {
        val p=Path().apply { moveTo(x,y); cubicTo(x-width,y-length*.4f,x-width*.6f,y-length*.9f,x,y-length)
            cubicTo(x+width*.8f,y-length*.8f,x+width,y-length*.3f,x,y); close() }
        drawPath(p,Brush.linearGradient(listOf(color.shade(-.25f),color.shade(.28f)),Offset(x-width,y),Offset(x+width,y-length)))
        drawLine(color.shade(.45f).copy(alpha=.6f),Offset(x,y),Offset(x,y-length*.9f),.7f)
        if(lobed) repeat(4) { n -> val yy=y-length*(.2f+n*.15f)
            drawLine(color.shade(-.4f),Offset(x,yy),Offset(x+width*.65f,yy-length*.06f),1.8f)
            drawLine(color.shade(-.4f),Offset(x,yy),Offset(x-width*.65f,yy-length*.06f),1.8f) }
    }
}
private fun DrawScope.bloom(x: Float,y: Float,r: Float,color: Color,petals: Int=8,layers: Int=1,center: Color=gold) {
    repeat(layers) { layer ->
        val radius=r*(1f-layer*.23f)
        repeat(petals) { n ->
            rotate(n*360f/petals+layer*19f,Offset(x,y)) {
                drawOval(Brush.linearGradient(listOf(color.shade(.35f),color.shade(-.08f)),Offset(x,y-radius),Offset(x,y)),
                    Offset(x-radius*.34f,y-radius),Size(radius*.68f,radius*1.12f))
            }
        }
    }
    drawCircle(center,r*.23f,Offset(x,y)); drawCircle(center.shade(.35f),r*.1f,Offset(x-r*.045f,y-r*.04f))
}

/** Normalized botanical illustration. Level 99 receives the same adult stage index as 90. */
internal fun DrawScope.drawBotanicalPlant(id: String, stage: Int) {
    val appearance=BotanicalIllustrations.species.getValue(id)
    val fit=min(size.width/200f,size.height/240f)
    withTransform({ translate((size.width-200f*fit)/2,(size.height-240f*fit)/2); scale(fit,fit,Offset.Zero) }) {
        val leaf=appearance.foliage; val flower=appearance.flower
        drawOval(Color(0xFF173E37).copy(alpha=.15f),Offset(27f,213f),Size(146f,15f))
        if(stage==0) {
            stem(100f,217f,99f,180f,2.5f,leaf)
            leaf(100f,196f,24f,10f,-58f,leaf); leaf(101f,194f,28f,11f,54f,leaf.shade(.12f))
            drawOval(bark,Offset(94f,212f),Size(12f,6f))
        } else {
            val height=65f+stage*22f
            val top=216f-height
            val fullness=(stage+1)/7f
            when(appearance.form) {
                BotanicalForm.TREE, BotanicalForm.CONIFER -> tree(id,stage,top,leaf,flower,appearance.form==BotanicalForm.CONIFER)
                BotanicalForm.FERN -> fern(stage,leaf)
                BotanicalForm.MOSS -> repeat(12+stage*5) { n ->
                    val x=28f+(n*29%145); val y=204f+(n*13%18)
                    drawCircle(leaf.shade((n%5-2)*.1f),7f+stage,Offset(x,y))
                    if(n%4==0) { stem(x,y,x+4,y-14,1f,leaf);drawCircle(flower,2f,Offset(x+4,y-14)) }
                }
                BotanicalForm.LILY, BotanicalForm.LOTUS -> {
                    val giant=id in setOf("giant_water_lily","victoria_lily")
                    repeat(if(giant) 1+stage/3 else 1+stage/2) { n ->
                        val x=if(n==0) 100f else 48f+n*34;val y=207f-n*11
                        val w=if(giant) 120f*fullness else 68f*fullness
                        drawOval(leaf.shade(-.25f),Offset(x-w/2,y-16),Size(w,31f))
                        drawOval(leaf.shade(.1f),Offset(x-w/2+2,y-19),Size(w-4,28f))
                        repeat(9) { k -> val a=k*PI*2/9;drawLine(leaf.shade(-.12f),Offset(x,y-5),Offset(x+cos(a).toFloat()*w*.46f,y-5+sin(a).toFloat()*12),.8f) }
                        if(giant) drawOval(Color(0xFFBB7284),Offset(x-w/2,y-19),Size(w,29f),style=Stroke(2.2f))
                    }
                    if(stage>=3) {
                        val y=if(appearance.form==BotanicalForm.LOTUS) 126f else 181f
                        if(appearance.form==BotanicalForm.LOTUS) stem(103f,213f,103f,y,3f,leaf)
                        bloom(103f,y,13f+stage*2.2f,flower,10,3)
                    }
                }
                BotanicalForm.ROSETTE, BotanicalForm.BROMELIAD, BotanicalForm.WELWITSCHIA -> {
                    if(appearance.form==BotanicalForm.WELWITSCHIA) {
                        // Two persistent ribbon leaves, splitting and curling with age.
                        drawOval(bark,Offset(83f,198f),Size(34f,20f))
                        for(side in listOf(-1,1)) repeat(1+stage/2) { n ->
                            val p=Path().apply { moveTo(100f,207f);cubicTo(100f+side*45,170f-n*5,100f+side*90,230f,100f+side*(42+n*10),205f-n*3) }
                            strokePath(p,leaf.shade(n*.07f),7f+stage)
                        }
                    } else {
                        repeat(4+stage*2) { n -> leaf(100f,216f,45f+stage*7f+(n%3)*9,if(id=="yucca") 5f else 10f+stage,n*150f%(140f)-70f,leaf.shade((n%3-1)*.16f)) }
                        if(stage>=3 && appearance.form==BotanicalForm.BROMELIAD) {
                            stem(100f,212f,100f,top+24,3f,leaf)
                            repeat(stage+1) { n -> leaf(100f,top+63-n*7,29f,10f,if(n%2==0) -45f else 45f,flower.shade(n*.025f)) }
                        }
                        if(stage>=5 && id in setOf("aloe","yucca")) {
                            stem(107f,211f,110f,30f,2.5f,leaf)
                            repeat(10) { n -> bloom(110f+(if(n%2==0) -7 else 7),40f+n*7,4f,flower,5) }
                        }
                    }
                }
                BotanicalForm.SAGUARO, BotanicalForm.BARREL, BotanicalForm.PRICKLY_PEAR, BotanicalForm.JADE -> succulent(id,stage,top,leaf,flower)
                BotanicalForm.REED, BotanicalForm.CATTAIL, BotanicalForm.PAPYRUS -> {
                    repeat(3+stage) { n ->
                        val x=63f+n*10; val y=top+(n*17%44)
                        stem(100f+(n-3)*4,217f,x,y,2.5f,leaf)
                        leaf(98f,215f,65f+stage*7,4f,(n-3)*14f,leaf.shade(.08f))
                        if(appearance.form==BotanicalForm.CATTAIL && stage>=3) drawRoundRect(flower,Offset(x-4,y),Size(8f,28f),androidx.compose.ui.geometry.CornerRadius(4f))
                        if(appearance.form==BotanicalForm.REED && stage>=3) repeat(7) { k -> leaf(x,y+18-k*3,11f,2f,if(k%2==0) -48f else 48f,flower) }
                        if(appearance.form==BotanicalForm.PAPYRUS) repeat(12) { k -> leaf(x,y,20f+stage*2,1.8f,k*30f,leaf.shade(.1f)) }
                    }
                }
                BotanicalForm.MONSTERA, BotanicalForm.BANANA -> {
                    repeat(2+stage) { n ->
                        val x=100f+(if(n%2==0) -1 else 1)*(22+n%3*12)
                        val y=top+20+(n*29%65)
                        stem(100f,218f,x,y+28,3f,leaf.shade(-.2f))
                        leaf(x,y+32,40f+stage*5,if(appearance.form==BotanicalForm.MONSTERA) 23f else 14f,(if(n%2==0) -1 else 1)*28f,leaf.shade((n%3)*.08f),true)
                    }
                    if(appearance.form==BotanicalForm.BANANA && stage>=5) repeat(7) { n ->
                        drawArc(flower,-20f,135f,false,Offset(104f+n%2*8,127f+n*5),Size(19f,24f),style=Stroke(5f,cap=StrokeCap.Round)) }
                }
                BotanicalForm.PITCHER -> {
                    repeat(2+stage) { n ->
                        val x=47f+n*17;val y=120f+(n%3)*17
                        leaf(100f,216f,70f+stage*5,11f,(n-3)*18f,leaf)
                        stem(100f,208f,x,y,1.3f,leaf)
                        if(stage>=3) {
                            val p=Path().apply {moveTo(x-8,y);cubicTo(x-14,y+53,x+17,y+53,x+7,y);close()}
                            drawPath(p,Brush.verticalGradient(listOf(flower.shade(.4f),flower.shade(-.35f)),y,y+43))
                            drawOval(flower.shade(.5f),Offset(x-10,y-3),Size(20f,8f))
                            drawOval(flower.shade(-.65f),Offset(x-7,y-1),Size(14f,4f));leaf(x,y-1,13f,10f,-20f,leaf)
                        }
                    }
                }
                BotanicalForm.TITAN -> {
                    if(stage<4) { stem(100f,218f,100f,top+25,4f,leaf);repeat(5) { n -> leaf(100f,top+40,45f,13f,(n-2)*42f,leaf) } }
                    else {
                        stem(100f,218f,100f,155f,12f,leaf)
                        val p=Path().apply {moveTo(100f,187f);cubicTo(48f,165f,28f,119f,42f,99f);quadraticTo(100f,131f,161f,97f);cubicTo(168f,133f,145f,177f,100f,187f);close()}
                        drawPath(p,Brush.horizontalGradient(listOf(flower.shade(.15f),flower.shade(-.4f),flower.shade(.2f)),40f,165f))
                        repeat(9) { n ->drawLine(flower.shade(.35f),Offset(100f,184f),Offset(44f+n*14,103f+sin(n.toFloat())*8),1.2f) }
                        val spadix=Path().apply {moveTo(89f,163f);cubicTo(87f,102f,94f,29f,101f,25f);cubicTo(113f,35f,111f,122f,109f,164f);close()}
                        drawPath(spadix,Brush.horizontalGradient(listOf(Color(0xFFBDA65D),Color(0xFFEEE2AB)),88f,112f))
                    }
                }
                BotanicalForm.RAFFLESIA -> {
                    // A flower at ground level: no invented stalk or leafy shrub.
                    val r=20f+stage*9f
                    if(stage<3) drawOval(flower.shade(-.4f),Offset(75f,170f),Size(50f,45f)) else {
                        bloom(100f,159f,r,flower,5,1,Color(0xFF742A26))
                        drawCircle(flower.shade(.1f),r*.42f,Offset(100f,159f),style=Stroke(r*.15f))
                        repeat(36) { n -> val a=n*2.4f; val rr=r*(.4f+(n%6)*.085f);drawCircle(Color(0xFFFFCF99),1.2f+n%2,Offset(100f+cos(a)*rr,159f+sin(a)*rr)) }
                    }
                }
                BotanicalForm.WISTERIA -> {
                    stem(98f,219f,90f,top+14,5f,bark)
                    val branch=Path().apply {moveTo(90f,top+40);cubicTo(34f,top+16,157f,top-5,172f,top+31)};strokePath(branch,bark,3f)
                    repeat(3+stage) { n -> val x=31f+n*17; leaf(x,top+27,22f,8f,(n%2*2-1)*48f,leaf)
                        if(stage>=3) repeat(4+stage) { k -> bloom(x+sin(k.toFloat())*3,top+39+k*6,5.5f-k*.35f,flower.shade((k%3)*.08f),5) } }
                }
                else -> flowers(id,stage,top,appearance)
            }
        }
    }
}

private fun DrawScope.flowers(id: String,stage: Int,top: Float,a: BotanicalAppearance) {
    val count=when(a.form) { BotanicalForm.SUNFLOWER -> if(stage>=5) 3 else 1; BotanicalForm.CUP -> 1+stage/3; BotanicalForm.SPIRE -> 2+stage; else -> 1+stage/2 }
    repeat(count) { n ->
        val x=100f+(n-(count-1)/2f)*(if(count>5) 17f else 30f)
        val y=top+abs(n-(count-1)/2f)*16+12
        stem(100f+(n-(count-1)/2f)*8,216f,x,y,if(a.form==BotanicalForm.SUNFLOWER) 4f else 2.3f,a.foliage)
        if(a.form==BotanicalForm.CUP) {
            leaf(x-5,214f,40f+stage*7,10f,-23f,a.foliage)
            leaf(x+5,213f,30f+stage*8,9f,24f,a.foliage.shade(.15f))
        } else repeat(1+stage/2) { k ->
            leaf(x,198f-k*22,22f+stage*(if(a.form==BotanicalForm.SUNFLOWER) 3.5f else 2f),
                when(a.form) {BotanicalForm.SPIRE->3f;BotanicalForm.SUNFLOWER,BotanicalForm.CLUSTER->17f;else->11f},
                (if(k%2==0) -1 else 1)*52f,a.foliage)
        }
        if(stage<3) { if(stage==2) leaf(x,y+7,12f,6f,0f,a.foliage);return@repeat }
        val r=if(a.form==BotanicalForm.SUNFLOWER) 13f+stage*2.7f else 9f+stage*2
        when(a.form) {
            BotanicalForm.CUP -> {
                for(angle in listOf(-32f,32f,0f)) leaf(x,y+18,r*1.9f,r*.7f,angle,a.flower.shade(angle/200))
            }
            BotanicalForm.SPIRE -> repeat(4+stage) { k ->
                val yy=y+k*4; val rr=2.5f+k*.18f
                bloom(x+(if(k%2==0) -3 else 3),yy,rr,a.flower.shade(k%3*.12f),5)
            }
            BotanicalForm.CLUSTER -> repeat(13) { k -> val angle=k*2.4f;val d=sqrt(k.toFloat())*r*.26f
                bloom(x+cos(angle)*d,y+sin(angle)*d,r*.29f,a.flower.shade(k%3*.1f),4) }
            BotanicalForm.ROSE -> bloom(x,y,r*1.15f,a.flower,if(id=="peony") 11 else 7,4,a.flower.shade(-.2f))
            BotanicalForm.ORCHID -> { bloom(x,y,r,a.flower,5,1,a.flower.shade(-.35f));leaf(x,y+4,r*.7f,r*.38f,180f,Color(0xFFFFF0C3)) }
            BotanicalForm.HELICONIA -> repeat(stage) { k ->leaf(x,y+k*13,27f,10f,if(k%2==0) -70f else 70f,a.flower.shade(k*.02f));leaf(x,y+k*13,8f,4f,if(k%2==0) -70f else 70f,gold) }
            BotanicalForm.SUNFLOWER -> { bloom(x,y,r,a.flower,15,2,Color(0xFF694333)); drawCircle(Color(0xFF4F352D),r*.38f,Offset(x,y));repeat(17) { k ->val angle=k*2.4f;val d=sqrt(k.toFloat())*r*.07f;drawCircle(gold.shade(-.25f),.8f,Offset(x+cos(angle)*d,y+sin(angle)*d)) } }
            else -> bloom(x,y,r,a.flower,if(id=="edelweiss") 7 else 12,if(id=="edelweiss") 2 else 1)
        }
    }
}

private fun DrawScope.fern(stage: Int,c: Color) {
    repeat(3+stage) { n ->
        val angle=(n-(stage+2)/2f)*17f
        rotate(angle,Offset(100f,217f)) {
            val length=55f+stage*14;stem(100f,217f,103f,217f-length,1.8f,c)
            repeat(5+stage) { k ->val y=209f-k*length/(stage+6); val l=(length*.24f)*(1-k.toFloat()/(stage+7))
                leaf(100f,y,l,3.5f,-63f,c.shade(.08f));leaf(101f,y-2,l,3.5f,63f,c) }
        }
    }
}
private fun DrawScope.tree(id: String,stage: Int,top: Float,c: Color,accent: Color,conifer: Boolean) {
    val thick=when(id) {"baobab" -> 9f+stage*3;"giant_sequoia" -> 5f+stage*1.7f;else -> 3f+stage*.8f}
    val trunk=if(id in setOf("birch","aspen")) Color(0xFFE8DED0) else if(id in setOf("redwood","giant_sequoia")) Color(0xFF9D6248) else bark
    val twisted=id=="bristlecone_pine"
    val p=Path().apply {moveTo(100f-thick,216f);cubicTo(if(twisted) 56f else 95f-thick,174f,105f-thick,top+31,100f-2,top);lineTo(105f,top);cubicTo(111f+thick,top+58,if(twisted) 77f else 110f+thick,188f,100f+thick,216f);close()}
    drawPath(p,Brush.horizontalGradient(listOf(trunk.shade(-.25f),trunk.shade(.3f),trunk),82f,120f))
    if(id in setOf("birch","aspen")) repeat(stage*3) { n ->drawLine(bark.shade(-.1f),Offset(97f-thick/2,208f-n*9),Offset(101f+thick/2,207f-n*9),1.3f) }
    if(id=="rainbow_eucalyptus" && stage>=4) repeat(6) { n ->stem(97f+n*2,211f,98f+n,top+36,1.8f,listOf(Color(0xFFE99D62),Color(0xFF9CCD99),Color(0xFFE58ABA),Color(0xFFB59ADD))[n%4]) }
    val umbrella=id in setOf("dragons_blood_tree","kapok","baobab","banyan")
    repeat(2+stage) { n ->
        val side=if(n%2==0) -1 else 1
        val yy=top+23+n*(if(umbrella) 4f else 12f)
        val xx=100f+side*(16+stage*5+n%3*4)
        stem(101f,yy+36,xx,yy,1.5f+stage*.28f,trunk)
        if(id in setOf("banyan","mangrove") && stage>=4) stem(xx,yy+6,xx+side*8,215f,1.8f,trunk.shade(.2f))
        if(conifer) {
            val spread=14f+stage*5+(if(id=="himalayan_cedar") 12 else 0)
            val b=Path().apply {moveTo(100f,yy-19);lineTo(100f+spread,yy+25);quadraticTo(100f,yy+19,100f-spread,yy+25);close()}
            drawPath(b,Brush.verticalGradient(listOf(c.shade(.2f),c.shade(-.2f)),yy-19,yy+25))
            repeat(7) { k ->drawLine(c.shade(.3f).copy(alpha=.6f),Offset(100f,yy+k*4),Offset(100f+side*(k+1)*spread/9,yy+k*4+8),.7f) }
        } else {
            repeat(7+stage*2) { k ->
                val angle=k*2.4f;val radius=sqrt(k.toFloat())*(3f+stage*.38f)
                val lx=xx+cos(angle)*radius*1.3f;val ly=yy+sin(angle)*radius
                if(id=="cherry_blossom" && stage>=3) bloom(lx,ly,6f+stage*.3f,accent.shade(k%3*.08f),5)
                else leaf(lx,ly+6,14f+stage,6f+stage*.6f,angle*50,c.shade((k%4-1)*.075f),id=="maple")
                if(id=="magnolia" && stage>=4 && k%5==0) bloom(lx,ly,8f,accent,7,2)
            }
        }
        if(id=="willow" && stage>=3) repeat(5) { k ->
            val x=xx+(k-2)*5;val l=30f+stage*6+(k%2)*10;stem(x,yy,x+side*9,yy+l,.9f,c)
            repeat(7) { j ->leaf(x+side*j,yy+j*l/7,9f,2f,side*25f,c.shade(.1f)) }
        }
        if(id=="cacao" && stage>=4 && n%2==0) {drawOval(accent,Offset(98f+side*6,130f+n*7),Size(10f,22f));drawLine(accent.shade(-.3f),Offset(103f+side*6,132f+n*7),Offset(103f+side*6,150f+n*7),1f)}
    }
    if(!conifer) {
        if(id=="dragons_blood_tree" && stage>=3) {
            val crown=Path().apply {moveTo(22f,top+59f);cubicTo(26f,top+9f,173f,top+9f,179f,top+59f);quadraticTo(99f,top+77f,22f,top+59f);close()}
            drawPath(crown,Brush.verticalGradient(listOf(c.shade(.28f),c.shade(-.2f)),top+10,top+77))
            repeat(24) { n -> val xx=31f+n*6;val yy=top+40f+abs(n-12)*1.1f
                leaf(xx,yy+19,15f,2f,(n-12)*2f,c.shade(.2f)) }
        } else repeat(7+stage) { n ->
            val angle=n*2.4f;val radius=sqrt(n.toFloat())*(2f+stage*.5f)
            val x=100f+cos(angle)*radius;val y=top+13+sin(angle)*radius*.55f
            if(id=="cherry_blossom" && stage>=3) bloom(x,y,7f,accent,5)
            else leaf(x,y+12,17f,9f,angle*50,c.shade((n%3)*.08f))
            if(id=="magnolia" && stage>=4 && n%4==0) bloom(x,y,8f,accent,7,2)
        }
    }
    if(id=="mangrove" && stage>=3) repeat(5) { n ->stem(100f,177f,65f+n*18,218f,2.3f,trunk) }
    if(id=="juniper" && stage>=4) repeat(9) { n ->drawCircle(accent,2.5f,Offset(65f+n*9,127f+sin(n.toFloat())*22)) }
}
private fun DrawScope.succulent(id: String,stage: Int,top: Float,c: Color,flower: Color) {
    if(id=="jade") {
        stem(100f,217f,100f,top+20,5f,bark)
        repeat(3+stage) { n ->val side=if(n%2==0) -1 else 1;val y=top+25+n*13;stem(100f,y+14,100f+side*29,y,2f,bark);leaf(100f+side*29,y+8,25f,13f,side*35f,c) }
        if(stage>=5) bloom(100f,top+17,9f,flower,5)
    } else if(id=="prickly_pear") {
        repeat(1+stage) { n ->val x=100f+(if(n%2==0) -1 else 1)*(n/2)*19;val y=196f-n/2*28
            drawOval(Brush.horizontalGradient(listOf(c.shade(-.2f),c.shade(.2f))),Offset(x-16,y-34),Size(32f,44f))
            repeat(7) { k ->drawCircle(Color(0xFFE5D3A2),.9f,Offset(x-9+k%3*8,y-27+k/3*11)) }
            if(stage>=4 && n>=stage-1) bloom(x,y-35,7f,flower,7)
        }
    } else {
        val barrel=id=="barrel_cactus";val width=if(barrel) 25f+stage*8 else 14f+stage*2
        val y=if(barrel) 211f-width*1.1f else top+5
        drawRoundRect(Brush.horizontalGradient(listOf(c.shade(-.25f),c.shade(.22f),c)),Offset(100-width/2,y),Size(width,217-y),androidx.compose.ui.geometry.CornerRadius(width/2))
        repeat(if(barrel) 9 else 5) { n ->val x=100-width*.44f+n*width*.88f/(if(barrel) 8 else 4)
            drawLine(c.shade(.5f),Offset(x,y+width*.45f),Offset(x,211f),1.1f)
            if(barrel) repeat(7) { k ->drawLine(Color(0xFFF6DB9A),Offset(x,y+20+k*8),Offset(x+3,y+17+k*8),.8f) }
        }
        if(!barrel && stage>=3) repeat(stage-2) { n ->val side=if(n%2==0) -1 else 1; val x=100f+side*(29+n%2*6);val yy=top+45+n*21
            val p=Path().apply {moveTo(100f,yy+27);quadraticTo(x,yy+32,x,yy);lineTo(x,yy-18)}
            strokePath(p,c.shade(-.2f),14f);strokePath(p,c.shade(.13f),10f)
        }
        if(stage>=5) repeat(if(barrel) 5 else 1) { n ->bloom(100f+(n-2)*(if(barrel) 8 else 0),y+7,6f,flower,7) }
    }
}
