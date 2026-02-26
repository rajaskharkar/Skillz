package com.kingkharnivore.skillz.ui.screen.atlas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CountdownText(
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
