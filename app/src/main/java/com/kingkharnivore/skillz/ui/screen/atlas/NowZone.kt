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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R
import com.kingkharnivore.skillz.ui.model.NowState
import com.kingkharnivore.skillz.ui.theme.Bronze
import com.kingkharnivore.skillz.ui.theme.GryffindorRed
import com.kingkharnivore.skillz.ui.theme.RavenclawBlue
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun NowZone(
    now: NowState,
    onStartFlow: (String) -> Unit,
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
            } else {
                1f
            }

            val minsLeft = (remainingMs / 60_000L).coerceAtLeast(0L)
            val pctLeft = (remainingFrac * 100f).roundToInt()
            val minsLeftText = stringResource(R.string.now_zone_minutes_left, minsLeft)
            val activeBeamA11y = stringResource(
                R.string.now_zone_active_beam_a11y,
                b.tagName,
                minsLeftText,
                pctLeft
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp)
                    .semantics {
                        contentDescription = activeBeamA11y
                    },
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
                    Text(
                        text = stringResource(R.string.now_zone_beam_active_title),
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 1.4.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.78f)
                    )

                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

                        CircularProgressIndicator(
                            strokeWidth = 12.dp,
                            color = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        )

                        CircularProgressIndicator(
                            progress = { remainingFrac },
                            strokeWidth = 12.dp,
                            color = ringColor.copy(alpha = lowPulseAlpha),
                            trackColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize()
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.now_zone_percent_left, pctLeft),
                                style = MaterialTheme.typography.headlineMedium,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = minsLeftText,
                                style = MaterialTheme.typography.labelMedium,
                                color = cs.onSurfaceVariant.copy(alpha = 0.70f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = b.tagName,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Button(
                        onClick = { onStartFlow(b.tagName) },
                        modifier = Modifier.fillMaxWidth(0.6f),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cs.primary,
                            contentColor = cs.onPrimary
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.now_zone_enter_flow),
                            letterSpacing = 1.sp
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        text = stringResource(R.string.now_zone_atlas_label),
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.2.sp,
                        fontFamily = FontFamily.Monospace,
                        color = cs.secondary
                    )

                    Text(
                        text = stringResource(R.string.now_zone_intro),
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
                        text = stringResource(R.string.now_zone_no_upcoming_beams),
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        color = cs.onSurfaceVariant.copy(alpha = 0.85f)
                    )

                    val nowZoneBeamA11y = stringResource(R.string.now_zone_schedule_beam_a11y)
                    Button(
                        onClick = onScheduleBeamClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = nowZoneBeamA11y
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        )
                    ) {
                        Text(stringResource(R.string.now_zone_schedule_beam))
                    }
                }
            }
        }
    }
}

private fun remainingToColor(remainingFrac: Float): Color {
    val r = remainingFrac.coerceIn(0f, 1f)
    if (r <= 0.30f) {
        val t = (r / 0.30f).coerceIn(0f, 1f)
        return lerp(
            Color(0xFF3A050B),
            GryffindorRed,
            t
        )
    }
    if (r <= 0.70f) {
        val t = ((r - 0.30f) / 0.40f).coerceIn(0f, 1f)
        return lerp(GryffindorRed, Bronze, t)
    }
    val t = ((r - 0.70f) / 0.30f).coerceIn(0f, 1f)
    return lerp(Bronze, RavenclawBlue, t)
}