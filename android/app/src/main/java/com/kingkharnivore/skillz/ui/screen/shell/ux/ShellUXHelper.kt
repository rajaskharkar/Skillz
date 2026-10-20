package com.kingkharnivore.skillz.ui.screen.shell.ux

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Diamond
import androidx.compose.material.icons.outlined.FilterVintage
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.entity.shell.UserShellFindInstanceEntity
import com.kingkharnivore.skillz.data.model.shell.ShellContentCatalog
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.utils.shell.CreatureStatus
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellAnimalCanvasIcon
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState
import kotlin.math.pow

@Composable
fun shellIndicatorColor(): Color {
    val scheme = MaterialTheme.colorScheme
    val secondaryContrast = contrastRatio(scheme.secondary, scheme.surface)
    return if (secondaryContrast >= 3f) scheme.secondary else scheme.primary
}

private fun contrastRatio(a: Color, b: Color): Float {
    fun channel(v: Float): Float = if (v <= 0.03928f) {
        v / 12.92f
    } else {
        ((v + 0.055f) / 1.055f).pow(2.4f)
    }

    fun luminance(color: Color): Float {
        val r = channel(color.red)
        val g = channel(color.green)
        val b = channel(color.blue)
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }

    val l1 = luminance(a)
    val l2 = luminance(b)
    val lighter = maxOf(l1, l2)
    val darker = minOf(l1, l2)
    return (lighter + 0.05f) / (darker + 0.05f)
}

@Composable
fun RoomHeader(
    title: Int,
    body: Int
) {
    val scheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = scheme.surface.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onSurface
            )

            Text(
                text = stringResource(body),
                color = scheme.onSurface.copy(alpha = 0.78f)
            )
        }
    }
}

@Composable
private fun ShellObjectIcon(
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
fun shellChamberBrush(): Brush {
    val scheme = MaterialTheme.colorScheme

    return Brush.radialGradient(
        colors = listOf(
            scheme.secondary.copy(alpha = 0.24f),
            scheme.primary.copy(alpha = 0.82f),
            scheme.background
        )
    )
}


fun isActiveChestCreature(instance: UserShellFindInstanceEntity): Boolean =
    instance.creatureStatus == CreatureStatus.ACTIVE &&
        ShellContentCatalog.find(instance.findId)?.kind == ShellRewardKind.ANIMAL

fun activeChestCreatures(uiState: ShellUiState): List<UserShellFindInstanceEntity> =
    uiState.finds.filter(::isActiveChestCreature)

fun activeChestCreatureCount(uiState: ShellUiState): Int = activeChestCreatures(uiState).size

@Composable
fun kindLabel(kind: ShellRewardKind): String = when (kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_kind_animal)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_kind_object)
    ShellRewardKind.TRINKET -> ""
    ShellRewardKind.DISCOVERY -> ""
}
