package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.draw

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.kingkharnivore.skillz.domain.shell.CreatureDefinition
import com.kingkharnivore.skillz.domain.shell.CreatureRenderFamily
import com.kingkharnivore.skillz.domain.shell.CreatureScaleClass
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import com.kingkharnivore.skillz.ui.screen.shell.depthOrder
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.TheBlueSceneSafeBounds
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.LifePresencePlan
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.creatures.TheBlueRenderedCreature
import com.kingkharnivore.skillz.ui.screen.shell.rooms.blue.isUniqueLegendaryCreature
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

fun DrawScope.drawTheBlueWaterBackground(
    zoneId: TheBlueZoneId,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    val depth = zoneId.depthOrder()
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(
                scheme.primary.copy(alpha = 0.24f - depth * 0.025f),
                scheme.background.copy(alpha = 0.18f + depth * 0.10f),
                scheme.onSurface.copy(alpha = 0.04f + depth * 0.045f)
            ),
            startY = 0f,
            endY = size.height
        )
    )
    repeat(3) { ray ->
        val offset = sin((drift * 0.55f + ray * 0.73f).toDouble()).toFloat() * size.width * 0.06f
        val path = Path().apply {
            moveTo(size.width * (0.12f + ray * 0.22f) + offset, 0f)
            lineTo(size.width * (0.20f + ray * 0.20f) + offset, size.height)
            lineTo(size.width * (0.30f + ray * 0.20f) + offset, size.height)
            lineTo(size.width * (0.20f + ray * 0.22f) + offset, 0f)
            close()
        }
        drawPath(path, scheme.secondary.copy(alpha = (0.07f - depth * 0.012f).coerceAtLeast(0.018f)))
    }
    repeat(18 - depth * 3) { index ->
        val x = ((index * 83f + drift * size.width * (0.10f + depth * 0.03f)) % (size.width + 70f)) - 35f
        val y = size.height - ((index * 47f + drift * size.height * (0.70f - depth * 0.10f)) % size.height)
        drawCircle(
            color = scheme.primary.copy(alpha = 0.055f + (index % 3) * 0.014f),
            radius = 1.8f + (index % 4),
            center = Offset(x, y)
        )
    }
}

fun DrawScope.drawSunlitReefEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    val baseY = size.height * 0.82f
    drawOval(scheme.secondary.copy(alpha = 0.11f), Offset(-size.width * 0.10f, baseY), Size(size.width * 1.20f, size.height * 0.34f))
    repeat(6) { i ->
        val rootX = size.width * (0.08f + i * 0.16f)
        val sway = sin((drift * 6.28f + i).toDouble()).toFloat() * 10f
        val height = size.height * (0.12f + (i % 3) * 0.035f)
        drawLine(scheme.primary.copy(alpha = 0.34f), Offset(rootX, size.height), Offset(rootX + sway, size.height - height), strokeWidth = 5f)
        drawCircle(scheme.secondary.copy(alpha = 0.28f), 8f + i, Offset(rootX + sway, size.height - height))
    }
    repeat(5 + min(animalDensity / 8, 4)) { i ->
        val x = size.width * (0.05f + i * 0.20f)
        val y = size.height * (0.78f + (i % 2) * 0.07f)
        drawBranchingCoral(x, y, 36f + (i % 3) * 12f, scheme.secondary.copy(alpha = 0.32f), drift + i * 0.1f)
    }
    repeat(5) { i ->
        drawOval(
            scheme.onSurface.copy(alpha = 0.08f),
            Offset(size.width * (0.12f + i * 0.18f), size.height * (0.88f + (i % 2) * 0.03f)),
            Size(36f + i * 7f, 18f + i * 2f)
        )
    }
}

fun DrawScope.drawBranchingCoral(x: Float, y: Float, height: Float, color: Color, drift: Float) {
    val sway = sin((drift * 6.28f).toDouble()).toFloat() * 5f
    drawLine(color, Offset(x, y), Offset(x + sway, y - height), strokeWidth = 5f)
    drawLine(color, Offset(x + sway * 0.6f, y - height * 0.55f), Offset(x - 16f + sway, y - height * 0.86f), strokeWidth = 4f)
    drawLine(color, Offset(x + sway * 0.7f, y - height * 0.45f), Offset(x + 17f + sway, y - height * 0.78f), strokeWidth = 4f)
}

fun DrawScope.drawRenderedCreature(
    placement: TheBlueRenderedCreature,
    scheme: androidx.compose.material3.ColorScheme
) {
    if (placement.alpha <= 0.05f) return

    fun drawCreatureBody() {
        val id = placement.rendererKey.lowercase()
        when {
            id == "minnow" -> drawMinnow(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme)
            id == "seahorse" -> drawSeahorse(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme)
            id == "manta" -> drawManta(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme)
            id == "base_whale" -> drawWhaleProfile(placement.center, placement.scale, placement.driftSeed, placement.glowing, scheme, "base_whale")
            id == "octopus" -> drawOctopus(placement.center, placement.scale, placement.glowing, scheme)
            id.contains("starfish") -> drawStarfishScene(placement.center, placement.scale * 0.95f, scheme)
            id.contains("urchin") -> drawUrchinScene(placement.center, placement.scale * 0.86f, scheme)
            id.contains("octopus") -> drawOctopus(placement.center, placement.scale, placement.glowing, scheme)
            id.contains("stingray") -> drawStingrayScene(placement.center, placement.scale * 0.70f, placement.driftSeed, scheme)
            id.contains("manta") -> drawManta(placement.center, placement.scale * 0.72f, placement.driftSeed, placement.glowing, scheme)
            id.contains("whale") -> drawWhaleProfile(placement.center, placement.scale * 0.65f, placement.driftSeed, placement.glowing, scheme, id)
            else -> drawSpeciesSwimmer(placement.center, placement.scale, placement.driftSeed, scheme, id, placement.definition.renderFamily.key)
        }
    }

    if (placement.facingRight) {
        drawCreatureBody()
    } else {
        withTransform({
            scale(scaleX = -1f, scaleY = 1f, pivot = placement.center)
        }) {
            drawCreatureBody()
        }
    }
}

fun DrawScope.drawZoneEnvironment(
    zoneId: TheBlueZoneId,
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    when (zoneId) {
        TheBlueZoneId.SUNLIT_REEF -> drawSunlitReefEnvironment(scheme, drift, animalDensity)
        TheBlueZoneId.DEEPER_REEF -> drawDeeperReefEnvironment(scheme, drift, animalDensity)
        TheBlueZoneId.OPEN_BLUE -> drawOpenBlueEnvironment(scheme, drift)
        TheBlueZoneId.GREAT_BLUE -> drawGreatBlueEnvironment(scheme, drift)
    }
}

