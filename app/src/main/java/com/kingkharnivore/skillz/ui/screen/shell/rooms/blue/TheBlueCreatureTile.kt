package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.domain.shell.CreatureCatalog
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueAnimalGroupUiModel
import com.kingkharnivore.skillz.ui.screen.shell.icons.ShellObjectIcon
import com.kingkharnivore.skillz.ui.screen.shell.inventory.ShellMetricPill

@Composable
fun TheBlueCreatureTile(animal: TheBlueAnimalGroupUiModel, isNewArrival: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val name = findName(animal.findId)
    val contentDescription = stringResource(R.string.the_blue_creature_tile_a11y, name, animal.totalCount, animal.highestLevel, if (isNewArrival) stringResource(R.string.the_blue_new_arrival) else "")
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = scheme.surface.copy(alpha = 0.84f),
        border = BorderStroke(1.dp, if (isNewArrival) scheme.secondary.copy(alpha = 0.70f) else scheme.primary.copy(alpha = 0.18f)),
        modifier = Modifier
            .widthIn(min = 110.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .semantics {
                this.contentDescription = contentDescription
                role = Role.Button
            }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShellObjectIcon(CreatureCatalog.get(animal.findId)?.staticIconKey ?: "animal", Modifier.size(30.dp))
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Surface(shape = RoundedCornerShape(999.dp), color = scheme.primary.copy(alpha = 0.15f)) {
                    Text(stringResource(R.string.the_blue_count_badge, animal.totalCount), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                ShellMetricPill(icon = Icons.Outlined.EmojiEvents, text = stringResource(R.string.shell_creature_level_short, animal.highestLevel))
            }
            if (isNewArrival) {
                Surface(shape = RoundedCornerShape(999.dp), color = scheme.secondary.copy(alpha = 0.2f)) {
                    Text(stringResource(R.string.the_blue_new_arrival), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), color = scheme.secondary)
                }
            }
        }
    }
}