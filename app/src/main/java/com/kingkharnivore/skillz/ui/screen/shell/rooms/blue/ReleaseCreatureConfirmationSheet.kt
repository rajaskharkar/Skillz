package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.shell.CreatureEconomy
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseCreatureConfirmationSheet(
    animal: TheBlueAnimalGroupUiModel,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val name = findName(animal.findId)
    val instanceId = animal.firstRestingInstanceId ?: animal.firstActiveInstanceId
    val releaseValue = animal.releaseValuePearls ?: CreatureEconomy.releaseValuePearls(animal.findId, animal.highestLevel)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(stringResource(R.string.shell_creature_release_confirm_title, name, releaseValue), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.shell_creature_release_confirm_body))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.shell_creature_keep_swimming)) }
                Button(
                    onClick = { instanceId?.let(onConfirm) },
                    enabled = instanceId != null,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.shell_creature_release_for_pearls)) }
            }
        }
    }
}