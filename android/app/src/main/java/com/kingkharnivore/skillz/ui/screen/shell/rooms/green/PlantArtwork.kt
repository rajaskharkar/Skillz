package com.kingkharnivore.skillz.ui.screen.shell.rooms.green

import android.annotation.SuppressLint
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.kingkharnivore.skillz.domain.green.*

val LocalGreenCalmMode = staticCompositionLocalOf { false }

/** Final drawable assets can replace any of the 420 stable keys independently. */
@SuppressLint("DiscouragedApi")
@Composable
fun PlantArtwork(species: PlantSpecies, level: Int, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val stage = PlantGrowthStageResolver.resolve(level)
    Crossfade(stage.assetIndex, modifier.clipToBounds().semantics { contentDescription = description },
        animationSpec = tween(if (LocalGreenCalmMode.current) 0 else if (stage.assetIndex == 6) 800 else 350), label = "plant stage") { index ->
        val asset = remember(resources, species.id, index) { resources.getIdentifier(species.stageAssetKeys[index], "drawable", context.packageName) }
        if (asset != 0) Image(painterResource(asset), null, Modifier.fillMaxSize())
        else Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            drawBotanicalPlant(species.id, index)
        }
    }
}
