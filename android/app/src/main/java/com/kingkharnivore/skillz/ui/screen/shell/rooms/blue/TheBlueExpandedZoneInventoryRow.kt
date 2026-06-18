package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellMetricPill

@Composable
fun TheBlueExpandedZoneInventoryRow(animal: TheBlueAnimalGroupUiModel, isNewArrival: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(findName(animal.findId), fontWeight = FontWeight.SemiBold)
            Text(stringResource(R.string.the_blue_highest_level_chip, animal.highestLevel))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            animal.levelCounts.forEach { level ->
                val lv = level.formStageId?.removePrefix("Level ")?.toIntOrNull() ?: 1
                ShellMetricPill(icon = Icons.Outlined.EmojiEvents, text = stringResource(R.string.shell_creature_level_count_chip, lv, level.count))
            }
        }
    }
}