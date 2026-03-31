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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kingkharnivore.skillz.R
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
    val a11yText = stringResource(R.string.countdown_a11y, headline, countdown)

    Column(
        modifier = modifier.semantics {
            contentDescription = a11yText
        },
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
                text = stringResource(R.string.countdown_until_beam_begins),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
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

    val headline = if (totalSeconds <= 0) {
        stringResource(R.string.countdown_headline_beam_imminent)
    } else {
        stringResource(R.string.countdown_headline_next_beam)
    }

    if (days >= 3) {
        val dayFmt = DateTimeFormatter.ofPattern("EEE, MMM d")
        val timeFmt = DateTimeFormatter.ofPattern("h:mm a")
        val zdt = Instant.ofEpochMilli(targetTimeMs).atZone(zoneId)
        val whenText = stringResource(
            R.string.countdown_day_time_format,
            zdt.format(dayFmt),
            zdt.format(timeFmt)
        )
        val countdownText = stringResource(
            R.string.countdown_days_hours_minutes,
            days,
            hours,
            minutes
        )
        return headline to "$countdownText\n$whenText"
    }

    val countdown = when {
        totalSeconds <= 0 -> stringResource(R.string.countdown_starting_now)
        days > 0 -> stringResource(R.string.countdown_days_hours_minutes, days, hours, minutes)
        hours > 0 -> stringResource(R.string.countdown_hours_minutes_seconds, hours, minutes, seconds)
        minutes > 0 -> stringResource(R.string.countdown_minutes_seconds, minutes, seconds)
        else -> stringResource(R.string.countdown_seconds, seconds)
    }

    return headline to countdown
}