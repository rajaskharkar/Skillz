package com.kingkharnivore.skillz.utils.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FMT = DateTimeFormatter.ofPattern("h:mm a")

fun formatRange(
    startMs: Long,
    endMs: Long,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    val s = Instant.ofEpochMilli(startMs).atZone(zoneId).format(TIME_FMT)
    val e = Instant.ofEpochMilli(endMs).atZone(zoneId).format(TIME_FMT)
    return "$s – $e"
}

fun formatDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%d hr %02d min %02d sec", hours, minutes, seconds)
        minutes > 0 -> String.format("%d min %02d sec", minutes, seconds)
        else -> String.format("%d sec", seconds)
    }
}

enum class StoryPeriod(val label: String) {
    DAY("Day"),
    WEEK("Week"),
    MONTH("Month")
}

data class TimeWindow(
    val startMs: Long,
    val endMs: Long // half-open [start, end)
)

object TimeWindowUtils {

    private fun zone(): ZoneId = ZoneId.systemDefault()

    fun toLocalDate(timeMs: Long): LocalDate {
        val z = zone()
        return Instant.ofEpochMilli(timeMs).atZone(z).toLocalDate()
    }

    fun startOfTodayMs(nowMs: Long = System.currentTimeMillis()): Long {
        val z = zone()
        val today = toLocalDate(nowMs)
        return today.atStartOfDay(z).toInstant().toEpochMilli()
    }

    /**
     * Start-of-period anchor for an arbitrary timestamp.
     * - DAY: that date
     * - WEEK: Monday of that week
     * - MONTH: first of month
     */
    fun startOfPeriodMs(timeMs: Long, period: StoryPeriod): Long {
        val z = zone()
        val d = toLocalDate(timeMs)

        val startDate = when (period) {
            StoryPeriod.DAY -> d
            StoryPeriod.WEEK -> d.with(DayOfWeek.MONDAY)
            StoryPeriod.MONTH -> d.withDayOfMonth(1)
        }

        return startDate.atStartOfDay(z).toInstant().toEpochMilli()
    }

    /**
     * Normalize an existing anchor into a correct start-of-period anchor.
     * (Useful when period changes but anchor was previously day-based.)
     */
    fun normalizeAnchor(anchorDayStartMs: Long, period: StoryPeriod): Long {
        return startOfPeriodMs(anchorDayStartMs, period)
    }

    fun windowFor(anchorStartMs: Long, period: StoryPeriod): TimeWindow {
        val z = zone()
        val startDate = toLocalDate(anchorStartMs)

        val endDate = when (period) {
            StoryPeriod.DAY -> startDate.plusDays(1)
            StoryPeriod.WEEK -> startDate.plusDays(7)
            StoryPeriod.MONTH -> startDate.withDayOfMonth(1).plusMonths(1)
        }

        val startMs = startDate.atStartOfDay(z).toInstant().toEpochMilli()
        val endMs = endDate.atStartOfDay(z).toInstant().toEpochMilli()

        return TimeWindow(startMs = startMs, endMs = endMs)
    }

    fun shiftAnchor(anchorStartMs: Long, period: StoryPeriod, dir: Int): Long {
        val z = zone()
        val date = toLocalDate(anchorStartMs)

        val shifted = when (period) {
            StoryPeriod.DAY -> date.plusDays(dir.toLong())
            StoryPeriod.WEEK -> date.plusWeeks(dir.toLong())
            StoryPeriod.MONTH -> date.plusMonths(dir.toLong()).withDayOfMonth(1)
        }

        // Always return a start-of-day ms.
        return shifted.atStartOfDay(z).toInstant().toEpochMilli()
    }

    fun clampNoFuture(anchorStartMs: Long, period: StoryPeriod, nowMs: Long = System.currentTimeMillis()): Long {
        val maxAnchor = startOfPeriodMs(nowMs, period)
        return minOf(anchorStartMs, maxAnchor)
    }

    /**
     * Clamp anchor to a data-aware range:
     * - min = first session's start-of-period (or today's if none)
     * - max = today's start-of-period
     */
    fun clampToFirstAndToday(
        anchorStartMs: Long,
        period: StoryPeriod,
        firstSessionStartMs: Long?,
        nowMs: Long = System.currentTimeMillis()
    ): Long {
        val todayStart = startOfTodayMs(nowMs)

        val maxAnchor = normalizeAnchor(todayStart, period)

        val minAnchor = if (firstSessionStartMs != null) {
            // snap first session day into the same period anchor
            val firstDayStart = startOfTodayMs(firstSessionStartMs)
            normalizeAnchor(firstDayStart, period)
        } else {
            maxAnchor
        }

        return normalizeAnchor(anchorStartMs, period).coerceIn(minAnchor, maxAnchor)
    }
}
