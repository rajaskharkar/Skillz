package com.kingkharnivore.skillz.ui.atlas

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.ui.atlas.components.HorizonControlsRow
import com.kingkharnivore.skillz.ui.atlas.components.HorizonTimeline
import com.kingkharnivore.skillz.ui.atlas.components.JourneyFilterRow
import com.kingkharnivore.skillz.ui.atlas.model.*
import com.kingkharnivore.skillz.ui.components.SkillzTopAppBar
import kotlin.math.max

@Composable
fun AtlasScreen(
    uiState: AtlasUiState,
    onFilterAll: () -> Unit,
    onFilterJourney: (Long) -> Unit,
    onStartFlow: () -> Unit,
    onGoToActiveFlow: () -> Unit,

    // Horizon actions
    onZoomHours: (Int) -> Unit,
    onShiftHours: (Int) -> Unit,
    onResetToNow: () -> Unit
) {
    Scaffold(
        topBar = { SkillzTopAppBar() }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                NowZone(
                    now = uiState.now,
                    onStartFlow = onStartFlow,
                    onGoToActiveFlow = onGoToActiveFlow
                )
            }

            item {
                HorizonZone(
                    uiState = uiState,
                    onFilterAll = onFilterAll,
                    onFilterJourney = onFilterJourney,
                    onZoomHours = onZoomHours,
                    onShiftHours = onShiftHours,
                    onResetToNow = onResetToNow
                )
            }

            // Aftermath later (kept but empty)
            item { AftermathZone(uiState.aftermath) }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun NowZone(
    now: NowState,
    onStartFlow: () -> Unit,
    onGoToActiveFlow: () -> Unit
) {
    if (now.isBeamActive && now.activeBeam != null) {
        Text(
            text = "Beam is active!",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = now.activeBeam.tagName,
                    style = MaterialTheme.typography.titleLarge
                )

                now.activeBeamRemainingMs?.let { ms ->
                    Text(
                        text = "Remaining: ${max(0L, ms) / 60_000L} min",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                now.activeBeamProgress?.let { p ->
                    LinearProgressIndicator(
                        progress = { p.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // You can swap to onGoToActiveFlow if you have a separate "active flow exists" signal.
                Button(onClick = onStartFlow) {
                    Text("Enter Flow")
                }
            }
        }
    } else {
        Text(text = "Atlas", style = MaterialTheme.typography.titleLarge)

        now.nextBeam?.let {
            Text(
                text = "Next Beam: ${it.tagName}",
                style = MaterialTheme.typography.bodyMedium
            )
        } ?: Text(
            text = "No upcoming Beams.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun HorizonZone(
    uiState: AtlasUiState,
    onFilterAll: () -> Unit,
    onFilterJourney: (Long) -> Unit,
    onZoomHours: (Int) -> Unit,
    onShiftHours: (Int) -> Unit,
    onResetToNow: () -> Unit
) {
    Text(
        text = "Horizon",
        style = MaterialTheme.typography.titleMedium
    )

    HorizonControlsRow(
        title = uiState.horizon.title(),
        selectedHours = uiState.horizon.hours,
        onZoomHours = onZoomHours,
        onEarlier = { onShiftHours(-2) },
        onNow = onResetToNow,
        onLater = { onShiftHours(2) }
    )

    JourneyFilterRow(
        journeys = uiState.availableJourneys,
        selected = uiState.journeyFilter,
        onSelectAll = onFilterAll,
        onSelect = onFilterJourney
    )

    if (uiState.timeline.blocks.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Nothing on the horizon",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Schedule a Beam to see your timeblocks here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    HorizonTimeline(
        horizon = uiState.horizon,
        ticks = uiState.timeline.ticks,
        blocks = uiState.timeline.blocks,
        height = 540.dp,
        canvasHeight = 1100.dp
    )
}

@Composable
private fun AftermathZone(aftermath: AftermathModel) {
    Text(
        text = "Aftermath",
        style = MaterialTheme.typography.titleMedium
    )

    if (aftermath.completed.isEmpty()) {
        Text(
            text = "No completed Beams yet.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}
