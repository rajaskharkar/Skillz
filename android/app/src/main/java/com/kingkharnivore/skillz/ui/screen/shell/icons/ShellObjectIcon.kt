package com.kingkharnivore.skillz.ui.screen.shell.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.drawStaticCreatureIcon

@Composable
fun ShellObjectIcon(
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val isCreature = iconKey.contains("creature", ignoreCase = true) ||
            listOf("minnow", "seahorse", "manta", "whale", "octopus", "jellyfish", "turtle", "shark", "dolphin", "squid", "starfish", "urchin", "eel", "fish", "seal", "otter", "penguin", "orca", "kraken", "leviathan").any { iconKey.contains(it, ignoreCase = true) }
    val vector = when {
        isCreature -> null
        "kelp" in iconKey || "curtain" in iconKey -> Icons.Outlined.Grass
        "bubble" in iconKey || "current" in iconKey -> Icons.Outlined.Waves
        "coral" in iconKey || "perch" in iconKey -> Icons.Outlined.FilterVintage
        else -> Icons.Outlined.Diamond
    }
    Surface(shape = CircleShape, color = scheme.primary.copy(alpha = 0.16f), modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            if (isCreature) {
                ShellAnimalCanvasIcon(iconKey, Modifier.fillMaxSize().padding(5.dp))
            } else if (vector != null) {
                Icon(imageVector = vector, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
fun ShellAnimalCanvasIcon(
    iconKey: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Canvas(modifier) {
        drawStaticCreatureIcon(iconKey.lowercase(), scheme)
    }
}