fun DrawScope.drawDeeperReefEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float,
    animalDensity: Int
) {
    repeat(4) { i ->
        val x = if (i % 2 == 0) size.width * (0.08f + i * 0.08f) else size.width * (0.78f - i * 0.05f)
        val top = size.height * (0.28f + (i % 2) * 0.08f)
        drawRoundRockColumn(x, top, size.height * 0.70f, 42f + i * 8f, scheme.onSurface.copy(alpha = 0.12f))
        drawBranchingCoral(x + 12f, top + 80f, 46f, scheme.primary.copy(alpha = 0.26f), drift + i)
    }
    val caveX = size.width * 0.62f
    val caveY = size.height * 0.70f
    drawOval(scheme.onSurface.copy(alpha = 0.22f), Offset(caveX, caveY), Size(size.width * 0.26f, size.height * 0.16f))
    drawOval(scheme.background.copy(alpha = 0.35f), Offset(caveX + 16f, caveY + 12f), Size(size.width * 0.18f, size.height * 0.10f))
    repeat(5 + min(animalDensity / 6, 4)) { i ->
        val x = size.width * (0.18f + i * 0.15f)
        val sway = sin((drift * 6.28f + i).toDouble()).toFloat() * 8f
        drawLine(scheme.primary.copy(alpha = 0.18f), Offset(x, 0f), Offset(x + sway, size.height * (0.16f + (i % 3) * 0.04f)), strokeWidth = 4f)
    }
    repeat(7) { i ->
        drawCircle(scheme.secondary.copy(alpha = 0.10f), 2.5f + (i % 2), Offset(size.width * (0.15f + i * 0.11f), size.height * (0.42f + (i % 3) * 0.08f)))
    }
}

fun DrawScope.drawOpenBlueEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    repeat(7) { i ->
        val y = size.height * (0.18f + i * 0.10f)
        val xOffset = sin((drift * 6.28f + i).toDouble()).toFloat() * 28f
        drawLine(
            scheme.primary.copy(alpha = 0.11f),
            Offset(-40f + xOffset, y),
            Offset(size.width + 40f + xOffset, y + 24f),
            strokeWidth = 2.5f
        )
    }
    drawOval(scheme.onSurface.copy(alpha = 0.055f), Offset(size.width * 0.62f, size.height * 0.78f), Size(size.width * 0.45f, size.height * 0.16f))
}

fun DrawScope.drawGreatBlueEnvironment(
    scheme: androidx.compose.material3.ColorScheme,
    drift: Float
) {
    repeat(5) { i ->
        val y = size.height * (0.18f + i * 0.14f)
        drawLine(scheme.onSurface.copy(alpha = 0.045f), Offset(0f, y), Offset(size.width, y + sin((drift * 6.28f + i).toDouble()).toFloat() * 10f), strokeWidth = 10f)
    }
    repeat(10) { i ->
        val x = ((i * 97f + drift * size.width * 0.04f) % size.width)
        val y = ((i * 61f + drift * size.height * 0.12f) % size.height)
        drawCircle(scheme.secondary.copy(alpha = 0.035f), 1.5f + (i % 2), Offset(x, y))
    }
}

private fun DrawScope.drawRoundRockColumn(x: Float, top: Float, bottom: Float, width: Float, color: Color) {
    drawOval(color, Offset(x - width / 2f, top), Size(width, bottom - top))
    drawOval(color.copy(alpha = color.alpha * 0.7f), Offset(x - width * 0.65f, top + 60f), Size(width * 1.3f, width * 0.75f))
}

fun movementLaneCount(definition: CreatureDefinition, plan: LifePresencePlan): Int = when {
    isUniqueLegendaryCreature(definition) -> 1
    definition.renderFamily == CreatureRenderFamily.WHALE -> max(3, plan.directIndividuals.size)
    definition.renderFamily == CreatureRenderFamily.RAY -> max(3, plan.directIndividuals.size)
    definition.scaleClass == CreatureScaleClass.GIANT -> max(3, plan.directIndividuals.size)
    definition.scaleClass == CreatureScaleClass.LARGE -> max(3, plan.directIndividuals.size)
    definition.scaleClass == CreatureScaleClass.MEDIUM -> max(4, plan.directIndividuals.size)
    else -> max(5, plan.directIndividuals.size)
}

fun offscreenMarginFor(visualWidth: Float, definition: CreatureDefinition): Float {
    val multiplier = when (definition.scaleClass) {
        CreatureScaleClass.GIANT, CreatureScaleClass.LEGENDARY -> 0.70f
        CreatureScaleClass.LARGE -> 0.55f
        else -> 0.45f
    }
    return (visualWidth * multiplier).coerceIn(48f, 180f)
}

fun rectsOverlap(a: Rect, b: Rect): Boolean = a.left < b.right && b.left < a.right && a.top < b.bottom && b.top < a.bottom

fun stableHash(findId: String, index: Int): Long = (findId.hashCode() * 31 + index * 997).toUInt().toLong()

fun stablePhase(findId: String, index: Int): Float = (stableHash(findId, index) % 1000L) / 1000f

fun stableLane(findId: String, index: Int, laneCount: Int): Int = if (laneCount <= 1) 0 else (stableHash(findId, index) % laneCount).toInt()

fun stableFacingRight(findId: String, index: Int): Boolean = (stableHash(findId, index) and 1L) == 0L

fun stableCruiseEntryPhase(findId: String, index: Int): Float = 0.42f + ((stableHash(findId, index) % 160L) / 1000f)

fun loopAlpha(centerX: Float, visualWidth: Float, bounds: TheBlueSceneSafeBounds): Float {
    val fade = (visualWidth * 0.75f).coerceAtLeast(48f)
    return when {
        centerX < bounds.left - visualWidth -> 0f
        centerX < bounds.left + fade -> ((centerX - (bounds.left - visualWidth)) / (visualWidth + fade)).coerceIn(0f, 1f)
        centerX > bounds.right + visualWidth -> 0f
        centerX > bounds.right - fade -> (((bounds.right + visualWidth) - centerX) / (visualWidth + fade)).coerceIn(0f, 1f)
        else -> 1f
    }
}

fun offscreenHorizontalPassX(
    progress: Float,
    left: Float,
    right: Float,
    animalWidth: Float,
    margin: Float,
    facingRight: Boolean
): Float {
    val start = left - animalWidth - margin
    val end = right + animalWidth + margin
    val x = start + (end - start) * progress
    return if (facingRight) x else end - (x - start)
}

fun offscreenHorizontalPassX(
    progress: Float,
    screenWidth: Float,
    animalWidth: Float,
    margin: Float,
    leftToRight: Boolean
): Float {
    val start = if (leftToRight) -animalWidth - margin else screenWidth + animalWidth + margin
    val end = if (leftToRight) screenWidth + animalWidth + margin else -animalWidth - margin
    return start + (end - start) * progress.coerceIn(0f, 1f)
}


