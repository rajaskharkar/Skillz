package com.kingkharnivore.skillz.domain.analytics

import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.health.FlowActiveInterval
import java.text.Collator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/** Recorded facts, independent of UI, rewards multipliers, and calendar projections. */
data class FlowObservation(
    val id: Long,
    val tagId: Long,
    val mode: FlowMode,
    val startedAtMs: Long,
    val endedAtMs: Long,
    val recordedAtMs: Long,
    val durationMs: Long,
    val activeIntervals: List<FlowActiveInterval> = emptyList()
)

data class ModeStats(val mode: FlowMode, val count: Int, val durationMs: Long)
data class JourneyStats(val tagId: Long, val name: String, val count: Int, val durationMs: Long)
/** Monday = 0, hour = 0..23. Starts and active time are intentionally different measures. */
data class RhythmCell(val weekday: Int, val hour: Int, val starts: Int, val durationMs: Long)

data class LifetimeStats(
    val totalDurationMs: Long = 0,
    val flowCount: Int = 0,
    val activeDays: Int = 0,
    val firstFlowDate: LocalDate? = null,
    val calendarDays: Long = 0,
    val calendarWeeks: Long = 0,
    val calendarMonths: Long = 0,
    val averageDailyMs: Double = 0.0,
    val averageWeeklyMs: Double = 0.0,
    val averageMonthlyMs: Double = 0.0,
    val averageFlowMs: Double = 0.0,
    val medianFlowMs: Double = 0.0,
    val modes: List<ModeStats> = emptyList(),
    val journeys: List<JourneyStats> = emptyList(),
    val rhythm: List<RhythmCell> = emptyList(),
    val estimatedTimingCount: Int = 0,
    val zoneId: String = ""
)

/**
 * Pure, deterministic aggregation. Calendar averages include zero-activity periods and count
 * first/current partial periods once. Weeks start Monday, matching Story. All-time totals use
 * retained records (including zero-duration saves); only positive time makes a day active.
 */
object LifetimeAnalytics {
    fun calculate(
        flows: List<FlowObservation>,
        tagNames: Map<Long, String>,
        today: LocalDate,
        zone: ZoneId,
        locale: Locale = Locale.getDefault()
    ): LifetimeStats {
        if (flows.isEmpty()) return LifetimeStats(zoneId = zone.id)
        val starts = IntArray(168)
        val durations = LongArray(168)
        val activeDates = mutableSetOf<LocalDate>()
        var estimated = 0
        for (flow in flows) {
            val start = Instant.ofEpochMilli(flow.startedAtMs).atZone(zone)
            starts[(start.dayOfWeek.value - 1) * 24 + start.hour]++
            val duration = flow.durationMs.coerceAtLeast(0)
            if (duration == 0L) continue
            val intervals = flow.activeIntervals.sortedBy { it.startTimeMs }
            val exact = intervals.isNotEmpty() && intervals.all {
                it.startTimeMs >= flow.startedAtMs && it.endTimeMs <= flow.endedAtMs && it.endTimeMs > it.startTimeMs
            } && intervals.zipWithNext().all { (a, b) -> a.endTimeMs <= b.startTimeMs } &&
                intervals.sumOf { it.endTimeMs - it.startTimeMs } == duration
            if (exact) {
                intervals.forEach { interval ->
                    split(interval.startTimeMs, interval.endTimeMs, zone) { from, to, local ->
                        durations[(local.dayOfWeek.value - 1) * 24 + local.hour] += to - from
                        activeDates += local.toLocalDate()
                    }
                }
            } else {
                // Older records do not locate pauses. Preserve their total, allocating time
                // proportionally across wall-clock hours; disclose this estimate in the UI.
                estimated++
                val span = flow.endedAtMs - flow.startedAtMs
                if (span > 0) {
                    fun elapsed(at: Long): Long = if (at == flow.endedAtMs) duration else
                        (duration.toDouble() * (at - flow.startedAtMs) / span).toLong()
                    split(flow.startedAtMs, flow.endedAtMs, zone) { from, to, local ->
                        val amount = elapsed(to) - elapsed(from)
                        durations[(local.dayOfWeek.value - 1) * 24 + local.hour] += amount
                        if (amount > 0) activeDates += local.toLocalDate()
                    }
                } else {
                    durations[(start.dayOfWeek.value - 1) * 24 + start.hour] += duration
                    activeDates += start.toLocalDate()
                }
            }
        }
        val first = flows.minOf { Instant.ofEpochMilli(it.startedAtMs).atZone(zone).toLocalDate() }
        // Include retained future-dated records without creating invalid denominators.
        val last = maxOf(today, flows.maxOf {
            Instant.ofEpochMilli(maxOf(it.startedAtMs, it.endedAtMs - 1)).atZone(zone).toLocalDate()
        })
        val days = ChronoUnit.DAYS.between(first, last) + 1
        val weeks = ChronoUnit.WEEKS.between(first.with(DayOfWeek.MONDAY), last.with(DayOfWeek.MONDAY)) + 1
        val months = ChronoUnit.MONTHS.between(YearMonth.from(first), YearMonth.from(last)) + 1
        val lengths = flows.map { it.durationMs.coerceAtLeast(0) }.sorted()
        val total = lengths.sum()
        val middle = lengths.size / 2
        val median = if (lengths.size % 2 == 0) lengths[middle - 1] / 2.0 + lengths[middle] / 2.0
            else lengths[middle].toDouble()
        val collator = Collator.getInstance(locale)
        val journeys = flows.groupBy { it.tagId }.map { (tagId, rows) ->
            JourneyStats(tagId, tagNames[tagId].orEmpty(), rows.size, rows.sumOf { it.durationMs.coerceAtLeast(0) })
        }.sortedWith { a, b -> collator.compare(a.name, b.name).takeIf { it != 0 } ?: a.tagId.compareTo(b.tagId) }
        return LifetimeStats(
            totalDurationMs = total, flowCount = flows.size, activeDays = activeDates.size,
            firstFlowDate = first, calendarDays = days, calendarWeeks = weeks, calendarMonths = months,
            averageDailyMs = total.toDouble() / days, averageWeeklyMs = total.toDouble() / weeks,
            averageMonthlyMs = total.toDouble() / months, averageFlowMs = total.toDouble() / flows.size,
            medianFlowMs = median,
            modes = FlowMode.entries.map { mode ->
                val rows = flows.filter { it.mode == mode }
                ModeStats(mode, rows.size, rows.sumOf { it.durationMs.coerceAtLeast(0) })
            },
            journeys = journeys,
            rhythm = (0 until 168).map { RhythmCell(it / 24, it % 24, starts[it], durations[it]) },
            estimatedTimingCount = estimated, zoneId = zone.id
        )
    }

    private inline fun split(
        start: Long, end: Long, zone: ZoneId,
        consume: (Long, Long, java.time.ZonedDateTime) -> Unit
    ) {
        var cursor = start
        while (cursor < end) {
            val local = Instant.ofEpochMilli(cursor).atZone(zone)
            // Zoned arithmetic handles skipped and repeated hours at DST transitions.
            val boundary = local.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli()
            val next = minOf(end, boundary)
            consume(cursor, next, local)
            cursor = next
        }
    }
}
