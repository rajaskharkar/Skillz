package com.kingkharnivore.skillz.ui.screen.shell.rooms.stillwater

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.data.model.shell.StillwaterPerspective
import com.kingkharnivore.skillz.ui.screen.shell.icons.draw.TurtleShellInteriorBackground
import com.kingkharnivore.skillz.ui.screen.shell.ux.RoomHeader
import com.kingkharnivore.skillz.ui.screen.shell.ux.shellChamberBrush
import com.kingkharnivore.skillz.viewmodel.shell.ShellUiState

@Composable
fun StillwaterRoomScreen(
    uiState: ShellUiState,
    onPerspective: (StillwaterPerspective) -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RoomHeader(
            title = R.string.shell_room_stillwater_title,
            body = R.string.shell_stillwater_body
        )

        ElevatedCard(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = scheme.primary
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(shellChamberBrush())
            ) {
                TurtleShellInteriorBackground(
                    modifier = Modifier.matchParentSize(),
                    centerGlow = true
                )

                Canvas(Modifier.matchParentSize()) {
                    repeat(6) { i ->
                        drawCircle(
                            color = scheme.primary.copy(alpha = 0.14f),
                            radius = 44f + i * 18f,
                            center = Offset(size.width / 2, size.height / 2),
                            style = Stroke(3f)
                        )
                    }
                }

                Text(
                    text = displayStillwater(uiState.stillwaterTotal, uiState.perspective),
                    color = scheme.onPrimary,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(24.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.secondary.copy(alpha = 0.24f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.shell_view_as),
                    color = scheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                val selectorDescription = stringResource(R.string.shell_stillwater_selector_a11y)

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .semantics {
                            contentDescription = selectorDescription
                        }
                ) {
                    StillwaterPerspective.entries.forEach { perspective ->
                        FilterChip(
                            selected = uiState.perspective == perspective,
                            onClick = { onPerspective(perspective) },
                            label = { Text(stringResource(labelFor(perspective))) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.shell_stillwater_same_water),
                    color = scheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = stringResource(R.string.shell_soft_flow_copy),
                    color = scheme.onSurface.copy(alpha = 0.76f)
                )
            }
        }
    }
}

private fun labelFor(p: StillwaterPerspective): Int = when (p) {
    StillwaterPerspective.CUPS -> R.string.shell_perspective_cups
    StillwaterPerspective.BOWLS -> R.string.shell_perspective_bowls
    StillwaterPerspective.TANK -> R.string.shell_perspective_tank
    StillwaterPerspective.POOL -> R.string.shell_perspective_pool
    StillwaterPerspective.LAKE -> R.string.shell_perspective_lake
    StillwaterPerspective.LAKE_TAHOE_PERCENT -> R.string.shell_perspective_tahoe
    StillwaterPerspective.WORLD_OCEAN_PERCENT -> R.string.shell_perspective_ocean
    StillwaterPerspective.STREAM_TIME -> R.string.shell_perspective_stream
}

@Composable
private fun displayStillwater(
    units: Long,
    p: StillwaterPerspective
): String = when (p) {
    StillwaterPerspective.CUPS -> stringResource(R.string.shell_stillwater_cups, units / 2.0)
    StillwaterPerspective.BOWLS -> stringResource(R.string.shell_stillwater_bowls, units / 10.0)
    StillwaterPerspective.TANK -> stringResource(R.string.shell_stillwater_tank, units / 600.0)
    StillwaterPerspective.POOL -> stringResource(R.string.shell_stillwater_pool, units / 20_000.0)
    StillwaterPerspective.LAKE -> stringResource(R.string.shell_stillwater_lake, units / 2_000_000.0)
    StillwaterPerspective.LAKE_TAHOE_PERCENT -> stringResource(
        R.string.shell_stillwater_tahoe,
        units / 39_000_000_000.0 * 100.0
    )
    StillwaterPerspective.WORLD_OCEAN_PERCENT -> stringResource(
        R.string.shell_stillwater_ocean,
        units / 1_350_000_000_000_000.0 * 100.0
    )
    StillwaterPerspective.STREAM_TIME -> stringResource(R.string.shell_stillwater_stream, units / 10L)
}