private fun DrawScope.drawSpeciesSwimmer(origin: Offset, scale: Float, drift: Float, scheme: androidx.compose.material3.ColorScheme, creatureId: String, familyKey: String) {
    val bob = sin((drift * 6.28f).toDouble()).toFloat() * 5f
    val id = creatureId.lowercase()
    fun fish(body: Color, accent: Color, wMul: Float = 1f, hMul: Float = 1f, stripes: Int = 0, beak: Boolean = false) {
        val w = 34f * scale * wMul
        val h = 18f * scale * hMul
        val c = Offset(origin.x, origin.y + bob)
        drawOval(body, Offset(c.x - w * 0.50f, c.y - h * 0.50f), Size(w, h))
        drawPath(Path().apply { moveTo(c.x - w*0.48f,c.y); lineTo(c.x - w*0.82f,c.y - h*0.52f); lineTo(c.x - w*0.82f,c.y + h*0.52f); close() }, accent)
        drawPath(Path().apply { moveTo(c.x,c.y - h*0.48f); lineTo(c.x + w*0.13f,c.y - h*1.05f); lineTo(c.x + w*0.24f,c.y - h*0.35f); close() }, accent.copy(alpha=0.72f))
        if (beak) drawPath(Path().apply { moveTo(c.x+w*0.48f,c.y-h*0.12f); lineTo(c.x+w*0.70f,c.y-h*0.25f); lineTo(c.x+w*0.50f,c.y+h*0.18f); close() }, accent)
        repeat(stripes) { n -> drawLine(scheme.surface.copy(alpha=0.42f), Offset(c.x - w*0.18f + n*w*0.14f, c.y-h*0.42f), Offset(c.x - w*0.12f + n*w*0.14f, c.y+h*0.42f), strokeWidth=2f*scale) }
        drawCircle(scheme.onSurface.copy(alpha=0.55f), 1.8f*scale, Offset(c.x+w*0.28f, c.y-h*0.16f))
    }
    when {
        "clownfish" in id -> fish(Color(0xFFE9782E).copy(alpha=0.74f), scheme.surface.copy(alpha=0.60f), hMul=1.10f, stripes=3)
        "blue_tang" in id -> fish(Color(0xFF2D77C8).copy(alpha=0.70f), Color(0xFFF2D14C).copy(alpha=0.70f), wMul=1.05f, stripes=1)
        "butterflyfish" in id -> fish(Color(0xFFF4D35E).copy(alpha=0.70f), scheme.onSurface.copy(alpha=0.50f), wMul=0.88f, hMul=1.45f, stripes=4)
        "angelfish" in id -> fish(Color(0xFF6C63C7).copy(alpha=0.66f), Color(0xFFEFB8C8).copy(alpha=0.56f), wMul=0.85f, hMul=1.55f, stripes=2)
        "parrotfish" in id -> fish(Color(0xFF13A999).copy(alpha=0.70f), Color(0xFFFF8F3D).copy(alpha=0.64f), wMul=1.18f, hMul=1.12f, stripes=2, beak=true)
        "lionfish" in id -> { fish(Color(0xFFB45A3C).copy(alpha=0.68f), Color(0xFFF3D6A2).copy(alpha=0.60f), hMul=1.2f, stripes=4); repeat(6){n->drawLine(scheme.secondary.copy(alpha=0.40f), Offset(origin.x-10f*scale+n*5f*scale, origin.y-7f*scale+bob), Offset(origin.x-22f*scale+n*8f*scale, origin.y-30f*scale+bob), strokeWidth=1.5f*scale)} }
        "pufferfish" in id -> { drawCircle(scheme.primary.copy(alpha=0.58f), 17f*scale, Offset(origin.x,origin.y+bob)); repeat(10){n->val a=n*6.28318f/10f; drawLine(scheme.secondary.copy(alpha=0.60f), Offset(origin.x,origin.y+bob), Offset(origin.x+kotlin.math.cos(a)*25f*scale, origin.y+bob+kotlin.math.sin(a)*25f*scale), strokeWidth=1.2f*scale)} }
        "jellyfish" in id -> drawJellyfishScene(origin, scale, bob, scheme)
        "seahorse" in id -> drawSeahorse(origin, scale, bob, false, scheme)
        "turtle" in id -> drawTurtleScene(origin, scale, bob, scheme)
        "sea_otter" in id || "otter" in id -> drawSeaOtterScene(origin, scale, bob, scheme)
        "sea_lion" in id -> drawSeaLionScene(origin, scale, bob, scheme)
        "penguin" in id -> drawPenguinScene(origin, scale, bob, scheme)
        "seal" in id -> drawSealScene(origin, scale, bob, scheme)
        "dolphin" in id -> drawDolphinScene(origin, scale, bob, scheme)
        "orca" in id -> drawOrcaScene(origin, scale, bob, scheme)
        "anglerfish" in id -> { fish(scheme.onSurface.copy(alpha=0.50f), scheme.secondary.copy(alpha=0.55f), wMul=1.15f, hMul=1.20f); drawLine(scheme.secondary.copy(alpha=0.65f), Offset(origin.x+10f*scale,origin.y-9f*scale+bob), Offset(origin.x+24f*scale,origin.y-30f*scale+bob), strokeWidth=1.7f*scale); drawCircle(scheme.secondary.copy(alpha=0.85f), 3f*scale, Offset(origin.x+25f*scale,origin.y-31f*scale+bob)) }
        "megalodon" in id -> drawMegalodonScene(origin, scale, bob, scheme)
        "great_white" in id || "shark" in id -> drawGreatWhiteSharkScene(origin, scale, bob, scheme)
        "whale" in id -> drawWhaleProfile(origin, scale * 0.65f, drift, false, scheme, id)
        "kraken" in id -> drawKrakenScene(origin, scale, bob, scheme)
        "giant_squid" in id -> drawSquidScene(origin, scale * 1.30f, bob, scheme, giant = true)
        "squid" in id -> drawSquidScene(origin, scale, bob, scheme, giant = false)
        "leviathan" in id -> drawLeviathanScene(origin, scale, bob, scheme)
        "moray_eel" in id || "eel" in id -> drawMorayEelScene(origin, scale, bob, scheme)
        "sea_snake" in id || "snake" in id -> drawSeaSnakeScene(origin, scale, bob, scheme)
        "sunfish" in id -> fish(scheme.primary.copy(alpha=0.56f), scheme.secondary.copy(alpha=0.36f), wMul=0.95f, hMul=1.8f)
        "swordfish" in id -> { fish(scheme.primary.copy(alpha=0.58f), scheme.secondary.copy(alpha=0.40f), wMul=1.65f, hMul=0.75f); drawLine(scheme.secondary.copy(alpha=0.55f), Offset(origin.x+24f*scale,origin.y+bob), Offset(origin.x+58f*scale,origin.y-4f*scale+bob), strokeWidth=1.7f*scale) }
        "flying_fish" in id -> { fish(scheme.primary.copy(alpha=0.58f), scheme.secondary.copy(alpha=0.40f), wMul=1.25f, hMul=0.75f); drawPath(Path().apply{moveTo(origin.x-4f*scale,origin.y-6f*scale+bob);lineTo(origin.x-28f*scale,origin.y-34f*scale+bob);lineTo(origin.x+18f*scale,origin.y-8f*scale+bob);close()}, scheme.secondary.copy(alpha=0.30f)) }
        "barracuda" in id -> fish(scheme.onSurface.copy(alpha=0.42f), scheme.secondary.copy(alpha=0.32f), wMul=1.75f, hMul=0.70f)
        else -> drawMissingCreatureRenderer(origin, scale, scheme)
    }
}

private fun DrawScope.drawMissingCreatureRenderer(origin: Offset, scale: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.42f)
    val accent = scheme.secondary.copy(alpha = 0.36f)
    drawOval(body, Offset(origin.x - 20f * scale, origin.y - 9f * scale), Size(36f * scale, 18f * scale))
    drawPath(Path().apply {
        moveTo(origin.x - 18f * scale, origin.y)
        lineTo(origin.x - 34f * scale, origin.y - 12f * scale)
        lineTo(origin.x - 34f * scale, origin.y + 12f * scale)
        close()
    }, accent)
    drawPath(Path().apply {
        moveTo(origin.x - 2f * scale, origin.y - 8f * scale)
        lineTo(origin.x + 8f * scale, origin.y - 23f * scale)
        lineTo(origin.x + 12f * scale, origin.y - 7f * scale)
        close()
    }, accent)
    drawCircle(scheme.surface.copy(alpha = 0.52f), 1.8f * scale, Offset(origin.x + 9f * scale, origin.y - 3f * scale))
}

