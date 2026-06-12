package com.kingkharnivore.skillz.ui.screen.shell.rooms.blue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.screen.shell.TheBlueZoneId
import kotlin.collections.forEach

@Composable
fun TheBlueDepthRail(
    zones: List<TheBlueZoneId>,
    activeZone: TheBlueZoneId,
    onZoneClick: (TheBlueZoneId) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val railDescription = stringResource(R.string.the_blue_depth_rail_a11y)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = scheme.surface.copy(alpha = 0.68f),
        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.18f)),
        modifier = modifier
            .fillMaxHeight(0.48f)
            .width(58.dp)
            .semantics { contentDescription = railDescription }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            zones.forEach { zone ->
                val active = zone == activeZone
                val title = zoneTitle(zone)
                val goToDescription = stringResource(R.string.the_blue_depth_rail_go_to_zone_a11y, title)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .clickable(role = Role.Button) { onZoneClick(zone) }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                        .semantics {
                            contentDescription = goToDescription
                            role = Role.Button
                        }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (active) scheme.secondary else scheme.primary.copy(alpha = 0.24f),
                        modifier = Modifier.size(if (active) 12.dp else 8.dp),
                        content = {}
                    )
                    Text(
                        text = zoneRailLabel(zone),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) scheme.secondary else scheme.onSurface.copy(alpha = 0.58f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 36.dp, max = 52.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}