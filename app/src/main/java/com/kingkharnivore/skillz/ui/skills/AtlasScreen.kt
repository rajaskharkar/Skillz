package com.kingkharnivore.skillz.ui.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.atlas.components.HorizonAnchorUi
import com.kingkharnivore.skillz.ui.atlas.components.HorizonControlsRow
import com.kingkharnivore.skillz.ui.atlas.components.HorizonTimeline
import com.kingkharnivore.skillz.ui.atlas.components.formatRange
import com.kingkharnivore.skillz.ui.atlas.model.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.95f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = "ATLAS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Your journeys await",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    now.nextBeam?.let { beam ->
                        CountdownText(
                            targetTimeMs = beam.startMs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } ?: Text(
                        text = "No upcoming Beams scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    )
                }
            }
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

    val (headline, countdown) = formatBeamCountdown(
        remainingMs = remainingMs,
        targetTimeMs = targetTimeMs
    )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizonZone(
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

    // ✅ Selected block for quick details
    var selectedBlock by remember { mutableStateOf<BeamBlockUi?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // ✅ Quick details sheet (tinted with journey color)
    // ✅ DROP-IN replacement for your selectedBlock ModalBottomSheet block
// - Sheet background is the Beam’s journey color
// - Content uses on-journey-color text for readability
// - Keeps your note logic + close button

    if (selectedBlock != null) {
        val b = selectedBlock!!
        val journeyColor = androidx.compose.ui.graphics.Color(b.journeyColorArgb)

        // Darken the journey color slightly (keeps hue intact)
        val sheetBase = journeyColor.copy(alpha = 0.88f)

        // Foreground color tuned for darker surface
        val onJourney = androidx.compose.ui.graphics.Color.White

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { selectedBlock = null },
            containerColor = sheetBase,
            contentColor = onJourney,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = onJourney.copy(alpha = 0.45f)
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f), // darker top
                                androidx.compose.ui.graphics.Color.Transparent
                            )
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = b.tagName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onJourney
                    )

                    val mins = ((b.endMs - b.startMs) / 60_000L).coerceAtLeast(1)

                    Text(
                        text = "${formatRange(b.startMs, b.endMs)} • ${mins}m",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onJourney.copy(alpha = 0.85f)
                    )

                    if (b.clippedTop || b.clippedBottom) {
                        val note = buildString {
                            if (b.clippedTop) append("Starts earlier (outside view). ")
                            if (b.clippedBottom) append("Ends later (outside view).")
                        }.trim()

                        Text(
                            text = note,
                            style = MaterialTheme.typography.bodySmall,
                            color = onJourney.copy(alpha = 0.72f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { selectedBlock = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = onJourney.copy(alpha = 0.16f),
                            contentColor = onJourney
                        )
                    ) {
                        Text("Close")
                    }

                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }

    HorizonControlsRow(
        title = uiState.horizon.title(),
        selectedHours = uiState.horizon.hours,
        selectedAnchor = anchor,
        onZoomHours = onZoomHours,
        onEarlier = { anchor = HorizonAnchorUi.EARLIER; onShiftHours(-2) },
        onNow = { anchor = HorizonAnchorUi.NOW; onResetToNow() },
        onLater = { anchor = HorizonAnchorUi.LATER; onShiftHours(2) }
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
        canvasHeight = 1100.dp,
        onBlockClick = { b -> selectedBlock = b } // ✅ THIS is the missing change
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

private fun formatBeamCountdown(
    remainingMs: Long,
    targetTimeMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): Pair<String, String> {
    val totalSeconds = (remainingMs / 1_000L).coerceAtLeast(0)

    val days = totalSeconds / 86_400
    val hours = (totalSeconds % 86_400) / 3_600
    val minutes = (totalSeconds % 3_600) / 60
    val seconds = totalSeconds % 60

    val headline = if (totalSeconds <= 0) "BEAM IMMINENT" else "NEXT BEAM"

    // If it's far out, show "DAY + TIME" vibe
    if (days >= 3) {
        val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d")   // "Fri, Feb 2"
        val timeFmt = DateTimeFormatter.ofPattern("h:mm a")      // "7:30 PM"
        val zdt = Instant.ofEpochMilli(targetTimeMs).atZone(zoneId)

        val whenText = "${zdt.format(dayFmt)} · ${zdt.format(timeFmt)}"
        val countdownText = "${days}d ${hours}h ${minutes}m"
        // You asked: "If more than two days, along with above mention Day and time"
        // So we include day+time AND still show the countdown.
        return headline to "$countdownText\n$whenText"
    }

    // Otherwise, normal countdown ladder
    val countdown = when {
        totalSeconds <= 0 -> "Starting now"
        days > 0 -> "${days}d ${hours}h ${minutes}m"
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
    return headline to countdown
}