private fun DrawScope.drawStarfishScene(origin: Offset, scale: Float, scheme: androidx.compose.material3.ColorScheme) {
    val star = Path()
    repeat(10) { i ->
        val radius = if (i % 2 == 0) 28f * scale else 12f * scale
        val angle = (-90f + i * 36f) * (Math.PI.toFloat() / 180f)
        val x = origin.x + kotlin.math.cos(angle) * radius
        val y = origin.y + kotlin.math.sin(angle) * radius
        if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
    }
    star.close()
    drawPath(star, scheme.secondary.copy(alpha = 0.74f))
    drawCircle(scheme.primary.copy(alpha = 0.30f), 4f * scale, origin)
    repeat(5) { i ->
        val angle = (-90f + i * 72f) * (Math.PI.toFloat() / 180f)
        drawCircle(
            scheme.surface.copy(alpha = 0.50f),
            2f * scale,
            Offset(origin.x + kotlin.math.cos(angle) * 15f * scale, origin.y + kotlin.math.sin(angle) * 15f * scale)
        )
    }
}

private fun DrawScope.drawUrchinScene(origin: Offset, scale: Float, scheme: androidx.compose.material3.ColorScheme) {
    repeat(18) { i ->
        val angle = i * 6.28318f / 18f
        drawLine(
            scheme.primary.copy(alpha = 0.58f),
            origin,
            Offset(origin.x + kotlin.math.cos(angle) * 25f * scale, origin.y + kotlin.math.sin(angle) * 25f * scale),
            strokeWidth = 1.6f * scale
        )
    }
    drawCircle(scheme.secondary.copy(alpha = 0.62f), 13f * scale, origin)
    drawCircle(scheme.surface.copy(alpha = 0.40f), 2.5f * scale, Offset(origin.x - 4f * scale, origin.y - 4f * scale))
}

private fun DrawScope.drawJellyfishScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val ink = scheme.secondary.copy(alpha = 0.66f)
    val w = 34f * scale
    val h = 18f * scale
    drawCircle(ink, w * 0.42f, Offset(origin.x, origin.y + bob))
    repeat(4) { t -> drawLine(ink, Offset(origin.x - w * 0.30f + t*w*0.20f, origin.y + bob + h*0.30f), Offset(origin.x - w * 0.38f + t*w*0.22f, origin.y + bob + h*1.4f), strokeWidth = 2.4f * scale) }
}

private fun DrawScope.drawTurtleScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    drawOval(scheme.primary.copy(alpha=0.52f), Offset(origin.x-22f*scale, origin.y-12f*scale+bob), Size(44f*scale, 28f*scale))
    drawCircle(scheme.secondary.copy(alpha=0.42f), 7f*scale, Offset(origin.x+26f*scale, origin.y-3f*scale+bob))
    listOf(-1f to -1f, -1f to 1f, 1f to -1f, 1f to 1f).forEach { (sx, sy) ->
        drawOval(scheme.secondary.copy(alpha=0.34f), Offset(origin.x + sx*22f*scale, origin.y + sy*13f*scale + bob), Size(12f*scale, 7f*scale))
    }
}

private fun DrawScope.drawStingrayScene(origin: Offset, scale: Float, drift: Float, scheme: androidx.compose.material3.ColorScheme) {
    val bob = kotlin.math.sin(drift * 6.28318f) * 4f * scale
    val c = Offset(origin.x, origin.y + bob)
    val wing = Path().apply {
        moveTo(c.x, c.y - 16f * scale)
        cubicTo(c.x - 34f * scale, c.y - 10f * scale, c.x - 46f * scale, c.y + 10f * scale, c.x - 42f * scale, c.y + 20f * scale)
        cubicTo(c.x - 17f * scale, c.y + 13f * scale, c.x - 8f * scale, c.y + 14f * scale, c.x, c.y + 27f * scale)
        cubicTo(c.x + 8f * scale, c.y + 14f * scale, c.x + 17f * scale, c.y + 13f * scale, c.x + 42f * scale, c.y + 20f * scale)
        cubicTo(c.x + 46f * scale, c.y + 10f * scale, c.x + 34f * scale, c.y - 10f * scale, c.x, c.y - 16f * scale)
        close()
    }
    drawPath(wing, scheme.primary.copy(alpha = 0.46f))
    drawLine(scheme.secondary.copy(alpha = 0.48f), Offset(c.x, c.y + 20f * scale), Offset(c.x, c.y + 58f * scale), strokeWidth = 2f * scale)
    drawCircle(scheme.surface.copy(alpha = 0.44f), 2.2f * scale, Offset(c.x - 7f * scale, c.y - 2f * scale))
    drawCircle(scheme.surface.copy(alpha = 0.44f), 2.2f * scale, Offset(c.x + 7f * scale, c.y - 2f * scale))
}

private fun DrawScope.drawSeaOtterScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.onSurface.copy(alpha = 0.38f)
    drawOval(body, Offset(origin.x - 28f * scale, origin.y - 3f * scale + bob), Size(54f * scale, 15f * scale))
    drawCircle(body.copy(alpha = 0.78f), 10f * scale, Offset(origin.x + 23f * scale, origin.y - 5f * scale + bob))
    drawCircle(scheme.secondary.copy(alpha = 0.38f), 3.2f * scale, Offset(origin.x - 3f * scale, origin.y + bob))
    drawCircle(scheme.secondary.copy(alpha = 0.38f), 3.2f * scale, Offset(origin.x + 7f * scale, origin.y - 1f * scale + bob))
    drawCircle(scheme.surface.copy(alpha = 0.55f), 1.8f * scale, Offset(origin.x + 27f * scale, origin.y - 8f * scale + bob))
}

private fun DrawScope.drawSealScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.50f)
    drawOval(body, Offset(origin.x - 32f * scale, origin.y - 10f * scale + bob), Size(62f * scale, 21f * scale))
    drawCircle(body.copy(alpha = 0.86f), 9f * scale, Offset(origin.x + 30f * scale, origin.y - 2f * scale + bob))
    drawPath(Path().apply { moveTo(origin.x - 4f * scale, origin.y + 7f * scale + bob); lineTo(origin.x - 22f * scale, origin.y + 24f * scale + bob); lineTo(origin.x + 6f * scale, origin.y + 10f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.35f))
    drawCircle(scheme.surface.copy(alpha = 0.55f), 1.8f * scale, Offset(origin.x + 33f * scale, origin.y - 5f * scale + bob))
}

