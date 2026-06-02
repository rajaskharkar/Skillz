package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseCreatureConfirmationSheet(
    animal: TheBlueAnimalGroupUiModel,
    onDismiss: () -> Unit,
    onConfirm: (findId: String, quantity: Int) -> Unit
) {
    val name = findName(animal.findId)
    val ownedQuantity = animal.totalCount.coerceAtLeast(0)
    val safeOwnedQuantity = ownedQuantity.coerceAtLeast(1)
    val releaseValue = animal.releaseValuePearls ?: CreatureEconomy.releaseValuePearls(animal.findId, animal.highestLevel)
    var selectedQuantity by rememberSaveable(animal.findId, ownedQuantity) { mutableStateOf(1) }
    var confirming by remember { mutableStateOf(false) }

    val clampedQuantity = selectedQuantity.coerceIn(1, safeOwnedQuantity)
    val totalReward = clampedQuantity * releaseValue
    val canRelease = ownedQuantity > 0 && !confirming

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                stringResource(R.string.shell_creature_release_confirm_title, name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (ownedQuantity > 1) {
                Text(stringResource(R.string.shell_creature_release_bulk_body, ownedQuantity, name))
                CreatureReleaseQuantitySelector(
                    selectedQuantity = clampedQuantity,
                    maxQuantity = ownedQuantity,
                    onQuantityChange = { selectedQuantity = it.coerceIn(1, safeOwnedQuantity) },
                    creatureName = name,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text(stringResource(R.string.shell_creature_release_single_body, name, releaseValue))
            }
            Text(
                text = stringResource(R.string.shell_creature_release_reward_preview, totalReward),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.semantics {
                    contentDescription = "You will receive $totalReward Pearls"
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.shell_creature_keep_swimming))
                }
                Button(
                    onClick = {
                        if (canRelease) {
                            confirming = true
                            onConfirm(animal.findId, clampedQuantity)
                        }
                    },
                    enabled = canRelease,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (ownedQuantity > 1) {
                            stringResource(R.string.shell_creature_release_confirm_bulk, clampedQuantity, totalReward)
                        } else {
                            stringResource(R.string.shell_creature_release_confirm_single, totalReward)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatureReleaseQuantitySelector(
    selectedQuantity: Int,
    maxQuantity: Int,
    onQuantityChange: (Int) -> Unit,
    creatureName: String,
    modifier: Modifier = Modifier
) {
    val safeMax = maxQuantity.coerceAtLeast(1)
    val decreaseDescription = stringResource(R.string.shell_creature_release_quantity_decrease)
    val increaseDescription = stringResource(R.string.shell_creature_release_quantity_increase)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.shell_creature_release_selected_quantity, selectedQuantity, safeMax),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.semantics {
                contentDescription = "Selected quantity: $selectedQuantity of $safeMax $creatureName"
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { onQuantityChange((selectedQuantity - 1).coerceAtLeast(1)) },
                enabled = selectedQuantity > 1
            ) {
                Icon(Icons.Outlined.Remove, contentDescription = decreaseDescription)
            }
            Text(
                text = selectedQuantity.toString(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { onQuantityChange((selectedQuantity + 1).coerceAtMost(safeMax)) },
                enabled = selectedQuantity < safeMax
            ) {
                Icon(Icons.Outlined.Add, contentDescription = increaseDescription)
            }
        }
        Slider(
            value = selectedQuantity.toFloat(),
            onValueChange = { value -> onQuantityChange(value.roundToInt().coerceIn(1, safeMax)) },
            valueRange = 1f..safeMax.toFloat(),
            steps = (safeMax - 2).coerceAtLeast(0)
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1")
            Text(safeMax.toString())
        }
    }
}
