package com.kingkharnivore.skillz.ui.screen.helpers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kingkharnivore.skillz.R
import java.util.Locale
import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils
import com.kingkharnivore.skillz.utils.time.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun journeySessionMeta(session: FlowListItemUiModel): String {
    val parts = buildList {
        add(stringResource(R.string.story_helpers_journey_duration, formatDuration(session.durationMs)))

        if (session.beamBonusPoints > 0) {
            add(stringResource(R.string.story_helpers_journey_beam_bonus, session.beamBonusPoints))
        }
        if (session.isSurge && session.surgePoints > 0) {
            add(stringResource(R.string.story_helpers_journey_surge_bonus, session.surgePoints))
        }
    }

    return parts.joinToString(separator = "  •  ")
}

@Composable
fun formatPeriodTitle(period: StoryPeriod, anchorDayStartMs: Long): String {
    val zone = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    val window = TimeWindowUtils.windowFor(anchorDayStartMs, period)
    val start = Instant.ofEpochMilli(window.startMs).atZone(zone).toLocalDate()

    return when (period) {
        StoryPeriod.DAY -> start.format(DateTimeFormatter.ofPattern("EEE, MMM d", locale))
        StoryPeriod.WEEK -> stringResource(
            R.string.story_helpers_period_title_week_of,
            start.format(DateTimeFormatter.ofPattern("MMM d", locale))
        )
        StoryPeriod.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
    }
}

@Composable
fun formatPeriodSubtitle(period: StoryPeriod, anchorDayStartMs: Long): String {
    val zone = ZoneId.systemDefault()
    val locale = Locale.getDefault()
    val window = TimeWindowUtils.windowFor(anchorDayStartMs, period)
    val start = Instant.ofEpochMilli(window.startMs).atZone(zone).toLocalDate()
    val endExclusive = Instant.ofEpochMilli(window.endMs).atZone(zone).toLocalDate()
    val end = endExclusive.minusDays(1)

    return when (period) {
        StoryPeriod.DAY -> stringResource(R.string.story_helpers_period_subtitle_day)
        StoryPeriod.WEEK -> stringResource(
            R.string.story_helpers_period_subtitle_week,
            start.format(DateTimeFormatter.ofPattern("MMM d", locale)),
            end.format(DateTimeFormatter.ofPattern("MMM d", locale))
        )
        StoryPeriod.MONTH -> stringResource(R.string.story_helpers_period_subtitle_month)
    }
}

@Composable
fun sagaSubtitle(
    period: StoryPeriod,
    anchorDayStartMs: Long
): String {
    val nowMs = System.currentTimeMillis()
    val normalizedAnchor = TimeWindowUtils.normalizeAnchor(anchorDayStartMs, period)
    val currentPeriodStart = TimeWindowUtils.startOfPeriodMs(nowMs, period)
    val isCurrent = normalizedAnchor == currentPeriodStart

    return when (period) {
        StoryPeriod.DAY -> {
            if (isCurrent) {
                stringResource(R.string.story_helpers_saga_subtitle_day_current)
            } else {
                stringResource(R.string.story_helpers_saga_subtitle_day_past)
            }
        }

        StoryPeriod.WEEK -> {
            if (isCurrent) {
                stringResource(R.string.story_helpers_saga_subtitle_week_current)
            } else {
                stringResource(R.string.story_helpers_saga_subtitle_week_past)
            }
        }

        StoryPeriod.MONTH -> {
            if (isCurrent) {
                stringResource(R.string.story_helpers_saga_subtitle_month_current)
            } else {
                stringResource(R.string.story_helpers_saga_subtitle_month_past)
            }
        }
    }
}