private fun DrawScope.drawSeaLionScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.56f)
    drawOval(body, Offset(origin.x - 35f * scale, origin.y - 9f * scale + bob), Size(58f * scale, 23f * scale))
    drawOval(body.copy(alpha = 0.90f), Offset(origin.x + 12f * scale, origin.y - 18f * scale + bob), Size(24f * scale, 21f * scale))
    drawPath(Path().apply { moveTo(origin.x - 7f * scale, origin.y + 8f * scale + bob); lineTo(origin.x - 32f * scale, origin.y + 28f * scale + bob); lineTo(origin.x + 5f * scale, origin.y + 13f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.42f))
    drawPath(Path().apply { moveTo(origin.x + 4f * scale, origin.y + 8f * scale + bob); lineTo(origin.x + 31f * scale, origin.y + 25f * scale + bob); lineTo(origin.x + 15f * scale, origin.y + 5f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.32f))
    drawCircle(scheme.surface.copy(alpha = 0.55f), 1.8f * scale, Offset(origin.x + 30f * scale, origin.y - 10f * scale + bob))
}

private fun DrawScope.drawPenguinScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val black = Color(0xFF263238).copy(alpha = 0.72f)
    drawOval(black, Offset(origin.x - 13f * scale, origin.y - 28f * scale + bob), Size(26f * scale, 52f * scale))
    drawOval(scheme.surface.copy(alpha = 0.68f), Offset(origin.x - 7f * scale, origin.y - 14f * scale + bob), Size(14f * scale, 30f * scale))
    drawPath(Path().apply { moveTo(origin.x - 12f * scale, origin.y - 5f * scale + bob); lineTo(origin.x - 32f * scale, origin.y + 8f * scale + bob); lineTo(origin.x - 11f * scale, origin.y + 8f * scale + bob); close() }, scheme.primary.copy(alpha = 0.35f))
    drawPath(Path().apply { moveTo(origin.x + 12f * scale, origin.y - 5f * scale + bob); lineTo(origin.x + 32f * scale, origin.y + 8f * scale + bob); lineTo(origin.x + 11f * scale, origin.y + 8f * scale + bob); close() }, scheme.primary.copy(alpha = 0.35f))
    drawPath(Path().apply { moveTo(origin.x + 8f * scale, origin.y - 22f * scale + bob); lineTo(origin.x + 22f * scale, origin.y - 18f * scale + bob); lineTo(origin.x + 8f * scale, origin.y - 14f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.72f))
}

private fun DrawScope.drawDolphinScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val body = scheme.primary.copy(alpha = 0.64f)
    val rim = scheme.secondary.copy(alpha = 0.24f)
    val silhouette = Path().apply {
        moveTo(c.x - 44f * scale, c.y + 5f * scale)
        cubicTo(c.x - 28f * scale, c.y - 26f * scale, c.x + 18f * scale, c.y - 28f * scale, c.x + 42f * scale, c.y - 6f * scale)
        cubicTo(c.x + 24f * scale, c.y + 14f * scale, c.x - 16f * scale, c.y + 20f * scale, c.x - 44f * scale, c.y + 5f * scale)
        close()
    }
    drawPath(silhouette, rim)
    drawPath(silhouette, body)
    drawPath(Path().apply { moveTo(c.x + 36f * scale, c.y - 8f * scale); lineTo(c.x + 66f * scale, c.y - 13f * scale); lineTo(c.x + 38f * scale, c.y + 1f * scale); close() }, body)
    drawPath(Path().apply { moveTo(c.x - 40f * scale, c.y + 4f * scale); lineTo(c.x - 66f * scale, c.y - 14f * scale); lineTo(c.x - 53f * scale, c.y + 2f * scale); lineTo(c.x - 67f * scale, c.y + 20f * scale); close() }, body.copy(alpha = 0.90f))
    drawPath(Path().apply { moveTo(c.x - 4f * scale, c.y - 19f * scale); lineTo(c.x + 7f * scale, c.y - 43f * scale); lineTo(c.x + 14f * scale, c.y - 17f * scale); close() }, scheme.secondary.copy(alpha = 0.56f))
    drawPath(Path().apply { moveTo(c.x + 0f, c.y + 9f * scale); lineTo(c.x - 24f * scale, c.y + 34f * scale); lineTo(c.x + 12f * scale, c.y + 13f * scale); close() }, scheme.secondary.copy(alpha = 0.34f))
    drawCircle(scheme.onSurface.copy(alpha = 0.58f), 2f * scale, Offset(c.x + 28f * scale, c.y - 10f * scale))
}

private fun DrawScope.drawOrcaScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val black = Color(0xFF18242A).copy(alpha = 0.82f)
    val white = scheme.surface.copy(alpha = 0.86f)
    val outline = scheme.secondary.copy(alpha = 0.22f)
    drawOval(outline, Offset(c.x - 62f * scale, c.y - 22f * scale), Size(118f * scale, 48f * scale))
    drawOval(black, Offset(c.x - 58f * scale, c.y - 18f * scale), Size(108f * scale, 38f * scale))
    drawOval(white, Offset(c.x - 12f * scale, c.y + 4f * scale), Size(42f * scale, 12f * scale))
    drawOval(white.copy(alpha = 0.78f), Offset(c.x + 20f * scale, c.y - 12f * scale), Size(12f * scale, 7f * scale))
    drawPath(Path().apply { moveTo(c.x - 4f * scale, c.y - 18f * scale); lineTo(c.x + 10f * scale, c.y - 55f * scale); lineTo(c.x + 20f * scale, c.y - 16f * scale); close() }, black)
    drawPath(Path().apply { moveTo(c.x - 54f * scale, c.y); lineTo(c.x - 86f * scale, c.y - 22f * scale); lineTo(c.x - 70f * scale, c.y); lineTo(c.x - 88f * scale, c.y + 22f * scale); close() }, black)
    drawCircle(scheme.surface.copy(alpha = 0.72f), 1.8f * scale, Offset(c.x + 34f * scale, c.y - 8f * scale))
}

private fun DrawScope.drawGreatWhiteSharkScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val top = Color(0xFF6F8792).copy(alpha = 0.74f)
    val belly = scheme.surface.copy(alpha = 0.68f)
    val body = Path().apply {
        moveTo(c.x - 62f * scale, c.y + 2f * scale)
        cubicTo(c.x - 34f * scale, c.y - 23f * scale, c.x + 28f * scale, c.y - 22f * scale, c.x + 58f * scale, c.y - 2f * scale)
        cubicTo(c.x + 22f * scale, c.y + 18f * scale, c.x - 38f * scale, c.y + 19f * scale, c.x - 62f * scale, c.y + 2f * scale)
        close()
    }
    drawPath(body, top)
    drawOval(belly, Offset(c.x - 26f * scale, c.y + 4f * scale), Size(58f * scale, 12f * scale))
    drawPath(Path().apply { moveTo(c.x - 4f * scale, c.y - 19f * scale); lineTo(c.x + 8f * scale, c.y - 48f * scale); lineTo(c.x + 18f * scale, c.y - 16f * scale); close() }, top)
    drawPath(Path().apply { moveTo(c.x - 8f * scale, c.y + 10f * scale); lineTo(c.x - 30f * scale, c.y + 34f * scale); lineTo(c.x + 8f * scale, c.y + 13f * scale); close() }, top.copy(alpha = 0.66f))
    drawPath(Path().apply { moveTo(c.x - 58f * scale, c.y + 1f * scale); lineTo(c.x - 91f * scale, c.y - 20f * scale); lineTo(c.x - 75f * scale, c.y + 1f * scale); lineTo(c.x - 92f * scale, c.y + 22f * scale); close() }, top)
    drawCircle(scheme.onSurface.copy(alpha = 0.62f), 1.8f * scale, Offset(c.x + 38f * scale, c.y - 8f * scale))
}

