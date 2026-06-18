package com.kingkharnivore.skillz.ui.screen.flow

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kingkharnivore.skillz.R
import java.util.Locale
import kotlin.math.sqrt

@Composable
fun ArcPill(
    arcMultiplier: Double,
    arcNextIndex: Int?,
    isPending: Boolean,
    graceRemainingMs: Long?,
    pauseRemainingMs: Long?,
    isInFlow: Boolean,
    modifier: Modifier = Modifier,
    calmMode: Boolean = false
) {
    val m = arcMultiplier.coerceAtLeast(1.0)
    val intensity = multiplierIntensity(m)

    val baseA = MaterialTheme.colorScheme.primary
    val baseB = MaterialTheme.colorScheme.tertiary

    val t = intensity.coerceIn(0f, 1f)
    val boostedT = sqrt(t)
    val rawTargetAccent = lerpColor(baseA, baseB, boostedT)

    // Calm Mode: neutral accent (no animated shifting)
    val targetAccent = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
    } else {
        rawTargetAccent
    }

    val accent: Color = if (calmMode) {
        targetAccent
    } else {
        val animated by animateColorAsState(
            targetValue = targetAccent,
            animationSpec = tween(450, easing = FastOutSlowInEasing),
            label = "arcAccent"
        )
        animated
    }

    // Calm Mode: no pulse
    val pulse: Float = if (calmMode) {
        0.0f
    } else {
        val infinite = rememberInfiniteTransition(label = "arcPulse")
        val p by infinite.animateFloat(
            initialValue = 0.06f,
            targetValue = 0.10f + (0.10f * t),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = (1300 - (450 * t)).toInt().coerceAtLeast(750),
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )
        p
    }

    val shape = RoundedCornerShape(999.dp)

    val (statusLabel, statusEmoji) = buildArcStatus(
        isInFlow = isInFlow,
        isPending = isPending,
        graceRemainingMs = graceRemainingMs,
        pauseRemainingMs = pauseRemainingMs,
        multiplier = m
    )

    val showPendingChip = isPending && !isInFlow && (graceRemainingMs != null)

    val arcLabel = stringResource(R.string.arc_pill_title)
    val flowLabel = stringResource(R.string.arc_pill_flow_label)
    val pendingLabel = stringResource(R.string.arc_pill_pending)
    val multiplierText = stringResource(
        R.string.arc_pill_multiplier_value,
        String.format(Locale.getDefault(), "%.1f", m)
    )
    val nextFlowText = arcNextIndex?.let {
        stringResource(R.string.arc_pill_next_flow_value, it)
    }
    val pillA11y = buildString {
        append(arcLabel)
        if (!calmMode) {
            append(". ")
            append(multiplierText)
        }
        if (nextFlowText != null) {
            append(". ")
            append(nextFlowText)
        }
        if (showPendingChip) {
            append(". ")
            append(pendingLabel)
        }
        append(". ")
        append(statusLabel)
    }

    // Calm Mode: simpler background
    val bg = if (calmMode) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
                MaterialTheme.colorScheme.surface.copy(alpha = 1.0f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                accent.copy(alpha = 0.24f + pulse),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
            )
        )
    }

    val borderColor = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    } else {
        accent.copy(alpha = 0.28f)
    }

    Surface(
        modifier = modifier.semantics {
            role = Role.Image
            contentDescription = pillA11y
        },
        shape = shape,
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(bg)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = shape
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LEFT: shrinkable group
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Calm Mode: hide tier icon/runes (multiplier vibe)
                if (!calmMode) {
                    Text(
                        text = tierIcon(m),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Text(
                    text = arcLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Calm Mode: hide ×multiplier
                if (!calmMode) {
                    Text(
                        text = multiplierText,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent,
                        maxLines = 1
                    )
                }

                nextFlowText?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showPendingChip) {
                PendingChip(
                    tint = accent,
                    calmMode = calmMode,
                    text = pendingLabel
                )
            }

            StatusCapsule(
                emoji = statusEmoji,
                text = statusLabel,
                tint = accent,
                calmMode = calmMode,
                contentDescription = statusLabel
            )
        }
    }
}

@Composable
private fun PendingChip(tint: Color, calmMode: Boolean, text: String) {
    val bg = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
    } else {
        tint.copy(alpha = 0.16f)
    }

    val border = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    } else {
        tint.copy(alpha = 0.30f)
    }

    val textColor = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f)
    } else {
        tint
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusCapsule(emoji: String, text: String, tint: Color, calmMode: Boolean, contentDescription: String) {
    val border = if (calmMode) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    } else {
        tint.copy(alpha = 0.22f)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics { this.contentDescription = contentDescription }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                maxLines = 1
            )
        }
    }
}

// ---- helpers (same as your current file) ----

private fun buildArcStatus(
    isInFlow: Boolean,
    isPending: Boolean,
    graceRemainingMs: Long?,
    pauseRemainingMs: Long?,
    multiplier: Double
): Pair<String, String> {
    return when {
        !isInFlow && pauseRemainingMs != null -> {
            "Paused ${formatMmSs(pauseRemainingMs)}" to "⏸️"
        }

        !isInFlow && graceRemainingMs != null -> {
            "Expires ${formatMmSs(graceRemainingMs)}" to "⏳"
        }

        isInFlow -> {
            val emoji = if (multiplier >= 3.5) "👑" else "🔥"
            "Flow Active" to emoji
        }

        isPending -> "Wrapping Up" to "🟡"
        else -> "Ready" to "✅"
    }
}

private fun multiplierIntensity(multiplier: Double): Float {
    val base = 1.3
    val high = 3.0
    val linear = ((multiplier - base) / (high - base)).toFloat()
    val clamped = linear.coerceIn(0f, 1f)
    val extra = ((multiplier - high) / 8.0).toFloat().coerceIn(0f, 0.25f)
    return (clamped + extra).coerceIn(0f, 1.25f)
}

private fun tierIcon(multiplier: Double): String {
    val step = (((multiplier - 1.3) * 10.0) + 1e-9).toInt().coerceAtLeast(0)
    val prog = step % 7

    val rune = when (prog) {
        0 -> ""
        1 -> "ᛁ"
        2 -> "ᛁᛁ"
        3 -> "ᛁᛁᛁ"
        4 -> "ᚱ"
        5 -> "ᚱᛁ"
        else -> "ᚱᛁᛁ"
    }

    val base = when {
        multiplier < 2.0 -> "🏔️"
        multiplier < 2.6 -> "🌙"
        multiplier < 3.5 -> "👑"
        else -> "✨"
    }

    return base + rune
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = 1f
    )
}

private fun formatMmSs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}