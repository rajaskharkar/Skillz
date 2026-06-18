package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneUiModel

@Composable
fun TheBlueCreatureTray(
    modifier: Modifier = Modifier,
    zone: TheBlueZoneUiModel,
    entryNewAnimalFindIds: Set<String>,
    onAnimalClick: (TheBlueAnimalGroupUiModel) -> Unit
) {
    var expanded by remember(zone.zoneId) { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TheBlueOverlaySurface {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.the_blue_swimming_here), fontWeight = FontWeight.Bold)
                    Text(
                        text = stringResource(if (expanded) R.string.shell_hide else R.string.shell_view_all),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
                if (zone.animals.isEmpty()) {
                    Text(stringResource(R.string.the_blue_zone_waiting), style = MaterialTheme.typography.bodySmall)
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        zone.animals.forEach { animal ->
                            TheBlueCreatureTile(animal, animal.isNew || animal.findId in entryNewAnimalFindIds) { onAnimalClick(animal) }
                        }
                    }
                }
            }
        }
        if (expanded && zone.animals.isNotEmpty()) {
            TheBlueOverlaySurface {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.the_blue_zone_life_title, zoneTitle(zone.zoneId)), fontWeight = FontWeight.SemiBold)
                    zone.animals.forEach { animal ->
                        TheBlueExpandedZoneInventoryRow(animal, animal.isNew || animal.findId in entryNewAnimalFindIds) { onAnimalClick(animal) }
                    }
                }
            }
        }
    }
}