package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.atlas.components.HorizonAnchorUi
import com.kingkharnivore.skillz.ui.atlas.components.HorizonControlsRow
import com.kingkharnivore.skillz.ui.atlas.components.HorizonTimeline
import com.kingkharnivore.skillz.ui.atlas.components.JourneyFilterRow
import com.kingkharnivore.skillz.ui.atlas.model.*
import com.kingkharnivore.skillz.ui.components.SkillzTopAppBar
import kotlinx.coroutines.delay
import kotlin.math.max

@Composable
fun AtlasScreen(
    uiState: AtlasUiState,
    onFilterAll: () -> Unit,
    onFilterJourney: (Long) -> Unit,
    onStartFlow: () -> Unit,
    onGoToActiveFlow: () -> Unit,
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        if (now.isBeamActive && now.activeBeam != null) {

            Text(
                text = "Beam is active",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = now.activeBeam.tagName,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    now.activeBeamRemainingMs?.let { ms ->
                        Text(
                            text = "Remaining · ${max(0L, ms) / 60_000L} min",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                    now.activeBeamProgress?.let { p ->
                        LinearProgressIndicator(
                            progress = { p.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            trackColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.25f)
                        )
                    }

                    Button(
                        onClick = onStartFlow,
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        Text("Enter Flow")
                    }
                }
            }
        } else {

            Text(
                text = "Atlas",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )

            now.nextBeam?.let { beam ->
                CountdownText(
                    targetTimeMs = beam.startMs,
                    modifier = Modifier.fillMaxWidth()
                )
            } ?: Text(
                text = "No upcoming Beams",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CountdownText(
    targetTimeMs: Long,
    modifier: Modifier = Modifier
) {
    var remainingMs by remember(targetTimeMs) {
        mutableStateOf(targetTimeMs - System.currentTimeMillis())
    }

    LaunchedEffect(targetTimeMs) {
        while (remainingMs > 0) {
            delay(1_000L)
            remainingMs = targetTimeMs - System.currentTimeMillis()
        }
    }

    val totalSeconds = (remainingMs / 1_000L).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60

    val headline = when {
        totalSeconds <= 0 -> "BEAM IMMINENT"
        else -> "NEXT BEAM"
    }

    val countdown = when {
        totalSeconds <= 0 -> "Prepare: ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = headline,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 1.4.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            textAlign = TextAlign.Center
        )

        Text(
            text = countdown,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        if (totalSeconds > 0) {
            Text(
                text = "Until Beam Begins",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
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

    var anchor by remember { mutableStateOf(HorizonAnchorUi.NOW) }
    HorizonControlsRow(
        title = uiState.horizon.title(),
        selectedHours = uiState.horizon.hours,
        selectedAnchor = anchor,
        onZoomHours = onZoomHours,
        onEarlier = { anchor = HorizonAnchorUi.EARLIER; onShiftHours(-2) },
        onNow = { anchor = HorizonAnchorUi.NOW; onResetToNow() },
        onLater = { anchor = HorizonAnchorUi.LATER; onShiftHours(2) }
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
