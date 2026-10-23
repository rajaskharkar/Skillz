package com.kingkharnivore.skillz.domain.analytics

import com.kingkharnivore.skillz.model.FlowMode
import com.kingkharnivore.skillz.utils.health.FlowActiveInterval
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class LifetimeAnalyticsTest {
    private val zone = ZoneId.of("America/Chicago")
    private val hour = 3_600_000L
    private fun time(value: String) = java.time.LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli()
    private fun flow(start: String, end: String, mode: FlowMode = FlowMode.FLOW, tag: Long = 1): FlowObservation {
        val from = time(start); val to = time(end)
        return FlowObservation(from, tag, mode, from, to, to, to - from, listOf(FlowActiveInterval(from, to)))
    }
    private fun calculate(rows: List<FlowObservation>, today: String = "2026-10-21") =
        LifetimeAnalytics.calculate(rows, mapOf(1L to "Zebra", 2L to "Art", 3L to "Music"), LocalDate.parse(today), zone, Locale.US)

    @Test fun emptyHistoryHasNoInventedAveragesOrDates() {
        val stats = calculate(emptyList())
        assertEquals(0, stats.flowCount)
        assertEquals(0L, stats.totalDurationMs)
        assertEquals(0.0, stats.averageDailyMs, 0.0)
        assertNull(stats.firstFlowDate)
        assertTrue(stats.rhythm.isEmpty())
    }

    @Test fun everyModeCountsFullyAsTimeAndJourneysSortAlphabetically() {
        val rows = listOf(
            flow("2026-10-19T08:00", "2026-10-19T09:00"),
            flow("2026-10-20T08:00", "2026-10-20T10:00", FlowMode.SOFT, 2),
            flow("2026-10-21T08:00", "2026-10-21T11:00", FlowMode.POWER, 3))
        val stats = calculate(rows)
        assertEquals(6 * hour, stats.totalDurationMs)
        assertEquals(3, stats.flowCount)
        assertEquals(3, stats.activeDays)
        assertEquals(listOf("Art", "Music", "Zebra"), stats.journeys.map { it.name })
        assertEquals(listOf(hour, 2 * hour, 3 * hour), stats.modes.map { it.durationMs })
        assertEquals(listOf(1, 1, 1), stats.modes.map { it.count })
        assertEquals(2 * hour.toDouble(), stats.averageFlowMs, 0.0)
        assertEquals(2 * hour.toDouble(), stats.medianFlowMs, 0.0)
        assertEquals(stats.totalDurationMs, stats.rhythm.sumOf { it.durationMs })
        assertEquals(3, stats.rhythm.sumOf { it.starts })
    }

    @Test fun calendarAveragesIncludeInactivePeriodsAndPartialFirstAndLastPeriods() {
        val rows = listOf(flow("2026-08-31T08:00", "2026-08-31T09:00"),
            flow("2026-10-21T08:00", "2026-10-21T09:00"))
        val stats = calculate(rows)
        assertEquals(52L, stats.calendarDays)
        assertEquals(8L, stats.calendarWeeks)
        assertEquals(3L, stats.calendarMonths)
        assertEquals(2 * hour.toDouble() / 52, stats.averageDailyMs, 0.00001)
        assertEquals(2 * hour.toDouble() / 8, stats.averageWeeklyMs, 0.00001)
        assertEquals(2 * hour.toDouble() / 3, stats.averageMonthlyMs, 0.00001)
        assertEquals(2, stats.activeDays)
    }

    @Test fun pausedWholeDayDoesNotBecomeAnActiveDayAndLateSaveDoesNotMoveStarts() {
        val from = time("2026-10-19T23:00"); val to = time("2026-10-21T01:00")
        val row = FlowObservation(1, 1, FlowMode.FLOW, from, to, time("2026-10-25T10:00"), 2 * hour,
            listOf(FlowActiveInterval(from, from + hour), FlowActiveInterval(to - hour, to)))
        val stats = calculate(listOf(row))
        assertEquals(2, stats.activeDays)
        assertEquals(1, stats.rhythm.single { it.weekday == 0 && it.hour == 23 }.starts)
        assertEquals(0L, stats.rhythm.filter { it.weekday == 1 }.sumOf { it.durationMs })
        assertEquals(0, stats.estimatedTimingCount)
    }

    @Test fun legacyPauseEstimatesPreserveEveryMillisecond() {
        val row = flow("2026-10-20T23:00", "2026-10-21T01:00").copy(durationMs = hour + 1, activeIntervals = emptyList())
        val stats = calculate(listOf(row))
        assertEquals(hour + 1, stats.rhythm.sumOf { it.durationMs })
        assertEquals(2, stats.activeDays)
        assertEquals(1, stats.estimatedTimingCount)
    }

    @Test fun dstSkippedAndRepeatedHoursKeepActualDurationAndOneStart() {
        listOf("2026-03-08" to 3L, "2026-11-01" to 5L).forEach { (date, hours) ->
            val stats = calculate(listOf(flow("${date}T00:00", "${date}T04:00")), date)
            assertEquals(hours * hour, stats.totalDurationMs)
            assertEquals(hours * hour, stats.rhythm.sumOf { it.durationMs })
            assertEquals(1, stats.rhythm.sumOf { it.starts })
            assertEquals(1, stats.activeDays)
        }
    }

    @Test fun midnightEndIsExclusiveAndZeroDurationStillCountsAsSavedFlow() {
        val rows = listOf(flow("2026-10-20T23:00", "2026-10-21T00:00"),
            flow("2026-10-21T10:00", "2026-10-21T10:00"))
        val stats = calculate(rows)
        assertEquals(2, stats.flowCount)
        assertEquals(1, stats.activeDays)
        assertEquals(hour / 2.0, stats.averageFlowMs, 0.0)
        assertEquals(hour / 2.0, stats.medianFlowMs, 0.0)
    }

    @Test fun editsRenamesAndDeletionsAreReflectedWithoutStoredAggregateDrift() {
        val a = flow("2026-10-20T08:00", "2026-10-20T09:00")
        val b = flow("2026-10-21T08:00", "2026-10-21T09:00", tag = 2)
        assertEquals(2 * hour, calculate(listOf(a, b)).totalDurationMs)
        val updated = LifetimeAnalytics.calculate(listOf(b.copy(mode = FlowMode.POWER)), mapOf(2L to "Renamed"),
            LocalDate.parse("2026-10-21"), zone, Locale.US)
        assertEquals(hour, updated.totalDurationMs)
        assertEquals("Renamed", updated.journeys.single().name)
        assertEquals(1, updated.modes.single { it.mode == FlowMode.POWER }.count)
        assertEquals(1L, updated.calendarDays)
    }

    @Test fun timezoneChangesRebucketActivityWithoutChangingTotals() {
        val rows = listOf(flow("2026-10-20T23:30", "2026-10-21T00:30"))
        val local = calculate(rows)
        val utc = LifetimeAnalytics.calculate(rows, emptyMap(), LocalDate.parse("2026-10-21"), ZoneId.of("UTC"))
        assertEquals(2, local.activeDays)
        assertEquals(1, utc.activeDays)
        assertEquals(local.totalDurationMs, utc.totalDurationMs)
    }
}
