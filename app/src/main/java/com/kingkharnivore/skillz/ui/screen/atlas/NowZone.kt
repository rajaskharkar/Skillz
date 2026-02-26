package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.ui.model.NowState
import com.kingkharnivore.skillz.ui.theme.Bronze
import com.kingkharnivore.skillz.ui.theme.GryffindorRed
import com.kingkharnivore.skillz.ui.theme.RavenclawBlue
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun NowZone(
    now: NowState,
    onStartFlow: () -> Unit,
    onScheduleBeamClick: () -> Unit,
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
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cs.surface,
                    contentColor = cs.onSurface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    Text(
                        text = "ATLAS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.2.sp,
                        fontFamily = FontFamily.Monospace,
                        color = cs.secondary
                    )

                    Text(
                        text = "Create your path. Step forward. Walk it without hesitation.",
                        fontFamily = FontFamily.Cursive,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 26.sp,
                        style = MaterialTheme.typography.titleLarge,
                        color = cs.onSurface
                    )

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = cs.onSurface.copy(alpha = 0.08f)
                    )

                    now.nextBeam?.let { beam ->
                        CountdownText(
                            targetTimeMs = beam.startMs,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } ?: Text(
                        text = "No upcoming Beams scheduled.\n\nCommit your time. Schedule a Beam.\n\nHonor it and unlock up to a 2× Scyra Score multiplier.",
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.85f)
                    )

                    Button(
                        onClick = onScheduleBeamClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text("⭐ Schedule a Beam")
                    }
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
