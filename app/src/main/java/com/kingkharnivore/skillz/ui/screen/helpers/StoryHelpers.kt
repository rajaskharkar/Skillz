package com.kingkharnivore.skillz.ui.screen.helpers

import com.kingkharnivore.skillz.model.ui.FlowListItemUiModel
import com.kingkharnivore.skillz.utils.time.StoryPeriod
import com.kingkharnivore.skillz.utils.time.TimeWindowUtils
import com.kingkharnivore.skillz.utils.time.formatDuration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun journeySessionMeta(session: FlowListItemUiModel): String {
    return buildString {
        append("⏱ ")
        append(formatDuration(session.durationMs))

        if (session.beamBonusPoints > 0) {
            append("  •  ★ +")
            append(session.beamBonusPoints)
        }
        if (session.isSurge && session.surgePoints > 0) {
            append("  •  Surge +")
            append(session.surgePoints)
        }
    }
}

fun formatPeriodTitle(period: StoryPeriod, anchorDayStartMs: Long): String {
    val zone = ZoneId.systemDefault()
    val window = TimeWindowUtils.windowFor(anchorDayStartMs, period)
    val start = Instant.ofEpochMilli(window.startMs).atZone(zone).toLocalDate()
    val endExclusive = Instant.ofEpochMilli(window.endMs).atZone(zone).toLocalDate()
    val end = endExclusive.minusDays(1)

    return when (period) {
        StoryPeriod.DAY -> start.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
        StoryPeriod.WEEK -> "Week of ${start.format(DateTimeFormatter.ofPattern("MMM d"))}"
        StoryPeriod.MONTH -> start.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
}

fun formatPeriodSubtitle(period: StoryPeriod, anchorDayStartMs: Long): String {
    val zone = ZoneId.systemDefault()
    val window = TimeWindowUtils.windowFor(anchorDayStartMs, period)
    val start = Instant.ofEpochMilli(window.startMs).atZone(zone).toLocalDate()
    val endExclusive = Instant.ofEpochMilli(window.endMs).atZone(zone).toLocalDate()
    val end = endExclusive.minusDays(1)

    return when (period) {
        StoryPeriod.DAY -> "Sessions in this day"
        StoryPeriod.WEEK -> "${start.format(DateTimeFormatter.ofPattern("MMM d"))} – ${end.format(DateTimeFormatter.ofPattern("MMM d"))}"
        StoryPeriod.MONTH -> "Sessions in this month"
    }
}

/**
 * Subtitle under “Sagas”
 * You can keep this minimal; it reuses your existing date formatting utilities if you want,
 * but this is drop-in and safe.
 */
fun sagaSubtitle(
    period: StoryPeriod,
    anchorDayStartMs: Long
): String {

    val nowMs = System.currentTimeMillis()

    // Normalize anchor and current period start
    val normalizedAnchor = TimeWindowUtils.normalizeAnchor(anchorDayStartMs, period)
    val currentPeriodStart = TimeWindowUtils.startOfPeriodMs(nowMs, period)

    val isCurrent = normalizedAnchor == currentPeriodStart

    return when (period) {
        StoryPeriod.DAY ->
            if (isCurrent) "Record for today"
            else "Record for this day"

        StoryPeriod.WEEK ->
            if (isCurrent) "Record for this week"
            else "Record for the week"

        StoryPeriod.MONTH ->
            if (isCurrent) "Record for this month"
            else "Record for the month"
    }
}

