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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import com.kingkharnivore.skillz.data.model.shell.ShellFindDefinition
import com.kingkharnivore.skillz.data.model.shell.ShellRewardKind
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.domain.shell.CreatureStatus
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellAnimalCanvasIcon
import com.kingkharnivore.skillz.ui.screen.shell.sourceReasonFor
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjectCopySheet(
    item: UserShellFindInstanceEntity,
    pearlBalance: Int,
    displayed: Boolean,
    onDismiss: () -> Unit,
    onReturn: (String) -> Unit,
    onUpgrade: (String) -> Unit,
    onPlaceInFocus: (() -> Unit)?
) {
    val def = ShellContentCatalog.find(item.findId)
    val isAnimal = def?.kind == ShellRewardKind.ANIMAL

    val current = if (!isAnimal) {
        def?.let {
            ShellContentCatalog.upgradesFor(it.findId)
                .firstOrNull { stage -> stage.upgradeStageId == item.currentUpgradeStageId }
        }
    } else {
        null
    }

    val next = if (!isAnimal) {
        def?.let { ShellContentCatalog.nextUpgrade(it.findId, item.currentUpgradeStageId) }
    } else {
        null
    }

    val findTitle = if (def != null) {
        stringResource(def.titleRes)
    } else {
        item.findId
    }

    val currentTitle = if (current != null) {
        stringResource(current.titleRes)
    } else {
        findTitle
    }

    val kindText = if (def != null) {
        kindLabel(def.kind)
    } else {
        null
    }

    val sourceText = if (def != null) {
        stringResource(R.string.shell_source_label, sourceReasonFor(def))
    } else {
        null
    }

    val statusText = when {
        displayed -> stringResource(R.string.shell_status_displayed_focus)
        isAnimal && item.creatureStatus != CreatureStatus.ACTIVE ->
            "Lifetime record · ${item.creatureStatus.lowercase().replace('_', ' ')}"
        else -> stringResource(R.string.shell_status_resting)
    }

    val animalUpgradeA11y = stringResource(R.string.shell_upgrade_animal_a11y)
    val returnToChestText = returnToChestLabel(def)
    val placeInFocusText = placeInFocusLabel(def)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = findTitle,
                style = MaterialTheme.typography.titleLarge
            )

            if (kindText != null) {
                Text(kindText)
            }

            Text(statusText)

            if (isAnimal) {
                Text(stringResource(R.string.shell_creature_level_value, item.animalLevel.coerceAtLeast(1)))

                if (sourceText != null) {
                    Text(sourceText)
                }

                if (item.creatureStatus == CreatureStatus.ACTIVE) {
                    val cost = CreatureEconomy.growthCostPearls(
                        item.findId,
                        item.animalLevel.coerceAtLeast(1)
                    )
                    val canAfford = pearlBalance >= cost

                    if (!canAfford) {
                        Text(stringResource(R.string.shell_need_more_pearls, cost - pearlBalance))
                    }

                    Button(
                        onClick = { onUpgrade(item.instanceId) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = animalUpgradeA11y
                        }
                    ) {
                        Text(stringResource(R.string.shell_creature_grow_with_pearls_cost, cost))
                    }
                } else {
                    Text(stringResource(R.string.shell_creature_not_swimming_lifetime_remains))
                }
            } else {
                Text(stringResource(R.string.shell_form_label, currentTitle))

                if (sourceText != null) {
                    Text(sourceText)
                }

                if (next != null && def != null) {
                    val nextTitle = stringResource(next.titleRes)
                    val upgradeVerb = stringResource(next.upgradeVerbRes)
                    val upgradeDescription = upgradeA11yLabel(def)
                    val canAfford = pearlBalance >= next.pearlCost

                    Text(stringResource(R.string.shell_next_form, nextTitle))

                    if (!canAfford) {
                        Text(stringResource(R.string.shell_need_more_pearls, next.pearlCost - pearlBalance))
                    }

                    Button(
                        onClick = { onUpgrade(item.instanceId) },
                        enabled = canAfford,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = upgradeDescription
                        }
                    ) {
                        Text(
                            stringResource(
                                R.string.shell_upgrade_with_pearls,
                                upgradeVerb,
                                next.pearlCost
                            )
                        )
                    }
                } else {
                    Text(restingCurrentFormLabel(def))
                }
            }

            if (displayed) {
                OutlinedButton(onClick = { onReturn(item.instanceId) }) {
                    Text(returnToChestText)
                }
            } else if (onPlaceInFocus != null && canDisplayInstance(item, def)) {
                OutlinedButton(onClick = onPlaceInFocus) {
                    Text(placeInFocusText)
                }
            }
        }
    }
}

@Composable
private fun returnToChestLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_let_rest_in_chest)
    else -> stringResource(R.string.shell_return_to_chest)
}

@Composable
private fun placeInFocusLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_display_in_focus)
    else -> stringResource(R.string.shell_place_in_focus)
}

@Composable
private fun upgradeA11yLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_upgrade_animal_a11y)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_upgrade_object_a11y)
    else -> stringResource(R.string.shell_upgrade_reward_a11y)
}

@Composable
private fun restingCurrentFormLabel(def: ShellFindDefinition?): String = when (def?.kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_animal_no_more_forms)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_object_no_more_forms)
    else -> stringResource(R.string.shell_reward_no_more_forms)
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

fun canDisplayInstance(instance: UserShellFindInstanceEntity, def: ShellFindDefinition?): Boolean =
    isUserVisibleShellFind(def) && (def?.kind != ShellRewardKind.ANIMAL || instance.creatureStatus == CreatureStatus.ACTIVE)

fun isUserVisibleShellFind(def: ShellFindDefinition?): Boolean =
    def != null && def.kind != ShellRewardKind.TRINKET

fun restingFinds(uiState: ShellUiState): List<UserShellFindInstanceEntity> {
    val displayed = displayedInstanceIds(uiState)
    return uiState.finds.filter { item ->
        item.instanceId !in displayed && canDisplayInstance(item, ShellContentCatalog.find(item.findId))
    }
}

fun displayedInstanceIds(uiState: ShellUiState): Set<String> =
    uiState.focusPlacements.map { it.instanceId }.toSet()

@Composable
fun kindLabel(kind: ShellRewardKind): String = when (kind) {
    ShellRewardKind.ANIMAL -> stringResource(R.string.shell_kind_animal)
    ShellRewardKind.OBJECT -> stringResource(R.string.shell_kind_object)
    ShellRewardKind.TRINKET -> stringResource(R.string.shell_kind_trinket)
    ShellRewardKind.DISCOVERY -> stringResource(R.string.shell_kind_discovery)
}