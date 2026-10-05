package com.kingkharnivore.skillz.ui.screen.shell.inventory

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.domain.achievement.LandBadgeMotif
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon

/** Shared by the Badge Book, detail sheets and Mastery celebrations. No bitmap allocations. */
@Composable
internal fun BadgeCoreArtwork(presentation: BadgePresentation, diameter: Dp) {
    Box(Modifier.size(diameter), contentAlignment = Alignment.Center) {
        when {
            com.kingkharnivore.skillz.domain.achievement.RedBadgeCatalog.byId.containsKey(presentation.badgeId) ->
                RedBadgeArtwork(presentation.badgeId, Modifier.size(diameter * .72f))
            presentation.motif != null -> {
                if (presentation.collectionIdentity != null) {
                    CollectionCrest(presentation.collectionIdentity, Modifier.size(diameter * .55f))
                    BadgeSymbol(presentation.motif, Modifier.align(Alignment.BottomCenter).padding(bottom=4.dp).size(diameter * .20f))
                } else {
                    BadgeSymbol(presentation.motif, Modifier.offset(y = if (presentation.centerLabel != null) diameter * .05f else 0.dp).size(diameter * if (presentation.centerLabel != null) .55f else .62f))
                }
                presentation.centerLabel?.let {
                    Text(it, Modifier.align(Alignment.TopCenter).padding(top = 3.dp),
                        style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
                }
            }
            presentation.artworkKind == BadgeArtworkKind.SPECIES_MASTERY -> {
                presentation.creatureIconKey?.let { ShellObjectIcon(it, Modifier.offset(y = diameter * .06f).size(diameter * .62f)) }
                Text("99", Modifier.align(Alignment.TopCenter).padding(top = 3.dp),
                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black)
            }
            presentation.artworkKind == BadgeArtworkKind.FLOW_DURATION -> Text(
                presentation.centerLabel.orEmpty(), style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Black)
            else -> {
                Icon(when (presentation.artworkKind) {
                    BadgeArtworkKind.COLLECTOR -> Icons.Outlined.TravelExplore
                    BadgeArtworkKind.CURATOR -> Icons.Outlined.Inventory2
                    BadgeArtworkKind.COMPLETIONIST -> Icons.Outlined.WorkspacePremium
                    BadgeArtworkKind.MASTERY -> Icons.Outlined.AutoAwesome
                    BadgeArtworkKind.OBJECTIVE -> Icons.Outlined.TrackChanges
                    BadgeArtworkKind.ACTIVITY -> Icons.Outlined.Bolt
                    else -> Icons.Outlined.Stars
                }, null, Modifier.size(diameter * .48f))
                presentation.collectionIdentity?.let { CollectionCrest(it, Modifier.align(Alignment.TopCenter).padding(top=3.dp).size(diameter * .29f)) }
                if (presentation.artworkKind == BadgeArtworkKind.OBJECTIVE) presentation.centerLabel?.let {
                    Text(it, Modifier.align(Alignment.TopCenter).padding(top = 3.dp),fontWeight=FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun BadgeSymbol(motif: LandBadgeMotif, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) { scale(size.width / 100f, size.height / 100f, Offset.Zero) { symbol(motif, color) } }
}

private fun DrawScope.symbol(motif: LandBadgeMotif, c: Color) {
    val stroke = Stroke(5f, cap=StrokeCap.Round)
    fun line(x:Float,y:Float,xx:Float,yy:Float) = drawLine(c,Offset(x,y),Offset(xx,yy),5f,StrokeCap.Round)
    fun path(vararg points:Float, close:Boolean=false) = drawPath(Path().apply {
        moveTo(points[0],points[1]); for(i in 2 until points.size step 2) lineTo(points[i],points[i+1]); if(close) close()
    },c,style=stroke)
    fun leaf(x:Float=25f,y:Float=15f) {
        drawPath(Path().apply { moveTo(x,y+55); cubicTo(x-20,y+18,x+12,y,x+50,y); cubicTo(x+57,y+45,x+30,y+66,x,y+55); close() },c,style=stroke)
        line(x,y+55,x+35,y+17)
    }
    fun paw(x:Float,y:Float,r:Float=15f) {
        drawOval(c,Offset(x-r,y),Size(r*2,r*1.45f))
        listOf(-.9f,-.32f,.32f,.9f).forEachIndexed { i, dx -> drawCircle(c,r*.27f,Offset(x+dx*r,y-(if(i==1||i==2) .6f else .25f)*r)) }
    }
    when(motif) {
        LandBadgeMotif.FOOTPRINTS -> { paw(30f,45f);paw(71f,25f) }
        LandBadgeMotif.LEAF -> leaf()
        LandBadgeMotif.EARTH -> {
            drawCircle(c,38f,Offset(50f,50f),style=stroke)
            drawOval(c,Offset(31f,12f),Size(38f,76f),style=stroke)
            line(13f,50f,87f,50f);line(22f,30f,78f,30f);line(22f,70f,78f,70f)
        }
        LandBadgeMotif.SEA -> repeat(3) { i ->
            drawPath(Path().apply { moveTo(8f,30f+i*21); cubicTo(30f,8f+i*21,40f,53f+i*21,60f,30f+i*21); quadraticTo(78f,14f+i*21,94f,30f+i*21) },c,style=stroke)
        }
        LandBadgeMotif.LANDSCAPE -> { path(7f,82f,38f,27f,69f,82f);path(46f,82f,70f,43f,95f,82f);drawCircle(c,9f,Offset(74f,18f),style=stroke) }
        LandBadgeMotif.ROOTS, LandBadgeMotif.GROWTH -> {
            line(50f,70f,50f,35f)
            drawOval(c,Offset(18f,20f),Size(32f,20f),style=stroke);drawOval(c,Offset(51f,9f),Size(32f,23f),style=stroke)
            if(motif==LandBadgeMotif.ROOTS) { path(20f,91f,35f,76f,50f,67f,67f,77f,80f,92f);line(50f,68f,50f,93f) }
            else { line(12f,81f,88f,81f);path(72f,67f,85f,54f,98f,67f) }
        }
        LandBadgeMotif.CROWN, LandBadgeMotif.MYTHIC -> {
            path(18f,75f,10f,35f,34f,51f,50f,17f,66f,51f,90f,35f,82f,75f,18f,75f,close=true)
            line(20f,87f,80f,87f)
            if(motif==LandBadgeMotif.MYTHIC) drawCircle(c,5f,Offset(50f,58f))
        }
        LandBadgeMotif.TIGERS -> { paw(27f,44f,19f);paw(74f,44f,19f);line(16f,78f,28f,90f);line(40f,78f,52f,90f);line(66f,78f,78f,90f) }
        LandBadgeMotif.COMPANIONS -> { repeat(5) { i -> val a=i*Math.PI*2/5-Math.PI/2; paw(50f+31f*kotlin.math.cos(a).toFloat(),43f+31f*kotlin.math.sin(a).toFloat(),9f) } }
        LandBadgeMotif.RETURN -> { drawArc(c,25f,290f,false,Offset(14f,14f),Size(72f,72f),style=stroke);path(90f,17f,82f,38f,65f,21f);paw(50f,46f,14f) }
        LandBadgeMotif.DROPLEAF -> {
            drawPath(Path().apply { moveTo(50f,9f); cubicTo(45f,28f,16f,48f,19f,67f); cubicTo(24f,100f,82f,95f,81f,65f); cubicTo(80f,49f,61f,31f,50f,9f);close() },c,style=stroke)
            path(35f,73f,63f,50f);path(35f,73f,33f,54f,63f,50f,59f,74f,35f,73f)
        }
        LandBadgeMotif.RARE -> { path(50f,8f,84f,42f,50f,90f,16f,42f,50f,8f,close=true);line(16f,42f,84f,42f);path(50f,8f,37f,42f,50f,90f,63f,42f,50f,8f) }
        LandBadgeMotif.ENCOUNTER -> { drawCircle(c,26f,Offset(50f,50f),style=stroke);path(5f,18f,24f,18f,24f,5f);path(95f,82f,76f,82f,76f,95f);line(7f,18f,27f,38f);line(93f,82f,73f,62f) }
        LandBadgeMotif.RELEASE -> { paw(31f,49f,15f);path(57f,57f,93f,28f,74f,28f);line(93f,28f,91f,47f);line(10f,86f,63f,86f) }
        LandBadgeMotif.TRADE -> { path(9f,30f,87f,30f,71f,13f);path(91f,71f,13f,71f,29f,89f);paw(50f,47f,10f) }
    }
}

@Composable
private fun CollectionCrest(identity: CollectionArtworkIdentity, modifier: Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) { scale(size.width/100f,size.height/100f,Offset.Zero) {
        val stroke=Stroke(7f,cap=StrokeCap.Round)
        fun line(x:Float,y:Float,xx:Float,yy:Float)=drawLine(color,Offset(x,y),Offset(xx,yy),7f,StrokeCap.Round)
        fun tree(x:Float) { line(x,45f,x,94f);drawCircle(color,21f,Offset(x,29f),style=stroke) }
        when(identity) {
            CollectionArtworkIdentity.GOLDEN_FIELDS, CollectionArtworkIdentity.PASTURE -> {
                line(50f,10f,50f,92f); repeat(3) { i -> val y=20f+i*20;line(50f,y+12,25f,y);line(50f,y+12,75f,y) }
                if(identity==CollectionArtworkIdentity.PASTURE) line(10f,93f,90f,93f)
            }
            CollectionArtworkIdentity.ANCIENT_WOODS, CollectionArtworkIdentity.GLADE -> { tree(25f);tree(75f);if(identity==CollectionArtworkIdentity.GLADE) drawCircle(color,8f,Offset(50f,70f)) }
            CollectionArtworkIdentity.OPEN_SANDS, CollectionArtworkIdentity.OASIS -> {
                drawCircle(color,14f,Offset(70f,23f),style=stroke)
                drawPath(Path().apply { moveTo(5f,89f);quadraticTo(35f,38f,95f,89f) },color,style=stroke)
                if(identity==CollectionArtworkIdentity.OASIS) drawOval(color,Offset(32f,77f),Size(35f,15f),style=stroke)
            }
            CollectionArtworkIdentity.HIGH_PEAKS, CollectionArtworkIdentity.RAVINE -> {
                drawPath(Path().apply { moveTo(4f,89f);lineTo(34f,15f);lineTo(62f,89f);moveTo(48f,89f);lineTo(72f,36f);lineTo(97f,89f) },color,style=stroke)
                if(identity==CollectionArtworkIdentity.RAVINE) line(40f,94f,55f,62f)
            }
            CollectionArtworkIdentity.GREAT_WILD, CollectionArtworkIdentity.SANCTUARY -> {
                line(50f,35f,50f,95f);drawOval(color,Offset(3f,14f),Size(94f,32f),style=stroke)
                if(identity==CollectionArtworkIdentity.SANCTUARY) drawArc(color,0f,180f,false,Offset(8f,29f),Size(84f,68f),style=stroke)
            }
            CollectionArtworkIdentity.LAND -> symbol(LandBadgeMotif.LEAF,color)
            CollectionArtworkIdentity.ALL_LAND -> { symbol(LandBadgeMotif.LEAF,color); drawArc(color,0f,150f,false,Offset(6f,14f),Size(88f,81f),style=stroke) }
            CollectionArtworkIdentity.LAND_STILLWATER -> symbol(LandBadgeMotif.DROPLEAF,color)
            CollectionArtworkIdentity.EARTH -> symbol(LandBadgeMotif.EARTH,color)
            CollectionArtworkIdentity.FISHBOWL -> {drawArc(color,0f,180f,false,Offset(5f,10f),Size(90f,80f),style=stroke);line(7f,45f,93f,45f)}
            CollectionArtworkIdentity.AQUARIUM -> {drawRect(color,Offset(5f,15f),Size(90f,70f),style=stroke);line(7f,40f,93f,40f)}
            CollectionArtworkIdentity.POND -> {drawOval(color,Offset(4f,30f),Size(92f,50f),style=stroke);drawOval(color,Offset(26f,43f),Size(48f,24f),style=stroke)}
            CollectionArtworkIdentity.LAKE -> {symbol(LandBadgeMotif.LANDSCAPE,color);line(10f,95f,90f,95f)}
            CollectionArtworkIdentity.STILLWATER -> symbol(LandBadgeMotif.DROPLEAF,color)
            CollectionArtworkIdentity.SUNLIT_REEF -> {drawCircle(color,22f,Offset(50f,25f),style=stroke);line(10f,83f,90f,83f)}
            CollectionArtworkIdentity.DEEPER_REEF -> {line(50f,94f,50f,14f);line(50f,55f,20f,32f);line(50f,76f,84f,39f)}
            CollectionArtworkIdentity.OPEN_BLUE -> { symbol(LandBadgeMotif.SEA,color); drawCircle(color,6f,Offset(50f,8f)) }
            CollectionArtworkIdentity.GREAT_BLUE -> {
                drawOval(color,Offset(8f,33f),Size(65f,38f),style=stroke)
                drawPath(Path().apply { moveTo(73f,50f);lineTo(95f,29f);lineTo(95f,75f);close() },color,style=stroke)
                drawCircle(color,4f,Offset(25f,45f));line(28f,24f,23f,9f);line(30f,23f,37f,9f)
            }
            CollectionArtworkIdentity.THE_BLUE -> symbol(LandBadgeMotif.SEA,color)
            CollectionArtworkIdentity.ALL_WATERS -> { symbol(LandBadgeMotif.SEA,color);drawCircle(color,46f,Offset(50f,50f),style=Stroke(4f)) }
        }
    } }
}
