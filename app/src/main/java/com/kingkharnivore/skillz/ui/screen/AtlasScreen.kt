package com.kingkharnivore.skillz.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.screen.components.atlas.AtlasHeader
import com.kingkharnivore.skillz.ui.screen.components.atlas.DayAgendaTimeline
import com.kingkharnivore.skillz.ui.atlas.model.*
import com.kingkharnivore.skillz.ui.theme.Bronze
import com.kingkharnivore.skillz.ui.theme.GryffindorRed
import com.kingkharnivore.skillz.ui.theme.RavenclawBlue
import com.kingkharnivore.skillz.utils.time.formatRange
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtlasScreen(
    uiState: AtlasUiState,
    onStartFlow: () -> Unit,
    onSelectMode: (AtlasViewMode) -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onAdvanceDay: (Long) -> Unit
) {
    var selectedBeam by remember { mutableStateOf<BeamBlockUi?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    if (selectedBeam != null) {
        val b = selectedBeam!!
        val journeyColor = Color(b.journeyColorArgb)
        val sheetBase = journeyColor.copy(alpha = 0.88f)
        val onJourney = Color.White

        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { selectedBeam = null },
            containerColor = sheetBase,
            contentColor = onJourney,
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = onJourney.copy(alpha = 0.45f)
                )
            }
        ) {
            BeamDetailsSheetContent(
                b = b,
                onClose = { selectedBeam = null }
            )
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Keep NowZone if you want it — but slim later.
        // For now we can keep it; it's not scroll-nested anymore.
        NowZone(
            now = uiState.now,
            onStartFlow = onStartFlow
        )

        val beamsCountLabel = when (uiState.viewMode) {
            AtlasViewMode.DAY -> {
                val n = uiState.dayPlan.beamsCount
                if (n == 1) "1 beam ⭐" else "$n beams ⭐"
            }
            AtlasViewMode.WEEK -> "Week"
            AtlasViewMode.MONTH -> "Month"
        }
        val canGoPrev = uiState.minSelectableDayStartMs?.let { uiState.selectedDayStartMs > it } ?: true

        AtlasHeader(
            mode = uiState.viewMode,
            dayStartMs = uiState.selectedDayStartMs,
            beamsCountLabel = beamsCountLabel,
            canGoPrev = canGoPrev,
            onSelectMode = onSelectMode,
            onPrev = onPrevDay,
            onNext = onNextDay,
            onToday = onToday
        )

        // Content
        // Content
        when (uiState.viewMode) {
            AtlasViewMode.DAY -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)           // ✅ prevents overlap with NowZone
                ) {
                    DayAgendaTimeline(
                        dayPlan = uiState.dayPlan,
                        onAdvanceDay = { delta -> onAdvanceDay(delta) }, // whatever you already do
                        onBeamClick = { beam -> selectedBeam = beam }    // ✅ add this
                    )
                }
            }
            AtlasViewMode.WEEK -> { /* ... */ }
            AtlasViewMode.MONTH -> { /* ... */ }
        }
    }
}

@Composable
private fun BeamDetailsSheetContent(
    b: BeamBlockUi,
    onClose: () -> Unit
) {
    val onJourney = Color.White
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

        Text(
            text = "${b.status} • ${b.readiness.displayLabel}",
            style = MaterialTheme.typography.labelMedium,
            color = onJourney.copy(alpha = 0.72f)
        )

        if (b.clippedTop || b.clippedBottom) {
            val note = buildString {
                if (b.clippedTop) append("Starts earlier (outside day).")
                if (b.clippedBottom) append("Ends later (outside day).")
            }.trim()

            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = onJourney.copy(alpha = 0.72f)
            )
        }

        Spacer(Modifier.height(6.dp))

        Button(
            onClick = onClose,
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

@Composable
private fun NowZone(
    now: NowState,
    onStartFlow: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val b = now.activeBeam

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (now.isBeamActive && b != null) {
            val durationMs = max(1L, b.endMs - b.startMs)
            val remainingMs = max(0L, b.endMs - System.currentTimeMillis())
            val remainingFracRaw =
                (remainingMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            val remainingFrac by animateFloatAsState(
                targetValue = remainingFracRaw,
                animationSpec = tween(900, easing = FastOutSlowInEasing),
                label = "remainingFrac"
            )
            val ringColor = remainingToColor(remainingFracRaw)
            // 🔥 Visible but classy pulse when <= 30% left
            val lowPulseAlpha: Float = if (remainingFracRaw <= 0.30f) {
                val transition = rememberInfiniteTransition(label = "lowPulse")
                val alpha by transition.animateFloat(
                    initialValue = 0.72f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                alpha
            } else 1f
            val minsLeft = (remainingMs / 60_000L).coerceAtLeast(0L)
            val pctLeft = (remainingFrac * 100f).roundToInt()

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cs.secondary,
                    contentColor = cs.onSecondary
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    // Status label
                    Text(
                        text = "⭐ BEAM ACTIVE",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.4.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.78f)
                    )

                    // Circular Energy Core
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Halo pulse when low
                        if (remainingFracRaw <= 0.30f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = ringColor.copy(alpha = 0.10f * lowPulseAlpha),
                                        shape = CircleShape
                                    )
                            )
                        }

                        // Track
                        CircularProgressIndicator(
                            strokeWidth = 12.dp,
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Remaining arc
                        CircularProgressIndicator(
                            progress = { remainingFrac },
                            strokeWidth = 12.dp,
                            color = ringColor.copy(alpha = lowPulseAlpha),
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            Text(
                                text = "$pctLeft%",
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "$minsLeft min left",
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant.copy(alpha = 0.70f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Beam name
                    Text(
                        text = b.tagName,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    // CTA 60% width
                    Button(
                        onClick = onStartFlow,
                        modifier = Modifier.fillMaxWidth(0.6f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        )
                    ) {
                        Text(
                            text = "Enter flow!",
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else {
            // No active beam
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cs.surfaceVariant,
                    contentColor = cs.onSurfaceVariant
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Text(
                        text = "ATLAS",
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.6.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.75f)
                    )

                    Text(
                        text = "Your journeys await",
                        style = MaterialTheme.typography.titleMedium
                    )

                    now.nextBeam?.let { beam ->
                        CountdownText(
                            targetTimeMs = beam.startMs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } ?: Text(
                        text = "No upcoming Beams scheduled",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant.copy(alpha = 0.80f)
                    )
                }
            }
        }
    }
}

/**
 * remainingFrac = fraction of time LEFT (1.0 at start, 0.0 at end).
 *
 * Rules:
 * - Start of beam is Scyra Blue.
 * - When remaining < 30%, it becomes red and gets increasingly darker as it approaches 0.
 * - Between 30%..100% we ease from Scyra Blue toward a warm red (not harsh), then hand off to darkening reds.
 */
private fun remainingToColor(remainingFrac: Float): Color {
    val r = remainingFrac.coerceIn(0f, 1f)
    // <= 30% left → deepen into crimson
    if (r <= 0.30f) {
        val t = (r / 0.30f).coerceIn(0f, 1f)
        return lerp(
            Color(0xFF3A050B),     // dark ink red
            GryffindorRed,         // your theme red
            t
        )
    }
    // 30%–70% left → Bronze transition
    if (r <= 0.70f) {
        val t = ((r - 0.30f) / 0.40f).coerceIn(0f, 1f)
        return lerp(GryffindorRed, Bronze, t)
    }
    // 70%–100% left → RavenclawBlue to Bronze
    val t = ((r - 0.70f) / 0.30f).coerceIn(0f, 1f)
    return lerp(Bronze, RavenclawBlue, t)
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