private fun DrawScope.drawMegalodonScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val top = Color(0xFF44515A).copy(alpha = 0.82f)
    val belly = scheme.surface.copy(alpha = 0.54f)
    drawOval(scheme.secondary.copy(alpha = 0.10f), Offset(c.x - 96f * scale, c.y - 38f * scale), Size(178f * scale, 76f * scale))
    val body = Path().apply {
        moveTo(c.x - 82f * scale, c.y + 3f * scale)
        cubicTo(c.x - 46f * scale, c.y - 34f * scale, c.x + 42f * scale, c.y - 31f * scale, c.x + 76f * scale, c.y - 4f * scale)
        cubicTo(c.x + 36f * scale, c.y + 26f * scale, c.x - 48f * scale, c.y + 28f * scale, c.x - 82f * scale, c.y + 3f * scale)
        close()
    }
    drawPath(body, top)
    drawOval(belly, Offset(c.x - 36f * scale, c.y + 7f * scale), Size(78f * scale, 16f * scale))
    drawPath(Path().apply { moveTo(c.x - 8f * scale, c.y - 29f * scale); lineTo(c.x + 9f * scale, c.y - 70f * scale); lineTo(c.x + 25f * scale, c.y - 24f * scale); close() }, top)
    drawPath(Path().apply { moveTo(c.x - 12f * scale, c.y + 13f * scale); lineTo(c.x - 46f * scale, c.y + 48f * scale); lineTo(c.x + 14f * scale, c.y + 18f * scale); close() }, top.copy(alpha = 0.68f))
    drawPath(Path().apply { moveTo(c.x - 80f * scale, c.y + 2f * scale); lineTo(c.x - 124f * scale, c.y - 31f * scale); lineTo(c.x - 101f * scale, c.y + 2f * scale); lineTo(c.x - 125f * scale, c.y + 34f * scale); close() }, top)
    drawCircle(scheme.secondary.copy(alpha = 0.72f), 2.3f * scale, Offset(c.x + 50f * scale, c.y - 12f * scale))
}

private fun DrawScope.drawMorayEelScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = 0.55f)
    drawArc(body, 165f, 250f, false, Offset(origin.x - 38f * scale, origin.y - 20f * scale + bob), Size(74f * scale, 50f * scale), style = Stroke(width = 9f * scale))
    drawOval(body.copy(alpha = 0.88f), Offset(origin.x + 20f * scale, origin.y - 12f * scale + bob), Size(22f * scale, 16f * scale))
    drawLine(scheme.surface.copy(alpha = 0.55f), Offset(origin.x + 29f * scale, origin.y - 2f * scale + bob), Offset(origin.x + 41f * scale, origin.y + 1f * scale + bob), strokeWidth = 1.3f * scale)
    drawCircle(scheme.surface.copy(alpha = 0.62f), 1.7f * scale, Offset(origin.x + 30f * scale, origin.y - 7f * scale + bob))
    drawOval(scheme.onSurface.copy(alpha = 0.12f), Offset(origin.x - 42f * scale, origin.y + 14f * scale + bob), Size(34f * scale, 14f * scale))
}

private fun DrawScope.drawSeaSnakeScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.secondary.copy(alpha = 0.58f)
    val path = Path().apply {
        moveTo(origin.x - 42f * scale, origin.y + 8f * scale + bob)
        cubicTo(origin.x - 22f * scale, origin.y - 24f * scale + bob, origin.x - 6f * scale, origin.y + 30f * scale + bob, origin.x + 12f * scale, origin.y - 4f * scale + bob)
        cubicTo(origin.x + 25f * scale, origin.y - 26f * scale + bob, origin.x + 38f * scale, origin.y - 4f * scale + bob, origin.x + 48f * scale, origin.y - 12f * scale + bob)
    }
    drawPath(path, body, style = Stroke(width = 5f * scale))
    repeat(6) { i ->
        val x = origin.x - 28f * scale + i * 13f * scale
        drawLine(scheme.surface.copy(alpha = 0.45f), Offset(x, origin.y + bob - 7f * scale), Offset(x + 5f * scale, origin.y + bob + 4f * scale), strokeWidth = 1.3f * scale)
    }
    drawCircle(body.copy(alpha = 0.86f), 5.5f * scale, Offset(origin.x + 48f * scale, origin.y - 12f * scale + bob))
}

private fun DrawScope.drawSquidScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme, giant: Boolean) {
    val color = if (giant) scheme.secondary.copy(alpha = 0.58f) else scheme.primary.copy(alpha = 0.48f)
    val bodyHeight = if (giant) 58f else 42f
    drawPath(Path().apply {
        moveTo(origin.x, origin.y - bodyHeight * 0.60f * scale + bob)
        cubicTo(origin.x - 18f * scale, origin.y - 22f * scale + bob, origin.x - 16f * scale, origin.y + 8f * scale + bob, origin.x, origin.y + 18f * scale + bob)
        cubicTo(origin.x + 16f * scale, origin.y + 8f * scale + bob, origin.x + 18f * scale, origin.y - 22f * scale + bob, origin.x, origin.y - bodyHeight * 0.60f * scale + bob)
        close()
    }, color)
    repeat(if (giant) 8 else 5) { i ->
        val startX = origin.x - (if (giant) 16f else 11f) * scale + i * (if (giant) 4.8f else 5.5f) * scale
        val endX = origin.x - 28f * scale + i * (if (giant) 8f else 11f) * scale
        drawLine(color.copy(alpha = 0.85f), Offset(startX, origin.y + 14f * scale + bob), Offset(endX, origin.y + (if (giant) 54f else 42f) * scale + bob), strokeWidth = if (giant) 3.2f * scale else 2.4f * scale)
    }
}

private fun DrawScope.drawKrakenScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val color = scheme.secondary.copy(alpha = 0.66f)
    drawCircle(color.copy(alpha = 0.16f), 64f * scale, origin)
    drawOval(color, Offset(origin.x - 26f * scale, origin.y - 34f * scale + bob), Size(52f * scale, 46f * scale))
    repeat(10) { i ->
        val angle = -2.8f + i * 0.62f
        val start = Offset(origin.x + kotlin.math.cos(angle) * 12f * scale, origin.y + 6f * scale + bob)
        val end = Offset(origin.x + kotlin.math.cos(angle) * (42f + (i % 3) * 8f) * scale, origin.y + 52f * scale + kotlin.math.sin(angle) * 12f * scale + bob)
        drawLine(color.copy(alpha = 0.78f), start, end, strokeWidth = 4f * scale)
    }
    drawCircle(scheme.onSurface.copy(alpha = 0.60f), 2.4f * scale, Offset(origin.x - 8f * scale, origin.y - 14f * scale + bob))
    drawCircle(scheme.onSurface.copy(alpha = 0.60f), 2.4f * scale, Offset(origin.x + 8f * scale, origin.y - 14f * scale + bob))
}

