package com.kingkharnivore.skillz.domain.voyage

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoyageStatsCalculatorTest {
    private val calculator = VoyageStatsCalculator()
    private val zone = ZoneId.of("America/New_York")
    private val now = LocalDateTime.of(2026, 5, 30, 9, 0).atZone(zone).toInstant()

    @Test
    fun eligibility_excludesIncompleteZeroDurationSoftFlowsAndIncludesSurgeFlows() {
        val stats = calculate(
            listOf(
                session(id = 1, end = null, durationMinutes = 30, points = 900),
                session(id = 2, date = LocalDate.of(2026, 5, 30), durationMinutes = 0, points = 800),
                session(id = 3, date = LocalDate.of(2026, 5, 30), durationMinutes = 40, points = 700, soft = true),
                session(id = 4, date = LocalDate.of(2026, 5, 30), durationMinutes = 25, points = 600, surgePlannedMs = 25 * 60_000L)
            )
        )

        assertTrue(stats.hasEligibleFlows)
        assertEquals(600, stats.bestFlowByPoints?.points)
        assertEquals(25 * 60_000L, stats.longestFlow?.durationMs)
        assertNull(stats.longestArcByTime)
    }

    @Test
    fun emptyInput_returnsNoRecordsAndNoEligibleFlows() {
        val stats = calculate(emptyList())

        assertFalse(stats.hasEligibleFlows)
        assertNull(stats.bestFlowByPoints)
        assertNull(stats.currentDailyStreak)
        assertNull(stats.bestDayByPoints)
    }

    @Test
    fun currentDailyStreak_countsThroughTodayAndCollapsesDuplicateDates() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 28)),
                session(id = 2, date = LocalDate.of(2026, 5, 29)),
                session(id = 3, date = LocalDate.of(2026, 5, 30)),
                session(id = 4, date = LocalDate.of(2026, 5, 30), points = 200)
            )
        )

        assertEquals(3, stats.currentDailyStreak?.days)
        assertEquals(LocalDate.of(2026, 5, 28), stats.currentDailyStreak?.startDate)
        assertEquals(3, stats.longestDailyStreak?.days)
    }

    @Test
    fun currentDailyStreak_remainsActiveIfLatestFlowWasYesterday() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 27)),
                session(id = 2, date = LocalDate.of(2026, 5, 28)),
                session(id = 3, date = LocalDate.of(2026, 5, 29))
            )
        )

        assertEquals(3, stats.currentDailyStreak?.days)
        assertEquals(LocalDate.of(2026, 5, 29), stats.currentDailyStreak?.endDate)
    }

    @Test
    fun currentDailyStreak_isNotYetIfLatestFlowOlderThanYesterday() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 26)),
                session(id = 2, date = LocalDate.of(2026, 5, 27)),
                session(id = 3, date = LocalDate.of(2026, 5, 28))
            )
        )

        assertNull(stats.currentDailyStreak)
        assertEquals(3, stats.longestDailyStreak?.days)
    }

    @Test
    fun longestDailyStreak_prefersLongestRunThenMostRecentRun() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 1)),
                session(id = 2, date = LocalDate.of(2026, 5, 2)),
                session(id = 3, date = LocalDate.of(2026, 5, 10)),
                session(id = 4, date = LocalDate.of(2026, 5, 11)),
                session(id = 5, date = LocalDate.of(2026, 5, 12))
            )
        )

        assertEquals(3, stats.longestDailyStreak?.days)
        assertEquals(LocalDate.of(2026, 5, 10), stats.longestDailyStreak?.startDate)
        assertEquals(LocalDate.of(2026, 5, 12), stats.longestDailyStreak?.endDate)
    }

    @Test
    fun periodRecordsUseEndTimeMondayWeeksCalendarMonthsAndProvidedZone() {
        val lateStartPreviousDay = LocalDateTime.of(2026, 5, 25, 0, 30).atZone(zone).toInstant().toEpochMilli()
        val previousDayStart = LocalDateTime.of(2026, 5, 24, 23, 30).atZone(zone).toInstant().toEpochMilli()
        val stats = calculate(
            listOf(
                session(id = 1, startMillis = previousDayStart, end = lateStartPreviousDay, points = 900),
                session(id = 2, date = LocalDate.of(2026, 5, 31), points = 200),
                session(id = 3, date = LocalDate.of(2026, 6, 1), points = 100)
            )
        )

        assertEquals(LocalDate.of(2026, 5, 25), stats.bestDayByPoints?.startDate)
        assertEquals(1_100, stats.bestWeekByPoints?.points)
        assertEquals(LocalDate.of(2026, 5, 25), stats.bestWeekByPoints?.startDate)
        assertEquals(LocalDate.of(2026, 5, 31), stats.bestWeekByPoints?.endDate)
        assertEquals(1_100, stats.bestMonthByPoints?.points)
        assertEquals(LocalDate.of(2026, 5, 1), stats.bestMonthByPoints?.startDate)
        assertEquals(LocalDate.of(2026, 5, 31), stats.bestMonthByPoints?.endDate)
    }

    @Test
    fun weekAndMonthRecordsUseLocalDatesFromSuppliedZone() {
        val utcInstant = Instant.parse("2026-06-01T02:00:00Z")
        val newYorkStats = calculator.calculate(
            sessions = listOf(session(id = 1, end = utcInstant.toEpochMilli(), points = 100)),
            now = now,
            zoneId = zone
        )
        val utcStats = calculator.calculate(
            sessions = listOf(session(id = 1, end = utcInstant.toEpochMilli(), points = 100)),
            now = now,
            zoneId = ZoneId.of("UTC")
        )

        assertEquals(LocalDate.of(2026, 5, 1), newYorkStats.bestMonthByPoints?.startDate)
        assertEquals(LocalDate.of(2026, 6, 1), utcStats.bestMonthByPoints?.startDate)
    }

    @Test
    fun arcRecordsRequireAtLeastTwoEligibleFlowsAfterSoftFiltering() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 20), arcId = 10, durationMinutes = 30),
                session(id = 2, date = LocalDate.of(2026, 5, 20), arcId = 20, durationMinutes = 30),
                session(id = 3, date = LocalDate.of(2026, 5, 20), arcId = 20, durationMinutes = 30, soft = true),
                session(id = 4, date = LocalDate.of(2026, 5, 21), arcId = 30, durationMinutes = 40),
                session(id = 5, date = LocalDate.of(2026, 5, 22), arcId = 30, durationMinutes = 45)
            )
        )

        assertEquals(30L, stats.longestArcByTime?.arcId)
        assertEquals(85 * 60_000L, stats.longestArcByTime?.totalDurationMs)
        assertEquals(2, stats.mostChainedFlowsInArc?.flowCount)
    }

    @Test
    fun arcRecordsCalculateDurationChainMultiplierAndArcVolumePeriods() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 19), arcId = 1, durationMinutes = 30, multiplier = 1.2),
                session(id = 2, date = LocalDate.of(2026, 5, 20), arcId = 1, durationMinutes = 50, multiplier = 1.8),
                session(id = 3, date = LocalDate.of(2026, 5, 20), arcId = 2, durationMinutes = 20, multiplier = 1.4),
                session(id = 4, date = LocalDate.of(2026, 5, 20), arcId = 2, durationMinutes = 25, multiplier = 1.5),
                session(id = 5, date = LocalDate.of(2026, 5, 26), arcId = 3, durationMinutes = 10, multiplier = 1.1),
                session(id = 6, date = LocalDate.of(2026, 5, 27), arcId = 3, durationMinutes = 10, multiplier = 1.1),
                session(id = 7, date = LocalDate.of(2026, 5, 28), arcId = 3, durationMinutes = 10, multiplier = 1.1)
            )
        )

        assertEquals(1L, stats.longestArcByTime?.arcId)
        assertEquals(80 * 60_000L, stats.longestArcByTime?.totalDurationMs)
        assertEquals(3L, stats.mostChainedFlowsInArc?.arcId)
        assertEquals(3, stats.mostChainedFlowsInArc?.flowCount)
        assertEquals(1.8, stats.highestArcMultiplier?.multiplier ?: 0.0, 0.001)
        assertEquals(2, stats.mostArcsInDay?.count)
        assertEquals(LocalDate.of(2026, 5, 20), stats.mostArcsInDay?.startDate)
        assertEquals(2, stats.mostArcsInWeek?.count)
        assertEquals(LocalDate.of(2026, 5, 18), stats.mostArcsInWeek?.startDate)
    }

    @Test
    fun bonusStatsPickBestFlowLongestFlowCountsAndTimePeriods() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 1), durationMinutes = 30, points = 300),
                session(id = 2, date = LocalDate.of(2026, 5, 1), durationMinutes = 90, points = 200),
                session(id = 3, date = LocalDate.of(2026, 5, 2), durationMinutes = 50, points = 500),
                session(id = 4, date = LocalDate.of(2026, 6, 1), durationMinutes = 10, points = 100)
            )
        )

        assertEquals(3L, stats.bestFlowByPoints?.sessionId)
        assertEquals(2L, stats.longestFlow?.sessionId)
        assertEquals(2, stats.mostFlowsInDay?.count)
        assertEquals(LocalDate.of(2026, 5, 1), stats.mostFlowsInDay?.startDate)
        assertEquals(120 * 60_000L, stats.mostTimeInDay?.durationMs)
        assertEquals(170 * 60_000L, stats.mostTimeInWeek?.durationMs)
        assertEquals(170 * 60_000L, stats.mostTimeInMonth?.durationMs)
    }

    @Test
    fun tiesPreferMostRecentThenLowestStableIdOrKey() {
        val flowTieStats = calculate(
            listOf(
                session(id = 5, date = LocalDate.of(2026, 5, 20), durationMinutes = 60, points = 1000),
                session(id = 4, date = LocalDate.of(2026, 5, 21), durationMinutes = 60, points = 1000)
            )
        )
        assertEquals(4L, flowTieStats.bestFlowByPoints?.sessionId)
        assertEquals(4L, flowTieStats.longestFlow?.sessionId)

        val deterministicStats = calculate(
            listOf(
                session(id = 9, date = LocalDate.of(2026, 5, 22), durationMinutes = 60, points = 1000),
                session(id = 2, date = LocalDate.of(2026, 5, 22), durationMinutes = 60, points = 1000),
                session(id = 10, date = LocalDate.of(2026, 5, 23), arcId = 7, durationMinutes = 30),
                session(id = 11, date = LocalDate.of(2026, 5, 23), arcId = 7, durationMinutes = 30),
                session(id = 12, date = LocalDate.of(2026, 5, 23), arcId = 4, durationMinutes = 30),
                session(id = 13, date = LocalDate.of(2026, 5, 23), arcId = 4, durationMinutes = 30)
            )
        )
        assertEquals(2L, deterministicStats.bestFlowByPoints?.sessionId)
        assertEquals(4L, deterministicStats.longestArcByTime?.arcId)
        assertEquals(4L, deterministicStats.mostChainedFlowsInArc?.arcId)
    }

    @Test
    fun eligibleFlowsWithoutValidArcsReturnFlowAndPointRecordsButNoArcRecords() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 29), points = 250),
                session(id = 2, date = LocalDate.of(2026, 5, 29), arcId = 99, points = 350)
            )
        )

        assertTrue(stats.hasEligibleFlows)
        assertNotNull(stats.bestFlowByPoints)
        assertNotNull(stats.bestDayByPoints)
        assertNull(stats.longestArcByTime)
        assertNull(stats.highestArcMultiplier)
        assertNull(stats.mostArcsInDay)
    }


    @Test
    fun sourceDetails_includeChronologicalArcFlowsTotalsPeakAndReachedSession() {
        val stats = calculate(
            listOf(
                session(id = 3, date = LocalDate.of(2026, 5, 22), arcId = 7, durationMinutes = 20, points = 90, multiplier = 1.1, tagName = "Writing"),
                session(id = 1, date = LocalDate.of(2026, 5, 20), arcId = 7, durationMinutes = 30, points = 100, multiplier = 1.4, tagName = "Scyra"),
                session(id = 2, date = LocalDate.of(2026, 5, 21), arcId = 7, durationMinutes = 40, points = 200, multiplier = 1.8, tagName = null)
            )
        )

        val arc = stats.longestArcByTime!!
        assertEquals(listOf(1L, 2L, 3L), arc.flows.map { it.sessionId })
        assertEquals(390, arc.totalPoints)
        assertEquals(1.8, arc.peakMultiplier ?: 0.0, 0.001)
        assertEquals("Scyra", arc.flows.first().tagName)
        assertNull(arc.flows[1].tagName)
        assertEquals(2L, stats.highestArcMultiplier?.reachedInSessionId)
        assertEquals(listOf(1L, 2L, 3L), stats.highestArcMultiplier?.flows?.map { it.sessionId })
    }

    @Test
    fun periodRecords_includeContributingFlowsAndTotals() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 25), durationMinutes = 20, points = 100),
                session(id = 2, date = LocalDate.of(2026, 5, 25), durationMinutes = 40, points = 300),
                session(id = 3, date = LocalDate.of(2026, 5, 26), durationMinutes = 90, points = 200)
            )
        )

        assertEquals(listOf(1L, 2L), stats.bestDayByPoints?.flows?.map { it.sessionId })
        assertEquals(60 * 60_000L, stats.bestDayByPoints?.totalDurationMs)
        assertEquals(2, stats.bestDayByPoints?.flowCount)
        assertEquals(listOf(1L, 2L, 3L), stats.mostTimeInWeek?.flows?.map { it.sessionId })
        assertEquals(600, stats.mostTimeInWeek?.points)
        assertEquals(listOf(1L, 2L), stats.mostFlowsInDay?.flows?.map { it.sessionId })
    }

    @Test
    fun arcVolumeRecords_includeArcSummariesWithFlows() {
        val stats = calculate(
            listOf(
                session(id = 1, date = LocalDate.of(2026, 5, 20), arcId = 1, durationMinutes = 10, points = 10),
                session(id = 2, date = LocalDate.of(2026, 5, 20), arcId = 1, durationMinutes = 20, points = 20),
                session(id = 3, date = LocalDate.of(2026, 5, 20), arcId = 2, durationMinutes = 30, points = 30),
                session(id = 4, date = LocalDate.of(2026, 5, 20), arcId = 2, durationMinutes = 40, points = 40)
            )
        )

        assertEquals(2, stats.mostArcsInDay?.arcs?.size)
        assertEquals(listOf(1L, 2L), stats.mostArcsInDay?.arcs?.first { it.arcId == 1L }?.flows?.map { it.sessionId })
        assertEquals(30, stats.mostArcsInDay?.arcs?.first { it.arcId == 1L }?.totalPoints)
        assertEquals(2, stats.mostArcsInWeek?.arcs?.size)
    }

    private fun calculate(sessions: List<VoyageSourceFlow>) = calculator.calculate(sessions, now, zone)

    private fun session(
        id: Long,
        date: LocalDate = LocalDate.of(2026, 5, 30),
        end: Long? = at(date),
        startMillis: Long = (end ?: at(date)) - 30 * 60_000L,
        durationMinutes: Long = 30,
        points: Int = 100,
        soft: Boolean = false,
        surgePlannedMs: Long? = null,
        arcId: Long? = null,
        multiplier: Double? = null,
        tagName: String? = "Scyra"
    ): VoyageSourceFlow = VoyageSourceFlow(
        id = id,
        title = "Flow $id",
        tagName = tagName,
        startTime = startMillis,
        endTime = end ?: 0L,
        durationMs = durationMinutes * 60_000L,
        scyraPoints = points,
        isSoftMode = soft,
        arcId = arcId,
        arcIndex = arcId?.let { 1 },
        arcMultiplierUsed = multiplier
    )

    private fun at(date: LocalDate): Long = LocalDateTime.of(date, LocalTime.NOON)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}