private fun DrawScope.drawLeviathanScene(origin: Offset, scale: Float, bob: Float, scheme: androidx.compose.material3.ColorScheme) {
    val c = Offset(origin.x, origin.y + bob)
    val body = Color(0xFF172A46).copy(alpha = 0.78f)
    val glow = scheme.secondary.copy(alpha = 0.30f)
    drawOval(glow.copy(alpha = 0.10f), Offset(c.x - 102f * scale, c.y - 48f * scale), Size(194f * scale, 92f * scale))
    val spine = Path().apply {
        moveTo(c.x - 88f * scale, c.y + 18f * scale)
        cubicTo(c.x - 52f * scale, c.y - 50f * scale, c.x + 16f * scale, c.y + 46f * scale, c.x + 70f * scale, c.y - 22f * scale)
    }
    drawPath(spine, body, style = Stroke(width = 15f * scale))
    drawPath(spine, glow, style = Stroke(width = 3f * scale))
    drawCircle(body.copy(alpha = 0.96f), 18f * scale, Offset(c.x + 76f * scale, c.y - 24f * scale))
    drawCircle(scheme.secondary.copy(alpha = 0.82f), 3f * scale, Offset(c.x + 83f * scale, c.y - 29f * scale))
    repeat(7) { i ->
        val x = c.x - 58f * scale + i * 20f * scale
        val y = c.y - 6f * scale + sin((i * 0.9f).toDouble()).toFloat() * 20f * scale
        drawPath(Path().apply { moveTo(x, y); lineTo(x + 7f * scale, y - 22f * scale); lineTo(x + 14f * scale, y + 1f * scale); close() }, glow.copy(alpha = 0.42f))
    }
    drawPath(Path().apply { moveTo(c.x + 88f * scale, c.y - 22f * scale); lineTo(c.x + 118f * scale, c.y - 42f * scale); lineTo(c.x + 98f * scale, c.y - 12f * scale); close() }, body)
}

private fun DrawScope.drawMinnow(origin: Offset, scale: Float, wiggle: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val body = scheme.primary.copy(alpha = if (glowing) 0.82f else 0.64f)
    val fin = scheme.secondary.copy(alpha = if (glowing) 0.58f else 0.36f)
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.18f), 22f * scale, origin)
    drawOval(body, Offset(origin.x - 14f * scale, origin.y - 6f * scale), Size(28f * scale, 12f * scale))
    val tail = Path().apply {
        moveTo(origin.x - 13f * scale, origin.y)
        lineTo(origin.x - 26f * scale, origin.y - (8f + wiggle * 3f) * scale)
        lineTo(origin.x - 25f * scale, origin.y + (8f - wiggle * 3f) * scale)
        close()
    }
    drawPath(tail, fin)
    val dorsal = Path().apply {
        moveTo(origin.x - 2f * scale, origin.y - 6f * scale)
        lineTo(origin.x + 5f * scale, origin.y - 13f * scale)
        lineTo(origin.x + 9f * scale, origin.y - 5f * scale)
        close()
    }
    drawPath(dorsal, fin.copy(alpha = fin.alpha * 0.75f))
    drawCircle(scheme.onSurface.copy(alpha = 0.74f), 1.6f * scale, Offset(origin.x + 9f * scale, origin.y - 1.5f * scale))
    drawCircle(scheme.secondary.copy(alpha = 0.32f), 1.7f * scale, Offset(origin.x + 2f * scale, origin.y + 2f * scale))
}

private fun DrawScope.drawSeahorse(origin: Offset, scale: Float, bob: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.16f), 30f * scale, origin)
    val color = scheme.secondary.copy(alpha = 0.58f)
    drawCircle(color, 10f * scale, Offset(origin.x, origin.y - 18f * scale))
    drawCircle(color.copy(alpha = 0.82f), 13f * scale, Offset(origin.x - 2f * scale, origin.y + 2f * scale))
    drawLine(color, Offset(origin.x + 7f * scale, origin.y - 19f * scale), Offset(origin.x + 22f * scale, origin.y - 23f * scale), strokeWidth = 5f * scale)
    val crest = Path().apply {
        moveTo(origin.x - 4f * scale, origin.y - 29f * scale)
        lineTo(origin.x + 2f * scale, origin.y - 38f * scale)
        lineTo(origin.x + 7f * scale, origin.y - 28f * scale)
    }
    drawPath(crest, color, style = Stroke(width = 3f * scale))
    val tail = Path().apply {
        moveTo(origin.x - 4f * scale, origin.y + 14f * scale)
        cubicTo(origin.x - 8f * scale, origin.y + 30f * scale, origin.x + 16f * scale, origin.y + 34f * scale, origin.x + 12f * scale, origin.y + 18f * scale)
    }
    drawPath(tail, color, style = Stroke(width = 4f * scale))
    drawOval(scheme.primary.copy(alpha = 0.24f), Offset(origin.x - 15f * scale, origin.y - (2f + bob) * scale), Size(10f * scale, 16f * scale))
    drawCircle(scheme.onSurface.copy(alpha = 0.72f), 1.7f * scale, Offset(origin.x + 6f * scale, origin.y - 21f * scale))
}

private fun DrawScope.drawManta(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val wingPulse = sin((drift * 6.28f).toDouble()).toFloat()
    val wingLift = wingPulse * 6f * scale
    val bodyColor = scheme.primary.copy(alpha = 0.50f)
    val wingColor = scheme.primary.copy(alpha = 0.42f)
    val accent = scheme.secondary.copy(alpha = if (glowing) 0.36f else 0.20f)

    if (glowing) {
        drawOval(
            color = scheme.secondary.copy(alpha = 0.10f),
            topLeft = Offset(origin.x - 92f * scale, origin.y - 58f * scale),
            size = Size(190f * scale, 116f * scale)
        )
    }

    // Right-facing manta/ray silhouette: cephalic lobes and eyes at the right,
    // trailing tail to the left. drawRenderedCreature mirrors this for leftward motion.
    val manta = Path().apply {
        moveTo(origin.x + 78f * scale, origin.y - 6f * scale)
        cubicTo(origin.x + 44f * scale, origin.y - 48f * scale - wingLift, origin.x - 22f * scale, origin.y - 54f * scale, origin.x - 88f * scale, origin.y - 22f * scale - wingLift)
        cubicTo(origin.x - 48f * scale, origin.y - 8f * scale, origin.x - 28f * scale, origin.y + 28f * scale, origin.x + 8f * scale, origin.y + 42f * scale)
        cubicTo(origin.x + 42f * scale, origin.y + 28f * scale, origin.x + 66f * scale, origin.y + 14f * scale, origin.x + 78f * scale, origin.y - 6f * scale)
        close()
    }
    drawPath(manta, wingColor)

    val body = Path().apply {
        moveTo(origin.x + 62f * scale, origin.y - 2f * scale)
        cubicTo(origin.x + 28f * scale, origin.y - 22f * scale, origin.x - 18f * scale, origin.y - 18f * scale, origin.x - 34f * scale, origin.y)
        cubicTo(origin.x - 16f * scale, origin.y + 18f * scale, origin.x + 30f * scale, origin.y + 20f * scale, origin.x + 62f * scale, origin.y - 2f * scale)
        close()
    }
    drawPath(body, bodyColor)
    drawPath(Path().apply {
        moveTo(origin.x + 66f * scale, origin.y - 15f * scale)
        lineTo(origin.x + 92f * scale, origin.y - 28f * scale)
        lineTo(origin.x + 78f * scale, origin.y - 6f * scale)
        close()
    }, bodyColor.copy(alpha = 0.62f))
    drawPath(Path().apply {
        moveTo(origin.x + 66f * scale, origin.y + 4f * scale)
        lineTo(origin.x + 92f * scale, origin.y + 16f * scale)
        lineTo(origin.x + 78f * scale, origin.y - 6f * scale)
        close()
    }, bodyColor.copy(alpha = 0.56f))

    drawCircle(scheme.onSurface.copy(alpha = 0.40f), 1.8f * scale, Offset(origin.x + 58f * scale, origin.y - 10f * scale))
    drawCircle(scheme.onSurface.copy(alpha = 0.34f), 1.6f * scale, Offset(origin.x + 55f * scale, origin.y + 4f * scale))

    val tailSway = sin((drift * 6.28f - 0.8f).toDouble()).toFloat() * 9f * scale
    val tail = Path().apply {
        moveTo(origin.x - 34f * scale, origin.y)
        cubicTo(origin.x - 70f * scale, origin.y + tailSway * 0.2f, origin.x - 100f * scale, origin.y + tailSway, origin.x - 130f * scale, origin.y + tailSway * 0.65f)
    }
    drawPath(tail, bodyColor.copy(alpha = 0.54f), style = Stroke(width = 2.6f * scale))

    if (glowing) {
        drawLine(accent, Offset(origin.x - 60f * scale, origin.y - 16f * scale - wingLift), Offset(origin.x + 20f * scale, origin.y - 4f * scale), strokeWidth = 2f * scale)
        drawLine(accent, Offset(origin.x - 42f * scale, origin.y + 22f * scale + wingLift), Offset(origin.x + 28f * scale, origin.y + 10f * scale), strokeWidth = 2f * scale)
    }
}


private fun DrawScope.drawWhaleProfile(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme, profileKey: String) {
    val key = profileKey.lowercase()
    val bob = sin((drift * 6.28f).toDouble()).toFloat() * 5f * scale
    // Whale silhouettes were originally authored facing left; normalize them to the
    // scene renderer convention (right-facing by default) so the shared
    // facingRight mirror in drawRenderedCreature matches movement direction.
    withTransform({ scale(scaleX = -1f, scaleY = 1f, pivot = origin) }) {
        when {
            "blue_whale" in key -> {
                val color = scheme.onSurface.copy(alpha = 0.44f)
                val rim = if (glowing) scheme.secondary.copy(alpha = 0.24f) else scheme.primary.copy(alpha = 0.18f)
                drawOval(rim, Offset(origin.x - 146f * scale, origin.y - 36f * scale + bob), Size(280f * scale, 70f * scale))
                drawOval(color, Offset(origin.x - 136f * scale, origin.y - 22f * scale + bob), Size(248f * scale, 42f * scale))
                drawOval(scheme.surface.copy(alpha = 0.30f), Offset(origin.x - 70f * scale, origin.y + 1f * scale + bob), Size(116f * scale, 16f * scale))
                drawPath(Path().apply { moveTo(origin.x + 102f * scale, origin.y + bob); lineTo(origin.x + 146f * scale, origin.y - 20f * scale + bob); lineTo(origin.x + 132f * scale, origin.y + bob); lineTo(origin.x + 148f * scale, origin.y + 20f * scale + bob); close() }, color.copy(alpha = 0.86f))
                drawPath(Path().apply { moveTo(origin.x + 14f * scale, origin.y - 20f * scale + bob); lineTo(origin.x + 28f * scale, origin.y - 38f * scale + bob); lineTo(origin.x + 34f * scale, origin.y - 18f * scale + bob); close() }, scheme.secondary.copy(alpha = 0.36f))
            }
            "humpback" in key -> {
                val color = scheme.onSurface.copy(alpha = 0.38f)
                val rim = if (glowing) scheme.secondary.copy(alpha = 0.24f) else scheme.primary.copy(alpha = 0.16f)
                val back = Path().apply {
                    moveTo(origin.x - 104f * scale, origin.y + 6f * scale + bob)
                    cubicTo(origin.x - 70f * scale, origin.y - 56f * scale + bob, origin.x + 36f * scale, origin.y - 42f * scale + bob, origin.x + 90f * scale, origin.y - 2f * scale + bob)
                    cubicTo(origin.x + 42f * scale, origin.y + 36f * scale + bob, origin.x - 56f * scale, origin.y + 42f * scale + bob, origin.x - 104f * scale, origin.y + 6f * scale + bob)
                    close()
                }
                drawPath(back, rim)
                drawPath(back, color)
                drawLine(scheme.secondary.copy(alpha = 0.32f), Offset(origin.x - 18f * scale, origin.y + 18f * scale + bob), Offset(origin.x - 80f * scale, origin.y + 76f * scale + bob), strokeWidth = 6f * scale)
                drawPath(Path().apply { moveTo(origin.x + 82f * scale, origin.y + bob); lineTo(origin.x + 124f * scale, origin.y - 24f * scale + bob); lineTo(origin.x + 110f * scale, origin.y + bob); lineTo(origin.x + 126f * scale, origin.y + 24f * scale + bob); close() }, color.copy(alpha = 0.82f))
                repeat(4) { i -> drawCircle(scheme.secondary.copy(alpha = 0.24f), 2f * scale, Offset(origin.x - 82f * scale + i * 10f * scale, origin.y - 6f * scale + bob)) }
            }
            else -> drawWhale(origin, scale, drift, glowing, scheme)
        }
    }
}

private fun DrawScope.drawWhale(origin: Offset, scale: Float, drift: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    val color = scheme.onSurface.copy(alpha = 0.36f)
    val rim = if (glowing) scheme.secondary.copy(alpha = 0.26f) else scheme.primary.copy(alpha = 0.18f)
    drawOval(rim, Offset(origin.x - 118f * scale, origin.y - 38f * scale), Size(220f * scale, 78f * scale))
    drawOval(color, Offset(origin.x - 108f * scale, origin.y - 28f * scale), Size(190f * scale, 56f * scale))
    drawOval(color.copy(alpha = 0.16f), Offset(origin.x - 54f * scale, origin.y + 2f * scale), Size(94f * scale, 22f * scale))
    val tailWave = sin((drift * 6.28f).toDouble()).toFloat() * 8f * scale
    val tail = Path().apply {
        moveTo(origin.x + 78f * scale, origin.y)
        lineTo(origin.x + 126f * scale, origin.y - 25f * scale + tailWave)
        lineTo(origin.x + 114f * scale, origin.y)
        lineTo(origin.x + 128f * scale, origin.y + 25f * scale + tailWave)
        close()
    }
    drawPath(tail, color.copy(alpha = 0.26f))
    drawCircle(scheme.background.copy(alpha = 0.45f), 2.4f * scale, Offset(origin.x - 74f * scale, origin.y - 8f * scale))
}

private fun DrawScope.drawOctopus(origin: Offset, pulse: Float, glowing: Boolean, scheme: androidx.compose.material3.ColorScheme) {
    if (glowing) drawCircle(scheme.secondary.copy(alpha = 0.15f), 46f * pulse, origin)
    val color = scheme.secondary.copy(alpha = 0.46f)
    drawOval(color, Offset(origin.x - 22f * pulse, origin.y - 30f * pulse), Size(44f * pulse, 38f * pulse))
    repeat(6) { i ->
        val startX = origin.x - 18f + i * 7f
        val curl = sin((pulse * 4f + i).toDouble()).toFloat() * 8f
        val tentacle = Path().apply {
            moveTo(startX, origin.y + 2f)
            cubicTo(startX - 10f, origin.y + 22f, startX + curl, origin.y + 32f, startX - 4f, origin.y + 44f)
        }
        drawPath(tentacle, color, style = Stroke(width = 4f))
    }
    drawCircle(scheme.onSurface.copy(alpha = 0.75f), 2.4f * pulse, Offset(origin.x + 8f * pulse, origin.y - 14f * pulse))
}